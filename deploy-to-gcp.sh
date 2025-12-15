#!/bin/bash

# ADK ETL Pipeline - GCP Deployment Script
# Deploys the pure ADK-based solution to Google Cloud Platform

set -e

echo "=========================================="
echo "ADK ETL Pipeline - GCP Deployment"
echo "=========================================="
echo ""

# Configuration
PROJECT_ID=${1:-"your-project-id"}
LOCATION=${2:-"us-central1"}
DATASET_ID=${3:-"etl_pipeline"}
BUCKET_NAME="${PROJECT_ID}-etl-data"
SERVICE_ACCOUNT="etl-pipeline-sa"

echo "Configuration:"
echo "  Project ID: $PROJECT_ID"
echo "  Location: $LOCATION"
echo "  Dataset ID: $DATASET_ID"
echo "  Bucket: $BUCKET_NAME"
echo "  Service Account: $SERVICE_ACCOUNT"
echo ""

# 1. Enable required APIs
echo "[1/8] Enabling Google Cloud APIs..."
gcloud services enable \
  bigquery.googleapis.com \
  storage-api.googleapis.com \
  cloudscheduler.googleapis.com \
  cloudfunctions.googleapis.com \
  cloudrun.googleapis.com \
  vertexai.googleapis.com \
  aiplatform.googleapis.com \
  --project=$PROJECT_ID

# 2. Create service account
echo "[2/8] Creating service account..."
gcloud iam service-accounts create $SERVICE_ACCOUNT \
  --display-name="ETL Pipeline Service Account" \
  --project=$PROJECT_ID 2>/dev/null || echo "Service account already exists"

# 3. Grant IAM roles
echo "[3/8] Granting IAM roles..."
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:${SERVICE_ACCOUNT}@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/bigquery.admin" \
  --quiet

gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:${SERVICE_ACCOUNT}@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/storage.admin" \
  --quiet

gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:${SERVICE_ACCOUNT}@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/aiplatform.user" \
  --quiet

# 4. Create GCS bucket
echo "[4/8] Creating GCS bucket..."
gsutil mb -l $LOCATION -p $PROJECT_ID gs://$BUCKET_NAME 2>/dev/null || echo "Bucket already exists"

# 5. Create BigQuery dataset
echo "[5/8] Creating BigQuery dataset..."
bq mk --dataset \
  --location=$LOCATION \
  --description="ETL Pipeline Dataset" \
  $PROJECT_ID:$DATASET_ID 2>/dev/null || echo "Dataset already exists"

# 6. Create BigQuery tables
echo "[6/8] Creating BigQuery tables..."

# Transactions table
bq mk --table \
  $PROJECT_ID:$DATASET_ID.transactions \
  security_id:STRING,security_name:STRING,transaction_amount:NUMERIC,transaction_date:DATE,market_code:STRING,market_name:STRING,load_timestamp:TIMESTAMP,job_id:STRING \
  2>/dev/null || echo "Transactions table already exists"

# Staging errors table
bq mk --table \
  $PROJECT_ID:$DATASET_ID.staging_errors \
  error_id:STRING,record_id:STRING,field_name:STRING,error_type:STRING,error_message:STRING,raw_value:STRING,timestamp:TIMESTAMP,job_id:STRING \
  2>/dev/null || echo "Staging errors table already exists"

# Job lineage table
bq mk --table \
  $PROJECT_ID:$DATASET_ID.job_lineage \
  job_id:STRING,target_table:STRING,execution_time:TIMESTAMP,records_loaded:INT64,dataset_version:STRING,mapping_version:STRING,is_idempotent_load:BOOL \
  2>/dev/null || echo "Job lineage table already exists"

# 7. Build application
echo "[7/8] Building application..."
mvn clean package -DskipTests -q

# 8. Deploy to Cloud Functions
echo "[8/8] Deploying to Cloud Functions..."
gcloud functions deploy etl-pipeline-prod \
  --runtime java17 \
  --trigger-topic etl-trigger \
  --entry-point com.etl.agent.adk.ADKPipelineMain \
  --memory 1024MB \
  --timeout 600 \
  --set-env-vars "GCP_PROJECT_ID=$PROJECT_ID,GCP_LOCATION=$LOCATION" \
  --service-account="${SERVICE_ACCOUNT}@${PROJECT_ID}.iam.gserviceaccount.com" \
  --project=$PROJECT_ID \
  --quiet

echo ""
echo "=========================================="
echo "✓ Deployment Complete!"
echo "=========================================="
echo ""
echo "Next steps:"
echo "1. Upload test data to: gs://$BUCKET_NAME/data/input.csv"
echo "2. Trigger pipeline: gcloud pubsub topics publish etl-trigger --message 'start'"
echo "3. Check results in BigQuery dataset: $DATASET_ID"
echo ""

