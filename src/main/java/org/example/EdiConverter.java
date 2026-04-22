package org.example;

import org.example.model.ProductActivityDetail;
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

    public List<ProductActivityDetail> processFile(byte[] fileBytes) {
        try (Smooks smooks = new Smooks("/smooks-config.xml")) {

            ExecutionContext executionContext = smooks.createExecutionContext();
            JavaResult javaResult = new JavaResult();

            smooks.filterSource(
                    executionContext,
                    new StreamSource(new ByteArrayInputStream(fileBytes)),
                    javaResult
            );

            List<ProductActivityDetail> itemList =
                    (List<ProductActivityDetail>) javaResult.getBean("itemList");

            System.out.println(javaResult.getBean("productActivityReport"));

            return itemList != null ? itemList : new ArrayList<>();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}