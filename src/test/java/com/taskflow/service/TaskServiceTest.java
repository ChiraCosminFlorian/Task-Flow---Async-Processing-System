package com.taskflow.service;

import com.taskflow.config.RabbitMQConfig;
import com.taskflow.dto.CreateTaskRequest;
import com.taskflow.dto.TaskResponse;
import com.taskflow.model.JobTask;
import com.taskflow.model.TaskStatus;
import com.taskflow.model.TaskType;
import com.taskflow.repository.AuditLogRepository;
import com.taskflow.repository.JobTaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private JobTaskRepository jobTaskRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private TaskService taskService;

    @Test
    void testCreateTask_success() {
        CreateTaskRequest request = new CreateTaskRequest(TaskType.EMAIL, "{\"to\":\"user@test.com\"}");

        JobTask saved = JobTask.builder()
                .id(UUID.randomUUID())
                .taskType(TaskType.EMAIL)
                .payload(request.payload())
                .status(TaskStatus.PENDING)
                .build();

        when(jobTaskRepository.save(any(JobTask.class))).thenReturn(saved);

        TaskResponse response = taskService.createTask(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(saved.getId());
        assertThat(response.taskType()).isEqualTo(TaskType.EMAIL);
        assertThat(response.status()).isEqualTo(TaskStatus.PENDING);

        verify(jobTaskRepository).save(any(JobTask.class));
        verify(auditLogRepository).save(any());
    }

    @Test
    void testCreateTask_publishesMessageToRabbitMQ() {
        CreateTaskRequest request = new CreateTaskRequest(TaskType.REPORT, "{\"format\":\"PDF\"}");

        UUID taskId = UUID.randomUUID();
        JobTask saved = JobTask.builder()
                .id(taskId)
                .taskType(TaskType.REPORT)
                .payload(request.payload())
                .status(TaskStatus.PENDING)
                .build();

        when(jobTaskRepository.save(any(JobTask.class))).thenReturn(saved);

        taskService.createTask(request);

        ArgumentCaptor<String> exchangeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> messageCaptor = ArgumentCaptor.forClass(Object.class);

        verify(rabbitTemplate).convertAndSend(
                exchangeCaptor.capture(),
                routingKeyCaptor.capture(),
                messageCaptor.capture()
        );

        assertThat(exchangeCaptor.getValue()).isEqualTo(RabbitMQConfig.EXCHANGE_NAME);
        assertThat(routingKeyCaptor.getValue()).isEqualTo(RabbitMQConfig.ROUTING_KEY);
        assertThat(messageCaptor.getValue()).isEqualTo(taskId.toString());
    }

    @Test
    void testRetryTask_failsIfNotInFailedStatus() {
        UUID taskId = UUID.randomUUID();
        JobTask task = JobTask.builder()
                .id(taskId)
                .taskType(TaskType.CSV_IMPORT)
                .status(TaskStatus.PROCESSING)
                .build();

        when(jobTaskRepository.findById(taskId)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.retryTask(taskId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only FAILED tasks can be retried");

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }
}
