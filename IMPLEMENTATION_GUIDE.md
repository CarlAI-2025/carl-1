# ETL Agent Pipeline - Implementation Guide

## Quick Start

### Local Development

1. **Clone and Build**
```bash
git clone <repo>
cd carl-1
mvn clean install
```

2. **Set up GCP Credentials**
```bash
export GOOGLE_APPLICATION_CREDENTIALS=~/.config/gcloud/application_default_credentials.json
gcloud auth application-default login
```

3. **Run Tests**
```bash
mvn test
```

4. **Execute Pipeline**
```bash
java -cp target/carl-1-1.0-SNAPSHOT.jar com.etl.agent.ETLPipelineMain \
  gs://your-bucket/data.csv etl_dataset transactions
```

### Docker Deployment

1. **Build Image**
```bash
docker build -t etl-pipeline:latest .
```

2. **Run Locally**
```bash
docker-compose up
```

3. **Deploy to Container Registry**
```bash
docker tag etl-pipeline:latest gcr.io/PROJECT_ID/etl-pipeline:latest
docker push gcr.io/PROJECT_ID/etl-pipeline:latest
```

### Infrastructure Setup with Terraform

1. **Initialize Terraform**
```bash
cd terraform
terraform init
```

2. **Plan Infrastructure**
```bash
terraform plan -var="project_id=your-project"
```

3. **Apply Configuration**
```bash
terraform apply -var="project_id=your-project"
```

## Architecture Deep Dive

