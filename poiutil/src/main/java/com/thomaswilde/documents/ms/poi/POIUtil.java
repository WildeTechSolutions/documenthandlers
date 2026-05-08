package com.thomaswilde.documents.ms.poi;

import org.apache.commons.io.FilenameUtils;
import org.apache.poi.extractor.ExtractorFactory;
import org.apache.poi.extractor.POITextExtractor;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.InvalidOperationException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.util.XMLHelper;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.model.Styles;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;

import org.openxmlformats.schemas.drawingml.x2006.main.CTNonVisualDrawingProps;
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTAnchor;
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTInline;
import org.openxmlformats.schemas.officeDocument.x2006.sharedTypes.STOnOff1;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFFCheckBox;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTOnOff;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.imageio.ImageIO;
import javax.xml.parsers.ParserConfigurationException;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by 1115095 on 3/11/2019.
 */
public class POIUtil {

    private static final Logger log = LoggerFactory.getLogger(POIUtil.class);
    public static final double WATER_MARK_HEIGHT_FACTOR = 0.04;
    
    public static String getTextFromWordDocument(String path){

        if(path.toLowerCase().endsWith(".doc")){

                return getTextFromDoc(path);

        }else if(path.toLowerCase().endsWith(".xlsx")){
            try {
                return getTextFromXlsxLarge(path);
            } catch (IOException | OpenXML4JException | SAXException e) {
                e.printStackTrace();
                return null;
            }
        }

        try (POITextExtractor textExtractor = ExtractorFactory.createExtractor(new File(path))) {



            return textExtractor.getText();
            
        }catch(InvalidOperationException e){
            log.warn("Doc {} is probably corrupt, returning a null string", path);
        }
        catch (IOException | IllegalArgumentException | Error e) {
//            e.printStackTrace();
            log.warn("Was not able to parse document {}", path);
            log.warn(e.getMessage());
        }

        return null;
    }

    public static String getTextFromXlsxNormal(String path){
        try (POITextExtractor textExtractor = ExtractorFactory.createExtractor(new File(path))) {


            return textExtractor.getText();

        } catch (IOException | IllegalArgumentException | Error e) {
//            e.printStackTrace();
            log.warn("Was not able to parse document {}", path);
            log.warn(e.getMessage());
        }

        return null;
    }

    public static String getTextFromDoc(String path) throws IllegalArgumentException{
        StringBuilder sb = new StringBuilder();
        File file = new File(path);
        try(
                FileInputStream  fis = new FileInputStream(file.getAbsolutePath());
                HWPFDocument doc = new HWPFDocument(fis);
                WordExtractor we = new WordExtractor(doc);
                ){



            String[] paragraphs = we.getParagraphText();

            System.out.println("Total no of paragraph "+paragraphs.length);
            for (String para : paragraphs) {
                sb.append(para);
            }


        } catch(IOException e){
            e.printStackTrace();
        }


        return sb.toString();
    }

    public static String getTextFromXlsxLarge(String path) throws IOException, OpenXML4JException, SAXException {

        StringBuilder text = new StringBuilder();

        OPCPackage xlsxPackage = null;
        try {
            xlsxPackage = OPCPackage.open(path, PackageAccess.READ);
        } catch (InvalidFormatException e) {
            log.warn("Could not open xlsx, invalid format");
            return "";
        } catch (InvalidOperationException e) {
            log.warn("Could not open xlsx, invalid operation, file may be corrupt");
            return "";
        }

        ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(xlsxPackage);
        XSSFReader xssfReader = new XSSFReader(xlsxPackage);


        StylesTable styles = xssfReader.getStylesTable();
        XSSFReader.SheetIterator iter = (XSSFReader.SheetIterator) xssfReader.getSheetsData();
        int index = 0;
        while (iter.hasNext()) {
            try (InputStream stream = iter.next()) {
                String sheetName = iter.getSheetName();

                log.debug(sheetName + " [index=" + index + "]:");
                final boolean[] rowHasText = {false};
                processSheet(styles, strings, new XSSFSheetXMLHandler.SheetContentsHandler() {

                    @Override
                    public void startRow(int rowNum) {
                        rowHasText[0] = false;
                    }

                    @Override
                    public void endRow(int rowNum) {
                        if (rowHasText[0]) {
                            text.delete(text.length() - 2, text.length()-1);
                            text.append("\n");
                        }

                    }

                    @Override
                    public void cell(String cellReference, String formattedValue, XSSFComment comment) {

                        // If the formatted value contains a decimal, and it parses to a double, we don't want to index it
                        // Integers however may be important
                        if(formattedValue.contains(".")){
                            try {
                                double num = Double.parseDouble(formattedValue);
                                return;
                            } catch (NumberFormatException e) {

                            }
                        }
                        rowHasText[0] = true;


                        text.append(formattedValue + ", ");
                    }
                }, stream);
            }
            ++index;
        }

        xlsxPackage.close();

        return text.toString();
    }

