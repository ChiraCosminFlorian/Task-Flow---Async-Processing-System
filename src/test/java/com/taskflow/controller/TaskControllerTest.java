package com.taskflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.dto.CreateTaskRequest;
import com.taskflow.dto.TaskResponse;
import com.taskflow.dto.TaskStatusResponse;
import com.taskflow.model.TaskStatus;
import com.taskflow.model.TaskType;
import com.taskflow.service.TaskNotFoundException;
import com.taskflow.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskService taskService;

    @Test
    void testCreateTask_returns201() throws Exception {
        UUID taskId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        TaskResponse response = new TaskResponse(
                taskId, TaskType.EMAIL, TaskStatus.PENDING,
                "{\"to\":\"user@test.com\"}", now, now, 0, null
        );

        when(taskService.createTask(any(CreateTaskRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateTaskRequest(TaskType.EMAIL, "{\"to\":\"user@test.com\"}")
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(taskId.toString()))
                .andExpect(jsonPath("$.taskType").value("EMAIL"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void testGetTask_returns404WhenNotFound() throws Exception {
        UUID taskId = UUID.randomUUID();

        when(taskService.getTask(taskId)).thenThrow(new TaskNotFoundException(taskId));

        mockMvc.perform(get("/api/tasks/{id}", taskId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task not found with id: " + taskId));
    }

    @Test
    void testGetStats_returnsAggregatedData() throws Exception {
        TaskStatusResponse stats = new TaskStatusResponse(100, 40, 50, 10);

        when(taskService.getStats()).thenReturn(stats);

        mockMvc.perform(get("/api/tasks/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalJobs").value(100))
                .andExpect(jsonPath("$.pendingJobs").value(40))
                .andExpect(jsonPath("$.completedJobs").value(50))
                .andExpect(jsonPath("$.failedJobs").value(10));
    }
}
