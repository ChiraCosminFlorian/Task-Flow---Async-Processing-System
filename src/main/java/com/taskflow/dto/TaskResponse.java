package com.taskflow.dto;

import com.taskflow.model.TaskStatus;
import com.taskflow.model.TaskType;

import java.time.LocalDateTime;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        TaskType taskType,
        TaskStatus status,
        String payload,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        int retryCount,
        String errorMessage
) {
}
