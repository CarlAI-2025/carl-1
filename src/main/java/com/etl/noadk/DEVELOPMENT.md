````markdown
# Non-ADK Testing and Development Guide

## Overview

This guide covers testing, debugging, and development practices for the traditional, non-ADK based ETL pipeline.

## Development Environment Setup

### Prerequisites
- JDK 11+
- Maven 3.6+
- Git
- IDE (IntelliJ, Eclipse, or VS Code)
- gcloud CLI
- Docker (optional)

### IDE Setup

#### IntelliJ IDEA
```
1. File → Open → Select project directory
2. Configure → Project Structure
   - SDK: Select JDK 11+
   - Language level: 11
3. Enable Annotation Processing (for Lombok if used)
   - Settings → Build, Execution, Deployment → Compiler → Annotation Processors
   - Check "Enable annotation processing"
4. Run → Edit Configurations
   - Add Application configuration
   - Main class: com.etl.noadk.ETLPipelineMain
   - Environment variables: GCP_PROJECT_ID=dev-project
```

#### VS Code
```json
// .vscode/launch.json
{
  "version": "0.2.0",
  "configurations": [
    {
      "name": "ETL Pipeline",
      "type": "java",
      "name": "ETLPipelineMain",
      "request": "launch",
      "mainClass": "com.etl.noadk.ETLPipelineMain",
      "projectName": "carl-1",
      "cwd": "${workspaceFolder}",
      "console": "integratedTerminal",
      "env": {
        "GCP_PROJECT_ID": "dev-project",
        "GCS_BUCKET": "dev-bucket"
      }
    }
  ]
}
```

## Local Development

### Quick Start for Development
```bash
# 1. Clone and navigate
git clone <repo>
cd carl-1

# 2. Install dependencies
mvn clean install

# 3. Set local environment
export GCP_PROJECT_ID=dev-project
export GCS_BUCKET=dev-etl-bucket
export GOOGLE_APPLICATION_CREDENTIALS=$HOME/.config/gcloud/application_default_credentials.json

# 4. Create BigQuery dataset
bq mk --dataset dev-project:etl_dataset_dev

# 5. Run application
mvn spring-boot:run
```

### Development with Hot Reload
```bash
# Using Spring Boot DevTools for fast reload
mvn clean spring-boot:run -Dspring-boot.run.arguments="--spring.devtools.restart.enabled=true"

# Or with Maven watch
mvn -f pom.xml compile exec:java -Dexec.mainClass="com.etl.noadk.ETLPipelineMain" -Dexec.classpathScope=runtime
```

## Testing

### Unit Tests

#### Test Structure
```
src/test/java/com/etl/noadk/
├── agents/
│   ├── ScoutAgentTest.java
│   ├── CartographerAgentTest.java
│   ├── NavigatorAgentTest.java
│   ├── AlchemistAgentTest.java
│   ├── ArchitectAgentTest.java
│   └── AuditorAgentTest.java
├── domain/
│   ├── PipelineJobTest.java
│   ├── SchemaContractTest.java
│   └── FieldMappingTest.java
└── services/
    ├── CSVServiceTest.java
    ├── BigQueryServiceTest.java
    └── AnomalyDetectionServiceTest.java
```

#### Example Unit Test
```java
@RunWith(JUnit4.class)
public class ScoutAgentTest {
    
    private ScoutAgent scoutAgent;
    private PipelineJob job;
    
    @Before
    public void setUp() {
        scoutAgent = new ScoutAgent();
        job = new PipelineJob();
        job.setJobId(UUID.randomUUID().toString());
    }
    
    @Test
    public void testScoutAgent_ValidCSV() throws Exception {
        // Arrange
        String testFile = "gs://test-bucket/data/test.csv";
        job.setSourcePath(testFile);
        
        // Act
        scoutAgent.execute(job);
        
        // Assert
        assertNotNull(job.getFileInfo());
        assertTrue(job.getFileInfo().getRowCount() > 0);
        assertEquals("CSV", job.getFileInfo().getFormat());
    }
    
    @Test
    public void testScoutAgent_InvalidFile() throws Exception {
        // Arrange
        String testFile = "gs://test-bucket/data/nonexistent.csv";
        job.setSourcePath(testFile);
        
        // Act & Assert
        assertThrows(IOException.class, () -> scoutAgent.execute(job));
    }
}
```

### Integration Tests

#### Test Configuration
```java
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ETLPipelineMain.class)
@TestPropertySource(locations = "classpath:application-test.properties")
public class ETLPipelineIntegrationTest {
    
    @Autowired
    private ConductorAgent conductorAgent;
    
    @Autowired
    private BigQueryService bigQueryService;
    
    @Before
    public void setUp() {
        // Create test dataset
        bigQueryService.createDataset("etl_dataset_test");
    }
    
    @After
    public void tearDown() {
        // Cleanup test dataset
        bigQueryService.deleteDataset("etl_dataset_test");
    }
    
    @Test
    public void testEndToEndETLPipeline() throws Exception {
        // Arrange
        PipelineJob job = new PipelineJob();
        job.setSourcePath("gs://test-bucket/sample.csv");
        job.setDatasetName("etl_dataset_test");
        
        // Act
        conductorAgent.execute(job);
        
        // Assert
        assertEquals(JobStatus.COMPLETE, job.getStatus());
        assertTrue(bigQueryService.tableExists("etl_dataset_test", "data_records"));
    }
}
```