    private static void processSheet(
            Styles styles,
            SharedStrings strings,
            XSSFSheetXMLHandler.SheetContentsHandler sheetHandler,
            InputStream sheetInputStream) throws IOException, SAXException {
        // set emulateCSV=true on DataFormatter - it is also possible to provide a Locale
        // when POI 5.2.0 is released, you can call formatter.setUse4DigitYearsInAllDateFormats(true)
        // to ensure all dates are formatted with 4 digit years
        DataFormatter formatter = new DataFormatter(true);
        InputSource sheetSource = new InputSource(sheetInputStream);
        try {
            XMLReader sheetParser = XMLHelper.newXMLReader();
            ContentHandler handler = new XSSFSheetXMLHandler(
                    styles, null, strings, sheetHandler, formatter, false);
            sheetParser.setContentHandler(handler);

            sheetParser.parse(sheetSource);

        } catch(ParserConfigurationException e) {
            throw new RuntimeException("SAX parser appears to be broken - " + e.getMessage());
        }
    }

    public static XWPFDocument createDocument(String path){
        InputStream fis = null;
        XWPFDocument document = null;
        try {
            fis = new FileInputStream(new File(path));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        try {
            document = new XWPFDocument(fis);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return document;
    }

    public static Map<String, String> getContentControlTitleValue(String path){
        Map<String, String> map = new LinkedHashMap<>();

        XWPFDocument document = createDocument(path);

        List<IBodyElement> elements = document.getBodyElements();

//        List<XWPFAbstractSDT> sdts = new ArrayList<XWPFAbstractSDT>();
        for (IBodyElement e : elements) {
            if (e instanceof XWPFSDT) {
                XWPFSDT sdt = (XWPFSDT) e;
//                System.out.println(String.format("title: %s, tag: %s, text: %s", sdt.getTitle(), sdt.getTag(), sdt.getContent().getText()));
                map.put(sdt.getTitle(), sdt.getContent().getText());

//                sdts.add(sdt);
            } else if (e instanceof XWPFParagraph) {
                XWPFParagraph p = (XWPFParagraph) e;
                for (IRunElement e2 : p.getIRuns()) {
                    if (e2 instanceof XWPFSDT) {
                        XWPFSDT sdt = (XWPFSDT) e2;

                        map.put(sdt.getTitle(), sdt.getContent().getText());
//                        sdts.add(sdt);
                    }
                }
            }
        }
        return map;

    }

    public static XWPFDocument replacePOI(XWPFDocument doc, String placeHolder, String replaceText) {
        // REPLACE ALL HEADERS

        for (XWPFHeader header : doc.getHeaderList())
            replaceAllBodyElements(header.getBodyElements(), placeHolder, replaceText);
        // REPLACE BODY
        replaceAllBodyElements(doc.getBodyElements(), placeHolder, replaceText);
        return doc;
    }

    public static void replaceTextInTextBox(XWPFDocument document, String placeHolder, String replaceText){
        try {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                XmlCursor cursor = paragraph.getCTP().newCursor();
                cursor.selectPath("declare namespace w='http://schemas.openxmlformats.org/wordprocessingml/2006/main' .//*/w:txbxContent/w:p/w:r");

                List<XmlObject> ctrsintxtbx = new ArrayList<XmlObject>();

                while(cursor.hasNextSelection()) {
                    cursor.toNextSelection();
                    XmlObject obj = cursor.getObject();
                    ctrsintxtbx.add(obj);
                }
                for (XmlObject obj : ctrsintxtbx) {
                    CTR ctr = (CTR) CTR.Factory.parse(obj.xmlText());
                    //CTR ctr = CTR.Factory.parse(obj.newInputStream());
                    XWPFRun bufferrun = new XWPFRun(ctr, (IRunBody)paragraph);
                    String text = bufferrun.getText(0);
                    if (text != null && text.contains(placeHolder)) {
                        text = text.replace(placeHolder, replaceText);
                        bufferrun.setText(text, 0);
                    }
                    obj.set(bufferrun.getCTR());
                }
            }
        } catch (XmlException e) {
            e.printStackTrace();
        }
    }

    private static void replaceAllBodyElements(List<IBodyElement> bodyElements, String placeHolder, String replaceText) {
        for (IBodyElement bodyElement : bodyElements) {
            if (bodyElement.getElementType().compareTo(BodyElementType.PARAGRAPH) == 0) {
//                replaceParagraph((XWPFParagraph) bodyElement, placeHolder, replaceText);
                HashMap<String, String> map = new HashMap<>();
                map.put(placeHolder, replaceText);
                List<XWPFParagraph> xwpfParagraphs = new ArrayList<XWPFParagraph>();
                xwpfParagraphs.add((XWPFParagraph) bodyElement);

                replaceInParagraphs(map, xwpfParagraphs);
            }
            if (bodyElement.getElementType().compareTo(BodyElementType.TABLE) == 0)
                replaceTextInTable((XWPFTable) bodyElement, placeHolder, replaceText);
        }
    }

    public static void replaceTextInTable(XWPFTable table, String placeHolder, String replaceText) {
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                for (IBodyElement bodyElement : cell.getBodyElements()) {
                    if (bodyElement.getElementType().compareTo(BodyElementType.PARAGRAPH) == 0) {
//                        replaceInParagraphs((XWPFParagraph) bodyElement, placeHolder, replaceText);
                        HashMap<String,String> map = new HashMap<>();
                        map.put(placeHolder, replaceText);
                        List<XWPFParagraph> xwpfParagraphs = new ArrayList<XWPFParagraph>();
                        xwpfParagraphs.add((XWPFParagraph) bodyElement);

                        replaceInParagraphs(map, xwpfParagraphs);
                    }
                    if (bodyElement.getElementType().compareTo(BodyElementType.TABLE) == 0) {
                        replaceTextInTable((XWPFTable) bodyElement, placeHolder, replaceText);
                    }
                }
            }
        }
    }

    private static long replaceInParagraphs(Map<String, String> replacements, List<XWPFParagraph> xwpfParagraphs) {
        long count = 0;
        for (XWPFParagraph paragraph : xwpfParagraphs) {
            List<XWPFRun> runs = paragraph.getRuns();

            for (Map.Entry<String, String> replPair : replacements.entrySet()) {
                String find = replPair.getKey();
                String repl = replPair.getValue();
                TextSegment found = paragraph.searchText(find, new PositionInParagraph());
                if ( found != null ) {
                    count++;
                    if ( found.getBeginRun() == found.getEndRun() ) {
                        // whole search string is in one Run
                        XWPFRun run = runs.get(found.getBeginRun());
                        String runText = run.getText(run.getTextPosition());

                            String replaced = runText.replace(find, repl);


                            // New code to ensure new lines are created
                            BufferedReader bufferedReader = new BufferedReader(new StringReader(replaced));

                            try {
                                String line = bufferedReader.readLine();
                                if (line != null) {
                                    run.setText(line, 0);
                                }else {
                                	run.setText("", 0);
                                }
                                // Continue to add runs for each line
                                line = bufferedReader.readLine();
                                while (line != null) {
//                                run = paragraph.createRun();
                                    run.addBreak();
                                    run.setText(line);
                                    line = bufferedReader.readLine();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }



                    } else {
                        // The search string spans over more than one Run
                        // Put the Strings together
                        StringBuilder b = new StringBuilder();
                        for (int runPos = found.getBeginRun(); runPos <= found.getEndRun(); runPos++) {
                            XWPFRun run = runs.get(runPos);
                            b.append(run.getText(run.getTextPosition()));
                        }
                        String connectedRuns = b.toString();
                        String replaced = connectedRuns.replace(find, repl);

                        // The first Run receives the replaced String of all connected Runs
                        XWPFRun partOne = runs.get(found.getBeginRun());
                        partOne.setText(replaced, 0);
                        // Removing the text in the other Runs.
                        for (int runPos = found.getBeginRun()+1; runPos <= found.getEndRun(); runPos++) {
                            XWPFRun partNext = runs.get(runPos);
                            partNext.setText("", 0);
                        }
                    }
                }
            }
        }
        return count;
    }

    public static void setAltText(XWPFRun run, String text){
        for (XWPFRun r : run.getParagraph().getRuns()) {
            r.getCTR().getDrawingList().forEach(d -> {
                // Inline images
                CTInline inline = d.getInlineArray(0);
                if (inline != null) {
                    CTNonVisualDrawingProps docPr = inline.getDocPr();
                    docPr.setDescr(text);
                    docPr.setName(text);
                }

                // Floating images
                if (d.getAnchorList() != null) {
                    for (CTAnchor anchor : d.getAnchorList()) {
                        CTNonVisualDrawingProps docPr = anchor.getDocPr();
                        docPr.setDescr(text);
                        docPr.setName(text);
                    }
                }
            });
        }
    }


    public static void checkCheckBoxes(XWPFDocument document, List<Boolean> selections) {
        List<CTFFCheckBox> checkBoxes = getAllCheckBoxes(document);

        for (int i = 0; i < checkBoxes.size(); i++) {
            CTFFCheckBox checkBox = checkBoxes.get(i);
            Boolean selection = selections.get(i);
            if(selection != null){
                CTOnOff onOff = CTOnOff.Factory.newInstance();
                onOff.setVal(selection ? STOnOff1.ON : STOnOff1.OFF);
                checkBox.setChecked(onOff);
            }

        }
    }

    private static List<CTFFCheckBox> getCheckBoxesFromParagraphs(List<XWPFParagraph> paragraphs){

        List<CTFFCheckBox> checkBoxes = new ArrayList<>();
        for (XWPFParagraph paragraph : paragraphs) {
            XmlCursor cursor = paragraph.getCTP().newCursor();
            cursor.selectPath("declare namespace w='http://schemas.openxmlformats.org/wordprocessingml/2006/main' .//w:checkBox");

            while(cursor.hasNextSelection()) {
                cursor.toNextSelection();
                CTFFCheckBox checkBox = (CTFFCheckBox) cursor.getObject();
                checkBoxes.add(checkBox);
            }
        }

        return checkBoxes;
    }

    private static List<CTFFCheckBox> getCheckBoxesFromTable(XWPFTable table){
        List<CTFFCheckBox> checkBoxes = new ArrayList<>();

        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                for (IBodyElement bodyElement : cell.getBodyElements()) {
                    if (bodyElement.getElementType().compareTo(BodyElementType.PARAGRAPH) == 0) {
                        List<XWPFParagraph> xwpfParagraphs = new ArrayList<XWPFParagraph>();
                        xwpfParagraphs.add((XWPFParagraph) bodyElement);

                        checkBoxes.addAll(getCheckBoxesFromParagraphs(xwpfParagraphs));
                    }
                    if (bodyElement.getElementType().compareTo(BodyElementType.TABLE) == 0) {
                        checkBoxes.addAll(getCheckBoxesFromTable((XWPFTable) bodyElement));
                    }
                }
            }
        }
        return checkBoxes;
    }

    public static List<CTFFCheckBox> getAllCheckBoxes(XWPFDocument doc){
        List<CTFFCheckBox> checkBoxes = new ArrayList<>();

        List<IBodyElement> bodyElements = doc.getBodyElements();
        for (IBodyElement bodyElement : bodyElements) {
            if (bodyElement.getElementType().compareTo(BodyElementType.PARAGRAPH) == 0) {

                List<XWPFParagraph> xwpfParagraphs = new ArrayList<XWPFParagraph>();
                xwpfParagraphs.add((XWPFParagraph) bodyElement);

                checkBoxes.addAll(getCheckBoxesFromParagraphs(xwpfParagraphs));
            }
            if (bodyElement.getElementType().compareTo(BodyElementType.TABLE) == 0) {
                checkBoxes.addAll(getCheckBoxesFromTable((XWPFTable) bodyElement));
            }

        }
        return checkBoxes;
    }

    public static void modifyCellContent(XWPFTable headerTable, Cell cell, String value){

        // Get the contact row
        XWPFTableRow rowToModify = headerTable.getRows().get(cell.getRow());

        // Get the contact row cells
        List<XWPFTableCell> cellToModify = rowToModify.getTableCells();

        // Set the Contents

        // XWPFParagraph paragraph = header.getListParagraph().iterator().next();

        XWPFParagraph paragraph = cellToModify.get(cell.getCol()).getParagraphs().iterator().next();
        String fontFamily = paragraph.getRuns().get(0).getFontFamily();
        int fontSize = paragraph.getRuns().get(0).getFontSize();
        boolean isBold = paragraph.getRuns().get(0).isBold();
//        paragraph.getRuns().forEach(xwpfRun -> xwpfRun.setText(""));

        // Clear the cell contents
        cellToModify.get(cell.getCol()).getCTTc().setPArray(new CTP[] {(CTP) CTP.Factory.newInstance()});

        XWPFRun run = paragraph.createRun();
        run.setFontFamily("Arial");
        run.setFontSize(11);
        run.setBold(true);
        // Set the job number
        run.setText(value);
    }

    public static void modifyCellContent(XWPFTable headerTable, Cell cell, String value, boolean bold){

        // Get the contact row
        XWPFTableRow rowToModify = headerTable.getRows().get(cell.getRow());

        // Get the contact row cells
        List<XWPFTableCell> cellToModify = rowToModify.getTableCells();

        // Set the Contents
        // XWPFParagraph paragraph = header.getListParagraph().iterator().next();
        XWPFParagraph paragraph = cellToModify.get(cell.getCol()).getParagraphs().iterator().next();
        XWPFRun run = paragraph.createRun();

        // Set the job number
        run.setBold(bold);
        run.setText(value);
    }

    public static void modifyCellContent(XWPFTable table, Cell cell, String value, boolean bold, int fontSize) {

        // With different templates, ensure that the cell is not null because not all templates have the same
        // fields


        //
        if (cell != null) {
            // Get the  row
            XWPFTableRow rowToModify = table.getRows().get(cell.getRow());

            // Get the cells of that row (columns)
            List<XWPFTableCell> cellToModify = rowToModify.getTableCells();

            // Set the Contents
//            
//            
            XWPFParagraph paragraph = cellToModify.get(cell.getCol()).getParagraphs().iterator().next();
//			paragraph.removeRun(0);
            XWPFRun run = paragraph.createRun();

            // Set the value
            run.setBold(bold);
            run.setFontSize(fontSize);
            run.setText(value);
        }
    }

    public static void modifyRowContent(XWPFTableRow rowToModify, int col, String value, boolean bold, int fontSize){
        List<XWPFTableCell> cellToModify = rowToModify.getTableCells();

        // Set the Contents
//            
//            
        XWPFParagraph paragraph = cellToModify.get(col).getParagraphs().iterator().next();
//        paragraph.setStyle("Normal");
//			paragraph.removeRun(0);
        XWPFRun run = paragraph.createRun();

        // Set the value
        run.setBold(bold);
        run.setFontSize(fontSize);
        run.setFontFamily("Arial");

        run.setText(value);

    }

    public static void modifyRowContent(XWPFTableRow rowToModify, int col, String value){
        List<XWPFTableCell> cellToModify = rowToModify.getTableCells();

        // Set the Contents
//            
//            
        XWPFParagraph paragraph = cellToModify.get(col).getParagraphs().iterator().next();
//			paragraph.removeRun(0);
        XWPFRun run = paragraph.createRun();

        // Set the value
        run.setText(value);

    }

    public static void addRowToTable(XWPFTable table){
//        //insert new row, which is a copy of row 2, as new row 3:
//        XWPFTableRow oldRow = table.getRow(1);
//        CTRow ctrow = null;
//        try {
//            ctrow = CTRow.Factory.parse(oldRow.getCtRow().newInputStream());
//        } catch (XmlException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        XWPFTableRow newRow = new XWPFTableRow(ctrow, table);
//
//        int i = 1;
//        for (XWPFTableCell cell : newRow.getTableCells()) {
//            for (XWPFParagraph paragraph : cell.getParagraphs()) {
//                for (XWPFRun run : paragraph.getRuns()) {
//                    run.setText("New row 3 cell " + i++, 0);
//                }
//            }
//        }
//
//        table.addRow(newRow, 2);

        CTRow ctrow = null;
        //insert new last row, which is a copy previous last row:
        XWPFTableRow lastRow = table.getRows().get(table.getNumberOfRows() - 1);
        try {
            ctrow = (CTRow) CTRow.Factory.parse(lastRow.getCtRow().newInputStream());
        } catch (XmlException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        XWPFTableRow newRow = new XWPFTableRow(ctrow, table);

        int i = 1;
        for (XWPFTableCell cell : newRow.getTableCells()) {
            for (XWPFParagraph paragraph : cell.getParagraphs()) {
//                paragraph.removeRun(0);
                for (XWPFRun run : paragraph.getRuns()) {
                    run.setText("" + i++, 0);
                }
            }
        }

        table.addRow(newRow);
    }

    /**
     * @param table
     * @return Returns a new row that was copied from the last row of the passed table. Make changes to this new row and then add it
     * to the table manually.
     */
    public static XWPFTableRow getNewRow(XWPFTable table) {
        CTRow ctrow = null;
        //insert new last row, which is a copy previous last row:
        XWPFTableRow lastRow = table.getRows().get(table.getNumberOfRows() - 1);
        try {
            ctrow = CTRow.Factory.parse(lastRow.getCtRow().newInputStream());
        } catch (XmlException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        XWPFTableRow newRow = new XWPFTableRow(ctrow, table);


        for (XWPFTableCell cell : newRow.getTableCells()) {
            for (XWPFParagraph paragraph : cell.getParagraphs()) {
//                 paragraph.removeRun(0);
                for (XWPFRun run : paragraph.getRuns()) {
                    run.setText("", 0);
                }
            }
        }

        return newRow;
    }

    /**
     * @param row
     * @return Returns a new row that is a copy (with blank cells) of the row that you passed through.
     */
    public static XWPFTableRow cloneRow(XWPFTableRow row) {
        CTRow ctrow = null;
        try {
            ctrow = CTRow.Factory.parse(row.getCtRow().newInputStream());
        } catch (XmlException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        XWPFTableRow newRow = new XWPFTableRow(ctrow, row.getTable());


        for (XWPFTableCell cell : newRow.getTableCells()) {
            for (XWPFParagraph paragraph : cell.getParagraphs()) {
//                paragraph.removeRun(0);
                for (XWPFRun run : paragraph.getRuns()) {
                    run.setText("", 0);
                }
            }
        }

        return newRow;
    }

    public static void modifyRowCell(XWPFTableRow newRow, int column, String text){
        newRow.getCell(column).setText(text);
    }


    // -------------- MyWorkspace Specific

    public static void openDocument(Path filePath){
//        
//        progressCallback.update("Opening Document...");
        if (filePath != null){
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(filePath.toFile());
                    
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    public static Path saveDocument(Path filePath, SXSSFWorkbook document){
        OutputStream out;

        try {

            if(filePath != null){

                out = new FileOutputStream(filePath.toFile());
                document.write(out);
                out.close();
                document.close();
            }

        } catch (FileNotFoundException e) {
            // This is called if the network drive is not set to N.
            e.printStackTrace();
            filePath = null;
        } catch (IOException ioe){
            
            ioe.printStackTrace();
            filePath = null;
        }
        return filePath;
    }

    public static Path saveDocument(Path filePath, XWPFDocument document){
        OutputStream out;

        try {

            if(filePath != null){

                int num = 0;
                String baseName = FilenameUtils.getBaseName(filePath.getFileName().toString());
                while(Files.exists(filePath)){
//                    filePath.res
                    num++;
                    filePath = filePath.resolveSibling(
                            baseName + "_" + num + "." +  FilenameUtils.getExtension(filePath.getFileName().toString()));
                }
                out = new FileOutputStream(filePath.toFile());
                document.write(out);
                out.close();
                document.close();
            }

        } catch (FileNotFoundException e) {
            // This is called if the network drive is not set to N.
            e.printStackTrace();
            filePath = null;
        } catch (IOException ioe){
            
            ioe.printStackTrace();
            filePath = null;
        }
        return filePath;
    }

    public static Path getNextPath(Path filePath){
        int num = 0;
        String baseFileName = FilenameUtils.getBaseName(filePath.getFileName().toString());
        while(Files.exists(filePath)){
//                    filePath.res
            num++;

            filePath = filePath.resolveSibling(baseFileName + "_" + num + "." +  FilenameUtils.getExtension(filePath.getFileName().toString()));
        }
        return filePath;
    }

    public static Path saveDocument(Path filePath, XSSFWorkbook document){
        OutputStream out;

        try {

            if(filePath != null){

                out = new FileOutputStream(filePath.toFile());
                document.write(out);
                out.close();
                document.close();
            }

        } catch (FileNotFoundException e) {
            // This is called if the network drive is not set to N.
            e.printStackTrace();
            filePath = null;
        } catch (IOException ioe){
            
            ioe.printStackTrace();
            filePath = null;
        }
        return filePath;
    }

//    public static void setDocumentMetadata(Job job, XWPFDocument document){
//        POIXMLProperties.CoreProperties meta = document.getProperties().getCoreProperties();
////		meta.setCreator(this.labContact);
////		meta.setSubjectProperty(this.jobName);
////		meta.setTitle(this.jobTitle);
//        if(job.getLead() != null && job.getLead().getFirstAndLastName() != null){
//            meta.setCreator(job.getLead().getFirstAndLastName());
//        }
//
//        meta.setSubjectProperty(job.getJobNo());
//        meta.setTitle(job.getJobNo());
//
//        String keyWords = generateKeywords(job);
//        if(keyWords != null && keyWords.length() > 0){
//            meta.setKeywords(keyWords);
//        }
//    }

    public static int getImageFormat(String imgFileName) {
        int format;
        imgFileName = imgFileName.toLowerCase();
        if (imgFileName.endsWith(".emf"))
            format = XWPFDocument.PICTURE_TYPE_EMF;
        else if (imgFileName.endsWith(".wmf"))
            format = XWPFDocument.PICTURE_TYPE_WMF;
        else if (imgFileName.endsWith(".pict"))
            format = XWPFDocument.PICTURE_TYPE_PICT;
        else if (imgFileName.endsWith(".jpeg") || imgFileName.endsWith(".jpg"))
            format = XWPFDocument.PICTURE_TYPE_JPEG;
        else if (imgFileName.endsWith(".png"))
            format = XWPFDocument.PICTURE_TYPE_PNG;
        else if (imgFileName.endsWith(".dib"))
            format = XWPFDocument.PICTURE_TYPE_DIB;
        else if (imgFileName.endsWith(".gif"))
            format = XWPFDocument.PICTURE_TYPE_GIF;
        else if (imgFileName.endsWith(".tiff") || imgFileName.endsWith(".tif"))
            format = XWPFDocument.PICTURE_TYPE_TIFF;
        else if (imgFileName.endsWith(".eps"))
            format = XWPFDocument.PICTURE_TYPE_EPS;
        else if (imgFileName.endsWith(".bmp"))
            format = XWPFDocument.PICTURE_TYPE_BMP;
        else if (imgFileName.endsWith(".wpg"))
            format = XWPFDocument.PICTURE_TYPE_WPG;
        else {
            return 0;
        }
        return format;
    }


    public static void addTextWatermark(String text, BufferedImage sourceImage, String sourceFileName, File destFile) {

        Graphics2D g2d = (Graphics2D) sourceImage.getGraphics();

        // initializes necessary graphic properties
        AlphaComposite alphaChannel = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
        g2d.setComposite(alphaChannel);
        g2d.setColor(Color.BLUE);

        

        int fontSize = (int) (sourceImage.getHeight()*WATER_MARK_HEIGHT_FACTOR);

        g2d.setFont(new Font("Arial", Font.BOLD, fontSize));
        FontMetrics fontMetrics = g2d.getFontMetrics();
        Rectangle2D rect = fontMetrics.getStringBounds(text, g2d);

        // calculates the coordinate where the String is painted
//        int centerX = (int) rect.getWidth() / 2;
        int centerX = 0;
        int centerY = (int) (sourceImage.getHeight()*0.99);

        // paints the textual watermark
        g2d.drawString(text, centerX, centerY);


        try {
            ImageIO.write(sourceImage, FilenameUtils.getExtension(sourceFileName), destFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        g2d.dispose();

//        

    }

    



}
