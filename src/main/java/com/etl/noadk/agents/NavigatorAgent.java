package com.etl.noadk.agents;

import com.etl.noadk.domain.FieldMapping;
import com.etl.noadk.domain.PipelineJob;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Navigator Agent: Standards mapping and canonical model.
 * Responsibilities:
 * - Maps source fields to canonical security master model
 * - Resolves conflicts
 * - Recommends identifier enrichment (FIGI) and standard codes (MIC, CFI)
 * - Generates Mapping Spec YAML/JSON + rationale
 * - Outputs confidence score per mapping
 */
@Slf4j
public class NavigatorAgent implements ETLAgent {

    @Override
    public String getName() {
        return "Navigator";
    }

    @Override
    public void execute(PipelineJob job) throws Exception {
        log.info("Navigator: Mapping source fields to canonical model");
        long startTime = System.currentTimeMillis();

        try {
            List<FieldMapping> mappings = generateMappings();

            // Add lineage
            PipelineJob.LineageEntry entry = new PipelineJob.LineageEntry("FIELD_MAPPING", "Navigator");
            entry.setInputRecords(job.getStatistics().getTotalRecordsRead());
            entry.setOutputRecords(mappings.size());
            job.getLineage().add(entry);

            job.setMappingVersion("m1_" + System.currentTimeMillis());
            job.setStatus(PipelineJob.JobStatus.MAPPED);

            log.info("Navigator: Generated {} field mappings", mappings.size());

        } catch (Exception e) {
            log.error("Navigator: Error during field mapping", e);
            PipelineJob.ErrorRecord error = new PipelineJob.ErrorRecord(
                    UUID.randomUUID().toString(),
                    "FIELD_MAPPING",
                    "MAPPING_ERROR",
                    e.getMessage()
            );
            job.getErrors().add(error);
            job.setStatus(PipelineJob.JobStatus.FAILED);
            throw e;
        }

        long duration = System.currentTimeMillis() - startTime;
        job.getLineage().get(job.getLineage().size() - 1).setDurationMs(duration);
    }

    private List<FieldMapping> generateMappings() {
        List<FieldMapping> mappings = new ArrayList<>();

        // Map ID field
        FieldMapping idMapping = new FieldMapping("id", "security_id", "STRING");
        idMapping.setConfidenceScore(0.99);
        idMapping.setKeyField(true);
        idMapping.setRationale("Primary identifier for securities");
        idMapping.getSuggestedStandards().put("FIGI", "Recommended for financial instrument identification");
        idMapping.getSuggestedStandards().put("ISIN", "Alternative standard code");
        mappings.add(idMapping);

        // Map Name field
        FieldMapping nameMapping = new FieldMapping("name", "security_name", "STRING");
        nameMapping.setConfidenceScore(0.95);
        nameMapping.setRationale("Human-readable security name");
        FieldMapping.ValidationRule nameRule = new FieldMapping.ValidationRule(
                "LENGTH",
                "length <= 255",
                "Security name exceeds maximum length"
        );
        nameMapping.getValidationRules().add(nameRule);
        mappings.add(nameMapping);

        // Map Amount field
        FieldMapping amountMapping = new FieldMapping("amount", "transaction_amount", "NUMERIC");
        amountMapping.setConfidenceScore(0.98);
        amountMapping.setRationale("Transactional value in currency units");
        amountMapping.setTransformationRule("CAST_TO_DECIMAL(18,2)");
        FieldMapping.ValidationRule amountRule = new FieldMapping.ValidationRule(
                "RANGE",
                "amount >= 0",
                "Amount cannot be negative"
        );
        amountMapping.getValidationRules().add(amountRule);
        mappings.add(amountMapping);

        // Map Date field
        FieldMapping dateMapping = new FieldMapping("transaction_date", "transaction_date", "DATE");
        dateMapping.setConfidenceScore(0.97);
        dateMapping.setRationale("Date of transaction in ISO format");
        dateMapping.setTransformationRule("PARSE_ISO_DATE(source_value)");
        FieldMapping.ValidationRule dateRule = new FieldMapping.ValidationRule(
                "FORMAT",
                "matches YYYY-MM-DD",
                "Invalid date format"
        );
        dateMapping.getValidationRules().add(dateRule);
        mappings.add(dateMapping);

        return mappings;
    }
}