### Agent Execution Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    ConductorAgent                           │
│                  (Orchestrator/Root)                        │
└────────────────────┬────────────────────────────────────────┘
                     │
        ┌────────────┼────────────┬────────────┬────────────┐
        │            │            │            │            │
        ▼            ▼            ▼            ▼            ▼
    ┌───────┐   ┌──────────┐ ┌────────────┐ ┌──────────┐ ┌────────┐
    │ Scout │   │Cartograph│ │ Navigator  │ │Alchemist │ │Architect
    │(Input)│──▶│(Discover)│▶│(Mapping)   │▶│(Transform)▶│(Generate
    └───────┘   └──────────┘ └────────────┘ └──────────┘ └────────┘
        │            │            │            │            │
        └────────────┼────────────┼────────────┼────────────┘
                     │
                     ▼
              ┌──────────────┐
              │   Auditor    │
              │(Quality/DQ)  │
              └──────────────┘
```

### Data Flow Through Pipeline

```
CSV Input (GCS)
    │
    ▼
Scout: Extract & Validate
    │ → Statistics, Fingerprint, Samples
    ▼
Cartographer: Infer Schema
    │ → Schema Contract with confidence scores
    ▼
Navigator: Map to Canonical Model
    │ → Field Mappings with rationale & standards
    ▼
Alchemist: Plan Transformations
    │ → Cleaning, Dedup, Enrichment, Aggregation specs
    ▼
Architect: Generate SQL
    │ → BigQuery DDL + executable SQL
    ▼
Load to BigQuery
    │
    ▼
Auditor: Quality Scoring & Audit Trail
    │
    ▼
Final Outputs (Tables, Lineage, Errors)
```

## Tier Implementation Details

### Bronze: Basic ETL

**Minimum Requirements:**
- CSV ingestion from GCS
- Schema autodetection
- Basic type inference
- BigQuery table creation
- Simple data load

**Key Classes:**
- `ScoutAgent` - CSV reading
- `CartographerAgent` - Type inference
- `BigQueryService` - DDL generation
- `ETLPipelineMain` - Entry point

**Running Bronze:**
```bash
mvn test -Dtest=ScoutAgentTest,CartographerAgentTest
mvn clean package
java -cp target/carl-1-1.0-SNAPSHOT.jar com.etl.agent.ETLPipelineMain \
  gs://bucket/file.csv etl_dataset table
```

### Silver: Quality Controls

**Additional Features:**
- Deduplication with survivorship rules
- Type coercion and normalization
- Validation error reporting
- Data quality scorecards

**Key Classes:**
- `AlchemistAgent` - Transformation rules
- `AuditorAgent` - Quality scoring
- `TransformationSpec` - Declarative rules
- `BigQueryService.createStagingErrorTable()`

**Quality Tables Created:**
- `staging_errors` - validation failures
- `job_lineage` - execution history

**Test Coverage:**
```bash
mvn test -Dtest=AlchemistAgentTest,AuditorAgentTest
```

### Gold: Anomaly Detection

**Advanced Capabilities:**
- Statistical outlier detection (Z-score, IQR)
- Distribution analysis (skewness, kurtosis)
- Missing value pattern detection
- Cardinality anomalies
- Suggested corrective transformations

**Key Classes:**
- `AnomalyDetectionService` - Statistical analysis
- `AnomalyReport` - Anomaly findings
- Enhanced `AuditorAgent` with anomaly logic

**Example Usage:**
```java
AnomalyDetectionService anomaly = new AnomalyDetectionService();
List<AnomalyReport> outliers = anomaly.detectOutliers("amount", values);
List<AnomalyReport> distribution = anomaly.detectDistributionAnomalies("amount", values);
```

**Test Coverage:**
```bash
mvn test -Dtest=AnomalyDetectionServiceTest
```

### Platinum: Auto SQL & CI/CD

**Enterprise Features:**
- Automatic BigQuery SQL generation
- Idempotent load patterns
- Version lineage tracking
- Dataflow template generation
- Cloud Scheduler integration
- GitHub Actions CI/CD
- Infrastructure as Code (Terraform)

**Key Classes:**
- `ArchitectAgent` - SQL generation
- `ConductorAgent` - Retry & state management
- `PipelineJob` - Complete lineage tracking

**Deployment:**
```bash
# Using Terraform
terraform apply -var="project_id=my-project"

# Using GitHub Actions (auto on push)
git push origin main

# Manual Cloud Functions deployment
gcloud functions deploy etl-pipeline \
  --runtime java17 \
  --trigger-topic etl-trigger
```

## Configuration & Customization

### Environment Variables

```bash
export PROJECT_ID=your-project
export DATASET_ID=etl_dataset
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/key.json
export JAVA_TOOL_OPTIONS="-Xmx1024m"
```

### Custom Validation Rules

Add to `FieldMapping`:
```java
FieldMapping mapping = new FieldMapping("field", "target", "STRING");
mapping.getValidationRules().add(
    new FieldMapping.ValidationRule(
        "CUSTOM",
        "custom_logic(value)",
        "Validation failed"
    )
);
```

### Custom Transformations

Extend `TransformationSpec`:
```java
TransformationSpec spec = new TransformationSpec();
spec.getCleaningRules().add(
    new TransformationSpec.CleaningRule("currency")
        .getOperations().add("PARSE_CURRENCY")
        .getOperations().add("CONVERT_TO_USD")
);
```

### Custom Aggregations

```java
TransformationSpec.AggregationRule agg = 
    new TransformationSpec.AggregationRule("amount", "SUM");
agg.getGroupByFields().add("date");
agg.getGroupByFields().add("category");
agg.setTargetFieldName("daily_category_total");
spec.getAggregations().add(agg);
```

## Monitoring & Operations

### Logging

Configure log level:
```bash
java -Dorg.slf4j.simpleLogger.defaultLogLevel=DEBUG \
  -cp target/carl-1-1.0-SNAPSHOT.jar \
  com.etl.agent.ETLPipelineMain gs://bucket/file.csv dataset table
```

### BigQuery Monitoring

Check job execution:
```sql
SELECT 
  job_id,
  target_table,
  records_loaded,
  dataset_version,
  mapping_version,
  execution_time
FROM `project.dataset.job_lineage`
ORDER BY execution_time DESC
LIMIT 10;
```

Check data quality:
```sql
SELECT 
  job_id,
  error_type,
  COUNT(*) as error_count,
  COUNT(DISTINCT field_name) as affected_fields
FROM `project.dataset.staging_errors`
WHERE timestamp >= CURRENT_TIMESTAMP() - INTERVAL 24 HOUR
GROUP BY job_id, error_type
ORDER BY error_count DESC;
```

### GCP Monitoring

Set up Cloud Monitoring:
```bash
gcloud monitoring dashboards create \
  --config-from-file=monitoring-dashboard.json
```

## Troubleshooting

### Issue: Schema Not Discovered
**Solution:**
1. Verify CSV format: `head -1 gs://bucket/file.csv`
2. Check sample size in CartographerAgent
3. Review logs: `--Dorg.slf4j.simpleLogger.defaultLogLevel=DEBUG`

### Issue: Field Mapping Conflicts
**Solution:**
1. Check canonical model in NavigatorAgent
2. Verify confidence thresholds
3. Review mapping rationale in output logs

### Issue: Load Failures
**Solution:**
1. Check BigQuery table exists
2. Verify schema compatibility
3. Review staging_errors table
4. Check job lineage for retry status

### Issue: Performance Degradation
**Solution:**
1. Adjust sample sizes in agents
2. Enable BigQuery table partitioning
3. Use clustering for large tables
4. Consider Dataflow for parallel processing

## Advanced Topics

### Extending with Custom Agents

1. Implement `ETLAgent` interface
2. Add to `ConductorAgent.agents` list
3. Update execution order
4. Add tests

```java
public class CustomAgent implements ETLAgent {
    @Override
    public String getName() {
        return "CustomAgent";
    }

    @Override
    public void execute(PipelineJob job) throws Exception {
        // Implementation
    }
}
```

### Integration with External Systems

Enhance `NavigatorAgent` for external lookups:
```java
public class ExternalLookupService {
    public String enrichWithFIGI(String securityId) {
        // Call FIGI API
        return figiClient.lookup(securityId);
    }
}
```

### Real-time Processing

Extend for streaming with Dataflow:
```bash
# Generate Dataflow template
ArchitectAgent architect = new ArchitectAgent();
String template = architect.generateDataflowTemplate();
// Submit to Dataflow
```

## Support & Resources

- **Documentation:** See README.md
- **Design:** See design.txt for agent specifications
- **Tests:** Run `mvn test` for comprehensive coverage
- **Issues:** Check GitHub Issues
- **Community:** Reach out to the team

## License & Attribution

Apache License 2.0 - See LICENSE file

