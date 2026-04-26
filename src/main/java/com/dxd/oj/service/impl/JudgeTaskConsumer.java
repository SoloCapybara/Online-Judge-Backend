package com.dxd.oj.service.impl;

import com.dxd.oj.model.dto.judge.JudgeTaskMessage;
import com.dxd.oj.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class JudgeTaskConsumer {

    private final SubmissionService submissionService;

    @Value("${oj.judge.mq.queue:oj.judge.queue}")
    private String queueName;

    @RabbitListener(queues = "${oj.judge.mq.queue:oj.judge.queue}")
    public void onJudgeTask(JudgeTaskMessage message) {
        if (message == null || message.getSubmissionId() == null || message.getSubmissionId() <= 0) {
            log.warn("判题队列收到非法消息，queue={}", queueName);
            return;
        }
        try {
            submissionService.processSubmissionTask(message.getSubmissionId(), message.getToken());
        } catch (Exception e) {
            log.error("异步判题消费异常：submissionId={}, userId={}",
                    message.getSubmissionId(), message.getUserId(), e);
            if (StringUtils.hasText(e.getMessage())) {
                log.warn("异步判题消费异常详情：{}", e.getMessage());
            }
        }
    }
}
