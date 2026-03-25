package com.taskflow.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ReportTaskHandler {

    public void handle(String payload) {
        log.info("Processing REPORT task with payload: {}", payload);
        try {
            // Simulate report generation
            Thread.sleep(3000);
            log.info("Report generated successfully");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Report task interrupted", e);
        }
    }
}
