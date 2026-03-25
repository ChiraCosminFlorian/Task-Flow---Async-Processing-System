package com.taskflow.batch;

import com.taskflow.dto.CsvTaskRecord;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.core.io.Resource;

public class CsvTaskItemReader {

    public static FlatFileItemReader<CsvTaskRecord> create(Resource resource) {
        return new FlatFileItemReaderBuilder<CsvTaskRecord>()
                .name("csvTaskReader")
                .resource(resource)
                .delimited()
                .names("taskType", "payload")
                .linesToSkip(1) // skip header row
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(CsvTaskRecord.class);
                }})
                .build();
    }
}
