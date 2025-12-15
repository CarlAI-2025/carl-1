terraform {
  required_version = ">= 1.0"
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.0"
    }
  }
}

variable "project_id" {
  description = "GCP Project ID"
  type        = string
}

variable "region" {
  description = "GCP Region"
  type        = string
  default     = "us-central1"
}

variable "dataset_id" {
  description = "BigQuery Dataset ID"
  type        = string
  default     = "etl_pipeline_dataset"
}

provider "google" {
  project = var.project_id
  region  = var.region
}

# BigQuery Dataset
resource "google_bigquery_dataset" "etl_dataset" {
  dataset_id    = var.dataset_id
  friendly_name = "ETL Pipeline Dataset"
  description   = "Dataset for ETL agent pipeline outputs"
  location      = "US"

  default_table_expiration_ms = 7776000000 # 90 days
}

# Main transactions table
resource "google_bigquery_table" "transactions" {
  dataset_id = google_bigquery_dataset.etl_dataset.dataset_id
  table_id   = "transactions"

  schema = jsonencode([
    {
      name        = "security_id"
      type        = "STRING"
      mode        = "REQUIRED"
      description = "Security identifier"
    },
    {
      name        = "security_name"
      type        = "STRING"
      description = "Security name"
    },
    {
      name        = "transaction_amount"
      type        = "NUMERIC"
      description = "Transaction amount"
    },
    {
      name        = "transaction_date"
      type        = "DATE"
      description = "Transaction date"
    },
    {
      name        = "market_code"
      type        = "STRING"
      description = "Market code"
    },
    {
      name        = "market_name"
      type        = "STRING"
      description = "Market name"
    },
    {
      name        = "load_timestamp"
      type        = "TIMESTAMP"
      description = "Load timestamp"
    },
    {
      name        = "job_id"
      type        = "STRING"
      description = "ETL job ID"
    }
  ])

  time_partitioning {
    type          = "DAY"
    field         = "transaction_date"
    require_partition_filter = true
  }

  clustering = ["market_code"]
}

# Staging errors table
resource "google_bigquery_table" "staging_errors" {
  dataset_id = google_bigquery_dataset.etl_dataset.dataset_id
  table_id   = "staging_errors"

  schema = jsonencode([
    {
      name = "error_id"
      type = "STRING"
    },
    {
      name = "record_id"
      type = "STRING"
    },
    {
      name = "field_name"
      type = "STRING"
    },
    {
      name = "error_type"
      type = "STRING"
    },
    {
      name = "error_message"
      type = "STRING"
    },
    {
      name = "raw_value"
      type = "STRING"
    },
    {
      name = "timestamp"
      type = "TIMESTAMP"
    },
    {
      name = "job_id"
      type = "STRING"
    }
  ])

  time_partitioning {
    type  = "DAY"
    field = "timestamp"
  }
}

# Job lineage table
resource "google_bigquery_table" "job_lineage" {
  dataset_id = google_bigquery_dataset.etl_dataset.dataset_id
  table_id   = "job_lineage"

  schema = jsonencode([
    {
      name = "job_id"
      type = "STRING"
    },
    {
      name = "target_table"
      type = "STRING"
    },
    {
      name = "execution_time"
      type = "TIMESTAMP"
    },
    {
      name = "records_loaded"
      type = "INT64"
    },
    {
      name = "dataset_version"
      type = "STRING"
    },
    {
      name = "mapping_version"
      type = "STRING"
    },
    {
      name = "is_idempotent_load"
      type = "BOOL"
    }
  ])
}

# Cloud Storage bucket for CSV uploads
resource "google_storage_bucket" "etl_data_bucket" {
  name          = "${var.project_id}-etl-data"
  location      = var.region
  force_destroy = false

  uniform_bucket_level_access = true

  lifecycle_rule {
    action {
      type = "Delete"
    }
    condition {
      age = 90 # Delete after 90 days
    }
  }
}

# Service account for ETL pipeline
resource "google_service_account" "etl_sa" {
  account_id   = "etl-pipeline"
  display_name = "ETL Pipeline Service Account"
}

# IAM roles for service account
resource "google_project_iam_member" "etl_bq_admin" {
  project = var.project_id
  role    = "roles/bigquery.admin"
  member  = "serviceAccount:${google_service_account.etl_sa.email}"
}

resource "google_project_iam_member" "etl_storage_admin" {
  project = var.project_id
  role    = "roles/storage.admin"
  member  = "serviceAccount:${google_service_account.etl_sa.email}"
}

resource "google_project_iam_member" "etl_logging" {
  project = var.project_id
  role    = "roles/logging.logWriter"
  member  = "serviceAccount:${google_service_account.etl_sa.email}"
}

# Cloud Scheduler job for daily execution
resource "google_cloud_scheduler_job" "etl_daily_trigger" {
  name            = "etl-pipeline-daily"
  description     = "Daily ETL pipeline trigger"
  schedule        = "0 2 * * *"  # 2 AM UTC daily
  time_zone       = "America/New_York"
  attempt_deadline = "600s"

  pubsub_target {
    topic_name = google_pubsub_topic.etl_trigger.id
    data       = base64encode(jsonencode({
      action = "etl"
      source = "scheduler"
    }))
  }
}

# Pub/Sub topic for event-driven execution
resource "google_pubsub_topic" "etl_trigger" {
  name = "etl-trigger-topic"
}

# Pub/Sub subscription
resource "google_pubsub_subscription" "etl_trigger_sub" {
  name             = "etl-trigger-subscription"
  topic            = google_pubsub_topic.etl_trigger.name
  ack_deadline_seconds = 60

  dead_letter_policy {
    dead_letter_topic = google_pubsub_topic.etl_dlq.id
    max_delivery_attempts = 5
  }
}

# Dead-letter topic for failed messages
resource "google_pubsub_topic" "etl_dlq" {
  name = "etl-trigger-dlq"
}

# Outputs
output "dataset_id" {
  value       = google_bigquery_dataset.etl_dataset.dataset_id
  description = "BigQuery Dataset ID"
}

output "bucket_name" {
  value       = google_storage_bucket.etl_data_bucket.name
  description = "GCS bucket name for input data"
}

output "service_account_email" {
  value       = google_service_account.etl_sa.email
  description = "Service account email for ETL pipeline"
}

