package com.taskflow.controller;

import com.taskflow.batch.CsvTaskItemReader;
import com.taskflow.dto.CsvTaskRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
@Slf4j
public class BatchController {

    private final JobLauncher jobLauncher;
    private final Job csvImportJob;
    private final JobExplorer jobExplorer;

    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importCsv(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }

        try {
            // Save uploaded file to a temp location
            Path tempFile = Files.createTempFile("csv-import-", ".csv");
            file.transferTo(tempFile.toFile());

            // Create a reader for the uploaded file
            FlatFileItemReader<CsvTaskRecord> reader = CsvTaskItemReader.create(
                    new FileSystemResource(tempFile.toFile())
            );

            JobParameters params = new JobParametersBuilder()
                    .addString("inputFile", tempFile.toAbsolutePath().toString())
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            JobExecution execution = jobLauncher.run(csvImportJob, params);

            Map<String, Object> response = new HashMap<>();
            response.put("jobId", execution.getJobId());
            response.put("status", execution.getStatus().toString());
            response.put("message", "Batch import started");

            return ResponseEntity.accepted().body(response);

        } catch (Exception ex) {
            log.error("Failed to start batch import", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to start batch import: " + ex.getMessage()));
        }
    }

    @GetMapping("/status/{jobId}")
    public ResponseEntity<Map<String, Object>> getJobStatus(@PathVariable Long jobId) {
        JobExecution execution = jobExplorer.getJobExecution(jobId);

        if (execution == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("jobId", execution.getJobId());
        response.put("status", execution.getStatus().toString());
        response.put("startTime", execution.getStartTime());
        response.put("endTime", execution.getEndTime());
        response.put("exitStatus", execution.getExitStatus().getExitCode());

        // Collect step details
        execution.getStepExecutions().forEach(step -> {
            Map<String, Object> stepInfo = new HashMap<>();
            stepInfo.put("stepName", step.getStepName());
            stepInfo.put("readCount", step.getReadCount());
            stepInfo.put("writeCount", step.getWriteCount());
            stepInfo.put("skipCount", step.getSkipCount());
            stepInfo.put("status", step.getStatus().toString());
            response.put("step_" + step.getStepName(), stepInfo);
        });

        return ResponseEntity.ok(response);
    }
}
