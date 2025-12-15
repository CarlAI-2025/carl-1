# Pure ADK-Based ETL Pipeline - Clean Architecture

## Overview

This is a production-ready, **pure ADK-based ETL pipeline** built on Google's Agent Development Kit, using Vertex AI (Gemini Pro) for intelligent agent orchestration. All components are implemented as ADK agents following the `design.txt` specifications.

## Architecture

### Agent Pipeline (from design.txt)

```
Conductor (Root Orchestrator)
    ↓
Scout (Ingestion)
    ↓
Cartographer (Schema Inference)
    ↓
Navigator (Field Mapping)
    ↓
Alchemist (Transformation)
    ↓
Architect (SQL Generation)
    ↓
Auditor (Quality Assessment)
```

### Key Design Principles

✅ **Pure ADK Architecture** - All agents extend ADKBaseAgent interface
✅ **Vertex AI Integration** - Each agent uses Gemini Pro for reasoning
✅ **GCP Services** - BigQuery, Cloud Storage, Dataflow, Cloud Logging
✅ **Stateful Orchestration** - Conductor maintains job state and lineage
✅ **Retry Logic** - Exponential backoff for resilience
✅ **Lineage Tracking** - Dataset and mapping versioning

## File Structure

### Core Framework
```
src/main/java/com/etl/agent/adk/core/
├── ADKBaseAgent.java           # Base interface for all agents
└── VertexAIClientCore.java     # Unified Vertex AI client
```

### Agents
```
src/main/java/com/etl/agent/adk/agents/
├── ConductorADKAgent.java      # Root orchestrator
├── ScoutADKAgent.java          # Ingestion agent
├── CartographerADKAgent.java   # Schema inference
├── NavigatorADKAgent.java      # Field mapping
├── AlchemistADKAgent.java      # Transformation rules
├── ArchitectADKAgent.java      # SQL generation
└── AuditorADKAgent.java        # Quality assessment
```

### Main Entry Point
```
src/main/java/com/etl/agent/adk/
└── ADKPipelineMain.java        # Production entry point
```

## Agent Responsibilities

### Scout Agent
- **Input**: GCS file path
- **Output**: File analysis (format, row count, sample data)
- **Uses Vertex AI**: For intelligent format validation and statistics

### Cartographer Agent
- **Input**: File analysis from Scout
- **Output**: Schema contract (JSON) with confidence scores
- **Uses Vertex AI**: For type inference and drift detection

### Navigator Agent
- **Input**: Schema contract from Cartographer
- **Output**: Field mapping spec with standard codes (FIGI, ISIN, MIC, CFI)
- **Uses Vertex AI**: For semantic mapping and conflict resolution

### Alchemist Agent
- **Input**: Field mappings from Navigator
- **Output**: Transformation rules (cleaning, dedup, enrichment)
- **Uses Vertex AI**: For intelligent transformation logic

### Architect Agent
- **Input**: Transformation rules from Alchemist
- **Output**: BigQuery SQL scripts for data load
- **Uses Vertex AI**: For optimized SQL generation

### Auditor Agent
- **Input**: Load results from Architect
- **Output**: DQ scorecard, compliance score, reasoning log
- **Uses Vertex AI**: For quality assessment and recommendations

### Conductor Agent (Orchestrator)
- **Responsibilities**:
  1. Owns end-to-end workflow
  2. Routes tasks to agents in order
  3. Maintains job state
  4. Handles retries with exponential backoff
  5. Enforces execution ordering
  6. Records lineage (dataset_version, mapping_version)
  7. Manages state transitions

## Quick Start

### Prerequisites
```bash
export GCP_PROJECT_ID=your-project-id
export GCP_LOCATION=us-central1
export GCS_BUCKET=your-bucket
export GCS_OBJECT=data/input.csv
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account-key.json
```

### Build
```bash
mvn clean package
```

### Run
```bash
java -cp target/carl-1-1.0-SNAPSHOT.jar \
  com.etl.agent.adk.ADKPipelineMain
```

## Configuration

### Environment Variables
```
GCP_PROJECT_ID    - Google Cloud Project ID (required)
GCP_LOCATION      - Vertex AI location (default: us-central1)
GCS_BUCKET        - Input data bucket (default: {PROJECT_ID}-etl-data)
GCS_OBJECT        - Input data path (default: data/input.csv)
```

