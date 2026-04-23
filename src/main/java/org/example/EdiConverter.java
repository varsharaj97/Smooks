package org.example;

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

    @PreDestroy
    public void close() throws Exception {
        smooks.close();
    }
}
