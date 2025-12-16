# Non-ADK ETL Implementation (com.etl.noadk)

## Overview

This package contains a **traditional agent-based ETL implementation** without the Google ADK framework. It provides a straightforward, custom agent architecture suitable for environments where ADK integration is not feasible or preferred.

**For new projects and full GCP integration, consider the pure ADK-based implementation in `com.etl.agent.adk`.**

## ✨ Key Characteristics

- **Custom Agent Framework**: Lightweight, custom ETLAgent interface
- **Vertex AI Optional**: Direct Vertex AI integration without ADK abstractions
- **Manual State Management**: PipelineJob model for explicit state control
- **GCP Service Integration**: BigQuery, Cloud Storage, Cloud Logging
- **Flexible Deployment**: Cloud Functions, Compute Engine, or on-premise
- **Standalone**: No external agent framework dependencies

## Package Structure

### Entry Point
- **ETLPipelineMain.java** - Main entry point for non-ADK pipeline

### Domain Models (com.etl.noadk.domain)
- **PipelineJob.java** - Pipeline job state management with full lifecycle control
- **SchemaContract.java** - Schema inference contracts with confidence scores
- **FieldMapping.java** - Field mapping specifications with validation rules
- **TransformationSpec.java** - Declarative transformation rules
- **AnomalyReport.java** - Anomaly detection reports for data quality

### Agents (com.etl.noadk.agents)
- **ETLAgent.java** - Custom base interface for all agents
- **ConductorAgent.java** - Root orchestrator agent (owns end-to-end workflow)
- **ScoutAgent.java** - Data ingestion agent (file analysis & validation)
- **CartographerAgent.java** - Schema inference agent (type detection)
- **NavigatorAgent.java** - Field mapping agent (standard codes: FIGI, ISIN, MIC, CFI)
- **AlchemistAgent.java** - Transformation rules agent (cleaning, dedup, enrichment)
- **ArchitectAgent.java** - Pipeline generation agent (SQL script creation)
- **AuditorAgent.java** - Quality assessment agent (DQ scoring & compliance)

### Services (com.etl.noadk.services)
- **CSVService.java** - CSV parsing, validation, and statistics
- **BigQueryService.java** - BigQuery schema management and SQL execution
- **AnomalyDetectionService.java** - Statistical anomaly detection on numeric data
- **SampleDataGenerator.java** - Test data generation for validation

## Architecture Comparison

| Aspect | Non-ADK | ADK |
|--------|---------|-----|
| **Base Interface** | Custom ETLAgent | ADKBaseAgent (standardized) |
| **Framework** | Custom lightweight | Google ADK framework |
| **Vertex AI Integration** | Direct calls | Unified VertexAIClientCore |
| **State Model** | PipelineJob | AgentRequest/AgentResponse |
| **Agent Pattern** | Traditional orchestration | ADK agent delegation |
| **Dependencies** | Minimal | ADK libraries required |
| **Deployment** | Flexible (any platform) | GCP-native deployment |
| **Learning Curve** | Easier (transparent logic) | Framework concepts required |

## Agent Pipeline

```
INPUT (CSV from GCS)
  ↓
Scout → File analysis (format validation, row count, sample stats)
  ↓
Cartographer → Schema contract (field types, confidence scores)
  ↓
Navigator → Field mappings (canonical model, standard identifiers)
  ↓
Alchemist → Transformation rules (cleaning, dedup, enrichment logic)
  ↓
Architect → BigQuery SQL scripts (executable DDL & DML)
  ↓
Auditor → Quality report (DQ score, compliance score, reasoning log)
  ↓
Conductor → Record lineage (dataset_version, mapping_version)
  ↓
OUTPUT (Data loaded to BigQuery, lineage tracked)
```

## GCP Services Integration

### Supported Services
- **BigQuery** - Schema storage, transformation SQL, lineage tables
- **Cloud Storage (GCS)** - Input data ingestion, metadata storage
- **Cloud Logging** - Structured execution logs and audit trails
- **Cloud Monitoring** - Metrics and alerting (optional)
- **Pub/Sub** - Event-driven triggers for data arrival (optional)
- **Cloud Scheduler** - Scheduled pipeline execution (optional)

### Authentication
Service account with required IAM roles:
- `roles/bigquery.admin` - BigQuery operations
- `roles/storage.objectViewer` - GCS read access
- `roles/storage.objectCreator` - GCS write access
- `roles/logging.logWriter` - Cloud Logging
- `roles/aiplatform.user` - Vertex AI (if enabled)

## Configuration

### Environment Variables
```bash
# GCP Configuration
export GCP_PROJECT_ID=your-project-id
export GCP_LOCATION=us-central1
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account-key.json

# Data Configuration
export GCS_BUCKET=your-etl-data-bucket
export GCS_OBJECT=data/input.csv
export BQ_DATASET=etl_dataset
export BQ_TABLE=data_records

# Pipeline Configuration
export PIPELINE_MAX_RETRIES=3
export PIPELINE_TIMEOUT_SECONDS=600
export USE_VERTEX_AI=true  # Set to false to skip Vertex AI reasoning
```

