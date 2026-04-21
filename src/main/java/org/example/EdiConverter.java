package org.example;

import org.smooks.Smooks;
import org.smooks.api.ExecutionContext;
import org.smooks.io.payload.JavaResult;
import org.smooks.io.payload.StringSource;
import org.springframework.stereotype.Service;

import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class EdiConverter {

    public List<String> processFile(byte[] fileBytes) {
        List<String> processedSegments = new ArrayList<>();

        // Convert the input EDI bytes into a String (Presuming it's UTF-8 Encoded).
        String ediData = new String(fileBytes, StandardCharsets.UTF_8);

        // Initialize Smooks
        try (Smooks smooks = new Smooks("/smooks.config.xml")) {
            // Create an ExecutionContext for the EDI message processing
            ExecutionContext executionContext = smooks.createExecutionContext();

            // Parse and bind the EDI message data into result
            JavaResult javaResult = new JavaResult();
            smooks.filterSource(executionContext, new StreamSource(new ByteArrayInputStream(fileBytes)), javaResult);

            // Assuming the Smooks mapping results in a List<String> called "segments"
            List<String> ediSegments = (List<String>) javaResult.getBean("itemList");

            if (ediSegments != null) {
                processedSegments.addAll(ediSegments); // Store the segments for processing further
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to process EDI file with Smooks", e);
        }

        return processedSegments;
    }

    private String getSafe(List<String> elements, int index) {
        if (index < 0 || index >= elements.size()) {
            return null; // Avoid IndexOutOfBoundsException

        }
        return elements.get(index);
    }
}