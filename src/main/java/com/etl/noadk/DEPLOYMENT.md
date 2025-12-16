````markdown
# Non-ADK Deployment Guide

## Overview

This guide covers deploying the traditional, non-ADK based ETL pipeline to various environments: local development, Docker, Cloud Run, Compute Engine, and on-premise servers.

## Prerequisites

### System Requirements
- **Java:** JDK 11 or higher
- **Maven:** 3.6+
- **Docker:** (optional, for containerized deployment)
- **gcloud CLI:** (for GCP deployments)

### GCP Requirements
- Google Cloud Project with enabled APIs:
  - BigQuery API
  - Cloud Storage API
  - Cloud Logging API
  - Vertex AI API (if using AI features)
- Service account with roles:
  - `roles/bigquery.admin`
  - `roles/storage.admin`
  - `roles/logging.logWriter`
  - `roles/aiplatform.user` (if using Vertex AI)

## Building the Application

### Clean Build
```bash
# Navigate to project root
cd /path/to/carl-1

# Build JAR
mvn clean package

# JAR location: target/carl-1-1.0-SNAPSHOT.jar
```

### Build with Specific Profile
```bash
# Development profile
mvn clean package -Pdev

# Production profile
mvn clean package -Pprod
```

### Verify Build
```bash
# Check JAR contents
jar tf target/carl-1-1.0-SNAPSHOT.jar | head -20

# Run help
java -jar target/carl-1-1.0-SNAPSHOT.jar --help
```

## Local Development Deployment

### Quick Start (Linux/Mac)
```bash
# 1. Set GCP credentials
export GOOGLE_APPLICATION_CREDENTIALS=$HOME/.config/gcloud/application_default_credentials.json

# 2. Set GCP project
export GCP_PROJECT_ID=my-project-id

# 3. Set data bucket
export GCS_BUCKET=my-bucket
export GCS_OBJECT=data/sample.csv

# 4. Create BigQuery dataset
bq mk --dataset \
  --description "ETL Pipeline Dataset" \
  my-project-id:etl_dataset

# 5. Run application
java -jar target/carl-1-1.0-SNAPSHOT.jar
```

### With Custom Properties File
```bash
# Create local properties file
cp src/main/resources/application-noadk.properties \
   application-local.properties

# Edit configuration
vim application-local.properties

# Run with custom properties
java -Dspring.config.location=file:./application-local.properties \
  -jar target/carl-1-1.0-SNAPSHOT.jar
```

### Debug Mode
```bash
# Enable debug logging
java -Dlogging.level.com.etl=DEBUG \
  -jar target/carl-1-1.0-SNAPSHOT.jar
```

## Docker Deployment

### Build Docker Image
```bash
# Create Dockerfile
cat > Dockerfile << 'EOF'
FROM openjdk:11-jre-slim

# Install gcloud SDK (optional)
# RUN apt-get update && apt-get install -y google-cloud-cli

WORKDIR /app

COPY target/carl-1-1.0-SNAPSHOT.jar app.jar
COPY src/main/resources/application-noadk.properties .

ENTRYPOINT ["java", "-jar", "app.jar"]
EOF

# Build image
docker build -t etl-pipeline-noadk:1.0 .

# Test locally
docker run -e GCP_PROJECT_ID=my-project \
  -e GCS_BUCKET=my-bucket \
  -v ~/.config/gcloud:/root/.config/gcloud \
  etl-pipeline-noadk:1.0
```

### Docker Compose (for local testing with multiple services)
```yaml
version: '3.8'

services:
  etl-pipeline:
    build: .
    image: etl-pipeline-noadk:1.0
    environment:
      GCP_PROJECT_ID: ${GCP_PROJECT_ID}
      GCS_BUCKET: ${GCS_BUCKET}
      BQ_DATASET: etl_dataset
      PIPELINE_MAX_RETRIES: 3
    volumes:
      - ~/.config/gcloud:/root/.config/gcloud
      - ./logs:/app/logs
    networks:
      - etl-network

  # Optional: Local BigQuery emulator
  bigquery-emulator:
    image: ghcr.io/goccy/bigquery-emulator:latest
    ports:
      - "9050:9050"
    networks:
      - etl-network

networks:
  etl-network:
    driver: bridge
```

