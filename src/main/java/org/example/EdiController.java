package org.example;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/edi")
public class    EdiController {

    private final EdiConverter ediConverter;

    public EdiController(EdiConverter ediConverter) {
        this.ediConverter = ediConverter;
    }

    @PostMapping("/upload")
    @ResponseBody
    public ResponseEntity<?> uploadEdiFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty. Please upload a valid EDI file.");
        }

        try {
            // Convert the file content to a byte array and process using EdiConverter
            byte[] fileBytes = file.getBytes();
            List<String> results = ediConverter.processFile(fileBytes);

            // Return the processed segments as a response
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
}

