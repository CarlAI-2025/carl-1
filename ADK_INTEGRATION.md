# Google ADK Integration - ETL Agent Pipeline

## Overview

The ETL Agent Pipeline now integrates **Google's Agent Development Kit (ADK)** using **Vertex AI Generative AI** capabilities. This enables intelligent, AI-powered agent orchestration for ETL pipelines.

## What is Google ADK?

Google Agent Development Kit (ADK) is a framework for building and deploying intelligent agents that can:
- Make autonomous decisions
- Call tools and APIs
- Process natural language
- Stream real-time responses
- Orchestrate complex workflows

## ADK Components in This Project

### 1. **ADKAgent Interface** (`ADKAgent.java`)
Base interface for ADK-based agents with:
- AI initialization
- Generative AI execution
- Tool calling capabilities

### 2. **VertexAIClient** (`VertexAIClient.java`)
Wrapper for Google Vertex AI Generative AI API:
- Gemini Pro model integration
- Content generation
- Streaming responses
- Error handling and logging

### 3. **ADKConductorAgent** (`ADKConductorAgent.java`)
Enhanced conductor using Vertex AI for:
- Pipeline analysis
- Transformation suggestions
- Data quality assessment
- Real-time optimization

### 4. **ADKToolRegistry** (`ADKToolRegistry.java`)
Manages tools available to agents:
- Schema validation
- Data transformation
- Quality checking
- Anomaly detection
- Data enrichment
- BigQuery loading

## Key Features

### Intelligent Decision Making
```java
// Use Vertex AI to analyze and optimize pipelines
ADKConductorAgent conductor = new ADKConductorAgent(projectId, location);
conductor.initialize();
conductor.analyzeAndOptimizePipeline(job);
```

### Tool-Based Function Calling
```java
// ADK agents can call predefined tools
Tool tool = registry.getTool("validateSchema");
// Agent makes intelligent decisions about which tool to use
```

### Streaming AI Responses
```java
// Real-time feedback from Vertex AI
conductor.streamPipelineOptimization(job);
```

### AI-Powered Recommendations
```java
// Get suggestions for data transformations
String suggestions = conductor.suggestTransformations(source, target);

// Analyze data quality with AI
String qualityAnalysis = conductor.analyzeDataQuality(dataProfile);
```

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│              ETL Agent Pipeline with ADK                   │
└─────────────────────────────────────────────────────────────┘
                          │
        ┌─────────────────┼─────────────────┐
        │                 │                 │
   ┌────▼──────┐     ┌───▼──────┐     ┌───▼──────┐
   │  Agents   │     │  Tools   │     │ Vertex AI│
   │(Scout,    │────▶│Registry  │────▶│ Generative
   │ Cartogr..)│     │(6 tools) │     │ AI Model │
   └───────────┘     └──────────┘     │(Gemini)  │
        │                                  │      │
        │                                  │      │
        ▼                                  ▼      │
   ┌──────────┐                     ┌──────────┐ │
   │ADK       │                     │ Vertex AI│◀┘
   │Conductor │────────────────────▶│ API      │
   │Agent     │                     │Client    │
   └──────────┘                     └──────────┘
```

## Dependencies Added

```xml
<!-- Google ADK (Agent Development Kit) -->
<dependency>
    <groupId>com.google.cloud</groupId>
    <artifactId>google-cloud-vertexai</artifactId>
    <version>0.7.0</version>
</dependency>

<!-- Google AI Platform -->
<dependency>
    <groupId>com.google.cloud</groupId>
    <artifactId>google-cloud-aiplatform</artifactId>
    <version>3.36.0</version>
</dependency>

<!-- gRPC for agent communication -->
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-netty-shaded</artifactId>
    <version>1.59.0</version>
</dependency>
```

## Configuration

ADK settings are in `application-adk.properties`:

```properties
# Vertex AI
adk.vertexai.project-id=your-project
adk.vertexai.location=us-central1
adk.vertexai.model=gemini-pro

# Model Parameters
adk.model.temperature=0.7
adk.model.max-output-tokens=1024
adk.model.top-p=0.95

# Agent Settings
adk.agent.execution-timeout=300s
adk.agent.max-retries=3
```

## Usage Examples

### Example 1: Analyze Pipeline with AI
```java
ADKConductorAgent conductor = new ADKConductorAgent(projectId, location);
conductor.initialize();

PipelineJob job = new PipelineJob();
// ... populate job details ...

// Use Vertex AI to analyze and provide recommendations
conductor.analyzeAndOptimizePipeline(job);

conductor.shutdown();
```

### Example 2: Get AI Transformation Suggestions
```java
String sourceSchema = "{ 'id': 'STRING', 'date': 'STRING', 'amount': 'NUMERIC' }";
String targetSchema = "{ 'security_id': 'STRING', 'transaction_date': 'DATE', 'amount': 'DECIMAL' }";

