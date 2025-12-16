````markdown
# Non-ADK Architecture Guide

## Overview

This document describes the architecture of the traditional, non-ADK based ETL pipeline implementation. This architecture emphasizes explicit control, transparency, and flexibility over automated framework orchestration.

## Architecture Principles

### 1. **Custom Agent Framework**
- Lightweight `ETLAgent` interface instead of ADK abstractions
- Direct control over agent lifecycle and execution flow
- Simple request/response pattern for agent communication
- Minimal external dependencies

### 2. **Explicit State Management**
- `PipelineJob` model maintains full execution state
- State transitions tracked through job lifecycle
- Each agent updates state explicitly
- Step-level recovery and retry capabilities

### 3. **GCP-First Design**
- Native BigQuery integration for storage and processing
- Cloud Storage for data staging and archiving
- Cloud Logging for structured audit trails
- Optional Vertex AI for intelligent processing

### 4. **Agent Pattern**
```
┌─────────────────────────────────────────────┐
│         ConductorAgent (Orchestrator)       │
├─────────────────────────────────────────────┤
│ Manages workflow, state transitions, retries│
└──────────┬──────────────────────────────────┘
           │
     ┌─────┴─────┐
     │           │
  ┌──▼──┐   ┌────▼───┐
  │Agent│   │Agent   │  (Sequential or Parallel)
  └─────┘   └────────┘
```

## Core Components

### ETLAgent Interface
```java
public interface ETLAgent {
    String getName();
    String getDescription();
    void initialize(PipelineJob job);
    void execute(PipelineJob job) throws Exception;
    void cleanup();
}
```

**Responsibilities:**
- Accept PipelineJob state
- Perform work (data validation, transformation, etc.)
- Update PipelineJob with results
- Handle errors gracefully

### PipelineJob Model
Central state object passed between agents:

```
├── jobId (UUID)
├── datasetName (String)
├── status (JobStatus enum)
├── startTime (LocalDateTime)
├── endTime (LocalDateTime)
├── statistics (JobStatistics)
├── lineage (List<LineageEntry>)
├── errorRecords (List<ErrorRecord>)
├── schemas (Map<String, SchemaContract>)
├── mappings (Map<String, FieldMapping>)
├── transformations (List<TransformationSpec>)
└── metadata (Map<String, String>)
```

### Domain Models

#### SchemaContract
Represents inferred or expected schema:
```
├── fields (List<FieldContract>)
├── confidence (Double) [0.0-1.0]
├── inferredAt (LocalDateTime)
├── dataSource (String)
└── notes (String)
```

#### FieldMapping
Maps source fields to canonical/target fields:
```
├── sourceField (String)
├── targetField (String)
├── targetType (DataType)
├── mappingRules (List<String>)
├── confidence (Double)
├── standardIdentifiers (List<String>)
└── conflictResolution (String)
```

#### TransformationSpec
Declarative transformation rules:
```
├── name (String)
├── description (String)
├── sourceFields (List<String>)
├── targetFields (List<String>)
├── transformationType (TransformType)
├── logic (String)  // SQL or pseudocode
└── priority (Integer)
```

## Agent Execution Flow

### 1. Scout Agent (Data Ingestion)
**Input:** GCS path
**Output:** File analysis, schema fingerprint, sample rows

**Steps:**
1. Read file from GCS
2. Validate format (CSV, JSON, Parquet)
3. Calculate file statistics (row count, size, columns)
4. Extract sample rows
5. Generate schema fingerprint (MD5 hash)

**State Updates:**
```
PipelineJob.fileInfo = FileInfo(format, rowCount, size)
PipelineJob.sampleData = firstNRows
PipelineJob.schemaFingerprint = hash
```

### 2. Cartographer Agent (Schema Inference)
**Input:** Sample rows from Scout
**Output:** SchemaContract with confidence scores

**Steps:**
1. Analyze sample data types
2. Detect nullability patterns
3. Infer constraints (min/max for numbers, length for strings)
4. Compare against previous schema (if exists)
5. Detect schema drift

**State Updates:**
```
PipelineJob.sourceSchema = SchemaContract(...)
PipelineJob.schemaDrift = DriftReport(...)
```

### 3. Navigator Agent (Field Mapping)
**Input:** SchemaContract from Cartographer
**Output:** FieldMappings with standard identifiers

**Steps:**
1. For each source field, suggest target mapping
2. Look up standard codes (FIGI, ISIN, MIC, CFI) if applicable
3. Calculate mapping confidence
4. Flag conflicts or ambiguous mappings
5. Generate mapping rationale

