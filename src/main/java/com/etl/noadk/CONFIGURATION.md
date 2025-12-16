````markdown
# Non-ADK Configuration Guide

## Overview

This guide explains how to configure the traditional, non-ADK based ETL pipeline for different environments and requirements.

## Quick Start Configuration

### Minimal Configuration (Local Development)
```bash
# Set environment variables
export GCP_PROJECT_ID=my-project-id
export GCS_BUCKET=my-bucket
export GCS_OBJECT=data/input.csv
export GOOGLE_APPLICATION_CREDENTIALS=$HOME/.config/gcloud/credentials.json

# Run
mvn clean package
java -jar target/carl-1-1.0-SNAPSHOT.jar
```

### Production Configuration (GCP)
```bash
# Set environment variables for Cloud Run
export GCP_PROJECT_ID=production-project
export GCS_BUCKET=prod-etl-bucket
export PIPELINE_MAX_RETRIES=5
export PIPELINE_TIMEOUT_SECONDS=1800
```

## Configuration File: application-noadk.properties

The main configuration file is located at:
```
src/main/resources/application-noadk.properties
```

### Structure

#### 1. GCP Project Configuration
```ini
gcp.project-id=${GCP_PROJECT_ID}
gcp.location=us-central1
gcp.credentials-path=${GOOGLE_APPLICATION_CREDENTIALS}
```

**Details:**
- `gcp.project-id` - Your Google Cloud Project ID
- `gcp.location` - Default region for services
- `gcp.credentials-path` - Path to service account JSON key file

#### 2. Cloud Storage Configuration
```ini
gcs.bucket=${GCS_BUCKET:data-lake}
gcs.input-prefix=input/
gcs.staging-prefix=staging/
gcs.archive-prefix=archive/
gcs.error-prefix=errors/
```

**Details:**
- `gcs.bucket` - Default bucket for all data operations
- `gcs.input-prefix` - Where to read input files
- `gcs.staging-prefix` - Where to write intermediate data
- `gcs.archive-prefix` - Where to archive processed files
- `gcs.error-prefix` - Where to store error records

**Example:**
```
Input:  gs://my-bucket/input/sales.csv
Staging: gs://my-bucket/staging/sales_2024-01-15.parquet
Archive: gs://my-bucket/archive/sales_2024-01-15.csv
Errors:  gs://my-bucket/errors/sales_2024-01-15_errors.json
```

#### 3. BigQuery Configuration
```ini
bq.dataset=${BQ_DATASET:etl_dataset}
bq.staging-errors-table=staging_errors
bq.job-lineage-table=job_lineage
bq.schema-versions-table=schema_versions
bq.mapping-history-table=mapping_history
bq.dq-scorecard-table=dq_scorecard
bq.load-timeout-seconds=600
bq.query-timeout-seconds=300
```

**Details:**
- `bq.dataset` - Default dataset for all tables
- `bq.*-table` - Specific table names for different purposes
- `bq.load-timeout-seconds` - Timeout for data loads
- `bq.query-timeout-seconds` - Timeout for analysis queries

**Tables Created:**
- `staging_errors` - Failed records during validation/transformation
- `job_lineage` - Execution history and audit trail
- `schema_versions` - Schema evolution tracking
- `mapping_history` - Field mapping changes
- `dq_scorecard` - Data quality scores

#### 4. CSV Parsing Configuration
```ini
csv.delimiter=,
csv.quote-character="
csv.skip-header=true
csv.charset=UTF-8
csv.max-sample-rows=1000
csv.max-file-size=5GB
```

**Details:**
- `csv.delimiter` - Field separator (`,`, `;`, `|`, `\t`, etc.)
- `csv.quote-character` - Character for quoted fields
- `csv.skip-header` - Skip first row (usually headers)
- `csv.charset` - Text encoding (UTF-8, ISO-8859-1, etc.)
- `csv.max-sample-rows` - Rows to analyze for schema inference
- `csv.max-file-size` - Maximum file size to process

