package com.thomaswilde.documents.ms.poi;

import org.apache.poi.sl.usermodel.Shape;
import org.apache.poi.sl.usermodel.Slide;
import org.apache.poi.sl.usermodel.TextBox;
import org.apache.poi.sl.usermodel.TextParagraph;
import org.apache.poi.sl.usermodel.TextRun;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableCell;
import org.apache.poi.xslf.usermodel.XSLFTableRow;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PPTUtil {
//    public static void replaceAllText(XMLSlideShow pptx, String placeHolder, String replaceText) {
//        for (Slide slide : pptx.getSlides()) {
//            replaceInSlide(slide, placeHolder, replaceText);
//        }
//    }
//
//    private static void replaceInSlide(Slide slide, String placeHolder, String replaceText) {
//        for (Object shape : slide.getShapes()) {
//            if (shape instanceof TextBox) {
//                replaceInTextBox((TextBox) shape, placeHolder, replaceText);
//            } else if (shape instanceof XSLFTable) {
//                replaceTextInTable((XSLFTable) shape, placeHolder, replaceText);
//            }
//        }
//    }
//
//    public static void replaceTextInTable(XSLFTable table, String placeHolder, String replaceText) {
//        for (XSLFTableRow row : table.getRows()) {
//            for (XSLFTableCell cell : row.getCells()) {
//                for (TextParagraph paragraph : cell.getTextParagraphs()) {
//                    HashMap<String, String> map = new HashMap<>();
//                    map.put(placeHolder, replaceText);
//                    replaceInParagraphs(map, List.of(paragraph));
//                }
//            }
//        }
//    }
//
//    private static void replaceInTextBox(TextBox textBox, String placeHolder, String replaceText) {
//        for (Object paragraph : textBox.getTextParagraphs()) {
//            HashMap<String, String> map = new HashMap<>();
//            map.put(placeHolder, replaceText);
//            replaceInParagraphs(map, List.of((TextParagraph) paragraph));
//        }
//    }
//
//    private static long replaceInParagraphs(Map<String, String> replacements, List<TextParagraph> paragraphs) {
//        long count = 0;
//        for (TextParagraph paragraph : paragraphs) {
//            for (Map.Entry<String, String> replPair : replacements.entrySet()) {
//                String find = replPair.getKey();
//                String repl = replPair.getValue();
//
//                List<TextRun> runs = paragraph.getTextRuns();
//                StringBuilder paragraphText = new StringBuilder();
//                for (TextRun run : runs) {
//                    paragraphText.append(run.getRawText());
//                }
//                String replacedParagraphText = paragraphText.toString().replace(find, repl);
//
//                // Clear existing runs
//                while (!runs.isEmpty()) {
//                    paragraph.removeTextRun(0);
//                }
//
//                // Re-add the replaced text as a new run (or potentially multiple runs
//                // if we want to try and preserve more granular formatting)
//                TextRun newRun = paragraph.addNewTextRun();
//                if (!runs.isEmpty()) {
//                    copyFormatting(runs.get(0), newRun); // Try to copy formatting from the first run
//                }
//                newRun.setText(replacedParagraphText);
//
//                if (replacedParagraphText.contains(find)) {
//                    count++;
//                }
//            }
//        }
//        return count;
//    }
//
//    private static void copyFormatting(TextRun source, TextRun target) {
//        target.setBold(source.isBold());
//        target.setItalic(source.isItalic());
//        target.setUnderline(source.isUnderline());
//        target.setFontFamily(source.getFontFamily());
//        target.setFontSize(source.getFontSize());
//        target.setFontColor(source.getFontColor());
//        // Add other formatting properties as needed
//    }
}
