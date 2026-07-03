package com.lat.web;

import com.lat.extract.WordExtractor;
import com.lat.model.StudentReport;
import com.lat.service.AnalysisService;
import com.lat.service.AnalysisService.AnalysisResult;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;

import jakarta.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

@Controller
public class AnalysisController {

    private final AnalysisService service;

    public AnalysisController(AnalysisService service) {
        this.service = service;
    }

    /** Landing page with the upload form. */
    @GetMapping("/")
    public String index() {
        return "index";
    }

    /** Handle upload -> run pipeline -> render dashboard. */
    @PostMapping("/analyze")
    public String analyze(@RequestParam("lesson") MultipartFile lesson,
                          @RequestParam("chat") MultipartFile chat,
                          Model model, HttpSession session) throws Exception {

        String lessonText = readLesson(lesson);
        String chatText = new String(chat.getBytes());

        AnalysisResult result = service.analyze(lessonText, chatText);
        session.setAttribute("lastResult", result);   // for Excel export

        model.addAttribute("concepts", result.concepts());
        model.addAttribute("reports", result.reports());
        model.addAttribute("weakSpots", result.classWeakSpots());
        model.addAttribute("classAvg", classAverage(result.reports()));
        return "dashboard";
    }

    /** Export the last analysis to Excel (Accuracy/Test Evidence artifact). */
    @GetMapping("/export.xlsx")
    @ResponseBody
    public ResponseEntity<byte[]> export(HttpSession session) throws Exception {
        AnalysisResult result = (AnalysisResult) session.getAttribute("lastResult");
        if (result == null) return ResponseEntity.badRequest().build();

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Student Analysis");
            Row h = sheet.createRow(0);
            String[] cols = {"Student", "Coverage %", "Strengths", "Gaps", "Curiosity"};
            for (int i = 0; i < cols.length; i++) h.createCell(i).setCellValue(cols[i]);

            int r = 1;
            for (StudentReport rep : result.reports()) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(rep.student());
                row.createCell(1).setCellValue(rep.coveragePct());
                row.createCell(2).setCellValue(String.join(", ", rep.strengths()));
                row.createCell(3).setCellValue(String.join(", ", rep.gaps()));
                row.createCell(4).setCellValue(String.join(", ", rep.curiosity()));
            }
            for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);
            wb.write(out);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=analysis.xlsx")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(out.toByteArray());
        }
    }

    private String readLesson(MultipartFile f) throws Exception {
        String name = f.getOriginalFilename() == null ? "" : f.getOriginalFilename().toLowerCase();
        if (name.endsWith(".docx")) {
            java.io.File tmp = java.io.File.createTempFile("lesson", ".docx");
            f.transferTo(tmp);
            String text = WordExtractor.extractText(tmp.getAbsolutePath());
            tmp.delete();
            return text;
        }
        return new String(f.getBytes());
    }

    private double classAverage(List<StudentReport> reports) {
        return reports.isEmpty() ? 0.0 :
                Math.round(10.0 * reports.stream()
                        .mapToDouble(StudentReport::coveragePct).average().orElse(0)) / 10.0;
    }
}
