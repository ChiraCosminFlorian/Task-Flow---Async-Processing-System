package com.taskflow.dto;

public record TaskStatusResponse(
        long totalJobs,
        long pendingJobs,
        long completedJobs,
        long failedJobs
) {
}
