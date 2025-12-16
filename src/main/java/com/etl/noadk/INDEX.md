````markdown
# Non-ADK ETL Pipeline - Documentation Index

## Quick Navigation

### 📖 Getting Started
- **[README.keep.md](README.keep.md)** - Project overview and package structure
- **[ARCHITECTURE.md](ARCHITECTURE.md)** - System design and component descriptions
- **[CONFIGURATION.md](CONFIGURATION.md)** - Configuration options and settings

### 🛠️ Operations
- **[DEPLOYMENT.md](DEPLOYMENT.md)** - Deployment guides for all platforms
- **[DEVELOPMENT.md](DEVELOPMENT.md)** - Development, testing, and debugging guides

### 📋 External References
- **[design.txt](../../design.txt)** - Original design specifications
- **[application-noadk.properties](../../src/main/resources/application-noadk.properties)** - Configuration file template

---

## Documentation by Role

### 👨‍💼 Project Managers / Business Analysts
Start here:
1. [README.keep.md](README.keep.md) - Understand what this package does
2. [ARCHITECTURE.md](ARCHITECTURE.md#agent-execution-flow) - See how it works
3. [design.txt](../../design.txt) - Review original specifications

### 👨‍💻 Developers
Start here:
1. [DEVELOPMENT.md](DEVELOPMENT.md) - Set up development environment
2. [ARCHITECTURE.md](ARCHITECTURE.md) - Understand the design
3. [README.keep.md](README.keep.md) - Know the components
4. [CONFIGURATION.md](CONFIGURATION.md) - Understand configuration

### 🔧 DevOps / Platform Engineers
Start here:
1. [DEPLOYMENT.md](DEPLOYMENT.md) - Choose your deployment method
2. [CONFIGURATION.md](CONFIGURATION.md) - Configure for your environment
3. [ARCHITECTURE.md](ARCHITECTURE.md) - Understand components and dependencies

### 🧪 QA / Testing Engineers
Start here:
1. [DEVELOPMENT.md](DEVELOPMENT.md#testing) - Testing approaches
2. [DEVELOPMENT.md](DEVELOPMENT.md#sample-data-generation) - Generate test data
3. [ARCHITECTURE.md](ARCHITECTURE.md) - Understand data flow

---

## Common Tasks

### Task: Deploy to Production
1. Review [DEPLOYMENT.md - GCP Cloud Run Deployment](DEPLOYMENT.md#gcp-cloud-run-deployment)
2. Follow [CONFIGURATION.md - Production Configuration](CONFIGURATION.md#production-configuration)
3. Verify using [DEPLOYMENT.md - Deployment Verification](DEPLOYMENT.md#deployment-verification)

### Task: Add a New Feature
1. Read [ARCHITECTURE.md - Agent Pattern](ARCHITECTURE.md#agent-pattern)
2. Follow [DEVELOPMENT.md - Adding a New Agent](DEVELOPMENT.md#adding-a-new-agent)
3. Write tests per [DEVELOPMENT.md - Testing](DEVELOPMENT.md#testing)

### Task: Debug Issues
1. Enable debugging: [DEVELOPMENT.md - Enable Debug Logging](DEVELOPMENT.md#enable-debug-logging)
2. Check logs and [ARCHITECTURE.md - Error Handling](ARCHITECTURE.md#error-handling-strategy)
3. Use [DEPLOYMENT.md - Troubleshooting](DEPLOYMENT.md#troubleshooting-deployments)

### Task: Set Up Local Development
1. Follow [DEVELOPMENT.md - IDE Setup](DEVELOPMENT.md#ide-setup)
2. Execute [DEVELOPMENT.md - Quick Start for Development](DEVELOPMENT.md#quick-start-for-development)
3. Run tests: [DEVELOPMENT.md - Run Tests](DEVELOPMENT.md#run-tests)

### Task: Configure for Different Environment
1. Check [CONFIGURATION.md - Environment-Specific Configuration](CONFIGURATION.md#environment-specific-configuration)
2. Modify [CONFIGURATION.md - Agent-Specific Configuration](CONFIGURATION.md#agent-specific-configuration) as needed
3. Verify using [DEVELOPMENT.md - Debugging](DEVELOPMENT.md#debugging)

---

## Document Descriptions

### README.keep.md
**Purpose:** Overview and orientation
**Length:** ~300 lines
**Key Sections:**
- Overview and key characteristics
- Package structure
- Architecture comparison
- When to use Non-ADK
- Usage instructions
- References

### ARCHITECTURE.md
**Purpose:** Technical design and implementation details
**Length:** ~400 lines
**Key Sections:**
- Architecture principles
- Core components (ETLAgent, PipelineJob, Domain Models)
- Agent execution flow (detailed steps)
- Error handling strategy
- BigQuery integration
- State transitions
- Performance considerations

### CONFIGURATION.md
**Purpose:** How to configure the system
**Length:** ~600 lines
**Key Sections:**
- Quick start configuration
- Configuration file structure
- Agent-specific settings
- Data quality settings
- Environment-specific profiles
- Configuration validation
- Best practices

### DEPLOYMENT.md
**Purpose:** How to deploy to various environments
**Length:** ~700 lines
**Key Sections:**
- Building the application
- Local development
- Docker deployment
- Cloud Run
- Compute Engine
- Cloud Functions
- Kubernetes
- On-premise
- Monitoring

### DEVELOPMENT.md
**Purpose:** Development, testing, and debugging
**Length:** ~800 lines
**Key Sections:**
- Environment setup
- Local development
- Unit and integration tests
- Debugging techniques
- Performance testing
- Sample data generation
- Code quality
- CI/CD integration
- Common tasks

---

## Architecture Overview

```
┌─────────────────────────────────────────────────┐
│     ETL Pipeline - Non-ADK Implementation       │
├─────────────────────────────────────────────────┤
│                                                 │
│  ┌──────────────────────────────────────────┐  │
│  │  ConductorAgent (Orchestrator)           │  │
│  │  - Manages workflow                      │  │
│  │  - Handles retries                       │  │
│  │  - Tracks lineage                        │  │
│  └────────────┬─────────────────────────────┘  │
│               │                                │
│  ┌────────────┴──────────────────────────────┐ │
│  │ Sequential Agent Execution               │ │
│  ├──────────────────────────────────────────┤ │
│  │                                          │ │
│  │  1. Scout Agent (Ingestion)              │ │
│  │     ↓                                    │ │
│  │  2. Cartographer Agent (Schema)          │ │
│  │     ↓                                    │ │
│  │  3. Navigator Agent (Mapping)            │ │
│  │     ↓                                    │ │
│  │  4. Alchemist Agent (Transformation)     │ │
│  │     ↓                                    │ │
│  │  5. Architect Agent (SQL Generation)     │ │
│  │     ↓                                    │ │
│  │  6. Auditor Agent (Quality)              │ │
│  │                                          │ │
│  └──────────────────────────────────────────┘ │
│                                                 │
│  ┌──────────────────────────────────────────┐  │
│  │  GCP Services Integration                │  │
│  ├──────────────────────────────────────────┤  │
│  │  - BigQuery (storage, execution)         │  │
│  │  - Cloud Storage (data staging)          │  │
│  │  - Cloud Logging (audit trail)           │  │
│  │  - Vertex AI (optional, AI-powered)      │  │
│  └──────────────────────────────────────────┘  │
│                                                 │
└─────────────────────────────────────────────────┘
```

---

## Agent Pipeline Data Flow

```
GCS Input File
    ↓
┌─────────────────────────────────┐
│ Scout Agent                     │
│ - Validate format               │
│ - Extract statistics            │
│ → File Info + Samples           │
└────────────┬────────────────────┘
             ↓
┌─────────────────────────────────┐
│ Cartographer Agent              │
│ - Infer schema from samples     │
│ - Detect schema drift           │
│ → Schema Contract               │
└────────────┬────────────────────┘
             ↓
┌─────────────────────────────────┐
│ Navigator Agent                 │
│ - Map source to target fields   │
│ - Add standard identifiers      │
│ → Field Mappings                │
└────────────┬────────────────────┘
             ↓
┌─────────────────────────────────┐
│ Alchemist Agent                 │
│ - Generate transformation rules │
│ - Deduplication, normalization  │
│ → Transformation Specs          │
└────────────┬────────────────────┘
             ↓
┌─────────────────────────────────┐
│ Architect Agent                 │
│ - Generate BigQuery SQL         │
│ - Optimize for performance      │
│ → SQL Scripts                   │
└────────────┬────────────────────┘
             ↓
┌─────────────────────────────────┐
│ Execute in BigQuery             │
│ - Load transformed data         │
│ - Record lineage                │
└────────────┬────────────────────┘
             ↓
┌─────────────────────────────────┐
│ Auditor Agent                   │
│ - Calculate DQ metrics          │
│ - Compliance scoring            │
│ → Quality Report                │
└────────────┬────────────────────┘
             ↓
BigQuery Tables + Lineage Tracked
```

---

## Key Concepts

### PipelineJob
Central state object that flows through all agents. Contains:
- Job metadata (id, status, timestamps)
- Source and target information
- Inferred schema and field mappings
- Transformation rules
- Error records
- Execution lineage

### ETLAgent Interface
All agents implement this interface:
- `getName()` - Agent identifier
- `getDescription()` - Agent purpose
- `initialize(job)` - Preparation phase
- `execute(job)` - Execution phase
- `cleanup()` - Cleanup phase

### SchemaContract
Represents inferred schema with confidence scores. Enables:
- Type inference validation
- Schema drift detection
- Backward compatibility checking

### BigQuery Integration
Four lineage/tracking tables:
- `job_lineage` - Execution history
- `staging_errors` - Failed records
- `schema_versions` - Schema evolution
- `mapping_history` - Mapping changes

---

## Troubleshooting Quick Reference

### Common Issues
| Problem | Documentation |
|---------|---|
| Build fails | [DEVELOPMENT.md - Troubleshooting](DEVELOPMENT.md#troubleshooting-development-issues) |
| Tests fail | [DEVELOPMENT.md - Testing](DEVELOPMENT.md#testing) |
| Deployment fails | [DEPLOYMENT.md - Troubleshooting](DEPLOYMENT.md#troubleshooting-deployments) |
| Performance issues | [ARCHITECTURE.md - Performance](ARCHITECTURE.md#performance-considerations) |
| Data quality issues | [ARCHITECTURE.md - Error Handling](ARCHITECTURE.md#error-handling-strategy) |

---

## Related Documentation

### ADK Implementation
For comparison with ADK-based approach:
- [ADK_PURE_ARCHITECTURE.md](../../ADK_PURE_ARCHITECTURE.md)
- [CLEAN_ARCHITECTURE_SUMMARY.md](../../CLEAN_ARCHITECTURE_SUMMARY.md)

### Deployment Infrastructure
- [deploy-to-gcp.sh](../../deploy-to-gcp.sh)
- [Dockerfile](../../Dockerfile)
- [docker-compose.yml](../../docker-compose.yml)
- [terraform/main.tf](../../terraform/main.tf)

### Design & Specifications
- [design.txt](../../design.txt)
- [PROJECT_ORGANIZATION.md](../../PROJECT_ORGANIZATION.md)
- [IMPLEMENTATION_GUIDE.md](../../IMPLEMENTATION_GUIDE.md)

---

## Document Maintenance

### When to Update Documentation
- ✅ When adding new agents or services
- ✅ When changing configuration options
- ✅ When deploying to new platforms
- ✅ When fixing bugs that others may encounter
- ✅ When performance characteristics change

### How to Update
1. Make code changes
2. Update relevant documentation file
3. Update this INDEX if adding new sections
4. Include documentation changes in pull request

### Version History
| Date | Author | Changes |
|------|--------|---------|
| 2024-12-15 | DevTeam | Initial documentation for non-ADK approach |

---

## Support

### Getting Help
1. Check the [troubleshooting section](#troubleshooting-quick-reference) of relevant guide
2. Search within the documentation using keywords
3. Review the [ARCHITECTURE.md](ARCHITECTURE.md) for design understanding
4. Check logs in Cloud Logging for specific errors

### Contributing
Improvements to documentation are welcome! Please:
1. Follow the same markdown format
2. Keep sections focused and concise
3. Include examples where helpful
4. Update this INDEX when adding new documents

---

**Last Updated:** December 15, 2025
**Total Documentation:** ~3000 lines across 6 files
**Audience:** All stakeholders (business, developers, ops)
**Status:** Complete and maintained

````
