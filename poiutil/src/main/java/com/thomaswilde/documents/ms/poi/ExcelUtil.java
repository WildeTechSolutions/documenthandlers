package com.thomaswilde.documents.ms.poi;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.apache.poi.ss.usermodel.Cell;

public class ExcelUtil {

    public static String getStringFromCell(Cell cell) {
        if(cell != null) {
            switch (cell.getCellType()) {
                case STRING:
                    String value = cell.getStringCellValue();
                    if(value != null){
                        value = value.replaceAll("\u00A0", "").trim();
                    }
                    return value;
                case NUMERIC:
                    return Integer.toString((int) cell.getNumericCellValue());
            }
        }
        return null;
    }

    public static int getIntFromCell(Cell cell){
        if(cell != null) {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return (int) cell.getNumericCellValue();
                default:
                    return 0;
            }
        }
        return 0;
    }

    public static double getDoubleFromCell(Cell cell){
        if(cell != null) {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return cell.getNumericCellValue();
                default:
                    return 0;
            }
        }
        return 0;
    }

    public static LocalDate getDateFromCell(Cell cell){
        if(cell != null) {
            switch (cell.getCellType()) {
                case STRING:
                    System.out.println("Date was of type string, value: " + cell.getStringCellValue());
                    break;
                case NUMERIC:
                    return convertToLocalDate(new Date(cell.getDateCellValue().getTime()));
            }
        }
        return null;
    }

    public static LocalDateTime getDateTimeFromCell(Cell cell){
        if(cell != null) {
            switch (cell.getCellType()) {
                case STRING:
                    System.out.println("Date was of type string, value: " + cell.getStringCellValue());
                    break;
                case NUMERIC:
                    return convertToLocalDateTime(new Date(cell.getDateCellValue().getTime()));
            }
        }
        return null;
    }

    public static LocalDate convertToLocalDate(Date dateToConvert) {
        return (new java.sql.Date(dateToConvert.getTime())).toLocalDate();
    }

    public static LocalDateTime convertToLocalDateTime(Date dateToConvert) {
//        return dateToConvert.toInstant()
//                .atZone(ZoneId.systemDefault())
//                .toLocalDateTime();
        if(dateToConvert != null)
            return Instant.ofEpochMilli(dateToConvert.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();

        return null;

    }
}
