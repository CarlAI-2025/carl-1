package com.etl.agent;

import com.etl.agent.agents.ConductorAgent;
import com.etl.agent.domain.PipelineJob;
import lombok.extern.slf4j.Slf4j;

/**
 * Main entry point for the ETL agent pipeline.
 */
@Slf4j
public class ETLPipelineMain {

    public static void main(String[] args) {
        log.info("=== ETL Agent Pipeline Starting ===");

        if (args.length < 3) {
            System.err.println("Usage: java ETLPipelineMain <source_path> <target_dataset> <target_table>");
            System.err.println("Example: java ETLPipelineMain gs://bucket/data.csv etl_dataset transactions");
            System.exit(1);
        }

        String sourcePath = args[0];
        String targetDataset = args[1];
        String targetTable = args[2];

        try {
            // Create pipeline job
            PipelineJob job = new PipelineJob();
            job.setSourcePath(sourcePath);
            job.setTargetDataset(targetDataset);
            job.setTargetTable(targetTable);

            log.info("Created pipeline job: {}", job.getJobId());
            log.info("Source: {}", sourcePath);
            log.info("Target: {}.{}", targetDataset, targetTable);

            // Execute pipeline
            ConductorAgent conductor = new ConductorAgent();
            conductor.executePipeline(job);

            // Print results
            printResults(job);

            log.info("=== ETL Agent Pipeline Completed Successfully ===");
            System.exit(0);

        } catch (Exception e) {
            log.error("=== ETL Agent Pipeline Failed ===", e);
            System.exit(1);
        }
    }

    private static void printResults(PipelineJob job) {
        System.out.println("\n=== PIPELINE EXECUTION SUMMARY ===");
        System.out.println("Job ID: " + job.getJobId());
        System.out.println("Status: " + job.getStatus());
        System.out.println("Total Records Read: " + job.getStatistics().getTotalRecordsRead());
        System.out.println("Total Records Loaded: " + job.getStatistics().getTotalRecordsLoaded());
        System.out.println("Total Records Rejected: " + job.getStatistics().getTotalRecordsRejected());
        System.out.println("Total Records Deduplicated: " + job.getStatistics().getTotalRecordsDeduplicated());
        System.out.println("\n=== LINEAGE TRACKING ===");
        for (PipelineJob.LineageEntry entry : job.getLineage()) {
            System.out.println(String.format("[%s] Agent: %s (Duration: %dms)",
                    entry.getStep(), entry.getAgentName(), entry.getDurationMs()));
        }

        if (!job.getErrors().isEmpty()) {
            System.out.println("\n=== ERRORS ===");
            for (PipelineJob.ErrorRecord error : job.getErrors()) {
                System.out.println(String.format("[%s] %s: %s",
                        error.getErrorType(), error.getFieldName(), error.getErrorMessage()));
            }
        }
    }
}

