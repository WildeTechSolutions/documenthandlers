package com.thomaswilde.documents.parser;

import com.thomaswilde.documents.ms.poi.POIUtil;
import com.thomaswilde.documents.pdf.pdfbox.PDFBoxUtil;
//import com.thomaswilde.documents.pdf.itext.ITextUtil;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.rtf.RTFEditorKit;

public class DocParser {

    private static Logger log = LoggerFactory.getLogger(DocParser.class);

    public static String getTextFromDocument(Path docPath) throws IOException {


        if(docPath == null) {
            log.trace("Provided path to document was null, returning null");
            return null;
        }

        String ext = FilenameUtils.getExtension(docPath.getFileName().toString());

        switch (ext){
            case "pdf":
                log.debug("Parsing pdf doc file");
//                return ITextUtil.getText(docPath.toString());
                return PDFBoxUtil.getText(docPath.toString());
            case "doc":
                log.debug("Parsing ms doc file, ext: {}", ext);
                try {
                    return POIUtil.getTextFromDoc(docPath.toString());
                } catch (IllegalArgumentException e) {
                    log.warn("Document was really an RTF");
                    return getTextFromRTF(docPath.toString());
                }

            case "docx":
            case "pptx":
            case "xlsx":
                log.debug("Parsing ms doc file, ext: {}", ext);
                return POIUtil.getTextFromWordDocument(docPath.toString());
            case "rtf":
                log.debug("Parsing rtf file");
                return getTextFromRTF(docPath.toString());
            case "txt":
                log.debug("Parsing txt file");
                return getTextFromFile(docPath);

            default:
                log.debug("Provided document extension {} is not supported for parsing", ext);
                return null;

        }

    }

    public static String getTextFromRTF(String path){
        try {
            InputStream inputStream = new FileInputStream(new File(path));
            DefaultStyledDocument styledDoc = new DefaultStyledDocument();
            new RTFEditorKit().read(inputStream, styledDoc, 0);

            String text = styledDoc.getText(0, styledDoc.getLength());
            inputStream.close();
            return text;
        } catch (IOException | BadLocationException | IllegalArgumentException | Error e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String getTextFromFile(Path path) throws IOException {
        byte[] encoded = Files.readAllBytes(path);
        return new String(encoded, StandardCharsets.UTF_8);
    }
}
