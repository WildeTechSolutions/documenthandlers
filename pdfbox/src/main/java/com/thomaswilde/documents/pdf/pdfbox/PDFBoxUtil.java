package com.thomaswilde.documents.pdf.pdfbox;

import com.thomaswilde.form.FormField;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.pdfbox.preflight.PreflightDocument;
import org.apache.pdfbox.preflight.ValidationResult;
import org.apache.pdfbox.preflight.ValidationResult.ValidationError;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.FileImageOutputStream;

public class PDFBoxUtil {

    private static Logger log = LoggerFactory.getLogger(PDFBoxUtil.class);

    public static String getText(String path) {

        String docText = null;
        PDDocument pdDoc = null;


        try {

            pdDoc = PDDocument.load(new File(path));
            docText = new PDFTextStripper().getText(pdDoc);

        } catch (IOException e) {
            e.printStackTrace();
            log.error(e.getMessage());
        } catch (NoClassDefFoundError e){
            e.printStackTrace();
            log.debug("Might be encrypted document");
            log.error(e.getMessage());
        } catch (IllegalArgumentException e){
            e.printStackTrace();
            log.error(e.getMessage());
        } catch (Error e){
            e.printStackTrace();
            log.error(e.getMessage());
        }finally {
            if(pdDoc != null){
                try {
                    pdDoc.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return docText;
    }

    public static void loadCorruptedDocument(Path docPath) throws IOException {
        // Load the corrupted PDF using PDFBox
        PDDocument document = PreflightDocument.load(docPath.toFile());

//        // Wrap the PDDocument with PreflightDocument for validation
//        PreflightDocument preflightDocument = new PreflightDocument(document);
//
//        // Validate the PreflightDocument
//        ValidationResult result = preflightDocument.validate();
//
//        // Check if the PDF is valid
//        if (result.isValid()) {
//            // The PDF is valid, save it to a new file
//            document.save(repairedPdfPath);
//            document.close();
//
//            System.out.println("PDF repaired successfully.");
//        } else {
//            // The PDF is not valid, print validation errors
//            List<ValidationError> errors = result.getErrors();
//            for (ValidationError error : errors) {
//                System.err.println("Validation Error: " + error.getErrorCode() + " - " + error.getDetails());
//            }
//
//            // Attempt to save the potentially repaired PDF (may not be successful)
//            document.save(repairedPdfPath);
//            document.close();
//        }
//
//        // Close the PreflightDocument
//        preflightDocument.close();

    }

    public static void extractImages(String docPath, Path outputDirectory, String outputFileNameBase){
        try {
            PDDocument document = PDDocument.load(new File(docPath));
            extractImages(document, outputDirectory, outputFileNameBase);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassCastException e) {
            System.out.println("Class castexception caught");
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            System.out.println(sw.toString());
        }catch (Exception e){
            System.out.println("Exception caught");
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            System.out.println(sw.toString());
        }
    }

    private static void extractImages(PDDocument document, Path directory, String fileNameBase){

        PDFStreamEngine pdfStreamEngine = new ImageExtractor(directory, fileNameBase);
        try {

            int pageNum = 0;
            for (PDPage page : document.getPages()) {
                pageNum++;
                pdfStreamEngine.processPage(page);
            }


        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (document != null) {
                try {
                    document.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static ByteArrayOutputStream fillForm(Path docPath, Map<String, String> formFieldValues) throws IOException {
        PDDocument pdfDocument = PDDocument.load(docPath.toFile());

        fillForm(pdfDocument, formFieldValues);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        pdfDocument.save(baos);
        pdfDocument.close();

        return baos;
    }

    public static void fillForm(Path docPath, Path outputPath, Map<String, String> formFieldValues) throws IOException {
        PDDocument pdfDocument = PDDocument.load(docPath.toFile());

        fillForm(pdfDocument, formFieldValues);

        pdfDocument.save(outputPath.toFile());
        pdfDocument.close();
    }

    public static void fillForm(Path docPath, Path outputPath, Object objectWithFormFields) throws IOException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        PDDocument pdfDocument = PDDocument.load(docPath.toFile());

        Map<String, String> formFieldValues = new HashMap<>();

        for(Field field : objectWithFormFields.getClass().getDeclaredFields()){
            if(field.isAnnotationPresent(FormField.class)){
                FormField formField = field.getAnnotation(FormField.class);
                Object value = PropertyUtils.getProperty(objectWithFormFields, field.getName());

                if(value != null){
                    if (value instanceof LocalDate){
                        formFieldValues.put(formField.name(), DateTimeFormatter.ofPattern(formField.dateFormat()).format((LocalDate) value));

                    }else{
                        formFieldValues.put(formField.name(), Objects.toString(value));
                    }
                }


            }
        }
        fillForm(pdfDocument, formFieldValues);

        pdfDocument.save(outputPath.toFile());
        pdfDocument.close();
    }

    private static void fillForm(PDDocument pdfDocument, Map<String, String> formFieldValues) throws IOException {
        PDDocumentCatalog docCatalog = pdfDocument.getDocumentCatalog();
        PDAcroForm acroForm = docCatalog.getAcroForm();
        if(acroForm != null){
            for(Map.Entry<String, String> entrySet : formFieldValues.entrySet()){
                log.debug("Setting {} to {}", entrySet.getKey(), entrySet.getValue());

                PDField field = acroForm.getField(entrySet.getKey());

                if(field == null){
                    log.warn("Field {} was did not exist", entrySet.getKey());
                }else{
                    field.setValue(entrySet.getValue());
                }

            }
        }
    }

    /**
     * Possible field types are Tx (Text), Ch (combo box), Btn (button? and/or checkbox?)
     * Values from Ch will be inside []
     * @param docPath
     * @return
     * @throws IOException
     */
    public static Map<String, Object> getFormFieldValues(Path docPath) throws IOException {

        Map<String, Object> fieldValues = new HashMap<>();

        try(PDDocument pdfDocument = PDDocument.load(docPath.toFile())){

            PDDocumentCatalog docCatalog = pdfDocument.getDocumentCatalog();
            PDAcroForm acroForm = docCatalog.getAcroForm();

            if(acroForm != null) {
                List<PDField> fields = acroForm.getFields();

                log.trace("Number of fields: {}", fields.size());

                fields.forEach(pdField -> {

                    log.trace("Field {} is of type {}, value {}", pdField.getFullyQualifiedName(), pdField.getFieldType(), pdField.getValueAsString());

                    String value = pdField.getValueAsString();

                    switch (pdField.getFieldType()) {
                        case "Ch":
                            if(value.endsWith("]")) value = value.substring(0, value.length() - 1);
                            if(value.startsWith("[")) value = value.substring(1);
                            break;
                    }

                    fieldValues.put(pdField.getFullyQualifiedName(), value);
                });
            }else{
                log.warn("Acro Form was null, nothing to parse");
            }

        }

        return fieldValues;

    }





}
