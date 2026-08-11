package ug.edu.ugmc.optimizer.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Migrates the legacy service-request CSV layout to the optimizer's expanded
 * snake_case layout.
 *
 * <p>The complete input file and all converted rows are held in memory before
 * the original file is opened for writing. This prevents the input from being
 * truncated before it has been read and validated.</p>
 */
public final class CsvMigrator {

    private static final String DEFAULT_FILE = "data/service_requests.csv";
    private static final String LEGACY_HEADER =
            "requestId,sourceId,destinationId,category,urgency,timeSubmitted,deadline,status";
    private static final String NEW_HEADER =
            "request_id,patient_name,source_id,destination_id,category,urgency_level,weight,value,"
                    + "time_submitted,deadline,status";

    private static final int LEGACY_COLUMN_COUNT = 8;
    private static final int MIN_WEIGHT = 10;
    private static final int MAX_WEIGHT = 50;
    private static final int MIN_VALUE = 50;
    private static final int MAX_VALUE = 200;

    private CsvMigrator() {
    }

    /**
     * Runs the migration. The first command-line argument may specify the CSV
     * path; otherwise {@code data/service_requests.csv} is used.
     *
     * @param args optional path to the service-request CSV file
     */
    public static void main(String[] args) {
        File csvFile = new File(args.length > 0 ? args[0] : DEFAULT_FILE);

        try {
            migrate(csvFile, new Random());
            System.out.println("Migrated service requests: " + csvFile.getAbsolutePath());
        } catch (IOException | IllegalArgumentException exception) {
            System.err.println("CSV migration failed: " + exception.getMessage());
            System.exit(1);
        }
    }

    /**
     * Reads, validates, converts, and overwrites a service-request CSV file.
     *
     * @param csvFile legacy CSV file to migrate in place
     * @param random random-number generator used for dummy weight and value data
     * @throws IOException if the file cannot be read or written
     * @throws IllegalArgumentException if the file is empty or malformed
     */
    public static void migrate(File csvFile, Random random) throws IOException {
        if (csvFile == null) {
            throw new IllegalArgumentException("CSV file cannot be null.");
        }
        if (random == null) {
            throw new IllegalArgumentException("Random generator cannot be null.");
        }

        // Complete this read before opening the same file for writing.
        List<String> inputLines = readAllLines(csvFile);
        List<String> migratedLines = migrateRows(inputLines, random);

        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(csvFile, StandardCharsets.UTF_8, false))) {
            for (String line : migratedLines) {
                writer.write(line);
                writer.newLine();
            }
        }
    }

    private static List<String> readAllLines(File csvFile) throws IOException {
        List<String> lines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new FileReader(csvFile, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }

        return lines;
    }

    private static List<String> migrateRows(List<String> inputLines, Random random) {
        if (inputLines.isEmpty()) {
            throw new IllegalArgumentException("CSV file is empty.");
        }
        if (!LEGACY_HEADER.equals(removeByteOrderMark(inputLines.get(0)))) {
            throw new IllegalArgumentException("Unexpected legacy CSV header.");
        }

        List<String> migratedLines = new ArrayList<>(inputLines.size());
        migratedLines.add(NEW_HEADER);

        for (int lineNumber = 2; lineNumber <= inputLines.size(); lineNumber++) {
            String sourceLine = inputLines.get(lineNumber - 1);
            if (sourceLine.isBlank()) {
                continue;
            }

            List<String> columns = parseCsvRow(sourceLine, lineNumber);
            if (columns.size() != LEGACY_COLUMN_COUNT) {
                throw new IllegalArgumentException(
                        "Line " + lineNumber + " has " + columns.size()
                                + " columns; expected " + LEGACY_COLUMN_COUNT + ".");
            }

            String requestId = columns.get(0);
            int weight = randomInRange(random, MIN_WEIGHT, MAX_WEIGHT);
            int value = randomInRange(random, MIN_VALUE, MAX_VALUE);

            String[] migratedColumns = {
                requestId,
                "Patient_" + requestId,
                columns.get(1),
                columns.get(2),
                columns.get(3),
                columns.get(4),
                Integer.toString(weight),
                Integer.toString(value),
                columns.get(5),
                columns.get(6),
                columns.get(7)
            };
            migratedLines.add(toCsvRow(migratedColumns));
        }

        return migratedLines;
    }

    private static int randomInRange(Random random, int minimum, int maximum) {
        return minimum + random.nextInt(maximum - minimum + 1);
    }

    private static List<String> parseCsvRow(String row, int lineNumber) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean insideQuotes = false;

        for (int index = 0; index < row.length(); index++) {
            char character = row.charAt(index);

            if (character == '"') {
                if (insideQuotes && index + 1 < row.length() && row.charAt(index + 1) == '"') {
                    field.append('"');
                    index++;
                } else {
                    insideQuotes = !insideQuotes;
                }
            } else if (character == ',' && !insideQuotes) {
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(character);
            }
        }

        if (insideQuotes) {
            throw new IllegalArgumentException("Line " + lineNumber + " contains an unclosed quote.");
        }

        fields.add(field.toString());
        return fields;
    }

    private static String toCsvRow(String[] fields) {
        StringBuilder row = new StringBuilder();
        for (int index = 0; index < fields.length; index++) {
            if (index > 0) {
                row.append(',');
            }
            appendEscapedField(row, fields[index]);
        }
        return row.toString();
    }

    private static void appendEscapedField(StringBuilder row, String field) {
        boolean requiresQuotes = field.indexOf(',') >= 0
                || field.indexOf('"') >= 0
                || field.indexOf('\n') >= 0
                || field.indexOf('\r') >= 0;

        if (!requiresQuotes) {
            row.append(field);
            return;
        }

        row.append('"');
        for (int index = 0; index < field.length(); index++) {
            char character = field.charAt(index);
            if (character == '"') {
                row.append('"');
            }
            row.append(character);
        }
        row.append('"');
    }

    private static String removeByteOrderMark(String value) {
        return value.startsWith("\uFEFF") ? value.substring(1) : value;
    }
}
