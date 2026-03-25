package com.taskflow.service;

import com.taskflow.config.RabbitMQConfig;
import com.taskflow.dto.CreateTaskRequest;
import com.taskflow.dto.TaskResponse;
import com.taskflow.dto.TaskStatusResponse;
import com.taskflow.model.*;
import com.taskflow.repository.AuditLogRepository;
import com.taskflow.repository.JobTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final JobTaskRepository jobTaskRepository;
    private final AuditLogRepository auditLogRepository;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public TaskResponse createTask(CreateTaskRequest request) {
        JobTask task = JobTask.builder()
                .taskType(request.taskType())
                .payload(request.payload())
                .status(TaskStatus.PENDING)
                .build();

        task = jobTaskRepository.save(task);
        log.info("Task created with id: {}", task.getId());

        auditLog(task.getId(), "CREATED", "Task created with type: " + task.getTaskType());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY,
                task.getId().toString()
        );
        log.info("Task {} published to queue", task.getId());

        return toResponse(task);
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(UUID id) {
        JobTask task = jobTaskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        return toResponse(task);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasks(TaskStatus status) {
        List<JobTask> tasks = (status != null)
                ? jobTaskRepository.findByStatus(status)
                : jobTaskRepository.findAll();

        return tasks.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TaskStatusResponse getStats() {
        long total = jobTaskRepository.count();
        long pending = jobTaskRepository.countByStatus(TaskStatus.PENDING);
        long completed = jobTaskRepository.countByStatus(TaskStatus.COMPLETED);
        long failed = jobTaskRepository.countByStatus(TaskStatus.FAILED);

        return new TaskStatusResponse(total, pending, completed, failed);
    }

    @Transactional
    public TaskResponse updateTaskStatus(UUID id, TaskStatus newStatus, String errorMessage) {
        JobTask task = jobTaskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));

        TaskStatus oldStatus = task.getStatus();
        task.setStatus(newStatus);

        if (errorMessage != null) {
            task.setErrorMessage(errorMessage);
        }

        task = jobTaskRepository.save(task);
        auditLog(task.getId(), "STATUS_CHANGED", oldStatus + " -> " + newStatus);

        log.info("Task {} status updated: {} -> {}", id, oldStatus, newStatus);
        return toResponse(task);
    }

    @Transactional
    public TaskResponse retryTask(UUID id) {
        JobTask task = jobTaskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));

        if (task.getStatus() != TaskStatus.FAILED) {
            throw new IllegalStateException("Only FAILED tasks can be retried. Current status: " + task.getStatus());
        }

        task.setStatus(TaskStatus.PENDING);
        task.setRetryCount(task.getRetryCount() + 1);
        task.setErrorMessage(null);
        task = jobTaskRepository.save(task);

        auditLog(task.getId(), "RETRY", "Retry #" + task.getRetryCount());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY,
                task.getId().toString()
        );
        log.info("Task {} requeued for retry #{}", id, task.getRetryCount());

        return toResponse(task);
    }

    private void auditLog(UUID taskId, String action, String details) {
        AuditLog entry = AuditLog.builder()
                .taskId(taskId)
                .action(action)
                .details(details)
                .build();
        auditLogRepository.save(entry);
    }

    private TaskResponse toResponse(JobTask task) {
        return new TaskResponse(
                task.getId(),
                task.getTaskType(),
                task.getStatus(),
                task.getPayload(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                task.getRetryCount(),
                task.getErrorMessage()
        );
    }

}