Run with Docker Compose:
```bash
docker-compose up
```

### Push to Container Registry
```bash
# Configure Docker for GCR
gcloud auth configure-docker gcr.io

# Tag image
docker tag etl-pipeline-noadk:1.0 \
  gcr.io/my-project-id/etl-pipeline-noadk:1.0

# Push to GCR
docker push gcr.io/my-project-id/etl-pipeline-noadk:1.0
```

## GCP Cloud Run Deployment

### Deploy from Source
```bash
# Simple deployment
gcloud run deploy etl-pipeline-noadk \
  --source . \
  --runtime java11 \
  --entry-point com.etl.noadk.ETLPipelineMain \
  --region us-central1 \
  --memory 2Gi \
  --cpu 2 \
  --timeout 1800s \
  --set-env-vars GCP_PROJECT_ID=my-project-id,GCS_BUCKET=my-bucket
```

### Deploy from Container Image
```bash
# Push image first (see Docker section)
gcloud run deploy etl-pipeline-noadk \
  --image gcr.io/my-project-id/etl-pipeline-noadk:1.0 \
  --region us-central1 \
  --memory 2Gi \
  --cpu 2 \
  --timeout 1800s
```

### Configure Service Account
```bash
# Create service account
gcloud iam service-accounts create etl-pipeline \
  --display-name "ETL Pipeline Service Account"

# Grant roles
gcloud projects add-iam-policy-binding my-project-id \
  --member serviceAccount:etl-pipeline@my-project-id.iam.gserviceaccount.com \
  --role roles/bigquery.admin

gcloud projects add-iam-policy-binding my-project-id \
  --member serviceAccount:etl-pipeline@my-project-id.iam.gserviceaccount.com \
  --role roles/storage.admin

gcloud projects add-iam-policy-binding my-project-id \
  --member serviceAccount:etl-pipeline@my-project-id.iam.gserviceaccount.com \
  --role roles/logging.logWriter

# Use with Cloud Run
gcloud run deploy etl-pipeline-noadk \
  --service-account etl-pipeline@my-project-id.iam.gserviceaccount.com \
  --source .
```

### Cloud Run with Secrets
```bash
# Create secret in Secret Manager
echo "gs://my-bucket" | gcloud secrets create etl-gcs-bucket

# Reference in Cloud Run
gcloud run deploy etl-pipeline-noadk \
  --update-secrets=GCS_BUCKET=etl-gcs-bucket:latest \
  --source .
```

### Monitor Cloud Run Service
```bash
# View logs
gcloud run logs read etl-pipeline-noadk --limit 50

# View metrics
gcloud monitoring time-series list \
  --filter 'resource.type="cloud_run_revision"'

# View invocations
gcloud run describe etl-pipeline-noadk \
  --region us-central1
```

## GCP Compute Engine Deployment

### Create VM Instance
```bash
# Create instance
gcloud compute instances create etl-pipeline-vm \
  --zone us-central1-a \
  --machine-type n1-standard-2 \
  --image-family debian-11 \
  --image-project debian-cloud \
  --scopes bigquery,cloud-platform,storage-rw

# Connect
gcloud compute ssh etl-pipeline-vm --zone us-central1-a
```

### Setup on Compute Engine
```bash
# SSH into VM
gcloud compute ssh etl-pipeline-vm

# Install Java
sudo apt-get update
sudo apt-get install -y openjdk-11-jdk

# Create directory
mkdir -p /opt/etl-pipeline
cd /opt/etl-pipeline

# Copy application
gsutil cp gs://my-bucket/carl-1-1.0-SNAPSHOT.jar .

# Copy properties
gsutil cp gs://my-bucket/application-noadk.properties .

# Create systemd service
sudo tee /etc/systemd/system/etl-pipeline.service > /dev/null <<EOF
[Unit]
Description=ETL Pipeline Service
After=network.target

[Service]
Type=simple
User=etl
WorkingDirectory=/opt/etl-pipeline
ExecStart=/usr/bin/java -jar carl-1-1.0-SNAPSHOT.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# Enable and start service
sudo systemctl daemon-reload
sudo systemctl enable etl-pipeline
sudo systemctl start etl-pipeline

# Check status
sudo systemctl status etl-pipeline
```

