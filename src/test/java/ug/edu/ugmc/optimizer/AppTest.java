package ug.edu.ugmc.optimizer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class AppTest {

    @Test
    void mainInitializesTheIntegratedApplicationAndLaunchesTheConsoleMenu() {
        InputStream originalInput = System.in;
        PrintStream originalOutput = System.out;
        PrintStream originalError = System.err;
        ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errorBytes = new ByteArrayOutputStream();

        try {
            System.setIn(new ByteArrayInputStream(
                    "0\n".getBytes(StandardCharsets.UTF_8)));
            System.setOut(new PrintStream(outputBytes, true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(errorBytes, true, StandardCharsets.UTF_8));

            App.main(new String[0]);
        } finally {
            System.setIn(originalInput);
            System.setOut(originalOutput);
            System.setErr(originalError);
        }

        String output = outputBytes.toString(StandardCharsets.UTF_8);
        String error = errorBytes.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("=== UGMC Smart Service Operations Optimizer ==="));
        assertTrue(output.contains("1. Request ID lookup"));
        assertTrue(output.contains("Exiting system."));
        assertFalse(output.contains("Foundation ready."));
        assertTrue(error.isBlank(), () -> "Unexpected startup error: " + error);
    }
}