**Examples:**
```ini
# Tab-separated values
csv.delimiter=\t
csv.quote-character="

# Pipe-delimited
csv.delimiter=|
csv.quote-character="

# Semicolon-separated (European)
csv.delimiter=;
csv.quote-character="
```

## Agent-Specific Configuration

### Scout Agent (Ingestion)
```ini
agent.scout.enabled=true
agent.scout.timeout-seconds=120
agent.scout.max-retries=2
agent.scout.retry-backoff-ms=1000
```

**When to adjust:**
- Increase `timeout-seconds` for large files (>500MB)
- Increase `max-retries` for unreliable network
- Adjust `retry-backoff-ms` for busy systems

### Cartographer Agent (Schema Inference)
```ini
agent.cartographer.enabled=true
agent.cartographer.timeout-seconds=180
agent.cartographer.max-retries=3
agent.cartographer.retry-backoff-ms=2000
agent.cartographer.confidence-threshold=0.75
agent.cartographer.detect-schema-drift=true
```

**Key settings:**
- `confidence-threshold` - Minimum confidence for schema acceptance (0.0-1.0)
  - 0.90: Very strict, only clear patterns
  - 0.75: Balanced (recommended)
  - 0.50: Permissive, accepts uncertain inferences
- `detect-schema-drift` - Compare against previous schema version

### Navigator Agent (Field Mapping)
```ini
agent.navigator.enabled=true
agent.navigator.timeout-seconds=180
agent.navigator.max-retries=3
agent.navigator.retry-backoff-ms=2000
agent.navigator.standard-identifiers=FIGI,ISIN,MIC,CFI
agent.navigator.mapping-confidence-threshold=0.80
```

**Key settings:**
- `standard-identifiers` - Financial/business standard codes to look up
  - FIGI: Financial Instrument Global Identifier
  - ISIN: International Securities Identification Number
  - MIC: Market Identifier Code
  - CFI: Classification of Financial Instruments
- `mapping-confidence-threshold` - Minimum confidence for field mapping

### Alchemist Agent (Transformation)
```ini
agent.alchemist.enabled=true
agent.alchemist.timeout-seconds=240
agent.alchemist.max-retries=3
agent.alchemist.retry-backoff-ms=2000
agent.alchemist.enable-deduplication=true
agent.alchemist.enable-normalization=true
agent.alchemist.enable-enrichment=true
```

**Key settings:**
- `enable-deduplication` - Remove duplicate records
- `enable-normalization` - Trim, case-adjust, standardize
- `enable-enrichment` - Join with lookup tables

### Architect Agent (SQL Generation)
```ini
agent.architect.enabled=true
agent.architect.timeout-seconds=180
agent.architect.max-retries=2
agent.architect.retry-backoff-ms=1000
agent.architect.generate-incremental-sql=true
agent.architect.partition-column=load_date
```

**Key settings:**
- `generate-incremental-sql` - Use MERGE for upserts instead of DELETE+INSERT
- `partition-column` - Column to partition BigQuery table

### Auditor Agent (Quality Assessment)
```ini
agent.auditor.enabled=true
agent.auditor.timeout-seconds=120
agent.auditor.max-retries=2
agent.auditor.retry-backoff-ms=1000
agent.auditor.dq-score-threshold=0.85
agent.auditor.compliance-score-threshold=0.90
```

**Key settings:**
- `dq-score-threshold` - Minimum acceptable data quality (0.0-1.0)
  - Completeness: % non-null
  - Validity: % valid according to schema
  - Uniqueness: % without duplicates
  - Timeliness: data freshness
  - Consistency: field relationships
- `compliance-score-threshold` - Minimum acceptable regulatory compliance

### Conductor Agent (Orchestration)
```ini
agent.conductor.timeout-seconds=900
agent.conductor.max-retries=1
agent.conductor.enable-lineage-tracking=true
agent.conductor.enable-audit-logs=true
```

**Key settings:**
- `enable-lineage-tracking` - Record execution history
- `enable-audit-logs` - Log all operations to Cloud Logging

## Vertex AI Configuration (Optional)

If using Vertex AI for intelligent transformations:

