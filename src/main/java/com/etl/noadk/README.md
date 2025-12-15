# Legacy Non-ADK ETL Implementation (com.etl.noadk)

## ⚠️ DEPRECATED - Use ADK Implementation Instead

This package contains the **legacy, non-ADK based ETL implementation** and is maintained for reference and backward compatibility purposes only.

**For new projects and deployments, please use the pure ADK-based implementation located in `com.etl.agent.adk`.**

## Package Structure

### Entry Point
- **ETLPipelineMain.java** - Legacy main entry point (deprecated)

### Domain Models (com.etl.noadk.domain)
- **PipelineJob.java** - Pipeline job state management
- **SchemaContract.java** - Schema inference contracts
- **FieldMapping.java** - Field mapping specifications
- **TransformationSpec.java** - Transformation rules
- **AnomalyReport.java** - Anomaly detection reports

### Agents (com.etl.noadk.agents)
- **ETLAgent.java** - Base interface for legacy agents
- **ConductorAgent.java** - Root orchestrator agent
- **ScoutAgent.java** - Data ingestion agent
- **CartographerAgent.java** - Schema inference agent
- **NavigatorAgent.java** - Field mapping agent
- **AlchemistAgent.java** - Transformation rules agent
- **ArchitectAgent.java** - Pipeline generation agent
- **AuditorAgent.java** - Quality assessment agent

### Services (com.etl.noadk.services)
- **CSVService.java** - CSV parsing and validation
- **BigQueryService.java** - BigQuery operations
- **AnomalyDetectionService.java** - Statistical anomaly detection
- **SampleDataGenerator.java** - Test data generation

## What's Different from ADK Implementation?

| Aspect | Non-ADK (Legacy) | ADK (New) |
|--------|------------------|-----------|
| **Base Interface** | Custom ETLAgent | ADKBaseAgent (standardized) |
| **Vertex AI Integration** | Individual agent calls | Unified VertexAIClientCore |
| **Architecture** | Traditional agent pattern | Pure ADK framework |
| **GCP Services** | Manual integration | Native GCP integration |
| **State Management** | PipelineJob model | AgentRequest/AgentResponse |
| **Deployment** | Cloud Functions + custom | Cloud Functions + standard ADK |

## Migration Path

If you're currently using the non-ADK implementation:

1. **Review** the ADK documentation: `ADK_PURE_ARCHITECTURE.md`
2. **Map** your existing logic to ADK agents
3. **Test** the new implementation locally
4. **Deploy** using the ADK entry point: `com.etl.agent.adk.ADKPipelineMain`

## Running Legacy Code

```bash
# Compile
mvn clean package

# Run (if needed for backward compatibility)
java -cp target/carl-1-1.0-SNAPSHOT.jar \
  com.etl.noadk.ETLPipelineMain \
  gs://bucket/data.csv \
  dataset_name \
  table_name
```

## References

- **New ADK Implementation**: `src/main/java/com/etl/agent/adk/`
- **Architecture Guide**: `ADK_PURE_ARCHITECTURE.md`
- **Quick Start**: `CLEAN_ARCHITECTURE_SUMMARY.md`
- **Deployment**: `deploy-to-gcp.sh`

## Status

🔴 **LEGACY - Maintenance Mode Only**

This implementation is no longer actively developed. Bug fixes only.

---

**Last Updated**: December 15, 2025
**Recommendation**: Migrate to ADK-based implementation in `com.etl.agent.adk`

