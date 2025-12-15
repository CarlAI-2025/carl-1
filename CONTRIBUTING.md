# Contribution Guidelines

## Getting Started

1. Fork the repository
2. Clone locally: `git clone <fork-url>`
3. Create feature branch: `git checkout -b feature/your-feature`
4. Make changes and commit: `git commit -m "Description"`
5. Push to fork: `git push origin feature/your-feature`
6. Create Pull Request

## Code Standards

### Java Style

- Use Java 17+ features
- Follow Google Java Style Guide
- Maximum line length: 120 characters
- Use meaningful variable names
- Add Javadoc for public APIs

### Testing Requirements

- Minimum 80% code coverage
- All tests must pass: `mvn test`
- Use descriptive test names
- Test both happy path and edge cases

Example test:
```java
@Test
public void testAgentExecutesSuccessfully() {
    // Arrange
    PipelineJob job = new PipelineJob();
    
    // Act
    agent.execute(job);
    
    // Assert
    assertEquals(JobStatus.COMPLETED, job.getStatus());
}
```

### Commit Messages

Format: `[TYPE] Subject - Description`

Types:
- `[FEATURE]` - New functionality
- `[BUGFIX]` - Bug fix
- `[REFACTOR]` - Code restructuring
- `[DOCS]` - Documentation
- `[TEST]` - Test additions

Example:
```
[FEATURE] Add anomaly detection - Implement Z-score and IQR methods for outlier detection in Auditor agent
```

## Pull Request Process

1. **Branch naming**: `feature/`, `bugfix/`, `docs/` prefix
2. **Description**: Clear description of changes
3. **Testing**: All tests pass locally
4. **Documentation**: Update README if needed
5. **Review**: Address reviewer feedback

## Tier Contributions

### Bronze Contributors
- Fix bugs
- Improve documentation
- Add basic tests
- Update dependencies

### Silver Contributors
- Enhance quality checks
- Add validation rules
- Improve error handling

### Gold Contributors
- Add anomaly detection
- Statistical analysis
- Pattern recognition

### Platinum Contributors
- SQL generation improvements
- Dataflow integration
- Infrastructure as Code
- CI/CD enhancements

## Reporting Issues

Use GitHub Issues with template:
1. Clear title
2. Description of the problem
3. Steps to reproduce
4. Expected vs actual behavior
5. Environment details

## Code Review Checklist

- [ ] Code follows style guide
- [ ] Tests added/updated
- [ ] Documentation updated
- [ ] No breaking changes
- [ ] Commits are logical
- [ ] PR description is clear

