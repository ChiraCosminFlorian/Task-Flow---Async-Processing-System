package com.taskflow.messaging;

import com.taskflow.config.RabbitMQConfig;
import com.taskflow.model.JobTask;
import com.taskflow.model.TaskStatus;
import com.taskflow.repository.JobTaskRepository;
import com.taskflow.service.CsvImportTaskHandler;
import com.taskflow.service.EmailTaskHandler;
import com.taskflow.service.ReportTaskHandler;
import com.taskflow.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaskMessageConsumer {

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 5000;

    private final JobTaskRepository jobTaskRepository;
    private final TaskService taskService;
    private final RabbitTemplate rabbitTemplate;
    private final EmailTaskHandler emailTaskHandler;
    private final ReportTaskHandler reportTaskHandler;
    private final CsvImportTaskHandler csvImportTaskHandler;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void processTask(String taskId) {
        UUID id = UUID.fromString(taskId);
        log.info("Received task from queue: {}", id);

        JobTask task = jobTaskRepository.findById(id).orElse(null);
        if (task == null) {
            log.error("Task not found in DB: {}", id);
            return;
        }

        // Mark as PROCESSING
        taskService.updateTaskStatus(id, TaskStatus.PROCESSING, null);

        try {
            // Route to the appropriate handler
            switch (task.getTaskType()) {
                case EMAIL -> emailTaskHandler.handle(task.getPayload());
                case REPORT -> reportTaskHandler.handle(task.getPayload());
                case CSV_IMPORT -> csvImportTaskHandler.handle(task.getPayload());
            }

            // Mark as COMPLETED on success
            taskService.updateTaskStatus(id, TaskStatus.COMPLETED, null);
            log.info("Task {} completed successfully", id);

        } catch (Exception ex) {
            log.error("Task {} failed: {}", id, ex.getMessage());
            handleFailure(task, ex);
        }
    }

    private void handleFailure(JobTask task, Exception ex) {
        int retryCount = task.getRetryCount() + 1;

        if (retryCount < MAX_RETRIES) {
            log.warn("Retrying task {} (attempt {}/{})", task.getId(), retryCount, MAX_RETRIES);

            // Update retry count in DB
            task.setRetryCount(retryCount);
            task.setStatus(TaskStatus.PENDING);
            jobTaskRepository.save(task);

            try {
                // Simulated delay before retry
                Thread.sleep(RETRY_DELAY_MS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }

            // Re-publish to queue
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.ROUTING_KEY,
                    task.getId().toString()
            );
        } else {
            log.error("Task {} failed after {} retries. Marking as FAILED.", task.getId(), MAX_RETRIES);
            taskService.updateTaskStatus(task.getId(), TaskStatus.FAILED, ex.getMessage());
        }
    }
}
