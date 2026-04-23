package org.example;

import org.example.model.ProductActivityDetail;
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

    @PostMapping(value = "/upload", produces = "text/csv")
    @ResponseBody
    public ResponseEntity<?> uploadEdiFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty.");
        }

        try {
            List<ProductActivityDetail> csvResult = ediConverter.processFile(file.getBytes());
            return ResponseEntity.ok(csvResult);
        } catch (Exception e) {
            // Return the actual error message to Postman for easier debugging
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}

