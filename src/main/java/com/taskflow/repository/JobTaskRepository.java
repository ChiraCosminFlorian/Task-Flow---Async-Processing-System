package com.taskflow.repository;

import com.taskflow.model.JobTask;
import com.taskflow.model.TaskStatus;
import com.taskflow.model.TaskType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobTaskRepository extends JpaRepository<JobTask, UUID> {

    List<JobTask> findByStatus(TaskStatus status);

    List<JobTask> findByTaskTypeAndStatus(TaskType taskType, TaskStatus status);

    long countByStatus(TaskStatus status);
}