### Scheduled Execution (Cloud Scheduler)
```bash
# Create Cloud Scheduler job
gcloud scheduler jobs create http etl-daily \
  --location us-central1 \
  --schedule "0 2 * * *" \
  --uri "https://us-central1-my-project.cloudfunctions.net/etl-trigger" \
  --http-method POST \
  --message-body '{"dataset": "sales", "file": "data/sales.csv"}'

# Test job
gcloud scheduler jobs run etl-daily --location us-central1

# Pause job
gcloud scheduler jobs pause etl-daily --location us-central1

# Resume job
gcloud scheduler jobs resume etl-daily --location us-central1
```

## Cloud Functions Deployment

### HTTP Trigger
```python
# main.py
import os
import json
from google.cloud import run_v2

def etl_pipeline_trigger(request):
    """HTTP Cloud Function to trigger ETL pipeline."""
    try:
        request_json = request.get_json()
        dataset = request_json.get('dataset', 'default')
        
        # Call Java application
        import subprocess
        result = subprocess.run([
            'java', '-jar',
            '/workspace/carl-1-1.0-SNAPSHOT.jar'
        ], capture_output=True, text=True, timeout=1800)
        
        return {
            'status': 'success',
            'dataset': dataset,
            'output': result.stdout
        }, 200
    except Exception as e:
        return {'error': str(e)}, 500

# requirements.txt
google-cloud-run==0.1.0
google-cloud-logging==3.0.0
```

Deploy:
```bash
gcloud functions deploy etl-trigger \
  --runtime python39 \
  --trigger-http \
  --allow-unauthenticated \
  --timeout 1800
```

## Kubernetes Deployment

### Create Deployment
```yaml
# etl-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: etl-pipeline-noadk
  labels:
    app: etl-pipeline
spec:
  replicas: 1
  selector:
    matchLabels:
      app: etl-pipeline
  template:
    metadata:
      labels:
        app: etl-pipeline
    spec:
      serviceAccountName: etl-pipeline
      containers:
      - name: etl-pipeline
        image: gcr.io/my-project-id/etl-pipeline-noadk:1.0
        imagePullPolicy: IfNotPresent
        resources:
          requests:
            memory: "2Gi"
            cpu: "1"
          limits:
            memory: "4Gi"
            cpu: "2"
        env:
        - name: GCP_PROJECT_ID
          valueFrom:
            configMapKeyRef:
              name: etl-config
              key: project-id
        - name: GCS_BUCKET
          valueFrom:
            configMapKeyRef:
              name: etl-config
              key: gcs-bucket
        - name: GOOGLE_APPLICATION_CREDENTIALS
          value: /var/secrets/google/key.json
        volumeMounts:
        - name: google-cloud-key
          mountPath: /var/secrets/google
      volumes:
      - name: google-cloud-key
        secret:
          secretName: etl-gcp-key

---
apiVersion: v1
kind: ServiceAccount
metadata:
  name: etl-pipeline
```

Deploy:
```bash
# Create namespace
kubectl create namespace etl

# Create secret
kubectl create secret generic etl-gcp-key \
  --from-file=key.json=/path/to/service-account-key.json \
  -n etl

# Create ConfigMap
kubectl create configmap etl-config \
  --from-literal=project-id=my-project-id \
  --from-literal=gcs-bucket=my-bucket \
  -n etl

# Deploy
kubectl apply -f etl-deployment.yaml -n etl

# Check status
kubectl get pods -n etl
kubectl logs -n etl -f deployment/etl-pipeline-noadk
```

## On-Premise Deployment

### CentOS/RHEL Setup
```bash
# Install Java
sudo yum install -y java-11-openjdk

# Create user
sudo useradd -m etl

# Create directories
sudo mkdir -p /opt/etl-pipeline /var/log/etl-pipeline
sudo chown etl:etl /opt/etl-pipeline /var/log/etl-pipeline

# Copy application
sudo cp carl-1-1.0-SNAPSHOT.jar /opt/etl-pipeline/

# Create systemd service
sudo tee /etc/systemd/system/etl-pipeline.service > /dev/null <<'EOF'
[Unit]
Description=ETL Pipeline
After=network.target

[Service]
Type=simple
User=etl
WorkingDirectory=/opt/etl-pipeline
EnvironmentFile=/opt/etl-pipeline/etl.env
ExecStart=/usr/bin/java -jar carl-1-1.0-SNAPSHOT.jar
StandardOutput=journal
StandardError=journal
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# Create environment file
sudo tee /opt/etl-pipeline/etl.env > /dev/null <<'EOF'
GCP_PROJECT_ID=my-project-id
GCS_BUCKET=my-bucket
GOOGLE_APPLICATION_CREDENTIALS=/opt/etl-pipeline/service-account-key.json
EOF

# Enable service
sudo systemctl daemon-reload
sudo systemctl enable etl-pipeline
sudo systemctl start etl-pipeline
```