```ini
vertex-ai.enabled=false
vertex-ai.project-id=${GCP_PROJECT_ID}
vertex-ai.location=us-central1
vertex-ai.model=gemini-pro
vertex-ai.temperature=0.7
vertex-ai.max-output-tokens=2048
vertex-ai.top-p=0.95
vertex-ai.top-k=40
```

**When to enable:**
- When you want AI-powered transformation suggestions
- When field mappings are ambiguous
- When detecting schema drift with context

**Model parameters:**
- `temperature` - Creativity (0.0=deterministic, 1.0=random)
  - 0.7: Good balance (recommended)
  - 0.3: Very literal, consistent
  - 0.9: Creative, varied
- `max-output-tokens` - Maximum response length
- `top-p` - Nucleus sampling (0.0-1.0)
- `top-k` - Top-k sampling (1-40)

## Data Quality Configuration

### Validation Rules
```ini
validation.enable-null-checks=true
validation.enable-type-validation=true
validation.enable-range-validation=true
validation.enable-pattern-validation=true
validation.max-error-threshold=0.05
```

**Details:**
- `enable-null-checks` - Reject rows with NULL in required fields
- `enable-type-validation` - Validate data types match schema
- `enable-range-validation` - Check numeric min/max bounds
- `enable-pattern-validation` - Check string patterns (regex)
- `max-error-threshold` - Max % of rows that can fail (default 5%)

### Anomaly Detection
```ini
anomaly-detection.enabled=true
anomaly-detection.method=statistical
anomaly-detection.zscore-threshold=3.0
anomaly-detection.iqr-multiplier=1.5
anomaly-detection.min-sample-size=30
```

**Details:**
- `enabled` - Enable or disable anomaly detection
- `method=statistical` - Use statistical methods (future: ML methods)
- `zscore-threshold` - Z-score for outlier detection
  - 3.0: Standard (catch ~0.3% of data)
  - 2.0: Strict (catch ~5% of data)
- `iqr-multiplier` - IQR multiplier for outlier bounds
- `min-sample-size` - Minimum data points for statistics

### Schema Validation
```ini
schema-validation.check-column-count=true
schema-validation.check-column-types=true
schema-validation.check-nullability=true
schema-validation.allow-schema-drift=false
schema-validation.drift-threshold=0.10
```

**Details:**
- `check-column-count` - Fail if column count changes
- `check-column-types` - Fail if column types change
- `check-nullability` - Fail if NULL handling changes
- `allow-schema-drift` - Allow schema evolution
- `drift-threshold` - Max % field changes allowed (default 10%)

## Logging Configuration
```ini
logging.level=INFO
logging.detailed-errors=true
logging.log-agent-inputs=false
logging.log-agent-outputs=false
logging.log-sql-statements=true
logging.log-lineage-events=true
logging.gcp-logging.enabled=true
logging.gcp-logging.log-name=etl-pipeline
```

**Details:**
- `level` - Logging level (DEBUG, INFO, WARN, ERROR)
- `detailed-errors=true` - Include full stack traces
- `log-agent-inputs=false` - Log input data (privacy concern!)
- `log-agent-outputs=false` - Log output data (privacy concern!)
- `log-sql-statements=true` - Log generated SQL
- `log-lineage-events=true` - Log lineage tracking events
- `gcp-logging.enabled=true` - Send to Cloud Logging

## Retry Configuration
```ini
retry.strategy=exponential
retry.initial-backoff-ms=1000
retry.max-backoff-ms=32000
retry.multiplier=2.0
```

**Details:**
- `strategy=exponential` - Use exponential backoff
- `initial-backoff-ms` - First retry wait time
- `max-backoff-ms` - Maximum wait time between retries
- `multiplier` - Backoff multiplier (2.0 = double each time)

**Example sequence:**
- Attempt 1: Fail immediately
- Attempt 2: Wait 1000ms, retry
- Attempt 3: Wait 2000ms, retry
- Attempt 4: Wait 4000ms, retry
- ... continues until max-backoff-ms

## Error Handling
```ini
error.halt-on-validation-error=false
error.halt-on-mapping-error=false
error.halt-on-quality-error=false
error.save-failed-records=true
error.max-errors-to-log=100
```

