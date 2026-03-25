package com.taskflow.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailTaskHandler {

    public void handle(String payload) {
        log.info("Processing EMAIL task with payload: {}", payload);
        try {
            // Simulate email sending
            Thread.sleep(2000);
            log.info("Email sent successfully");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Email task interrupted", e);
        }
    }
}
