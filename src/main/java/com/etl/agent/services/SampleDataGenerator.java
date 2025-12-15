package com.etl.agent.services;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Service for generating sample test data and validating transformations.
 */
@Slf4j
public class SampleDataGenerator {
    private final Random random = new Random(42); // Deterministic seed

    /**
     * Generate sample security transaction CSV data.
     */
    public String generateSecurityTransactionsSample(int recordCount) {
        StringBuilder csv = new StringBuilder();
        csv.append("id,name,amount,transaction_date\n");

        String[] companies = {"ACME Corp", "TechVenture Inc", "Global Finance Ltd", "Digital Solutions", "Quantum Systems"};
        String[] dates = {"2024-01-15", "2024-01-16", "2024-01-17", "2024-01-18", "2024-01-19"};

        for (int i = 1; i <= recordCount; i++) {
            String id = String.format("%05d", i);
            String name = companies[random.nextInt(companies.length)];
            double amount = 500 + random.nextDouble() * 5000;
            String date = dates[random.nextInt(dates.length)];

            csv.append(String.format("%s,%s,%.2f,%s\n", id, name, amount, date));
        }

        log.info("Generated sample data with {} records", recordCount);
        return csv.toString();
    }

    /**
     * Generate sample with data quality issues (duplicates, nulls, invalid values).
     */
    public String generateDirtySecurityData(int recordCount) {
        StringBuilder csv = new StringBuilder();
        csv.append("id,name,amount,transaction_date\n");

        String[] companies = {"ACME Corp", "TechVenture Inc", "Global Finance Ltd", null, "Digital Solutions"};
        String[] dates = {"2024-01-15", "2024-01-16", "invalid-date", "2024-01-18", ""};

        for (int i = 1; i <= recordCount; i++) {
            String id = String.format("%05d", i % 50); // Create duplicates
            String name = companies[random.nextInt(companies.length)];
            double amount = random.nextDouble() > 0.9 ? -100 : (500 + random.nextDouble() * 5000); // Negative values

            String date = dates[random.nextInt(dates.length)];

            csv.append(id).append(",");
            csv.append(name != null ? name : "").append(",");
            csv.append(String.format("%.2f", amount)).append(",");
            csv.append(date).append("\n");
        }

        log.info("Generated dirty sample data with {} records", recordCount);
        return csv.toString();
    }

    /**
     * Generate sample with outliers for anomaly detection.
     */
    public String generateDataWithOutliers(int recordCount) {
        StringBuilder csv = new StringBuilder();
        csv.append("id,name,amount,transaction_date\n");

        String[] companies = {"ACME Corp", "TechVenture Inc", "Global Finance Ltd", "Digital Solutions"};
        String[] dates = {"2024-01-15", "2024-01-16", "2024-01-17", "2024-01-18"};

        for (int i = 1; i <= recordCount; i++) {
            String id = String.format("%05d", i);
            String name = companies[random.nextInt(companies.length)];

            // Generate outliers in 5% of records
            double amount;
            if (random.nextDouble() < 0.05) {
                amount = random.nextDouble() > 0.5 ? 50000 : 10; // Extreme values
            } else {
                amount = 500 + random.nextDouble() * 5000; // Normal range
            }

            String date = dates[random.nextInt(dates.length)];

            csv.append(String.format("%s,%s,%.2f,%s\n", id, name, amount, date));
        }

        log.info("Generated data with outliers, {} records", recordCount);
        return csv.toString();
    }

    /**
     * Generate BigQuery DDL for sample table.
     */
    public String generateTableDDL(String projectId, String datasetId, String tableId) {
        return String.format("""
                CREATE TABLE IF NOT EXISTS `%s.%s.%s` (
                  security_id STRING NOT NULL,
                  security_name STRING,
                  transaction_amount NUMERIC(18,2),
                  transaction_date DATE,
                  market_code STRING,
                  market_name STRING,
                  load_timestamp TIMESTAMP,
                  job_id STRING
                )
                PARTITION BY transaction_date
                CLUSTER BY market_code
                OPTIONS(
                  description='Security transactions loaded by ETL pipeline',
                  require_partition_filter=true
                );
                """, projectId, datasetId, tableId);
    }

    /**
     * Generate Cloud Scheduler configuration (JSON).
     */
    public String generateSchedulerConfig(String projectId, String topicName) {
        return String.format("""
                {
                  "schedule": "0 2 * * *",
                  "description": "Daily ETL pipeline trigger",
                  "timezone": "America/New_York",
                  "pubsub_target": {
                    "topic_name": "projects/%s/topics/%s",
                    "data": "eyJjYWN0aW9uIjogImV0bCIsICJzb3VyY2UiOiAiZ2NzIn0="
                  }
                }
                """, projectId, topicName);
    }

