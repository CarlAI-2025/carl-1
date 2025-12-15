FROM openjdk:17-slim

WORKDIR /app

# Install gcloud SDK and other dependencies
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    gnupg \
    lsb-release \
    ca-certificates \
    && curl https://packages.cloud.google.com/apt/doc/apt-key.gpg | apt-key add - \
    && echo "deb [signed-by=/usr/share/keyrings/cloud.google.gpg] https://packages.cloud.google.com/apt cloud-sdk main" | tee -a /etc/apt/sources.list.d/google-cloud-sdk.list \
    && apt-get update && apt-get install -y google-cloud-cli \
    && rm -rf /var/lib/apt/lists/*

# Copy application JAR
COPY target/carl-1-1.0-SNAPSHOT.jar /app/etl-pipeline.jar

# Set metadata
LABEL maintainer="ETL Pipeline Team"
LABEL description="ETL Agent Pipeline for BigQuery"
LABEL version="1.0.0"

# Set environment variables
ENV JAVA_TOOL_OPTIONS="-Xms256m -Xmx1024m"
ENV GOOGLE_APPLICATION_CREDENTIALS="/secrets/key.json"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD java -version || exit 1

# Default entrypoint
ENTRYPOINT ["java", "-cp", "/app/etl-pipeline.jar"]
CMD ["com.etl.agent.ETLPipelineMain", "gs://bucket/data.csv", "dataset", "table"]

