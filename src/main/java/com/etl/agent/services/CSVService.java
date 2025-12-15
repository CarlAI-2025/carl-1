package com.etl.agent.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.StringReader;
import java.util.*;

/**
 * Service for CSV parsing and validation.
 */
@Slf4j
public class CSVService {

    /**
     * Parse CSV content and return as list of maps.
     */
    public List<Map<String, String>> parseCsv(String content) throws Exception {
        List<Map<String, String>> records = new ArrayList<>();

        CSVFormat csvFormat = CSVFormat.DEFAULT.withFirstRecordAsHeader();
        CSVParser csvParser = csvFormat.parse(new StringReader(content));

        for (CSVRecord record : csvParser) {
            records.add(record.toMap());
        }

        csvParser.close();
        return records;
    }

    /**
     * Validate CSV structure.
     */
    public ValidationResult validateCSVStructure(String content, int minColumns) {
        ValidationResult result = new ValidationResult();

        try {
            CSVFormat csvFormat = CSVFormat.DEFAULT.withFirstRecordAsHeader();
            CSVParser csvParser = csvFormat.parse(new StringReader(content));

            int headerCount = csvParser.getHeaderMap().size();
            if (headerCount < minColumns) {
                result.addError("CSV has " + headerCount + " columns, expected at least " + minColumns);
            }

            int rowCount = 0;
            for (CSVRecord record : csvParser) {
                rowCount++;
                if (record.size() != headerCount) {
                    result.addError("Row " + (rowCount + 1) + " has inconsistent column count");
                }
            }

            result.setRowCount(rowCount);
            result.setColumnCount(headerCount);
            csvParser.close();

        } catch (Exception e) {
            result.addError("CSV parsing error: " + e.getMessage());
        }

        return result;
    }

    /**
     * Extract sample rows from CSV.
     */
    public List<Map<String, String>> extractSampleRows(String content, int sampleSize) throws Exception {
        List<Map<String, String>> samples = new ArrayList<>();

        CSVFormat csvFormat = CSVFormat.DEFAULT.withFirstRecordAsHeader();
        CSVParser csvParser = csvFormat.parse(new StringReader(content));

        int count = 0;
        for (CSVRecord record : csvParser) {
            if (count >= sampleSize) break;
            samples.add(record.toMap());
            count++;
        }

        csvParser.close();
        return samples;
    }

    /**
     * Validation result wrapper.
     */
    public static class ValidationResult {
        private List<String> errors = new ArrayList<>();
        private int rowCount;
        private int columnCount;

        public void addError(String error) {
            errors.add(error);
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public List<String> getErrors() {
            return errors;
        }

        public int getRowCount() {
            return rowCount;
        }

        public void setRowCount(int rowCount) {
            this.rowCount = rowCount;
        }

        public int getColumnCount() {
            return columnCount;
        }

        public void setColumnCount(int columnCount) {
            this.columnCount = columnCount;
        }
    }
}

