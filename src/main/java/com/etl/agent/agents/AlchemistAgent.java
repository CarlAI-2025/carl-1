package com.etl.agent.agents;

import com.etl.agent.domain.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Alchemist Agent: Transformation and enrichment.
 * Responsibilities:
 * - Produces cleaning + enrichment logic
 * - Normalization (case/trim), currency/date parsing
 * - Dedup + survivorship
 * - Missing value handling
 * - Identifier validation
 * - Joins to lookups
 * - Outputs Transformation Rules (declarative)
 * - Generates Executable Templates
 */
@Slf4j
public class AlchemistAgent implements ETLAgent {

    @Override
    public String getName() {
        return "Alchemist";
    }

    @Override
    public void execute(PipelineJob job) throws Exception {
        log.info("Alchemist: Generating transformation rules");
        long startTime = System.currentTimeMillis();

        try {
            TransformationSpec spec = generateTransformationSpec();

            // Add lineage
            PipelineJob.LineageEntry entry = new PipelineJob.LineageEntry("TRANSFORMATION", "Alchemist");
            entry.setInputRecords(job.getStatistics().getTotalRecordsRead());
            entry.setOutputRecords(job.getStatistics().getTotalRecordsRead());
            job.getLineage().add(entry);

            job.setStatus(PipelineJob.JobStatus.TRANSFORMED);
            log.info("Alchemist: Generated transformation spec with {} cleaning rules", spec.getCleaningRules().size());

        } catch (Exception e) {
            log.error("Alchemist: Error during transformation planning", e);
            PipelineJob.ErrorRecord error = new PipelineJob.ErrorRecord(
                    UUID.randomUUID().toString(),
                    "TRANSFORMATION",
                    "TRANSFORM_ERROR",
                    e.getMessage()
            );
            job.getErrors().add(error);
            job.setStatus(PipelineJob.JobStatus.FAILED);
            throw e;
        }

        long duration = System.currentTimeMillis() - startTime;
        job.getLineage().get(job.getLineage().size() - 1).setDurationMs(duration);
    }

    private TransformationSpec generateTransformationSpec() {
        TransformationSpec spec = new TransformationSpec();
        spec.setSpecId(UUID.randomUUID().toString());
        spec.setVersion("1.0.0");

        // Cleaning rules
        TransformationSpec.CleaningRule idClean = new TransformationSpec.CleaningRule("id");
        idClean.getOperations().add("TRIM");
        idClean.getOperations().add("UPPERCASE");
        idClean.setNullHandling("reject");
        spec.getCleaningRules().add(idClean);

        TransformationSpec.CleaningRule nameClean = new TransformationSpec.CleaningRule("name");
        nameClean.getOperations().add("TRIM");
        nameClean.getOperations().add("NORMALIZE_UNICODE");
        nameClean.setNullHandling("fill_with_unknown");
        spec.getCleaningRules().add(nameClean);

        TransformationSpec.CleaningRule amountClean = new TransformationSpec.CleaningRule("amount");
        amountClean.getOperations().add("PARSE_NUMERIC");
        amountClean.getOperations().add("ROUND_TO_2_DECIMALS");
        amountClean.setNullHandling("zero");
        spec.getCleaningRules().add(amountClean);

        TransformationSpec.CleaningRule dateClean = new TransformationSpec.CleaningRule("transaction_date");
        dateClean.getOperations().add("PARSE_ISO_DATE");
        dateClean.setNullHandling("current_date");
        spec.getCleaningRules().add(dateClean);

        // Deduplication
        TransformationSpec.DeduplicationConfig dedupConfig = new TransformationSpec.DeduplicationConfig();
        dedupConfig.getKeyFields().add("id");
        dedupConfig.getSurvivorshipRules().put("name", "KEEP_FIRST");
        dedupConfig.getSurvivorshipRules().put("amount", "KEEP_MAX");
        dedupConfig.getSurvivorshipRules().put("transaction_date", "KEEP_LATEST");
        spec.setDeduplicationConfig(dedupConfig);

        // Enrichment steps
        TransformationSpec.EnrichmentStep enrichment = new TransformationSpec.EnrichmentStep(
                "ENRICH_WITH_MARKET_CODE",
                "market_codes_lookup"
        );
        enrichment.setJoinKey("id");
        enrichment.getTargetFields().add("market_code");
        enrichment.getTargetFields().add("market_name");
        spec.getEnrichmentSteps().add(enrichment);

        // Aggregations for reporting
        TransformationSpec.AggregationRule agg1 = new TransformationSpec.AggregationRule("amount", "SUM");
        agg1.getGroupByFields().add("transaction_date");
        agg1.setTargetFieldName("daily_total_amount");
        spec.getAggregations().add(agg1);

        TransformationSpec.AggregationRule agg2 = new TransformationSpec.AggregationRule("id", "COUNT_DISTINCT");
        agg2.getGroupByFields().add("transaction_date");
        agg2.setTargetFieldName("daily_transaction_count");
        spec.getAggregations().add(agg2);

        return spec;
    }
}