### Vertex AI Model
- **Default Model**: gemini-pro
- **Configuration**: Managed by VertexAIClientCore

## GCP Services Integration

### BigQuery
- Schema discovery and validation
- SQL generation and execution
- Lineage table storage (job_lineage, staging_errors)
- DQ scorecard persistence

### Cloud Storage (GCS)
- Input data ingestion (Scout Agent)
- CSV/JSON file handling
- Direct file read integration

### Dataflow/Beam
- Optional: Generate Dataflow templates (Architect Agent)
- For large-scale parallel processing

### Cloud Logging
- Agent execution logs
- Error tracking and audit trail
- Performance metrics

### Vertex AI
- Generative AI (Gemini Pro)
- Agent reasoning and decision-making
- Natural language generation for outputs

## Execution Flow

```
1. Main: Create job ID and initialize Conductor
2. Conductor: Initialize all agents
3. Conductor -> Scout: Read and analyze CSV
4. Scout: Use Gemini to validate format, generate stats
5. Conductor -> Cartographer: Infer schema
6. Cartographer: Use Gemini to detect types, output contract
7. Conductor -> Navigator: Map fields
8. Navigator: Use Gemini to recommend standards
9. Conductor -> Alchemist: Generate transformation rules
10. Alchemist: Use Gemini to create cleaning logic
11. Conductor -> Architect: Generate SQL
12. Architect: Use Gemini to create BigQuery SQL
13. Conductor -> Auditor: Assess quality
14. Auditor: Use Gemini to generate DQ report
15. Conductor: Record lineage (dataset_version, mapping_version)
16. Main: Return final report with execution summary
```

## Error Handling

### Retry Logic
- **Max Retries**: 3 attempts per agent
- **Backoff Strategy**: Exponential (2^n seconds)
- **Fallback**: Fail fast on critical errors

### State Management
- Job state tracked in jobState map
- Execution history maintained
- Failed agents prevent downstream execution

## Lineage Tracking

Each execution records:
- **Job ID**: Unique identifier (ETL_XXXXXXXX)
- **Dataset Version**: v{timestamp}
- **Mapping Version**: m{timestamp}
- **Agent Statuses**: Individual agent completion status
- **Processing Duration**: End-to-end execution time

## Output

Final report includes:
```json
{
  "jobId": "ETL_12345678",
  "status": "COMPLETED",
  "datasetVersion": "v1702649834",
  "mappingVersion": "m1702649834",
  "agentStatuses": {
    "scout": "COMPLETED",
    "cartographer": "COMPLETED",
    "navigator": "COMPLETED",
    "alchemist": "COMPLETED",
    "architect": "COMPLETED",
    "auditor": "COMPLETED"
  }
}
```

## Deployment to GCP

### Cloud Functions
```bash
gcloud functions deploy etl-pipeline \
  --runtime java17 \
  --trigger-topic etl-trigger \
  --entry-point com.etl.agent.adk.ADKPipelineMain
```

### Cloud Run
```bash
gcloud run deploy etl-pipeline \
  --image gcr.io/PROJECT/etl-pipeline:latest \
  --memory 1024Mi \
  --timeout 600
```

### Dataflow
Use Architect Agent output to deploy Beam pipelines.

## Security

- Service account with BigQuery, Storage permissions
- IAM roles: Viewer, BigQuery Admin, Storage Admin
- Credentials via GOOGLE_APPLICATION_CREDENTIALS
- No hardcoded credentials

## Monitoring

- Agent execution times logged
- Error messages captured
- Lineage table for audit trail
- Cloud Logging integration

## Extensibility

### Adding Custom Agents
1. Extend ADKBaseAgent interface
2. Implement execute() method
3. Add to ConductorADKAgent
4. Integrate into pipeline order

### Custom Vertex AI Models
Modify VertexAIClientCore to use different models:
```java
// Change from gemini-pro to other models
this.vertexAI = new VertexAIClientCore(projectId, location, "other-model");
```

## Status

✅ **Production Ready**
- All agents implemented
- GCP integration complete
- Error handling robust
- Lineage tracking enabled
- Pure ADK architecture

---

**Version**: 1.0.0
**Architecture**: Pure ADK-based
**Deployment**: GCP-native
**Vertex AI Model**: Gemini Pro

