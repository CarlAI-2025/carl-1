# ETL Agent Pipeline - Quick Reference

## Build & Run

```bash
# Build
mvn clean install

# Run tests
mvn test

# Package
mvn clean package -DskipTests

# Execute pipeline
java -cp target/carl-1-1.0-SNAPSHOT.jar \
  com.etl.agent.ETLPipelineMain gs://bucket/file.csv dataset table

# With logging
java -Dorg.slf4j.simpleLogger.defaultLogLevel=DEBUG \
  -cp target/carl-1-1.0-SNAPSHOT.jar \
  com.etl.agent.ETLPipelineMain gs://bucket/file.csv dataset table
```

## Docker

```bash
# Build image
docker build -t etl-pipeline:latest .

# Run locally
docker-compose up

# Push to registry
docker tag etl-pipeline:latest gcr.io/PROJECT/etl-pipeline:latest
docker push gcr.io/PROJECT/etl-pipeline:latest
```

## GCP Deployment

```bash
# Deploy to Cloud Functions
gcloud functions deploy etl-pipeline \
  --runtime java17 \
  --trigger-topic etl-trigger \
  --memory 512MB \
  --timeout 300

# Deploy to Cloud Run
gcloud run deploy etl-pipeline \
  --image gcr.io/PROJECT/etl-pipeline:latest \
  --memory 1024Mi \
  --timeout 300

# Deploy to Cloud Scheduler
gcloud scheduler jobs create pubsub etl-daily \
  --schedule="0 2 * * *" \
  --topic etl-trigger \
  --message-body='{"action":"etl"}'
```

## Infrastructure (Terraform)

```bash
cd terraform

# Initialize
terraform init

# Plan
terraform plan -var="project_id=my-project"

# Apply
terraform apply -var="project_id=my-project"

# Destroy
terraform destroy -var="project_id=my-project"
```

## BigQuery Queries

```sql
-- Check job execution history
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

-- Check data quality
SELECT 
  job_id,
  error_type,
  COUNT(*) as error_count
FROM `project.dataset.staging_errors`
WHERE timestamp >= CURRENT_TIMESTAMP() - INTERVAL 24 HOUR
GROUP BY job_id, error_type
ORDER BY error_count DESC;

-- Analyze transactions
SELECT 
  DATE(transaction_date) as date,
  COUNT(*) as record_count,
  SUM(transaction_amount) as total_amount,
  AVG(transaction_amount) as avg_amount
FROM `project.dataset.transactions`
GROUP BY 1
ORDER BY 1 DESC;
```

## Agent Execution Order

1. **Scout** - Ingestion & basic validation
2. **Cartographer** - Schema discovery & type inference
3. **Navigator** - Field mapping to canonical model
4. **Alchemist** - Transformation rules generation
5. **Architect** - SQL generation & pipeline creation
6. **Auditor** - Quality scoring & audit trail

## Key Classes & Methods

```java
// Create pipeline job
PipelineJob job = new PipelineJob();
job.setSourcePath("gs://bucket/file.csv");
job.setTargetDataset("etl_dataset");
job.setTargetTable("transactions");

// Execute pipeline
ConductorAgent conductor = new ConductorAgent();
conductor.executePipeline(job);

// Check status
System.out.println(job.getStatus()); // COMPLETED, FAILED, etc.
System.out.println(job.getLineage()); // Execution lineage

// Access results
job.getStatistics().getTotalRecordsRead();
job.getStatistics().getTotalRecordsLoaded();
job.getStatistics().getTotalRecordsRejected();

// Error handling
for (PipelineJob.ErrorRecord error : job.getErrors()) {
    System.out.println(error.getErrorMessage());
}
```

## Configuration

### Environment Variables
```bash
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/key.json
export GCP_PROJECT_ID=my-project
export JAVA_TOOL_OPTIONS="-Xmx1024m"
export DATASET_ID=etl_dataset
```

### Log Levels
```bash
DEBUG   # Detailed execution traces
INFO    # General information
WARN    # Warnings (recoverable issues)
ERROR   # Errors (non-fatal)
```

## Testing

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ScoutAgentTest

# Run with coverage
mvn clean test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Schema not discovered | Check CSV encoding (UTF-8), verify samples |
| Type inference low confidence | Increase sample size, check data consistency |
| Field mapping conflicts | Check canonical model, verify confidence thresholds |
| Load failures | Verify BigQuery table exists, check schema compatibility |
| Performance issues | Adjust sample sizes, enable table partitioning |
| Credential errors | Set GOOGLE_APPLICATION_CREDENTIALS, verify IAM roles |

## File Locations

| Component | Location |
|-----------|----------|
| Agents | `src/main/java/com/etl/agent/agents/` |
| Models | `src/main/java/com/etl/agent/domain/` |
| Services | `src/main/java/com/etl/agent/services/` |
| Tests | `src/test/java/com/etl/agent/` |
| Build Config | `pom.xml` |
| Docker | `Dockerfile`, `docker-compose.yml` |
| Infrastructure | `terraform/main.tf` |
| CI/CD | `.github/workflows/build-and-deploy.yml` |
| Docs | `README.md`, `IMPLEMENTATION_GUIDE.md` |

## Common Patterns

```java
// Create agent
CartographerAgent agent = new CartographerAgent();

// Execute with error handling
try {
    agent.execute(job);
} catch (Exception e) {
    logger.error("Agent failed", e);
    job.setStatus(PipelineJob.JobStatus.FAILED);
}

// Check lineage
for (PipelineJob.LineageEntry entry : job.getLineage()) {
    System.out.printf("[%s] %s: %dms%n", 
        entry.getStep(), 
        entry.getAgentName(), 
        entry.getDurationMs());
}

// Generate sample data
SampleDataGenerator gen = new SampleDataGenerator();
String csvData = gen.generateSecurityTransactionsSample(1000);
```

## Performance Tips

1. **Increase sample size** for better type inference
2. **Use partitioning** for large BigQuery tables
3. **Enable clustering** for frequently filtered fields
4. **Adjust heap size** with `-Xmx` for large datasets
5. **Enable parallel** Dataflow processing for 100K+ records

## Resources

- **Documentation:** [README.md](README.md)
- **Implementation:** [IMPLEMENTATION_GUIDE.md](IMPLEMENTATION_GUIDE.md)
- **Design:** [design.txt](design.txt)
- **Contributing:** [CONTRIBUTING.md](CONTRIBUTING.md)
- **Summary:** [PROJECT_SUMMARY.md](PROJECT_SUMMARY.md)

## Support

1. Check documentation first
2. Search existing issues
3. Create new issue with template
4. Include logs and error messages
5. Specify tier level and system details

