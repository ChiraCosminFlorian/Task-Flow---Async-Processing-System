package com.taskflow.batch;

import com.taskflow.config.RabbitMQConfig;
import com.taskflow.model.JobTask;
import com.taskflow.repository.JobTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CsvTaskItemWriter implements ItemWriter<JobTask> {

    private final JobTaskRepository jobTaskRepository;
    private final RabbitTemplate rabbitTemplate;

    @Override
    public void write(@NonNull Chunk<? extends JobTask> chunk) {
        for (JobTask task : chunk) {
            JobTask saved = jobTaskRepository.save(task);
            log.info("Saved task from CSV: id={}, type={}", saved.getId(), saved.getTaskType());

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.ROUTING_KEY,
                    saved.getId().toString()
            );
            log.info("Task {} published to queue from batch", saved.getId());
        }
    }
}
