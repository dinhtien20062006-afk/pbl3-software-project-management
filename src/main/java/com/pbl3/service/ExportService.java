package com.pbl3.service;

import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.pbl3.repository.TaskRepository;
import com.pbl3.entity.Task;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

@Service
public class ExportService {
    @Autowired
    private TaskRepository taskRepository;

    public void exportTasksToExcel(Long projectId, OutputStream out) throws IOException {
    List<Task> tasks = taskRepository.findByProjectId(projectId);
    try (Workbook workbook = new XSSFWorkbook()) {
        Sheet sheet = workbook.createSheet("Tasks");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Tên Task");
        header.createCell(1).setCellValue("Trạng thái");

        int rowIdx = 1;
        for (Task t : tasks) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(t.getTaskName());
            row.createCell(1).setCellValue(t.getStatus().toString());
        }
        workbook.write(out);
    }
    }

    public void exportTasksToPdf(Long projectId, OutputStream out) throws IOException {
    PdfWriter writer = new PdfWriter(out);
    PdfDocument pdf = new PdfDocument(writer);
    Document document = new Document(pdf);

    document.add(new Paragraph("Báo cáo danh sách công việc dự án"));
    Table table = new Table(2);
    table.addCell("Tên Task");
    table.addCell("Trạng thái");

    List<Task> tasks = taskRepository.findByProjectId(projectId);
    for (Task t : tasks) {
        table.addCell(t.getTaskName());
        table.addCell(t.getStatus().toString());
    }
    document.add(table);
    document.close();
    }
}
