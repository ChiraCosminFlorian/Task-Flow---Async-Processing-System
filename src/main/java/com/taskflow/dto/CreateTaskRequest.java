package com.taskflow.dto;

import com.taskflow.model.TaskType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTaskRequest(
        @NotNull(message = "Task type is required")
        TaskType taskType,

        @NotBlank(message = "Payload must not be blank")
        String payload
) {
}
