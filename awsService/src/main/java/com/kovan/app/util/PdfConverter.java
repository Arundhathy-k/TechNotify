package com.kovan.app.util;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import org.springframework.stereotype.Component;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@Component
public class PdfConverter {

    public File convertHtmlToPdf(String htmlContent, String fileName) throws IOException {
        File pdfFile = new File(fileName);

        try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
            ConverterProperties converterProperties = new ConverterProperties();
            converterProperties.setBaseUri(null);

            HtmlConverter.convertToPdf(htmlContent, fos, converterProperties);
        } catch (Exception e) {
            throw new IOException("Error generating PDF from HTML content using iText", e);
        }

        return pdfFile;
    }
}