    /**
     * Generate Terraform configuration for infrastructure as code.
     */
    public String generateTerraformConfig(String projectId, String datasetId) {
        return """
                terraform {
                  required_providers {
                    google = {
                      source  = "hashicorp/google"
                      version = "~> 5.0"
                    }
                  }
                }
                
                provider "google" {
                  project = "%PROJECT_ID%"
                  region  = "us-central1"
                }
                
                resource "google_bigquery_dataset" "etl_dataset" {
                  dataset_id    = "%DATASET_ID%"
                  friendly_name = "ETL Pipeline Dataset"
                  description   = "Auto-generated by ETL Agent Pipeline"
                  location      = "US"
                  
                  access {
                    role          = "OWNER"
                    user_by_email = google_service_account.etl_sa.email
                  }
                }
                
                resource "google_service_account" "etl_sa" {
                  account_id   = "etl-pipeline-sa"
                  display_name = "ETL Pipeline Service Account"
                }
                
                resource "google_project_iam_member" "etl_bq_editor" {
                  project = "%PROJECT_ID%"
                  role    = "roles/bigquery.dataEditor"
                  member  = "serviceAccount:${google_service_account.etl_sa.email}"
                }
                """.replace("%PROJECT_ID%", projectId)
                   .replace("%DATASET_ID%", datasetId);
    }

    /**
     * Generate GitHub Actions workflow for CI/CD.
     */
    public String generateGitHubActionsWorkflow(String gcpProject, String artifactRepo) {
        return """
                name: ETL Pipeline Build & Deploy
                
                on:
                  push:
                    branches: [main]
                  pull_request:
                    branches: [main]
                
                jobs:
                  build-and-test:
                    runs-on: ubuntu-latest
                    
                    steps:
                    - uses: actions/checkout@v3
                    
                    - name: Set up JDK 17
                      uses: actions/setup-java@v3
                      with:
                        java-version: '17'
                        distribution: 'temurin'
                    
                    - name: Run tests
                      run: mvn clean test
                    
                    - name: Build package
                      run: mvn clean package -DskipTests
                    
                    - name: Upload artifacts
                      uses: actions/upload-artifact@v3
                      with:
                        name: etl-pipeline-jar
                        path: target/carl-1-1.0-SNAPSHOT.jar
                
                  deploy:
                    needs: build-and-test
                    runs-on: ubuntu-latest
                    if: github.ref == 'refs/heads/main'
                    
                    steps:
                    - uses: actions/checkout@v3
                    
                    - name: Authenticate to Google Cloud
                      uses: google-github-actions/auth@v1
                      with:
                        credentials_json: ${{ secrets.GCP_SA_KEY }}
                    
                    - name: Deploy to Cloud Functions
                      run: |
                        gcloud functions deploy etl-trigger \\
                          --runtime java17 \\
                          --trigger-topic gs-file-upload \\
                          --entry-point com.etl.agent.ETLPipelineMain
                """;
    }

    /**
     * Generate Docker image for containerized execution.
     */
    public String generateDockerfile() {
        return """
                FROM openjdk:17-slim
                
                WORKDIR /app
                
                # Copy JAR
                COPY target/carl-1-1.0-SNAPSHOT.jar /app/etl-pipeline.jar
                
                # Install gcloud SDK
                RUN apt-get update && apt-get install -y \\
                    curl \\
                    gnupg \\
                    lsb-release \\
                    && curl https://packages.cloud.google.com/apt/doc/apt-key.gpg | apt-key add - \\
                    && apt-get install -y google-cloud-cli
                
                # Set entrypoint
                ENTRYPOINT ["java", "-cp", "/app/etl-pipeline.jar", "com.etl.agent.ETLPipelineMain"]
                """;
    }

    /**
     * Generate Docker Compose for local testing.
     */
    public String generateDockerCompose() {
        return """
                version: '3.8'
                
                services:
                  etl-pipeline:
                    build: .
                    container_name: etl-agent-pipeline
                    environment:
                      - GOOGLE_APPLICATION_CREDENTIALS=/secrets/key.json
                      - JAVA_TOOL_OPTIONS=-Xmx512m
                    volumes:
                      - ${GOOGLE_APPLICATION_CREDENTIALS}:/secrets/key.json
                      - ./data:/data
                    command: >
                      java -cp /app/etl-pipeline.jar
                      com.etl.agent.ETLPipelineMain
                      gs://test-bucket/data.csv
                      test_dataset
                      test_table
                """;
    }
}

