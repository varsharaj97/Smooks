package org.example;

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
            String csv = toFormattedResponse(file.getBytes(), items);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(csv);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Error: " + e.getMessage());
        }
    }

    private String toFormattedResponse(byte[] fileBytes, List<ProductActivityDetail> items) {
        FileLevelHeaders headers = extractFileLevelHeaders(fileBytes);
        StringBuilder csv = new StringBuilder();
        csv.append("FILE LEVEL HEADERS\n");
        csv.append("Interchange_ID,Group_ID,File_ID,Report_Date,Sender,Receiver,Vendor_ID\n");
        csv.append(csvValue(headers.interchangeId)).append(',')
                .append(csvValue(headers.groupId)).append(',')
                .append(csvValue(headers.fileId)).append(',')
                .append(csvValue(headers.reportDate)).append(',')
                .append(csvValue(headers.sender)).append(',')
                .append(csvValue(headers.receiver)).append(',')
                .append(csvValue(headers.vendorId)).append('\n');

        csv.append('\n');
        csv.append("TRANSACTIONAL DATA\n");
        csv.append("Ref_Num,UPC,Activity,Price,Store_ID,Quantity\n");

        for (ProductActivityDetail item : items) {
            csv.append(csvValue(item.getRefNum())).append(',')
                    .append(csvValue(item.getUpcCode())).append(',')
                    .append(csvValue(item.getActivity())).append(',')
                    .append(csvValue(item.getUnitPrice())).append(',')
                    .append(csvValue(item.getLocationId())).append(',')
                    .append(csvValue(item.getQuantity())).append('\n');
        }

        return csv.toString();
    }

    private FileLevelHeaders extractFileLevelHeaders(byte[] fileBytes) {
        String edi = new String(fileBytes, StandardCharsets.UTF_8).replace("\r", "").replace("\n", "");
        String[] segments = edi.split("~");
        FileLevelHeaders headers = new FileLevelHeaders();

        for (String rawSegment : segments) {
            String segment = rawSegment.trim();
            if (segment.isEmpty()) {
                continue;
            }

            String[] elements = segment.split("\\*", -1);
            String tag = elements[0];

            switch (tag) {
                case "ISA":
                    if (elements.length > 13) {
                        headers.interchangeId = elements[13];
                    }
                    break;
                case "GS":
                    if (elements.length > 1) {
                        headers.groupId = elements[1];
                    }
                    break;
                case "XQ":
                    if (elements.length > 2) {
                        headers.reportDate = elements[2];
                    }
                    break;
                case "N9":
                    if (elements.length > 2) {
                        if ("FI".equals(elements[1]) && headers.fileId == null) {
                            headers.fileId = elements[2];
                        } else if ("IA".equals(elements[1]) && headers.vendorId == null) {
                            headers.vendorId = elements[2];
                        }
                    }
                    break;
                case "N1":
                    if (elements.length > 2) {
                        if ("FR".equals(elements[1]) && headers.sender == null) {
                            headers.sender = elements[2];
                        } else if ("TO".equals(elements[1]) && headers.receiver == null) {
                            headers.receiver = elements[2];
                        }
                    }
                    break;
                default:
                    break;
            }
        }

        return headers;
    }

    private String csvValue(String value) {
        if (value == null) {
            return "";
        }

        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }

        return escaped;
    }

    private static class FileLevelHeaders {
        private String interchangeId;
        private String groupId;
        private String fileId;
        private String reportDate;
        private String sender;
        private String receiver;
        private String vendorId;
    }
}

