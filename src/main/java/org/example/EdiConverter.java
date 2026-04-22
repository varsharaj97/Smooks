package org.example;

import org.example.model.ProductActivityDetail;
import org.smooks.Smooks;
import org.smooks.api.ExecutionContext;
import org.smooks.io.sink.JavaSink;
import org.smooks.io.source.ByteSource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
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
                            "Verify that all Smooks modules use the same release and that the runtime classpath is refreshed.",
                    e
            );
        }
    }

    public List<ProductActivityDetail> processFile(byte[] fileBytes) {
        try {
            ExecutionContext executionContext = smooks.createExecutionContext();
            JavaSink javaSink = new JavaSink();

            smooks.filterSource(
                    executionContext,
                    new ByteSource(fileBytes),
                    javaSink
            );

            List<ProductActivityDetail> itemList =
                    (List<ProductActivityDetail>) javaSink.getBean("itemList");

            System.out.println(javaSink.getBean("productActivityReport"));

            return itemList != null ? itemList : new ArrayList<>();

        } catch (Exception e) {
            throw new IllegalStateException("Failed to process the uploaded EDI document.", e);
        }
    }

    @PreDestroy
    public void close() throws Exception {
        smooks.close();
    }
}