**State Updates:**
```
PipelineJob.fieldMappings = List<FieldMapping>
PipelineJob.standardIdentifiers = Map<FieldName, List<StandardCode>>
```

### 4. Alchemist Agent (Transformation Rules)
**Input:** Field mappings from Navigator
**Output:** TransformationSpecs for cleaning and enrichment

**Steps:**
1. Generate normalization rules (trim, case-adjust, etc.)
2. Generate type coercion logic
3. Generate deduplication rules
4. Generate enrichment rules (joins, lookups)
5. Generate survivorship rules (for duplicates)
6. Generate missing value handling

**State Updates:**
```
PipelineJob.transformations = List<TransformationSpec>
```

### 5. Architect Agent (SQL Generation)
**Input:** Transformations from Alchemist
**Output:** BigQuery SQL scripts (DDL + DML)

**Steps:**
1. Generate BigQuery schema DDL
2. Generate transformation SQL
3. Generate load SQL (INSERT or MERGE)
4. Optimize SQL for BigQuery
5. Add lineage tracking to SQL

**State Updates:**
```
PipelineJob.generatedSQL = SqlScripts(ddl, transformSql, loadSql)
```

### 6. Auditor Agent (Quality Assessment)
**Input:** Load results
**Output:** DQ report with compliance score

**Steps:**
1. Calculate data quality metrics (completeness, validity, timeliness, uniqueness, consistency)
2. Check against quality thresholds
3. Generate DQ scorecard
4. Assess regulatory compliance
5. Flag anomalies or issues
6. Generate recommendations

**State Updates:**
```
PipelineJob.dqScorecard = DQScorecard(...)
PipelineJob.complianceScore = 0.95
```

### 7. Conductor Agent (Orchestration)
**Responsibilities:**
1. Initialize all agents
2. Call agents in sequence
3. Handle retry logic with exponential backoff
4. Record lineage at each stage
5. Generate final execution report

**Execution Loop:**
```java
for (ETLAgent agent : agents) {
    int retries = 0;
    while (retries < MAX_RETRIES) {
        try {
            agent.initialize(job);
            agent.execute(job);
            recordLineage(agent, "SUCCESS");
            break;
        } catch (Exception e) {
            retries++;
            if (retries >= MAX_RETRIES) {
                recordLineage(agent, "FAILED");
                throw e;
            }
            Thread.sleep(backoffTime);
        }
    }
}
```

## Error Handling Strategy

### Three-Tier Approach

#### Tier 1: Validation Errors
- **Type:** Data doesn't match schema expectations
- **Handler:** Record in staging_errors, continue (if threshold not exceeded)
- **Action:** Flag for manual review

#### Tier 2: Mapping Errors
- **Type:** Cannot map source fields to target fields
- **Handler:** Record in staging_errors, halt if critical
- **Action:** Require manual mapping intervention

#### Tier 3: Processing Errors
- **Type:** SQL execution, Vertex AI, GCP service failures
- **Handler:** Retry with exponential backoff (configurable)
- **Action:** Escalate if max retries exceeded

### Retry Strategy
```
Attempt 1 → Fail → Wait 1000ms
Attempt 2 → Fail → Wait 2000ms
Attempt 3 → Fail → Wait 4000ms
Fail Finally → Record error and stop
```

## BigQuery Integration

### Tables Created

#### job_lineage
```sql
CREATE TABLE job_lineage (
    job_id STRING,
    timestamp TIMESTAMP,
    agent_name STRING,
    action STRING,
    status STRING,
    duration_ms INT64,
    details STRING
);
```

#### staging_errors
```sql
CREATE TABLE staging_errors (
    job_id STRING,
    error_timestamp TIMESTAMP,
    record_id STRING,
    source_data STRING,
    error_type STRING,
    error_message STRING,
    agent_name STRING
);
```

#### schema_versions
```sql
CREATE TABLE schema_versions (
    schema_id STRING,
    version INT64,
    created_at TIMESTAMP,
    source_schema JSON,
    target_schema JSON,
    confidence FLOAT64,
    notes STRING
);
```

#### mapping_history
```sql
CREATE TABLE mapping_history (
    mapping_id STRING,
    created_at TIMESTAMP,
    source_field STRING,
    target_field STRING,
    mapping_rules JSON,
    standard_identifiers ARRAY<STRING>,
    confidence FLOAT64,
    change_reason STRING
);
```

