package com.etl.adk;

import com.google.cloud.vertexai.api.Tool;
import com.google.cloud.vertexai.api.FunctionDeclaration;
import com.google.cloud.vertexai.api.Schema;
import com.google.cloud.vertexai.api.Type;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * ADK Tool Registry for agent function calling.
 * Defines tools that agents can use to interact with ETL components.
 */
@Slf4j
public class ADKToolRegistry {

    private final Map<String, ADKTool> tools = new HashMap<>();

    public ADKToolRegistry() {
        initializeDefaultTools();
    }

    /**
     * Initialize default tools available to ADK agents.
     */
    private void initializeDefaultTools() {
        log.info("Initializing ADK Tool Registry with default tools");

        // Tool: Validate Schema
        addTool("validateSchema", "Validate a data schema for correctness and compatibility",
                new String[]{"schemaJson", "targetFormat"},
                "Validates schema structure and returns validation results");

        // Tool: Transform Data
        addTool("transformData", "Apply transformations to data records",
                new String[]{"recordJson", "transformationRules"},
                "Executes transformation rules on data and returns transformed record");

        // Tool: Check Data Quality
        addTool("checkDataQuality", "Analyze data quality metrics",
                new String[]{"recordJson", "qualityThreshold"},
                "Analyzes record against quality rules and returns quality score");

        // Tool: Detect Anomalies
        addTool("detectAnomalies", "Detect anomalies in data values",
                new String[]{"valueList", "dataType"},
                "Applies statistical analysis to detect outliers and anomalies");

        // Tool: Enrich Data
        addTool("enrichData", "Enrich data with lookups and transformations",
                new String[]{"recordJson", "enrichmentConfig"},
                "Adds enriched data fields based on configuration");

        // Tool: Load to BigQuery
        addTool("loadToBigQuery", "Load data to BigQuery table",
                new String[]{"dataJson", "datasetId", "tableId"},
                "Loads data to specified BigQuery table and returns status");
    }

    /**
     * Add a tool to the registry.
     */
    public void addTool(String name, String description, String[] parameters, String resultDescription) {
        ADKTool tool = new ADKTool(name, description, parameters, resultDescription);
        tools.put(name, tool);
        log.debug("Registered tool: {} with {} parameters", name, parameters.length);
    }

    /**
     * Get a tool by name.
     */
    public ADKTool getTool(String name) {
        return tools.get(name);
    }

    /**
     * Get all registered tools.
     */
    public Collection<ADKTool> getAllTools() {
        return tools.values();
    }

    /**
     * Convert tools to Vertex AI Tool format for agent function calling.
     */
    public Tool convertToVertexAITool() {
        Tool.Builder toolBuilder = Tool.newBuilder();

        for (ADKTool tool : tools.values()) {
            FunctionDeclaration.Builder funcBuilder = FunctionDeclaration.newBuilder()
                    .setName(tool.getName())
                    .setDescription(tool.getDescription());

            // Create parameters schema
            Schema.Builder paramsSchema = Schema.newBuilder()
                    .setType(Type.OBJECT);

            for (String param : tool.getParameters()) {
                // Each parameter is a string type for simplicity
                paramsSchema.putProperties(param, Schema.newBuilder()
                        .setType(Type.STRING)
                        .setDescription("Parameter: " + param)
                        .build());
            }

            funcBuilder.setParameters(paramsSchema.build());
            toolBuilder.addFunctionDeclarations(funcBuilder.build());
        }

        return toolBuilder.build();
    }

    /**
     * ADK Tool definition.
     */
    public static class ADKTool {
        private final String name;
        private final String description;
        private final String[] parameters;
        private final String resultDescription;

        public ADKTool(String name, String description, String[] parameters, String resultDescription) {
            this.name = name;
            this.description = description;
            this.parameters = parameters;
            this.resultDescription = resultDescription;
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public String[] getParameters() { return parameters; }
        public String getResultDescription() { return resultDescription; }

        @Override
        public String toString() {
            return String.format("Tool[name=%s, params=%d]", name, parameters.length);
        }
    }

    /**
     * Get tool count.
     */
    public int getToolCount() {
        return tools.size();
    }

    /**
     * Print registry summary.
     */
    public void printSummary() {
        log.info("ADK Tool Registry Summary:");
        log.info("Total Tools: {}", tools.size());
        for (ADKTool tool : tools.values()) {
            log.info("  - {}: {} (params: {})", tool.getName(), tool.getDescription(), tool.getParameters().length);
        }
    }
}