### Linux Cron for Scheduling
```bash
# Create cron job
sudo crontab -e

# Add entry (run daily at 2 AM)
0 2 * * * /opt/etl-pipeline/run.sh

# Create run script
cat > /opt/etl-pipeline/run.sh << 'EOF'
#!/bin/bash
cd /opt/etl-pipeline
source etl.env
java -jar carl-1-1.0-SNAPSHOT.jar >> /var/log/etl-pipeline/run.log 2>&1
EOF

chmod +x /opt/etl-pipeline/run.sh
```

## Deployment Verification

### Test Deployment
```bash
# 1. Check service is running
curl http://localhost:8080/health

# 2. Check logs
kubectl logs -f deployment/etl-pipeline-noadk

# 3. Verify BigQuery tables created
bq ls -t my-project-id:etl_dataset

# 4. Check recent executions
bq query --use_legacy_sql=false \
  'SELECT * FROM `my-project-id.etl_dataset.job_lineage` ORDER BY timestamp DESC LIMIT 10'
```

### Monitoring

#### Cloud Monitoring Dashboard
```bash
# Create dashboard
gcloud monitoring dashboards create --config-from-file=- << 'EOF'
{
  "displayName": "ETL Pipeline Dashboard",
  "mosaicLayout": {
    "columns": 12,
    "tiles": [
      {
        "width": 6,
        "height": 4,
        "widget": {
          "title": "Pipeline Execution Time",
          "xyChart": {
            "dataSets": [{
              "timeSeriesQuery": {
                "timeSeriesFilter": {
                  "filter": "metric.type=\"custom.googleapis.com/etl/execution_time_seconds\"",
                  "aggregation": {"alignmentPeriod": "60s"}
                }
              }
            }]
          }
        }
      }
    ]
  }
}
EOF
```

#### Custom Metrics
```java
// Log custom metrics
MetricServiceClient metricClient = MetricServiceClient.create();
List<TimeSeries> timeSeries = new ArrayList<>();

Metric metric = Metric.newBuilder()
    .setType("custom.googleapis.com/etl/execution_time_seconds")
    .putLabels("job_id", jobId)
    .build();

Point point = Point.newBuilder()
    .setInterval(TimeInterval.newBuilder()
        .setEndTime(Timestamp.newBuilder().setSeconds(Instant.now().getEpochSecond()))
        .build())
    .setValue(TypedValue.newBuilder()
        .setDoubleValue(executionTimeSeconds)
        .build())
    .build();

timeSeries.add(TimeSeries.newBuilder()
    .setMetric(metric)
    .addPoints(point)
    .build());

CreateTimeSeriesRequest request = CreateTimeSeriesRequest.newBuilder()
    .setName(ProjectName.of(projectId).toString())
    .addAllTimeSeries(timeSeries)
    .build();

metricClient.createTimeSeries(request);
```

## Troubleshooting Deployments

| Issue | Symptoms | Solution |
|-------|----------|----------|
| Authentication Failure | "Permission denied" errors | Verify service account roles, check GOOGLE_APPLICATION_CREDENTIALS |
| Out of Memory | "java.lang.OutOfMemoryError" | Increase memory limit, reduce batch-size |
| Timeout | "Execution timed out" | Increase timeout-seconds in configuration |
| BigQuery Access | "Access Denied" to dataset | Verify service account has bigquery.admin role |
| GCS Access | "Bucket not found" | Verify bucket exists and service account can access |

---

**Last Updated:** December 15, 2025
**Audience:** DevOps Engineers, Platform Engineers
**Related:** `CONFIGURATION.md` (Configuration), `ARCHITECTURE.md` (Architecture)

````
