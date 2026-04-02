package com.dxd.oj.service.impl;

import com.dxd.oj.common.ErrorCode;
import com.dxd.oj.exception.BusinessException;
import com.dxd.oj.model.dto.run.RunCodeRequest;
import com.dxd.oj.model.vo.LoginUserVO;
import com.dxd.oj.model.vo.RunCodeResponse;
import com.dxd.oj.service.AuthService;
import com.dxd.oj.service.RunService;
import com.dxd.oj.service.RunStreamCallback;
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
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class RunServiceImpl implements RunService {

    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_COMPILE_ERROR = "COMPILE_ERROR";
    private static final String STATUS_RUNTIME_ERROR = "RUNTIME_ERROR";
    private static final String STATUS_TIMEOUT = "TIME_LIMIT_EXCEEDED";
    private static final String STATUS_UNSUPPORTED = "UNSUPPORTED_LANGUAGE";
    private static final String STATUS_SYSTEM_ERROR = "SYSTEM_ERROR";

    private final AuthService authService;

    @Value("${oj.run.default-time-limit-ms:15000}")
    private int defaultTimeLimitMs;

    @Value("${oj.run.max-time-limit-ms:30000}")
    private int maxTimeLimitMs;

    @Value("${oj.run.max-output-length:65536}")
    private int maxOutputLength;

    @Value("${oj.run.python-cmd:python3.14}")
    private String pythonCmd;

    @Override
    public RunCodeResponse runCode(RunCodeRequest request, String token) {
        return runCodeInternal(request, token, null);
    }

    @Override
    public RunCodeResponse runCodeStream(RunCodeRequest request, String token, RunStreamCallback callback) {
        return runCodeInternal(request, token, callback);
    }

    private RunCodeResponse runCodeInternal(RunCodeRequest request, String token, RunStreamCallback callback) {
        LoginUserVO loginUser = authService.getLoginUser(token);
        validateRequest(request);

        long totalStartNanos = System.nanoTime();
        String language = request.getLanguage().trim().toUpperCase(Locale.ROOT);
        int timeLimit = resolveTimeLimit(request.getTimeLimitMs());
        long compileTimeMs = 0L;
        Path workDir = createWorkDir();
        try {
            emitStatus(callback, "准备运行环境...");
            writeSourceFile(workDir, language, request.getSourceCode());

            if (requiresCompile(language)) {
                emitStatus(callback, "编译中...");
                RunCodeResponse compileResult = runProcess(buildCompileCommand(language), workDir, null, timeLimit, callback);
                compileTimeMs = safeMs(compileResult.getTimeUsedMs());
                if (!STATUS_SUCCESS.equals(compileResult.getStatus())) {
                    compileResult.setStatus(STATUS_COMPILE_ERROR);
                    compileResult.setCompileTimeMs(compileTimeMs);
                    compileResult.setExecuteTimeMs(0L);
                    compileResult.setTotalTimeMs(elapsedMs(totalStartNanos));
                    compileResult.setTimeUsedMs(compileTimeMs);
                    emitStatus(callback, "编译失败");
                    return compileResult;
                }
            }

            emitStatus(callback, "运行中...");
            RunCodeResponse runResult = runProcess(buildRunCommand(language), workDir, request.getInputData(), timeLimit, callback);
            long executeTimeMs = safeMs(runResult.getTimeUsedMs());
            runResult.setCompileTimeMs(compileTimeMs);
            runResult.setExecuteTimeMs(executeTimeMs);
            runResult.setTotalTimeMs(elapsedMs(totalStartNanos));
            runResult.setTimeUsedMs(executeTimeMs);
            log.info("自测运行完成：userId={}, language={}, status={}, compileMs={}, executeMs={}, totalMs={}",
                    loginUser.getId(), language, runResult.getStatus(),
                    runResult.getCompileTimeMs(), runResult.getExecuteTimeMs(), runResult.getTotalTimeMs());
            emitStatus(callback, "运行完成");
            return runResult;
        } catch (BusinessException e) {
            emitStatus(callback, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("自测运行异常：language={}", language, e);
            RunCodeResponse response = new RunCodeResponse();
            response.setStatus(STATUS_SYSTEM_ERROR);
            response.setStderr(e.getMessage());
            response.setExitCode(-1);
            response.setTimeUsedMs(0L);
            response.setCompileTimeMs(0L);
            response.setExecuteTimeMs(0L);
            response.setTotalTimeMs(elapsedMs(totalStartNanos));
            emitStatus(callback, "系统异常");
            emitStderr(callback, StringUtils.hasText(e.getMessage()) ? e.getMessage() : "系统异常");
            return response;
        } finally {
            deleteDirectoryQuietly(workDir);
        }
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

    private int resolveTimeLimit(Integer requestTimeLimit) {
        if (requestTimeLimit == null || requestTimeLimit <= 0) {
            return defaultTimeLimitMs;
        }
        return Math.min(requestTimeLimit, maxTimeLimitMs);
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

    private String[] buildRunCommand(String language) {
        return switch (language) {
            case "JAVA17" -> new String[]{"java", "-cp", ".", "Main"};
            case "PYTHON3142" -> new String[]{pythonCmd, "main.py"};
            case "C17", "CPP17" -> new String[]{"./main"};
            default -> new String[0];
        };
    }

    private RunCodeResponse runProcess(String[] command, Path workDir, String inputData, int timeoutMs, RunStreamCallback callback) {
        if (command == null || command.length == 0) {
            RunCodeResponse response = new RunCodeResponse();
            response.setStatus(STATUS_UNSUPPORTED);
            response.setExitCode(-1);
            response.setStdout("");
            response.setStderr("不支持的语言命令");
            response.setTimeUsedMs(0L);
            emitStderr(callback, "不支持的语言命令\n");
            return response;
        }

        long start = System.nanoTime();
        Process process;
        try {
            process = new ProcessBuilder(command)
                    .directory(workDir.toFile())
                    .redirectErrorStream(false)
                    .start();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "启动运行进程失败，请检查本机编译器环境");
        }

        OutputCollector stdoutCollector = new OutputCollector(maxOutputLength);
        OutputCollector stderrCollector = new OutputCollector(maxOutputLength);

        CompletableFuture<Void> stdoutFuture = CompletableFuture.runAsync(
                () -> readStream(process.getInputStream(), stdoutCollector, chunk -> emitStdout(callback, chunk))
        );
        CompletableFuture<Void> stderrFuture = CompletableFuture.runAsync(
                () -> readStream(process.getErrorStream(), stderrCollector, chunk -> emitStderr(callback, chunk))
        );

        try {
            if (StringUtils.hasText(inputData)) {
                try (OutputStream os = process.getOutputStream()) {
                    os.write(inputData.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }
            } else {
                process.getOutputStream().close();
            }
        } catch (IOException e) {
            // ignore input write error; exit code/stderr will reflect it
        }

        boolean finished;
        try {
            finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            finished = false;
        }

        RunCodeResponse response = new RunCodeResponse();
        response.setTimeUsedMs((System.nanoTime() - start) / 1_000_000);
        if (!finished) {
            process.destroyForcibly();
            awaitFuture(stdoutFuture);
            awaitFuture(stderrFuture);
            response.setStatus(STATUS_TIMEOUT);
            response.setExitCode(-1);
            response.setStdout(stdoutCollector.getText());
            String timeoutText = "运行超时";
            if (stderrCollector.getText().isEmpty()) {
                response.setStderr(timeoutText);
            } else {
                response.setStderr(stderrCollector.getText());
            }
            emitStatus(callback, timeoutText);
            return response;
        }

        awaitFuture(stdoutFuture);
        awaitFuture(stderrFuture);
        int exitCode = process.exitValue();
        String stdout = stdoutCollector.getText();
        String stderr = stderrCollector.getText();
        response.setExitCode(exitCode);
        response.setStdout(stdout);
        response.setStderr(stderr);
        response.setStatus(exitCode == 0 ? STATUS_SUCCESS : STATUS_RUNTIME_ERROR);
        return response;
    }

    private long safeMs(Long value) {
        return value == null ? 0L : Math.max(0L, value);
    }

    private long elapsedMs(long startNanos) {
        return Math.max(0L, (System.nanoTime() - startNanos) / 1_000_000);
    }

    private void awaitFuture(CompletableFuture<?> future) {
        try {
            future.get(3, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
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

    private void emitStatus(RunStreamCallback callback, String statusText) {
        if (callback != null && StringUtils.hasText(statusText)) {
            callback.onStatus(statusText);
        }
    }

    private void emitStdout(RunStreamCallback callback, String chunk) {
        if (callback != null && chunk != null && !chunk.isEmpty()) {
            callback.onStdout(chunk);
        }
    }

    private void emitStderr(RunStreamCallback callback, String chunk) {
        if (callback != null && chunk != null && !chunk.isEmpty()) {
            callback.onStderr(chunk);
        }
    }

    private static class OutputCollector {
        private final int maxLen;
        private final StringBuilder builder = new StringBuilder();
        private boolean truncated;

        OutputCollector(int maxLen) {
            this.maxLen = Math.max(1024, maxLen);
        }

        String accept(String chunk) {
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

        String getText() {
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
}
