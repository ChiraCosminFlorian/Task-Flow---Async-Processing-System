package com.taskflow.batch;

import com.taskflow.dto.CsvTaskRecord;
import com.taskflow.model.JobTask;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class CsvImportJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final CsvTaskItemProcessor processor;
    private final CsvTaskItemWriter writer;

    @Bean
    public Job csvImportJob(Step csvImportStep) {
        return new JobBuilder("csvImportJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(csvImportStep)
                .build();
    }

    @Bean
    public Step csvImportStep(FlatFileItemReader<CsvTaskRecord> csvReader) {
        return new StepBuilder("csvImportStep", jobRepository)
                .<CsvTaskRecord, JobTask>chunk(10, transactionManager)
                .reader(csvReader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<CsvTaskRecord> csvReader(
            @Value("#{jobParameters['inputFile']}") String inputFile) {
        return CsvTaskItemReader.create(new FileSystemResource(inputFile));
    }
}
