package org.example;

import org.example.model.FileLevelHeaders;
import org.example.model.ProductActivityDetail;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
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
    public ResponseEntity<String> uploadEdiFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("File is empty.");
        }

        try {
            List<ProductActivityDetail> items = ediConverter.processFile(file.getBytes());
            String csv = ediConverter.toFormattedResponse(file.getBytes(), items);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(csv);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Error: " + e.getMessage());
        }
    }
}