### Run Tests
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ScoutAgentTest

# Run specific test method
mvn test -Dtest=ScoutAgentTest#testScoutAgent_ValidCSV

# Run with coverage
mvn test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

## Debugging

### Enable Debug Logging
```bash
# Method 1: Command line
java -Dlogging.level.com.etl=DEBUG \
  -jar target/carl-1-1.0-SNAPSHOT.jar

# Method 2: Properties file
# application-noadk.properties
logging.level=DEBUG
logging.level.com.etl.noadk=DEBUG
logging.level.com.etl.noadk.agents=TRACE

# Method 3: Environment variable
export LOG_LEVEL=DEBUG
java -jar target/carl-1-1.0-SNAPSHOT.jar
```

### Remote Debugging
```bash
# Start application with debug port
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 \
  -jar target/carl-1-1.0-SNAPSHOT.jar

# In IDE: Run → Edit Configurations
# Create "Remote" debug configuration
# Host: localhost
# Port: 5005
```

### Debugging with Breakpoints (IDE)
```java
// ScoutAgent.java
public void execute(PipelineJob job) {
    // Set breakpoint here (line number)
    logger.debug("Starting Scout Agent");
    
    try {
        // Step through code
        FileInfo fileInfo = analyzeFile(job.getSourcePath());
        job.setFileInfo(fileInfo);
    } catch (Exception e) {
        logger.error("Scout Agent failed", e);
    }
}
```

### Logging for Debugging
```java
// Good logging practices
logger.debug("Scout Agent starting for file: {}", filePath);
logger.debug("File info: format={}, rows={}, size={}", 
    format, rowCount, fileSize);

// Track execution flow
logger.trace("Entering execute() method");
logger.trace("Exiting execute() method, status={}", job.getStatus());

// Conditional debugging
if (logger.isDebugEnabled()) {
    logger.debug("Detailed state: {}", job.toString());
}
```

## Performance Testing

### Benchmark Small Datasets
```bash
# Create 1MB test file
python3 << 'EOF'
import csv
import random

with open('test_1mb.csv', 'w', newline='') as f:
    writer = csv.DictWriter(f, fieldnames=['id', 'name', 'amount', 'date'])
    writer.writeheader()
    for i in range(10000):
        writer.writerow({
            'id': i,
            'name': f'Record_{i}',
            'amount': random.uniform(100, 10000),
            'date': '2024-01-15'
        })
EOF

# Upload and test
gsutil cp test_1mb.csv gs://dev-bucket/data/

java -Xmx2g -jar target/carl-1-1.0-SNAPSHOT.jar
```

### Profile Execution Time
```java
public class PerformanceTest {
    
    @Test
    public void testPerformance_LargeFile() throws Exception {
        long startTime = System.currentTimeMillis();
        
        // Execute pipeline
        PipelineJob job = new PipelineJob();
        conductorAgent.execute(job);
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        logger.info("Pipeline execution time: {} ms", duration);
        assertTrue(duration < 300000); // Should complete within 5 minutes
    }
}
```

### Load Testing
```bash
# Simple load test (sequential)
for i in {1..10}; do
    gsutil cp test_1mb.csv gs://dev-bucket/data/test_$i.csv
    java -jar target/carl-1-1.0-SNAPSHOT.jar -DGCS_OBJECT=data/test_$i.csv
done

# Parallel load test (concurrent)
for i in {1..10}; do
    (
        gsutil cp test_1mb.csv gs://dev-bucket/data/test_$i.csv
        java -jar target/carl-1-1.0-SNAPSHOT.jar -DGCS_OBJECT=data/test_$i.csv
    ) &
done
wait
```

## Sample Data Generation

### Using SampleDataGenerator Service
```java
// Generate test data
SampleDataGenerator generator = new SampleDataGenerator();

// Generate CSV
List<Map<String, Object>> records = generator.generateRecords(
    1000,  // number of records
    new String[]{"id", "name", "amount", "date"},
    Arrays.asList(
        "integer",   // id
        "string",    // name
        "decimal",   // amount
        "date"       // date
    )
);

// Save to file
generator.saveToCsv(records, "sample_data.csv");

// Upload to GCS
StorageClient storage = new StorageClient();
storage.uploadFile("sample_data.csv", "gs://dev-bucket/data/");
```