**Details:**
- `halt-on-*-error` - Whether to stop pipeline on errors
  - `true` - Strict mode, fail on any error
  - `false` - Tolerant mode, continue if possible
- `save-failed-records` - Store failed records in staging_errors
- `max-errors-to-log` - Limit error log entries

## Performance Tuning
```ini
performance.enable-parallel-processing=true
performance.thread-pool-size=10
performance.batch-size=10000
performance.enable-caching=true
performance.cache-ttl-seconds=3600
```

**Details:**
- `enable-parallel-processing` - Process multiple files in parallel
- `thread-pool-size` - Number of threads (10-20 for production)
- `batch-size` - Rows per batch for BigQuery loads (10k-100k)
- `enable-caching` - Cache schema inference results
- `cache-ttl-seconds` - Cache validity period

## Environment-Specific Configuration

### Development Configuration
```ini
# development.properties
logging.level=DEBUG
logging.log-agent-inputs=true
performance.thread-pool-size=2
error.halt-on-validation-error=true
agent.scout.timeout-seconds=60
agent.conductor.max-retries=1
```

### Staging Configuration
```ini
# staging.properties
logging.level=INFO
logging.detailed-errors=true
performance.thread-pool-size=5
error.halt-on-quality-error=true
agent.scout.timeout-seconds=120
agent.conductor.max-retries=2
```

### Production Configuration
```ini
# production.properties
logging.level=WARN
logging.detailed-errors=false
performance.thread-pool-size=10
error.halt-on-validation-error=false
agent.scout.timeout-seconds=300
agent.conductor.max-retries=3
anomaly-detection.enabled=true
vertex-ai.enabled=true
```

## Loading Configuration Files

### Method 1: System Properties
```bash
java -Dspring.config.location=classpath:application-noadk.properties \
  -jar carl-1-1.0-SNAPSHOT.jar
```

### Method 2: Environment Variable
```bash
export SPRING_CONFIG_LOCATION=file:/etc/etl/application.properties
java -jar carl-1-1.0-SNAPSHOT.jar
```

### Method 3: Command Line
```bash
java -jar carl-1-1.0-SNAPSHOT.jar \
  --spring.config.location=application-noadk.properties
```

### Method 4: Application.yml (if using Spring)
```yaml
spring:
  config:
    location: file:/etc/etl/application-noadk.properties
```

## Configuration Validation

### Check Configuration
```bash
# Print all configuration values
java -jar carl-1-1.0-SNAPSHOT.jar --print-config

# Validate configuration file
java -jar carl-1-1.0-SNAPSHOT.jar --validate-config
```

### Common Configuration Issues

| Issue | Cause | Fix |
|-------|-------|-----|
| Authentication failure | Missing credentials | Set GOOGLE_APPLICATION_CREDENTIALS |
| Bucket not found | Wrong bucket name | Check GCS_BUCKET environment variable |
| Dataset not found | Wrong dataset | Verify BQ_DATASET exists in BigQuery |
| Timeout errors | Network issues | Increase timeout-seconds values |
| OOM errors | batch-size too large | Reduce batch-size in configuration |

## Best Practices

1. **Use environment variables for secrets**
   ```bash
   export GCP_PROJECT_ID=my-project
   export GCS_BUCKET=my-bucket
   # Never commit credentials to git
   ```

2. **Version your configuration**
   ```ini
   # application-noadk-v1.0.properties
   # application-noadk-v1.1.properties
   ```

3. **Document custom settings**
   ```ini
   # Custom timeout for large files
   agent.scout.timeout-seconds=600  # 10 minutes
   ```

4. **Monitor configuration impact**
   - Test configuration changes on staging first
   - Monitor metrics after changes
   - Have rollback plan

5. **Keep defaults sensible**
   - Don't over-optimize initially
   - Tune based on actual performance data
   - Document why you changed each setting

---

**Last Updated:** December 15, 2025
**Audience:** DevOps, Platform Engineers, Data Engineers
**Related:** `ARCHITECTURE.md` (Agent architecture), README.keep.md (Overview)

````