String suggestions = conductor.suggestTransformations(sourceSchema, targetSchema);
System.out.println(suggestions);
// Output: "Apply CAST operations, parse dates using ISO format, ..."
```

### Example 3: Stream Real-Time AI Feedback
```java
// Get real-time optimization suggestions as they're generated
conductor.streamPipelineOptimization(job);
// Logs: "[INFO] Real-time AI feedback: Consider adding data validation..."
// Logs: "[INFO] Real-time AI feedback: Recommend enabling caching..."
```

### Example 4: Use Tool Registry
```java
ADKToolRegistry toolRegistry = new ADKToolRegistry();
toolRegistry.printSummary();

// Output:
// ADK Tool Registry Summary:
// Total Tools: 6
//   - validateSchema: Validate a data schema... (params: 2)
//   - transformData: Apply transformations... (params: 2)
//   - checkDataQuality: Analyze data quality... (params: 2)
//   - detectAnomalies: Detect anomalies in data... (params: 2)
//   - enrichData: Enrich data with lookups... (params: 2)
//   - loadToBigQuery: Load data to BigQuery... (params: 3)
```

## ADK Agent Capabilities

### 1. Schema Analysis
- Intelligent schema discovery
- Compatibility checking
- Type inference recommendations

### 2. Data Quality
- AI-driven anomaly detection
- Pattern recognition
- Quality threshold optimization

### 3. Transformation Planning
- Automatic transformation suggestions
- Optimization recommendations
- Data lineage analysis

### 4. Pipeline Optimization
- Performance analysis
- Bottleneck identification
- Resource optimization

### 5. Error Analysis
- Root cause analysis
- Recovery suggestions
- Preventive recommendations

## Security & Safety

ADK includes safety settings:
- Content filtering
- Harmful content blocking
- Bias detection

Configuration:
```properties
adk.safety.blocked-categories=HARASSMENT,HATE_SPEECH,SEXUALLY_EXPLICIT
adk.safety.threshold=BLOCK_ONLY_HIGH
```

## Advanced Features

### Streaming Responses
Real-time feedback from Vertex AI for long-running operations:
```java
conductor.streamPipelineOptimization(job);
```

### Function Calling
Agents can call predefined tools:
```java
Tool validationTool = toolRegistry.getTool("validateSchema");
// Agent autonomously decides to use this tool
```

### Model Configuration
Fine-tune Gemini model behavior:
- Temperature (0.0-1.0): Randomness
- Top-P: Diversity sampling
- Top-K: Token selection
- Max tokens: Output length

## Production Deployment

### Requirements
1. Google Cloud Project with Vertex AI enabled
2. Service account with Vertex AI access
3. Environment variable: `GCP_PROJECT_ID`

### Deployment
```bash
export GCP_PROJECT_ID=your-project-id
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/key.json

# Initialize ADK
ADKConductorAgent conductor = new ADKConductorAgent(projectId, "us-central1");
conductor.initialize();

# Run pipeline
conductor.analyzeAndOptimizePipeline(job);

# Cleanup
conductor.shutdown();
```

## Integration Points

### With Existing Agents
ADK is integrated with:
- Scout Agent - AI-powered file discovery
- Cartographer - Intelligent schema inference
- Navigator - Smart field mapping
- Alchemist - AI transformation suggestions
- Auditor - AI-based quality analysis

### With BigQuery
- Direct BigQuery integration via Tool
- Query optimization via AI
- Performance recommendations

### With Cloud Services
- Vertex AI Generative AI API
- Cloud Logging
- Cloud Storage

## Best Practices

1. **Initialize Once**: Create ADKConductorAgent once and reuse
2. **Error Handling**: Always wrap Vertex AI calls in try-catch
3. **Streaming**: Use streaming for long operations to get real-time feedback
4. **Tool Selection**: Let agents choose appropriate tools autonomously
5. **Monitoring**: Log all AI interactions for audit trails

## Troubleshooting

### Issue: "Vertex AI client not initialized"
Solution: Call `conductor.initialize()` before using

### Issue: "Access denied to Vertex AI"
Solution: Verify GCP credentials and IAM permissions

### Issue: "Model not found: gemini-pro"
Solution: Ensure Vertex AI API is enabled in GCP project

## Future Enhancements

1. Multi-agent coordination
2. Custom model fine-tuning
3. Advanced reasoning loops
4. Chain-of-thought reasoning
5. Agent memory and context management

---

**ADK Integration Status:** ✅ COMPLETE
**Vertex AI Model:** Gemini Pro
**Tool Count:** 6 predefined tools
**Streaming:** Enabled
**Version:** 1.0.0