### Configuration File
See `application-noadk.properties` for detailed property settings:
- Agent timeouts and retry policies
- BigQuery table naming conventions
- CSV parsing options
- Quality thresholds
- Logging levels

## Usage

### Build
```bash
mvn clean package
```

### Run Local Development
```bash
java -cp target/carl-1-1.0-SNAPSHOT.jar \
  com.etl.noadk.ETLPipelineMain
```

### Run with Parameters
```bash
java -cp target/carl-1-1.0-SNAPSHOT.jar \
  -DGCP_PROJECT_ID=my-project \
  -DGCS_BUCKET=my-bucket \
  -DGCS_OBJECT=data/sales.csv \
  com.etl.noadk.ETLPipelineMain
```

### Deploy to Cloud Run
```bash
gcloud run deploy etl-pipeline-noadk \
  --source . \
  --runtime java11 \
  --entry-point com.etl.noadk.ETLPipelineMain \
  --env GCP_PROJECT_ID=your-project-id \
  --env GCS_BUCKET=your-bucket
```

## State Management

The non-ADK approach uses an explicit **PipelineJob** model to manage state:

```
Created → Validating → Mapping → Transforming → Loading → Complete/Failed
  ↓        ↓            ↓         ↓              ↓         ↓
  │        └─ Error recovery & retry logic ─────────────┘
  └─ Lineage tracking at each stage
```

Each agent updates the PipelineJob state, enabling:
- Step-level recovery on failure
- Clear audit trail
- Manual intervention points
- Partial reprocessing

## Error Handling

### Retry Strategy
- **Exponential Backoff**: 1s → 2s → 4s (configurable)
- **Max Retries**: 3 by default (configurable)
- **Graceful Degradation**: Partial failures don't stop pipeline

### Quality Gates
- Schema validation confidence thresholds
- Field mapping completeness checks
- Data quality scoring at Auditor stage
- Compliance gate before load

## Data Lineage & Audit

BigQuery tables for tracking:
- **job_lineage** - Job execution history with timestamps
- **staging_errors** - Rejected records with reasons
- **schema_versions** - Schema evolution tracking
- **mapping_history** - Field mapping changes over time

## When to Use Non-ADK

✅ **Good for**:
- Environments without ADK support
- Simple ETL workflows (1-3 agents)
- Custom agent orchestration logic
- Transparent, auditable code
- Existing non-ADK codebases
- On-premise deployments

❌ **Not ideal for**:
- Large-scale agent networks
- Heavy ADK framework usage
- New GCP-native projects
- Complex distributed orchestration

## Comparison with ADK Implementation

For a detailed comparison, see the **ADK implementation** in `src/main/java/com/etl/agent/adk/`:

| Feature | Non-ADK | ADK |
|---------|---------|-----|
| Type Inference | ✅ Manual rules | ✅ AI-powered |
| Schema Drift Detection | ✅ Statistical | ✅ AI-powered |
| Field Mapping | ✅ Rules-based | ✅ AI-powered with standards |
| Transformation Suggestions | ✅ Limited | ✅ AI-generated |
| SQL Generation | ✅ Template-based | ✅ Gemini-generated |
| Error Recovery | ✅ Retry logic | ✅ ADK-managed |

## References

### Documentation
- **Design Specification**: `design.txt`
- **ADK Implementation**: `ADK_PURE_ARCHITECTURE.md`
- **Clean Architecture**: `CLEAN_ARCHITECTURE_SUMMARY.md`
- **Configuration**: `application-noadk.properties`
- **Documentation Index**: `INDEX.md`

### Detailed Guides
- **Architecture Guide**: `ARCHITECTURE.md`
- **Configuration Guide**: `CONFIGURATION.md`
- **Deployment Guide**: `DEPLOYMENT.md`
- **Development Guide**: `DEVELOPMENT.md`

### Deployment
- **GCP Deployment Guide**: `deploy-to-gcp.sh`
- **Docker**: `Dockerfile`
- **Terraform**: `terraform/main.tf`

### Code
- **Non-ADK Agents**: `src/main/java/com/etl/noadk/agents/`
- **ADK Agents**: `src/main/java/com/etl/agent/adk/agents/`

## Support & Maintenance

### Current Status
🟢 **Active Development** - Non-ADK implementation actively maintained

### Contributing
See `CONTRIBUTING.md` for guidelines on extending or improving the non-ADK implementation.

---

**Last Updated**: December 15, 2025
**Ecosystem**: GCP (BigQuery, Cloud Storage, Cloud Logging)
**Framework**: Custom agent-based ETL
**Compatibility**: Java 11+
