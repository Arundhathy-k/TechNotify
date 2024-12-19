package com.kovan.app.util;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

@Component
public class PdfConverter {

    public File convertHtmlToPdf(String htmlContent, String fileName) throws IOException {
        File pdfFile = new File(fileName);
        try (OutputStream os = new FileOutputStream(pdfFile)) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(htmlContent, null); // Set baseURL if required
            builder.toStream(os);
            builder.run();
        } catch (Exception e) {
            throw new IOException("Error generating PDF from HTML content", e);
        }
        return pdfFile;
    }
}
