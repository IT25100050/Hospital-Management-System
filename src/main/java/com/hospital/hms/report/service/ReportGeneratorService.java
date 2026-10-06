package com.hospital.hms.report.service;

import com.hospital.hms.report.util.PdfGenerator;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

@Service
public class ReportGeneratorService {

    private final PdfGenerator pdfGenerator;

    public ReportGeneratorService(PdfGenerator pdfGenerator) {
        this.pdfGenerator = pdfGenerator;
    }

    public ByteArrayInputStream generatePatientReport(Long patientId) {
        String title = "Patient Report - ID: " + patientId;
        String content = "Medical history, prescription list, and lab test details go here.";
        return pdfGenerator.generateSimplePdf(title, content);
    }

    public ByteArrayInputStream generateFinancialReport() {
        String title = "Hospital Financial Summary Report";
        String content = "Billing, payments collected, and outstanding dues overview.";
        return pdfGenerator.generateSimplePdf(title, content);
    }
}
