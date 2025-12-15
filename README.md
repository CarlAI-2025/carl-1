# ETL Agent Pipeline for BigQuery

A sophisticated, multi-agent orchestrated ETL pipeline built with Google ADK and GCP services for automated schema discovery, field mapping, data transformation, and validation with complete lineage tracking.

## Architecture Overview

### Agent-Based Design

The pipeline implements the architecture from `design.txt` with six specialized agents:

1. **Scout Agent** (Ingestion)
   - Reads CSV files from GCS
   - Performs format validation
   - Generates data statistics and schema fingerprints
   - Collects sample rows for analysis

2. **Cartographer Agent** (Schema Inference)
   - Infers data types with confidence scores
   - Detects schema patterns (KEY_FIELD, TEMPORAL_FIELD, NUMERIC_MEASURE)
   - Analyzes null percentages and uniqueness
   - Generates Source Schema Contract

3. **Navigator Agent** (Field Mapping)
   - Maps source fields to canonical security master model
   - Suggests standard codes (FIGI, ISIN, MIC, CFI)
   - Generates mapping specifications with rationale
   - Provides confidence scores per mapping

4. **Alchemist Agent** (Transformation)
   - Generates cleaning rules (trim, normalize, parse)
   - Configures deduplication with survivorship rules
   - Defines enrichment steps and lookups
   - Creates aggregation specifications for reporting

5. **Architect Agent** (Pipeline Generation)
   - Generates BigQuery SQL scripts
   - Implements idempotent load patterns with job lineage
   - Supports Dataflow template generation (Platinum tier)
   - Outputs executable transformation artifacts

6. **Auditor Agent** (Quality & Compliance)
   - Calculates data quality scores
   - Generates compliance reports (0-100)
   - Creates reasoning logs with transformation rationale
   - Tracks record flow through pipeline

### Conductor Agent (Orchestrator)

The **Conductor** orchestrates all agents with:
- Sequential workflow execution
- Automatic retry with exponential backoff
- State management and tracking
- Lineage recording
- Error handling and rollback

## Challenge Tiers

### Bronze: Basic ETL
- ✅ Reads CSV files from GCS
- ✅ Validates schema structure
- ✅ Generates BigQuery DDL
- ✅ Basic type inference
- ✅ Loads to BigQuery tables

**Files:**
- `ScoutAgent.java` - CSV ingestion
- `CartographerAgent.java` - Schema discovery
- `BigQueryService.java` - Table creation

### Silver: Enhanced with Quality Controls
- ✅ Automatic deduplication (with survivorship rules)
- ✅ Type coercion and normalization
- ✅ Validation rule enforcement
- ✅ Validation error reporting to staging_errors table
- ✅ Data quality scorecards

**Files:**
- `AlchemistAgent.java` - Cleaning/dedup rules
- `AuditorAgent.java` - DQ scorecard generation
- `TransformationSpec.java` - Declarative rules

### Gold: Anomaly Detection & Suggestions
- ✅ Automatic anomaly detection on numeric indicators
- ✅ Statistical analysis (mean, stddev, outliers)
- ✅ Suggested corrective transformations
- ✅ Explainable transformation steps
- ✅ Pattern-based field classification

**Files:**
- `CartographerAgent.java` - Pattern detection
- `FieldMapping.java` - Transformation rationale
- Anomaly detection logic in `AuditorAgent.java`

### Platinum: Auto SQL Generation & CI/CD
- ✅ Auto-generates executable BigQuery SQL
- ✅ Implements idempotent loads with job tracking
- ✅ Version lineage (dataset_version, mapping_version)
- ✅ Dataflow template skeleton generation
- ✅ Cloud Scheduler integration ready
- ✅ Event-driven execution patterns

**Files:**
- `ArchitectAgent.java` - SQL generation
- `ConductorAgent.java` - Workflow orchestration
- `PipelineJob.java` - Lineage tracking

## Installation & Setup

### Prerequisites
- Java 17+
- Maven 3.8+
- GCP Project with:
  - BigQuery API enabled
  - Cloud Storage API enabled
  - Appropriate IAM permissions

### Build

```bash
mvn clean install
```

### Configuration

Set GCP credentials:
```bash
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/key.json
```

### Running the Pipeline

```bash
java -cp target/carl-1-1.0-SNAPSHOT.jar com.etl.agent.ETLPipelineMain \
  gs://your-bucket/data.csv \
  your_dataset \
  your_table
```

## Usage Examples

### Basic CSV Load
```bash
java -cp target/carl-1-1.0-SNAPSHOT.jar com.etl.agent.ETLPipelineMain \
  gs://data-lake/transactions.csv \
  etl_dataset \
  transactions
```

### With Logging
```bash
java -Dorg.slf4j.simpleLogger.defaultLogLevel=DEBUG \
  -cp target/carl-1-1.0-SNAPSHOT.jar \
  com.etl.agent.ETLPipelineMain \
  gs://data-lake/transactions.csv \
  etl_dataset \
  transactions
```

## Key Features