### Create Test CSV Files
```bash
# Using Python
python3 << 'EOF'
import csv
from datetime import datetime, timedelta
import random

def generate_test_data(filename, rows=1000):
    with open(filename, 'w', newline='') as f:
        writer = csv.DictWriter(f, fieldnames=['customer_id', 'name', 'amount', 'transaction_date'])
        writer.writeheader()
        
        base_date = datetime(2024, 1, 1)
        for i in range(rows):
            writer.writerow({
                'customer_id': f'CUST_{i:06d}',
                'name': f'Customer {i}',
                'amount': round(random.uniform(10, 5000), 2),
                'transaction_date': (base_date + timedelta(days=random.randint(0, 365))).strftime('%Y-%m-%d')
            })

generate_test_data('test_data.csv', 5000)
EOF

# Upload to GCS
gsutil cp test_data.csv gs://dev-bucket/data/
```

## Code Quality

### Static Analysis
```bash
# SonarQube scan (if configured)
mvn clean verify sonar:sonar \
  -Dsonar.projectKey=etl-pipeline \
  -Dsonar.sources=src/main/java \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=<token>

# Checkstyle
mvn checkstyle:check

# SpotBugs
mvn spotbugs:check
```

### Code Coverage
```bash
# Generate coverage report
mvn clean test jacoco:report

# View report
open target/site/jacoco/index.html

# Minimum coverage threshold
mvn verify -Djacoco.haltOnFailure=true \
  -Djacocoargs="*:*!*Test:0.60"
```

## CI/CD Integration

### GitHub Actions Example
```yaml
# .github/workflows/test.yml
name: Test and Build

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v2
    
    - name: Set up JDK 11
      uses: actions/setup-java@v2
      with:
        java-version: '11'
    
    - name: Run tests
      run: mvn clean test
    
    - name: Build JAR
      run: mvn clean package
    
    - name: Upload coverage
      uses: codecov/codecov-action@v1
```

### GitLab CI Example
```yaml
# .gitlab-ci.yml
stages:
  - test
  - build

test:
  stage: test
  image: maven:3.8-jdk-11
  script:
    - mvn clean test
  coverage: '/TOTAL.*?([0-9]{1,3})%/'
  artifacts:
    reports:
      junit: target/surefire-reports/TEST-*.xml

build:
  stage: build
  image: maven:3.8-jdk-11
  script:
    - mvn clean package
  artifacts:
    paths:
      - target/*.jar
```

## Common Development Tasks

### Adding a New Agent
```java
// 1. Create agent class
public class CustomAgent implements ETLAgent {
    
    @Override
    public String getName() {
        return "CustomAgent";
    }
    
    @Override
    public void execute(PipelineJob job) throws Exception {
        // Implementation
    }
}

// 2. Register in ConductorAgent
private List<ETLAgent> initializeAgents() {
    List<ETLAgent> agents = new ArrayList<>();
    agents.add(new ScoutAgent());
    agents.add(new CartographerAgent());
    // ... add new agent
    agents.add(new CustomAgent());
    return agents;
}

// 3. Write tests
@Test
public void testCustomAgent() throws Exception {
    CustomAgent agent = new CustomAgent();
    PipelineJob job = new PipelineJob();
    agent.execute(job);
    // assertions
}
```

### Adding a New Service
```java
// 1. Create service interface
public interface CustomService {
    void doSomething() throws Exception;
}

// 2. Implement service
@Component
public class CustomServiceImpl implements CustomService {
    // Implementation
}

// 3. Inject in agent
@Autowired
private CustomService customService;

// 4. Use in agent
public void execute(PipelineJob job) {
    customService.doSomething();
}
```

### Modifying Configuration
```
# application-noadk.properties
# Add new property
custom.setting=value

# Access in code
@Value("${custom.setting}")
private String customSetting;
```

## Troubleshooting Development Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| "Cannot find symbol" | Missing dependency | Run `mvn clean install` |
| Compilation errors | Wrong Java version | Check `mvn -version`, use JDK 11+ |
| Test failures | Missing GCP credentials | Set GOOGLE_APPLICATION_CREDENTIALS |
| OutOfMemory during build | Insufficient heap | `export MAVEN_OPTS=-Xmx2g` |
| Slow IDE performance | Large classpath | Disable annotation processing temporarily |

## Best Practices

1. **Always run tests before committing**
   ```bash
   mvn clean test
   ```

2. **Use meaningful variable names**
   ```java
   // Good
   List<FieldMapping> sourceToTargetMappings = navigator.generateMappings(schema);
   
   // Bad
   List<FieldMapping> mappings = navigator.generateMappings(s);
   ```

3. **Add logging at key points**
   ```java
   logger.info("Processing file: {}", filePath);
   logger.debug("Generated {} field mappings", mappings.size());
   ```

4. **Handle exceptions gracefully**
   ```java
   try {
       // operation
   } catch (IOException e) {
       logger.error("Failed to read file: {}", filePath, e);
       job.addError(new ErrorRecord(e.getMessage()));
   }
   ```

5. **Keep methods small and focused**
   ```java
   // Method should do one thing
   private SchemaContract inferSchema(List<String> sampleRows) {
       // implementation
   }
   ```

---

**Last Updated:** December 15, 2025
**Audience:** Developers, QA Engineers
**Related:** `ARCHITECTURE.md` (Architecture), `README.keep.md` (Overview)

````
