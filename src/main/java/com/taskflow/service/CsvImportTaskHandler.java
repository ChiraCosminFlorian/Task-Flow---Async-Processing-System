package com.taskflow.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CsvImportTaskHandler {

    public void handle(String payload) {
        log.info("Processing CSV_IMPORT task with payload: {}", payload);
        try {
            // Simulate CSV parsing and import
            Thread.sleep(4000);
            log.info("CSV import completed successfully");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("CSV import task interrupted", e);
        }
    }
}