### Schema Discovery
- Automatic type inference from samples
- Confidence scoring for inferred types
- Pattern detection (keys, temporal fields, measures)
- Schema fingerprinting for drift detection

### Field Mapping
- Canonical model alignment
- Standard code recommendations (FIGI, ISIN)
- Mapping confidence scoring
- Rationale documentation

### Data Transformation
- Declarative cleaning rules
- Automatic deduplication
- Survivorship strategies
- Enrichment via lookup tables
- Aggregations for reporting

### Data Quality
- Quality scoring (0-100)
- Compliance scoring (0-100)
- Validation rule enforcement
- Error staging and tracking
- Reasoning logs

### Lineage & Idempotency
- Complete job lineage tracking
- Dataset and mapping versioning
- Idempotent load detection
- Job state persistence
- Execution history

## Data Model Examples

### Security Transactions Example
```sql
-- Source CSV
id,name,amount,transaction_date
12345,ACME CORP,1500.50,2024-01-15
12346,TECH INC,2000.75,2024-01-16

-- Canonical Target
security_id,security_name,transaction_amount,transaction_date,market_code,market_name,load_timestamp,job_id
12345,ACME CORP,1500.50,2024-01-15,NYSE,New York Stock Exchange,2024-01-15T10:30:00Z,job-abc-123
12346,TECH INC,2000.75,2024-01-16,NASDAQ,NASDAQ Exchange,2024-01-16T10:30:00Z,job-abc-123
```

### Quality Tables
```sql
-- staging_errors table
error_id | record_id | field_name | error_type | error_message | raw_value | timestamp | job_id
err-001  | rec-123   | amount     | FORMAT     | Invalid numeric format | "abc" | 2024-01-15T10:30:00Z | job-abc-123

-- job_lineage table
job_id | target_table | execution_time | records_loaded | dataset_version | mapping_version | is_idempotent_load
job-abc-123 | transactions | 2024-01-15T10:30:00Z | 1000 | v1_1705323000000 | m1_1705323000000 | false
```

## Testing

### Run All Tests
```bash
mvn test
```

### Test Coverage
- Agent unit tests: Scout, Cartographer, Navigator, Alchemist
- Service tests: CSVService, BigQueryService
- Integration tests: ConductorAgent orchestration

### Sample Test Assertions

```java
// Schema inference
assertEquals("NUMERIC", field.getInferredType());
assertTrue(field.getConfidenceScore() > 0.95);

// Field mapping
assertEquals("SECURITY_ID", mapping.getTargetField());
assertTrue(mapping.isKeyField());

// Deduplication
assertTrue(dedupConfig.isEnabled());
assertTrue(dedupConfig.getKeyFields().contains("id"));

// Quality scoring
assertTrue(dqScore >= 0 && dqScore <= 100);
assertTrue(complianceScore >= 0 && complianceScore <= 100);
```

## Advanced Topics

### Custom Transformation Rules
Extend `TransformationSpec` with domain-specific operations:
```java
spec.getCleaningRules().add(
    new CleaningRule("currency_amount")
        .addOperation("PARSE_CURRENCY")
        .addOperation("CONVERT_TO_USD")
        .setNullHandling("ZERO")
);
```

### Anomaly Detection
Implement statistical analysis in Auditor:
```java
double mean = calculateMean(values);
double stdDev = calculateStdDev(values);
List<Anomaly> outliers = detectOutliers(values, mean, stdDev, 3.0);
```

### CI/CD Integration
Deploy with Cloud Functions:
```bash
gcloud functions deploy etl-trigger \
  --runtime java17 \
  --trigger-topic gs-upload
```

## Troubleshooting

### Schema Not Discovered
- Check CSV format and encoding (UTF-8 required)
- Verify sample size (minimum 2 rows recommended)
- Review logs for type inference confidence

### Field Mapping Conflicts
- Check canonical model definitions
- Verify standard code lookups are available
- Review confidence thresholds

### Load Failures
- Verify BigQuery table exists
- Check schema compatibility
- Review staging_errors table

### Performance Issues
- Adjust sample sizes in agents
- Consider partitioning for large tables
- Enable parallel processing in Dataflow

## Architecture Extensibility

### Adding New Agents
1. Implement `ETLAgent` interface
2. Add to `ConductorAgent.agents` list
3. Update execution order
4. Add corresponding tests

### Custom Validators
Extend validation rules in `FieldMapping`:
```java
ValidationRule rule = new ValidationRule(
    "CUSTOM_TYPE",
    "my_validation_logic",
    "Custom validation failed"
);
```

### Storage Backends
Replace GCS with alternative storage:
```java
public interface DataSourceProvider {
    InputStream getDataStream(String path);
}
```

## Monitoring & Observability

### Logging
- All agents log via SLF4J
- Configurable log levels (DEBUG, INFO, WARN, ERROR)
- Structured lineage logs

### Metrics
- Record count tracking
- Duration per agent
- Error rates
- Data quality scores

### Audit Trail
- Complete job lineage
- Version tracking
- Error staging
- Reasoning logs

## License

Apache 2.0

## Support

For issues or questions, refer to design.txt or contact the development team.

