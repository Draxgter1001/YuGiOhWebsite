// CardOcrService.java
package taf.yugioh.scanner.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Service
public class CardOCRService {

    @Value("${app.python.script.path:src/main/resources/scripts/extract_name.py}")
    private String pythonScriptPath;

    @Value("${app.python.executable:python}")
    private String pythonExecutable;

    @Value("${app.upload.temp.dir:temp/uploads}")
    private String tempUploadDir;

    public String extractCardName(MultipartFile imageFile) throws Exception {
        // Create temp directory if it doesn't exist
        Path tempDir = Paths.get(tempUploadDir);
        if (!Files.exists(tempDir)) {
            Files.createDirectories(tempDir);
        }

        // Save uploaded file temporarily
        String tempFileName = System.currentTimeMillis() + "_" + imageFile.getOriginalFilename();
        Path tempFilePath = tempDir.resolve(tempFileName);
        
        try {
            // Save the uploaded file
            Files.copy(imageFile.getInputStream(), tempFilePath);

            // Execute Python script
            String cardName = executePythonScript(tempFilePath.toString());
            
            return cardName;
            
        } finally {
            // Clean up temporary file
            try {
                Files.deleteIfExists(tempFilePath);
            } catch (IOException e) {
                // Log the error but don't throw it
                System.err.println("Failed to delete temporary file: " + tempFilePath);
            }
        }
    }

    private String executePythonScript(String imagePath) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(
            pythonExecutable, pythonScriptPath, imagePath
        );
        
        // Don't redirect error stream to output - keep them separate
        processBuilder.redirectErrorStream(false);
        Process process = processBuilder.start();

        // Read stdout and stderr separately
        StringBuilder output = new StringBuilder();
        StringBuilder errorOutput = new StringBuilder();
        
        // Read stdout (card name)
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        
        // Read stderr (error messages/warnings)
        try (BufferedReader errorReader = new BufferedReader(
                new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
        }

        // Wait for the process to complete
        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Python script execution timed out");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            String errorMsg = errorOutput.toString().trim();
            throw new RuntimeException("Python script failed: " + errorMsg);
        }

        String result = output.toString().trim();
        if (result.isEmpty()) {
            throw new RuntimeException("No card name extracted from image");
        }

        return result;
    }
}