## State Transitions

```
           CREATE
             │
             ▼
    ┌──────────────────┐
    │ CREATED          │
    └────────┬─────────┘
             │
             ▼
    ┌──────────────────┐
    │ VALIDATING       │◄───┐
    │ (Scout Agent)    │    │ Retry on
    └────────┬─────────┘    │ transient
             │              │ error
             ├──Error────────┘
             │
             ▼
    ┌──────────────────┐
    │ MAPPING          │◄───┐
    │ (Navigator Agent)│    │ Retry on
    └────────┬─────────┘    │ transient
             │              │ error
             ├──Error────────┘
             │
             ▼
    ┌──────────────────┐
    │ TRANSFORMING     │◄───┐
    │ (Alchemist Agent)│    │ Retry on
    └────────┬─────────┘    │ transient
             │              │ error
             ├──Error────────┘
             │
             ▼
    ┌──────────────────┐
    │ LOADING          │◄───┐
    │ (Architect Agent)│    │ Retry on
    └────────┬─────────┘    │ transient
             │              │ error
             ├──Error────────┘
             │
             ▼
    ┌──────────────────┐
    │ AUDITING         │◄───┐
    │ (Auditor Agent)  │    │ Retry on
    └────────┬─────────┘    │ transient
             │              │ error
             ├──Error────────┘
             │
             ▼
    ┌──────────────────┐
    │ COMPLETE/FAILED  │
    │ (Final state)    │
    └──────────────────┘
```

## Comparison with ADK Architecture

### Non-ADK Advantages
✅ **Transparent** - Clear, auditable code flow
✅ **Flexible** - Easy to customize or extend
✅ **Lightweight** - Minimal dependencies
✅ **Debuggable** - Step-through debugging friendly
✅ **On-premise Ready** - Works anywhere

### ADK Advantages
✅ **Standardized** - Uses Google's ADK framework
✅ **Tool Integration** - Built-in tool registry and execution
✅ **Scalable** - Better for large agent networks
✅ **Managed** - Automatic retry, orchestration
✅ **GCP Native** - Deep integration with GCP services

## Deployment Options

### Local Development
```bash
java -cp target/carl-1-1.0-SNAPSHOT.jar com.etl.noadk.ETLPipelineMain
```

### Docker Container
```dockerfile
FROM openjdk:11-jre-slim
COPY target/carl-1-1.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Cloud Run
```bash
gcloud run deploy etl-pipeline \
  --source . \
  --runtime java11 \
  --entry-point com.etl.noadk.ETLPipelineMain
```

### Cloud Scheduler (Scheduled Execution)
```bash
gcloud scheduler jobs create pubsub etl-daily \
  --schedule "0 2 * * *" \
  --topic etl-trigger
```

## Performance Considerations

### Optimization Strategies
1. **Batch Processing** - Process multiple files in batch
2. **Parallel Agents** - Run independent agents in parallel (future enhancement)
3. **Caching** - Cache schema inference results
4. **Connection Pooling** - Reuse BigQuery connections
5. **Streaming** - Stream large files instead of loading into memory

### Resource Requirements
- **Memory:** 2GB minimum, 4GB recommended
- **Disk:** 10GB for staging (depends on file size)
- **Network:** 10Mbps for GCS/BigQuery access
- **CPU:** 2 cores recommended for local development

## Monitoring & Observability

### Logs
- Agent execution logs (CloudLogging)
- SQL statement logs (BigQuery audit logs)
- Error logs (stored in staging_errors table)
- Lineage logs (stored in job_lineage table)

### Metrics
- Pipeline execution time
- Error rate
- Data quality score
- Schema drift detection

### Alerts
- Pipeline failure
- Anomaly detection threshold exceeded
- Data quality score below threshold
- Schema drift detected

## Security Considerations

### Authentication & Authorization
- Service account with minimal required IAM roles
- GOOGLE_APPLICATION_CREDENTIALS for local development
- Workload Identity for GCP deployments

### Data Protection
- Encryption at rest (GCS, BigQuery)
- Encryption in transit (TLS)
- No secrets in logs
- PII handling and masking (if needed)

### Audit Trail
- All operations logged to job_lineage
- Failed records logged to staging_errors
- Schema changes tracked in schema_versions

---

**Last Updated:** December 15, 2025
**Audience:** Developers deploying non-ADK ETL pipelines
**Related:** `ADK_PURE_ARCHITECTURE.md` (for ADK comparison)

````
