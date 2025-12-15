package com.etl.agent.agents;

import com.etl.agent.domain.PipelineJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Navigator Agent (Field Mapping).
 */
public class NavigatorAgentTest {
    private NavigatorAgent agent;
    private PipelineJob job;

    @BeforeEach
    public void setUp() {
        agent = new NavigatorAgent();
        job = new PipelineJob();
        job.setSourcePath("gs://test-bucket/data.csv");
        job.setTargetDataset("test_dataset");
        job.setTargetTable("test_table");
        job.getStatistics().setTotalRecordsRead(100);
    }

    @Test
    public void testAgentName() {
        assertEquals("Navigator", agent.getName());
    }

    @Test
    public void testFieldMapping() throws Exception {
        agent.execute(job);

        assertEquals(PipelineJob.JobStatus.MAPPED, job.getStatus());
        assertNotNull(job.getMappingVersion());
        assertTrue(job.getLineage().size() > 0);
    }

    @Test
    public void testMappingVersionTracking() throws Exception {
        agent.execute(job);

        assertNotNull(job.getMappingVersion());
        assertTrue(job.getMappingVersion().startsWith("m1_"));
    }

    @Test
    public void testLineageEntry() throws Exception {
        agent.execute(job);

        PipelineJob.LineageEntry entry = job.getLineage().get(0);
        assertEquals("FIELD_MAPPING", entry.getStep());
        assertEquals("Navigator", entry.getAgentName());
        assertTrue(entry.getOutputRecords() > 0);
    }
}

