package com.hospital.hms.report.util;

import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Component
public class PdfGenerator {

    public ByteArrayInputStream generateSimplePdf(String title, String content) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Mock PDF content bytes generation
        String pdfText = "=== " + title + " ===\n\n" + content;
        out.writeBytes(pdfText.getBytes());

        return new ByteArrayInputStream(out.toByteArray());
    }
}
