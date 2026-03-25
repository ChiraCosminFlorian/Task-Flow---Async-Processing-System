package com.taskflow.batch;

import com.taskflow.dto.CsvTaskRecord;
import com.taskflow.model.JobTask;
import com.taskflow.model.TaskStatus;
import com.taskflow.model.TaskType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CsvTaskItemProcessor implements ItemProcessor<CsvTaskRecord, JobTask> {

    @Override
    public JobTask process(CsvTaskRecord item) {
        TaskType taskType;
        try {
            taskType = TaskType.valueOf(item.taskType().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid task type '{}', skipping row", item.taskType());
            return null; // skip invalid rows
        }

        if (item.payload() == null || item.payload().isBlank()) {
            log.warn("Empty payload for task type '{}', skipping row", item.taskType());
            return null;
        }

        return JobTask.builder()
                .taskType(taskType)
                .payload(item.payload().trim())
                .status(TaskStatus.PENDING)
                .retryCount(0)
                .build();
    }
}
