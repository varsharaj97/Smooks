package org.example;

import org.example.model.ProductActivityDetail;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EdiConverterTest {

    @Test
    void parsesSample852UsingSmooks() {
        String edi = """
                ISA*00**00**01*012650750*01*133040548*251117*0241*U*00501*852103694*0*P*>~
                GS*PD*925485US00*012650750*20251117*0241*852103694*X*005010~
                ST*852*2677134~
                XQ*H*20251116~
                N9*FI*00PFS~
                N9*IA*830430645~
                N1*FR*Retailer1*UL*0605388000002~
                N1*TO*Retailer2~
                DTM*097*20251116~
                LIN*783496*IN*990402230*UP*072470006431*****UK*00072470006431~
                PO4*1~
                N9*IK*253250472064548~
                ZA*QS~
                CTP**UCP*12.29~
                SDQ*EA*UL*0605388006059*3~
                LIN*783497*IN*990402230*UP*072470006431*****UK*00072470006431~
                PO4*1~
                N9*IK*253250478264548~
                ZA*QS~
                CTP**UCP*12.29~
                SDQ*EA*UL*0605388007391*7~
                LIN*791721*IN*579155281*UP*072470003225*VN*Zone 1***UK*00072470003225~
                PO4*1~
                N9*IK*980073392511210~
                ZA*QS~
                CTP**UCP*7.18~
                SDQ*EA*UL*0078742077857*2~
                LIN*791722*IN*594727122*UP*072470003829*VN*Zone 1***UK*00072470003829~
                PO4*1~
                N9*IK*980073392511210~
                ZA*QS~
                CTP**UCP*11.72~
                SDQ*EA*UL*0078742077857*4~
                CTT*20~
                SE*129*2678229~
                GE*1096*852103694~
                IEA*1*852103694~
                """;

        EdiConverter converter = new EdiConverter();

        List<ProductActivityDetail> items = converter.processFile(edi.getBytes(StandardCharsets.UTF_8));

        assertFalse(items.isEmpty());
        assertEquals("783496", items.get(0).getLineItemNumber());
        assertEquals("990402230", items.get(0).getProductId());
        assertEquals("072470006431", items.get(0).getUpcCode());
        assertEquals("12.29", items.get(0).getUnitPrice());
        assertEquals("0605388006059", items.get(0).getLocationId());
        assertEquals("3", items.get(0).getQuantity());
    }
}
