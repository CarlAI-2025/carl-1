package com.etl.noadk.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CSV Service.
 */
public class CSVServiceTest {
    private CSVService csvService;

    @BeforeEach
    public void setUp() {
        csvService = new CSVService();
    }

    @Test
    public void testCsvParsing() throws Exception {
        String csvContent = "id,name,amount\n" +
                "1,John,100.50\n" +
                "2,Jane,200.75\n";

        var records = csvService.parseCsv(csvContent);

        assertEquals(2, records.size());
        assertEquals("John", records.get(0).get("name"));
        assertEquals("200.75", records.get(1).get("amount"));
    }

    @Test
    public void testCsvValidation() {
        String csvContent = "id,name,amount\n" +
                "1,John,100.50\n" +
                "2,Jane,200.75\n";

        CSVService.ValidationResult result = csvService.validateCSVStructure(csvContent, 3);

        assertTrue(result.isValid());
        assertEquals(2, result.getRowCount());
        assertEquals(3, result.getColumnCount());
    }

    @Test
    public void testCsvValidationFailure() {
        String csvContent = "id,name\n" +
                "1,John\n" +
                "2,Jane\n";

        CSVService.ValidationResult result = csvService.validateCSVStructure(csvContent, 5);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().size() > 0);
    }

    @Test
    public void testSampleExtraction() throws Exception {
        String csvContent = "id,name\n" +
                "1,John\n" +
                "2,Jane\n" +
                "3,Bob\n";

        var samples = csvService.extractSampleRows(csvContent, 2);

        assertEquals(2, samples.size());
        assertEquals("John", samples.get(0).get("name"));
        assertEquals("Jane", samples.get(1).get("name"));
    }

    @Test
    public void testHeaderParsing() throws Exception {
        String csvContent = "id,name,email\n" +
                "1,John,john@example.com\n";

        var records = csvService.parseCsv(csvContent);

        assertFalse(records.isEmpty());
        assertTrue(records.get(0).containsKey("email"));
    }

    @Test
    public void testEmptyCsvHandling() throws Exception {
        String csvContent = "id,name\n";

        var records = csvService.parseCsv(csvContent);

        assertEquals(0, records.size());
    }
}

