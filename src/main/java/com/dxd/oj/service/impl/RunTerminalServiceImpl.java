package com.dxd.oj.service.impl;

import com.dxd.oj.common.ErrorCode;
import com.dxd.oj.exception.BusinessException;
import com.dxd.oj.model.dto.run.RunCodeRequest;
import com.dxd.oj.model.vo.RunCodeResponse;
import com.dxd.oj.service.AuthService;
import com.dxd.oj.service.RunTerminalEventSink;
import com.dxd.oj.service.RunTerminalService;
import com.dxd.oj.service.support.JudgeConcurrencyLimiter;
import com.dxd.oj.service.support.SystemConfigAccessor;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class RunTerminalServiceImpl implements RunTerminalService {

    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_COMPILE_ERROR = "COMPILE_ERROR";
    private static final String STATUS_RUNTIME_ERROR = "RUNTIME_ERROR";
    private static final String STATUS_TIMEOUT = "TIME_LIMIT_EXCEEDED";
    private static final String STATUS_UNSUPPORTED = "UNSUPPORTED_LANGUAGE";
    private static final String STATUS_SYSTEM_ERROR = "SYSTEM_ERROR";
    private static final String CONFIG_JUDGE_DEFAULT_TIME_LIMIT_MS = "judge.default_time_limit_ms";
    private static final String CONFIG_JUDGE_DEFAULT_MEMORY_LIMIT_MB = "judge.default_memory_limit_mb";
    private static final int MIN_TIME_LIMIT_MS = 200;
    private static final int MIN_MEMORY_LIMIT_MB = 16;
    private static final int MAX_MEMORY_LIMIT_MB = 2048;

    private final AuthService authService;
    private final SystemConfigAccessor systemConfigAccessor;
    private final JudgeConcurrencyLimiter judgeConcurrencyLimiter;
    private final ConcurrentHashMap<String, ActiveRun> activeRuns = new ConcurrentHashMap<>();
    private final ExecutorService ioExecutor = Executors.newCachedThreadPool();

    @Value("${oj.run.default-time-limit-ms:15000}")
    private int defaultTimeLimitMs;

    @Value("${oj.run.max-time-limit-ms:30000}")
    private int maxTimeLimitMs;

    @Value("${oj.run.default-memory-limit-mb:256}")
    private int defaultMemoryLimitMb;

    @Value("${oj.run.max-output-length:65536}")
    private int maxOutputLength;

    @Value("${oj.run.python-cmd:python3.14}")
    private String pythonCmd;

    @Override
    public void start(String sessionId, String token, RunCodeRequest request, RunTerminalEventSink eventSink) {
        stop(sessionId);
        try {
            authService.getLoginUser(token);
            validateRequest(request);
            eventSink.onStatus("准备运行环境...");
        } catch (Exception e) {
            eventSink.onError(resolveErrorMessage(e));
            return;
        }

        String language = request.getLanguage().trim().toUpperCase(Locale.ROOT);
        Integer requestedTimeLimit = request.getTimeLimitMs();
        int configuredDefaultTimeLimitMs = systemConfigAccessor.getInt(
                CONFIG_JUDGE_DEFAULT_TIME_LIMIT_MS,
                defaultTimeLimitMs,
                MIN_TIME_LIMIT_MS,
                maxTimeLimitMs
        );
        int configuredDefaultMemoryLimitMb = systemConfigAccessor.getInt(
                CONFIG_JUDGE_DEFAULT_MEMORY_LIMIT_MB,
                defaultMemoryLimitMb,
                MIN_MEMORY_LIMIT_MB,
                MAX_MEMORY_LIMIT_MB
        );
        int timeLimit = resolveTimeLimit(requestedTimeLimit, configuredDefaultTimeLimitMs);
        int memoryLimitMb = resolveMemoryLimit(request.getMemoryLimitMb(), configuredDefaultMemoryLimitMb);
        if (requestedTimeLimit != null && requestedTimeLimit > maxTimeLimitMs) {
            eventSink.onStatus("超时已按系统上限调整为 " + timeLimit + "ms");
        }
        long totalStartNanos = System.nanoTime();
        Path workDir = createWorkDir();

        ioExecutor.execute(() -> {
            JudgeConcurrencyLimiter.Permit permit = null;
            try {
                permit = judgeConcurrencyLimiter.acquirePermit();
                writeSourceFile(workDir, language, request.getSourceCode());
                long compileTimeMs = 0L;

                if (requiresCompile(language)) {
                    eventSink.onStatus("编译中...");
                    ProcessResult compile = runBlockingProcess(buildCompileCommand(language), workDir, null,
                            timeLimit, eventSink::onStdout, eventSink::onStderr);
                    compileTimeMs = compile.timeUsedMs;
                    if (compile.timedOut) {
                        RunCodeResponse timeoutResp = new RunCodeResponse();
                        timeoutResp.setStatus(STATUS_COMPILE_ERROR);
                        timeoutResp.setExitCode(-1);
                        timeoutResp.setStdout(compile.stdout);
                        timeoutResp.setStderr(StringUtils.hasText(compile.stderr) ? compile.stderr : "编译超时");
                        timeoutResp.setCompileTimeMs(compileTimeMs);
                        timeoutResp.setExecuteTimeMs(0L);
                        timeoutResp.setTotalTimeMs(elapsedMs(totalStartNanos));
                        timeoutResp.setTimeUsedMs(compileTimeMs);
                        eventSink.onResult(timeoutResp);
                        deleteDirectoryQuietly(workDir);
                        return;
                    }
                    if (compile.exitCode != 0) {
                        RunCodeResponse compileError = new RunCodeResponse();
                        compileError.setStatus(STATUS_COMPILE_ERROR);
                        compileError.setExitCode(compile.exitCode);
                        compileError.setStdout(compile.stdout);
                        compileError.setStderr(compile.stderr);
                        compileError.setCompileTimeMs(compileTimeMs);
                        compileError.setExecuteTimeMs(0L);
                        compileError.setTotalTimeMs(elapsedMs(totalStartNanos));
                        compileError.setTimeUsedMs(compileTimeMs);
                        eventSink.onResult(compileError);
                        deleteDirectoryQuietly(workDir);
                        return;
                    }
                }

                eventSink.onStatus("运行中...");
                Process process = new ProcessBuilder(buildRunCommand(language, memoryLimitMb))
                        .directory(workDir.toFile())
                        .redirectErrorStream(false)
                        .start();

                ActiveRun run = new ActiveRun(
                        sessionId, process, workDir, eventSink, maxOutputLength, compileTimeMs, totalStartNanos, permit
                );
                permit = null;
                ActiveRun previous = activeRuns.put(sessionId, run);
                if (previous != null) {
                    previous.forceStop();
                    previous.releasePermit();
                }
                run.startReaders(ioExecutor);
                ioExecutor.execute(() -> waitForCompletion(run, timeLimit));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                deleteDirectoryQuietly(workDir);
                eventSink.onError("获取执行槽位失败");
            } catch (Exception e) {
                deleteDirectoryQuietly(workDir);
                eventSink.onError(resolveErrorMessage(e));
            } finally {
                if (permit != null) {
                    permit.close();
                }
            }
        });
    }

    @Override
    public void sendInput(String sessionId, String data) {
        ActiveRun run = activeRuns.get(sessionId);
        if (run == null || !StringUtils.hasText(data)) {
            return;
        }
        run.writeInput(data);
    }

    @Override
    public void closeInput(String sessionId) {
        ActiveRun run = activeRuns.get(sessionId);
        if (run == null) {
            return;
        }
        run.closeInput();
    }

    @Override
    public void stop(String sessionId) {
        ActiveRun run = activeRuns.remove(sessionId);
        if (run != null) {
            run.forceStop();
            run.releasePermit();
            deleteDirectoryQuietly(run.workDir);
        }
    }

    @PreDestroy
    public void destroy() {
        activeRuns.forEach((id, run) -> {
            run.forceStop();
            run.releasePermit();
            deleteDirectoryQuietly(run.workDir);
        });
        activeRuns.clear();
        ioExecutor.shutdownNow();
    }

    private void waitForCompletion(ActiveRun run, int timeoutMs) {
        boolean finished = false;

        while (true) {
            try {
                if (run.process.waitFor(200, TimeUnit.MILLISECONDS)) {
                    finished = true;
                    break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            long idleMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - run.getTimeoutBaseNanos());
            if (idleMs >= timeoutMs) {
                break;
            }
        }

        RunCodeResponse response = new RunCodeResponse();
        long nowNanos = System.nanoTime();
        long executeWallMs = elapsedMs(run.executeStartNanos, nowNanos);
        long executeMs = run.resolveExecuteTimeMs(nowNanos, executeWallMs);
        long totalMs = elapsedMs(run.totalStartNanos, nowNanos);
        response.setCompileTimeMs(run.compileTimeMs);
        response.setExecuteTimeMs(executeMs);
        response.setTotalTimeMs(totalMs);
        response.setTimeUsedMs(executeMs);
        if (!finished) {
            run.process.destroyForcibly();
            run.awaitReaderTasks();
            response.setStatus(STATUS_TIMEOUT);
            response.setExitCode(-1);
            response.setStdout(run.stdoutCollector.getText());
            response.setStderr(StringUtils.hasText(run.stderrCollector.getText()) ? run.stderrCollector.getText() : "运行超时");
            run.eventSink.onResult(response);
            cleanupRun(run.sessionId, run);
            return;
        }

        run.awaitReaderTasks();
        int exitCode = run.process.exitValue();
        response.setExitCode(exitCode);
        response.setStdout(run.stdoutCollector.getText());
        response.setStderr(run.stderrCollector.getText());
        response.setStatus(exitCode == 0 ? STATUS_SUCCESS : STATUS_RUNTIME_ERROR);
        run.eventSink.onResult(response);
        cleanupRun(run.sessionId, run);
    }

    private void cleanupRun(String sessionId, ActiveRun run) {
        activeRuns.remove(sessionId, run);
        run.closeStdin();
        run.releasePermit();
        deleteDirectoryQuietly(run.workDir);
    }

    private void validateRequest(RunCodeRequest request) {
        if (request == null || !StringUtils.hasText(request.getLanguage()) || !StringUtils.hasText(request.getSourceCode())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        String language = request.getLanguage().trim().toUpperCase(Locale.ROOT);
        if (!"JAVA17".equals(language) && !"PYTHON3142".equals(language) && !"C17".equals(language) && !"CPP17".equals(language)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "暂不支持该语言");
        }
    }

    private int resolveTimeLimit(Integer requestTimeLimit, int defaultLimitMs) {
        if (requestTimeLimit == null || requestTimeLimit <= 0) {
            return defaultLimitMs;
        }
        return Math.min(requestTimeLimit, maxTimeLimitMs);
    }

    private int resolveMemoryLimit(Integer requestMemoryLimitMb, int defaultLimitMb) {
        if (requestMemoryLimitMb == null || requestMemoryLimitMb <= 0) {
            return defaultLimitMb;
        }
        return Math.min(Math.max(requestMemoryLimitMb, MIN_MEMORY_LIMIT_MB), MAX_MEMORY_LIMIT_MB);
    }

    private Path createWorkDir() {
        try {
            return Files.createDirectories(Path.of(System.getProperty("java.io.tmpdir"), "oj-run", UUID.randomUUID().toString()));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建运行目录失败");
        }
    }

    private void writeSourceFile(Path workDir, String language, String sourceCode) {
        String fileName;
        switch (language) {
            case "JAVA17" -> fileName = "Main.java";
            case "PYTHON3142" -> fileName = "main.py";
            case "C17" -> fileName = "main.c";
            case "CPP17" -> fileName = "main.cpp";
            default -> throw new BusinessException(ErrorCode.PARAMS_ERROR, "暂不支持该语言");
        }
        try {
            Files.writeString(workDir.resolve(fileName), sourceCode, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "写入源代码失败");
        }
    }

    private boolean requiresCompile(String language) {
        return !"PYTHON3142".equals(language);
    }

    private String[] buildCompileCommand(String language) {
        return switch (language) {
            case "JAVA17" -> new String[]{"javac", "-encoding", "UTF-8", "Main.java"};
            case "C17" -> new String[]{"gcc", "-std=c17", "-O2", "-o", "main", "main.c"};
            case "CPP17" -> new String[]{"g++", "-std=c++17", "-O2", "-o", "main", "main.cpp"};
            default -> new String[0];
        };
    }

    private String[] buildRunCommand(String language, int memoryLimitMb) {
        return switch (language) {
            case "JAVA17" -> new String[]{
                    "java",
                    "-Xms16m",
                    "-Xmx" + memoryLimitMb + "m",
                    "-cp",
                    ".",
                    "Main"
            };
            case "PYTHON3142" -> new String[]{pythonCmd, "main.py"};
            case "C17", "CPP17" -> new String[]{"./main"};
            default -> new String[0];
        };
    }

    private ProcessResult runBlockingProcess(
            String[] command,
            Path workDir,
            String inputData,
            int timeoutMs,
            Consumer<String> stdoutConsumer,
            Consumer<String> stderrConsumer
    ) throws IOException {
        Process process = new ProcessBuilder(command)
                .directory(workDir.toFile())
                .redirectErrorStream(false)
                .start();

        long start = System.nanoTime();
        OutputCollector stdoutCollector = new OutputCollector(maxOutputLength);
        OutputCollector stderrCollector = new OutputCollector(maxOutputLength);
        CompletableFuture<Void> stdoutFuture = CompletableFuture.runAsync(
                () -> readStream(process.getInputStream(), stdoutCollector, stdoutConsumer), ioExecutor
        );
        CompletableFuture<Void> stderrFuture = CompletableFuture.runAsync(
                () -> readStream(process.getErrorStream(), stderrCollector, stderrConsumer), ioExecutor
        );

        if (StringUtils.hasText(inputData)) {
            try (OutputStream os = process.getOutputStream()) {
                os.write(inputData.getBytes(StandardCharsets.UTF_8));
                os.flush();
            } catch (IOException ignored) {
            }
        } else {
            process.getOutputStream().close();
        }

        boolean finished;
        try {
            finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            finished = false;
        }

        if (!finished) {
            process.destroyForcibly();
        }
        awaitFuture(stdoutFuture);
        awaitFuture(stderrFuture);

        ProcessResult result = new ProcessResult();
        result.timedOut = !finished;
        result.exitCode = finished ? process.exitValue() : -1;
        result.stdout = stdoutCollector.getText();
        result.stderr = stderrCollector.getText();
        result.timeUsedMs = (System.nanoTime() - start) / 1_000_000;
        return result;
    }

    private void readStream(InputStream inputStream, OutputCollector collector, Consumer<String> chunkConsumer) {
        try {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                if (len <= 0) {
                    continue;
                }
                String chunk = new String(buffer, 0, len, StandardCharsets.UTF_8);
                String accepted = collector.accept(chunk);
                if (!accepted.isEmpty()) {
                    chunkConsumer.accept(accepted);
                }
            }
        } catch (IOException ignored) {
        }
    }

    private void awaitFuture(CompletableFuture<?> future) {
        try {
            future.get(3, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
    }

    private String resolveErrorMessage(Throwable e) {
        if (e instanceof BusinessException be) {
            return be.getMessage();
        }
        return StringUtils.hasText(e.getMessage()) ? e.getMessage() : "运行失败";
    }

    private void deleteDirectoryQuietly(Path path) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try (var stream = Files.walk(path)) {
            stream.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }

    private long elapsedMs(long startNanos) {
        return elapsedMs(startNanos, System.nanoTime());
    }

    private long elapsedMs(long startNanos, long endNanos) {
        return Math.max(0L, (endNanos - startNanos) / 1_000_000);
    }

    private static class ProcessResult {
        private int exitCode;
        private boolean timedOut;
        private String stdout;
        private String stderr;
        private long timeUsedMs;
    }

    private static class ActiveRun {
        private final String sessionId;
        private final Process process;
        private final Path workDir;
        private final RunTerminalEventSink eventSink;
        private final OutputCollector stdoutCollector;
        private final OutputCollector stderrCollector;
        private final OutputStream stdin;
        private final long compileTimeMs;
        private final long totalStartNanos;
        private final JudgeConcurrencyLimiter.Permit permit;
        private final long executeStartNanos = System.nanoTime();
        private final AtomicBoolean hasStdinInput = new AtomicBoolean(false);
        private final AtomicBoolean stopped = new AtomicBoolean(false);
        private final AtomicBoolean inputClosed = new AtomicBoolean(false);
        private final AtomicBoolean permitReleased = new AtomicBoolean(false);
        private volatile long timeoutBaseNanos = executeStartNanos;
        private volatile long lastInputNanos = executeStartNanos;
        private CompletableFuture<Void> stdoutTask;
        private CompletableFuture<Void> stderrTask;

        private ActiveRun(
                String sessionId,
                Process process,
                Path workDir,
                RunTerminalEventSink eventSink,
                int maxOutputLength,
                long compileTimeMs,
                long totalStartNanos,
                JudgeConcurrencyLimiter.Permit permit
        ) {
            this.sessionId = sessionId;
            this.process = process;
            this.workDir = workDir;
            this.eventSink = eventSink;
            this.stdoutCollector = new OutputCollector(maxOutputLength);
            this.stderrCollector = new OutputCollector(maxOutputLength);
            this.stdin = process.getOutputStream();
            this.compileTimeMs = Math.max(0L, compileTimeMs);
            this.totalStartNanos = totalStartNanos;
            this.permit = permit;
        }

        private void startReaders(ExecutorService executor) {
            stdoutTask = CompletableFuture.runAsync(
                    () -> read(process.getInputStream(), stdoutCollector, eventSink::onStdout),
                    executor
            );
            stderrTask = CompletableFuture.runAsync(
                    () -> read(process.getErrorStream(), stderrCollector, eventSink::onStderr),
                    executor
            );
        }

        private void writeInput(String data) {
            if (stopped.get() || inputClosed.get()) {
                return;
            }
            try {
                synchronized (stdin) {
                    stdin.write(data.getBytes(StandardCharsets.UTF_8));
                    stdin.flush();
                    hasStdinInput.set(true);
                    lastInputNanos = System.nanoTime();
                    timeoutBaseNanos = System.nanoTime();
                }
            } catch (IOException e) {
                eventSink.onError("写入输入失败");
            }
        }

        private void closeInput() {
            if (inputClosed.compareAndSet(false, true)) {
                try {
                    stdin.close();
                } catch (IOException ignored) {
                }
            }
        }

        private boolean isInputOpen() {
            return !inputClosed.get();
        }

        private long getTimeoutBaseNanos() {
            return timeoutBaseNanos;
        }

        private Long resolveExecuteCpuMs() {
            try {
                return process.toHandle()
                        .info()
                        .totalCpuDuration()
                        .map(Duration::toMillis)
                        .map(ms -> Math.max(0L, ms))
                        .orElse(null);
            } catch (Exception ignored) {
                return null;
            }
        }

        private long resolveExecuteTimeMs(long nowNanos, long wallMs) {
            Long cpuMs = resolveExecuteCpuMs();
            if (cpuMs != null && cpuMs > 0L) {
                return cpuMs;
            }
            // 某些环境无法提供可靠 CPU 时间，交互模式下回退为“最后一次输入后到进程结束”
            if (hasStdinInput.get()) {
                return Math.max(0L, (nowNanos - lastInputNanos) / 1_000_000);
            }
            return Math.max(0L, wallMs);
        }

        private void awaitReaderTasks() {
            await(stdoutTask);
            await(stderrTask);
        }

        private void closeStdin() {
            closeInput();
            stopped.set(true);
        }

        private void markStopped() {
            stopped.set(true);
            closeInput();
        }

        private void forceStop() {
            markStopped();
            process.destroyForcibly();
            awaitReaderTasks();
        }

        private void releasePermit() {
            if (permit != null && permitReleased.compareAndSet(false, true)) {
                permit.close();
            }
        }

        private static void await(CompletableFuture<?> task) {
            if (task == null) {
                return;
            }
            try {
                task.get(3, TimeUnit.SECONDS);
            } catch (Exception ignored) {
            }
        }

        private static void read(InputStream stream, OutputCollector collector, Consumer<String> consumer) {
            try {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = stream.read(buffer)) != -1) {
                    if (len <= 0) {
                        continue;
                    }
                    String chunk = new String(buffer, 0, len, StandardCharsets.UTF_8);
                    String accepted = collector.accept(chunk);
                    if (!accepted.isEmpty()) {
                        consumer.accept(accepted);
                    }
                }
            } catch (IOException ignored) {
            }
        }
    }

    private static class OutputCollector {
        private final int maxLen;
        private final StringBuilder builder = new StringBuilder();
        private boolean truncated;

        OutputCollector(int maxLen) {
            this.maxLen = Math.max(1024, maxLen);
        }

        synchronized String accept(String chunk) {
            if (chunk == null || chunk.isEmpty() || truncated) {
                return "";
            }
            int remain = maxLen - builder.length();
            if (remain <= 0) {
                appendTruncatedFlag();
                truncated = true;
                return "";
            }
            if (chunk.length() <= remain) {
                builder.append(chunk);
                return chunk;
            }

            String accepted = chunk.substring(0, remain);
            builder.append(accepted);
            appendTruncatedFlag();
            truncated = true;
            return accepted + "\n...(output truncated)";
        }

        synchronized String getText() {
            return builder.toString();
        }

        private void appendTruncatedFlag() {
            String suffix = "\n...(output truncated)";
            if (builder.length() >= suffix.length()
                    && suffix.contentEquals(builder.subSequence(builder.length() - suffix.length(), builder.length()))) {
                return;
            }
            builder.append(suffix);
        }
    }
}
