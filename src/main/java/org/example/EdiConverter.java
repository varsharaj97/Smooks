package org.example;

import org.example.model.FileLevelHeaders;
import org.example.model.ProductActivityDetail;
import org.smooks.Smooks;
import org.smooks.api.ExecutionContext;
import org.smooks.io.sink.JavaSink;
import org.smooks.io.source.ByteSource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class EdiConverter {

    private final Smooks smooks;

    public EdiConverter() {
        try {
            this.smooks = new Smooks("/smooks-config.xml");
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to initialize Smooks from '/smooks-config.xml'. " +
                            "Verify that the Smooks config and DFDL schema match the X12 payload.",
                    e
            );
        }
    }

    public List<ProductActivityDetail> processFile(byte[] fileBytes) {
        try {
            ExecutionContext executionContext = smooks.createExecutionContext();
            JavaSink javaSink = new JavaSink();
            byte[] normalizedFileBytes = normalizeEdi(fileBytes);

            smooks.filterSource(
                    executionContext,
                    new ByteSource(normalizedFileBytes),
                    javaSink
            );

            List<ProductActivityDetail> itemList =
                    (List<ProductActivityDetail>) javaSink.getBean("itemList");

            return itemList != null ? itemList : new ArrayList<>();
        } catch (Exception e) {
            throw new RuntimeException("Failed to process EDI: " + getRootCauseMessage(e), e);
        }
    }

    private String getRootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current.getMessage() != null ? current.getMessage() : current.getClass().getName();
    }

    private byte[] normalizeEdi(byte[] fileBytes) {
        String edi = new String(fileBytes, StandardCharsets.UTF_8);
        edi = edi.replace("\r", "").replace("\n", "");
        return edi.getBytes(StandardCharsets.UTF_8);
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


    public String toFormattedResponse(byte[] fileBytes, List<ProductActivityDetail> items) {
        FileLevelHeaders headers = extractFileLevelHeaders(fileBytes);
        StringBuilder csv = new StringBuilder();
        csv.append("FILE LEVEL HEADERS\n");
        csv.append("Interchange_ID,Group_ID,File_ID,Report_Date,Sender,Receiver,Vendor_ID\n");
        csv.append(csvValue(headers.getInterchangeId())).append(',')
                .append(csvValue(headers.getGroupId())).append(',')
                .append(csvValue(headers.getFileId())).append(',')
                .append(csvValue(headers.getReportDate())).append(',')
                .append(csvValue(headers.getSender())).append(',')
                .append(csvValue(headers.getReceiver())).append(',')
                .append(csvValue(headers.getVendorId())).append('\n');

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
                        headers.setInterchangeId(elements[13]);
                    }
                    break;
                case "GS":
                    if (elements.length > 1) {
                        headers.setGroupId(elements[1]);
                    }
                    break;
                case "XQ":
                    if (elements.length > 2) {
                        headers.setReportDate(elements[2]);
                    }
                    break;
                case "N9":
                    if (elements.length > 2) {
                        if ("FI".equals(elements[1]) && headers.getFileId() == null) {
                            headers.setFileId(elements[2]);
                        } else if ("IA".equals(elements[1]) && headers.getVendorId() == null) {
                            headers.setVendorId(elements[2]);
                        }
                    }
                    break;
                case "N1":
                    if (elements.length > 2) {
                        if ("FR".equals(elements[1]) && headers.getSender() == null) {
                            headers.setSender(elements[2]);
                        } else if ("TO".equals(elements[1]) && headers.getReceiver() == null) {
                            headers.setReceiver(elements[2]);
                        }
                    }
                    break;
                default:
                    break;
            }
        }

        return headers;
    }

    @PreDestroy
    public void close() throws Exception {
        smooks.close();
    }
}
