package com.dataimport.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class FileParserService {

    public List<Map<String, Object>> parseFile(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        if (fileName.endsWith(".csv")) {
            return parseCsv(file);
        } else if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
            return parseExcel(file);
        } else {
            throw new IllegalArgumentException("不支持的文件格式，仅支持CSV和Excel文件");
        }
    }

    private List<Map<String, Object>> parseCsv(MultipartFile file) throws IOException {
        List<Map<String, Object>> dataList = new ArrayList<>();

        try (CSVParser parser = CSVParser.parse(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8),
                CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            List<String> headers = parser.getHeaderNames();

            for (CSVRecord record : parser) {
                Map<String, Object> rowData = new LinkedHashMap<>();
                for (String header : headers) {
                    rowData.put(header, record.get(header));
                }
                dataList.add(rowData);
            }
        }

        return dataList;
    }

    private List<Map<String, Object>> parseExcel(MultipartFile file) throws IOException {
        List<Map<String, Object>> dataList = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return dataList;
            }

            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(getCellValue(cell));
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                Map<String, Object> rowData = new LinkedHashMap<>();
                for (int j = 0; j < headers.size(); j++) {
                    Cell cell = row.getCell(j);
                    rowData.put(headers.get(j), getCellValue(cell));
                }
                dataList.add(rowData);
            }
        }

        return dataList;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf((long) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
}
