package com.dxd.oj.model.dto.judge;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class JudgeTaskMessage implements Serializable {

    private Long submissionId;

    private Long userId;

    private String token;

    private LocalDateTime createdAt;
}
