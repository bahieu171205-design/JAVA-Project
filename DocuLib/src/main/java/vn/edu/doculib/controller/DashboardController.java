package vn.edu.doculib.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import vn.edu.doculib.service.DashboardService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("stats", dashboardService.getStats());
        model.addAttribute("recentMaterials", dashboardService.getRecentMaterials());
        model.addAttribute("recentAcquisitions", dashboardService.getRecentAcquisitions());
        model.addAttribute("todayLabel", LocalDate.now().format(
                DateTimeFormatter.ofPattern("EEEE, dd 'tháng' MM yyyy", Locale.forLanguageTag("vi-VN"))));
        return "dashboard";
    }
}
