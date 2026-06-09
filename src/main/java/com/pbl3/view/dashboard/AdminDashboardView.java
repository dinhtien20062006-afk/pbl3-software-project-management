package com.pbl3.view.dashboard;

import com.pbl3.dto.response.DashboardResponse;
import com.pbl3.dto.response.DashboardResponse.DashboardTaskItem;
import com.pbl3.service.DashboardService;
import com.pbl3.view.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;

import java.util.Map;

@Route(value = "admin/dashboard", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class AdminDashboardView extends VerticalLayout {

    public AdminDashboardView(DashboardService dashboardService) {
        // Cấu hình Layout chính
        setPadding(true);
        setSpacing(true);
        setSizeFull();

        // 1. Tiêu đề trang
        add(createHeader());

        // Gọi Service lấy dữ liệu bản tin Dashboard
        DashboardResponse data = dashboardService.getDashboardData();

        // 2. Vùng hiển thị thẻ Metric (Tổng số User, Project, Team, Task)
        add(createMetricsLayout(data.getMetrics()));

        // 3. Bảng danh sách công việc khẩn cấp hệ thống
        add(new H3("Công việc khẩn cấp toàn hệ thống"));
        add(createRecentTasksGrid(data));
    }

    // Tạo tiêu đề chào mừng
    private Component createHeader() {
        VerticalLayout header = new VerticalLayout();
        header.setPadding(false);
        header.setSpacing(false);
        
        H2 title = new H2("Chào mừng Quản trị viên quay trở lại!");
        title.addClassNames(LumoUtility.Margin.Bottom.NONE, LumoUtility.TextColor.PRIMARY);
        
        Paragraph subtitle = new Paragraph("Hệ thống quản lý tiến độ dự án - Số liệu tổng quan toàn cục.");
        subtitle.addClassNames(LumoUtility.Margin.Top.XSMALL, LumoUtility.TextColor.SECONDARY);
        
        header.add(title, subtitle);
        return header;
    }

    // Tạo các thẻ Metrics dạng lưới nằm ngang tự co giãn (Responsive)
    private Component createMetricsLayout(Map<String, Long> metrics) {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.setSpacing(true);
        layout.setFlexGrow(1, layout);

        // Lấy dữ liệu an toàn từ Map bằng các key đã định nghĩa ở Service
        long totalUsers = metrics.getOrDefault("totalUsers", 0L);
        long totalProjects = metrics.getOrDefault("totalProjects", 0L);
        long totalTeams = metrics.getOrDefault("totalTeams", 0L);
        long totalTasks = metrics.getOrDefault("totalTasks", 0L);

        // Tạo từng thẻ card kèm Icon Vaadin tương ứng
        layout.add(createCard("Tổng người dùng", String.valueOf(totalUsers), VaadinIcon.USERS, "bg-contrast-5"));
        layout.add(createCard("Dự án đang chạy", String.valueOf(totalProjects), VaadinIcon.BRIEFCASE, "bg-primary-10"));
        layout.add(createCard("Nhóm công nghệ", String.valueOf(totalTeams), VaadinIcon.CONNECT_O, "bg-success-10"));
        layout.add(createCard("Tổng số công việc", String.valueOf(totalTasks), VaadinIcon.TASKS, "bg-error-10"));

        return layout;
    }

    // Helper: Tạo cấu trúc giao diện cho 1 thẻ Card số liệu
    private Component createCard(String title, String value, VaadinIcon iconType, String bgThemeClass) {
        HorizontalLayout card = new HorizontalLayout();
        card.setPadding(true);
        card.setSpacing(true);
        card.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        card.setWidthFull();
        
        // Thêm các lớp Utility của Vaadin để trang trí (Border, Border-radius, Box-shadow)
        card.addClassNames(
                LumoUtility.Border.ALL,
                LumoUtility.BorderColor.CONTRAST_10,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Padding.MEDIUM,
                LumoUtility.Background.BASE
        );

        // Thiết kế block chứa Icon phía bên trái
        Icon icon = iconType.create();
        icon.setSize("28px");
        VerticalLayout iconContainer = new VerticalLayout(icon);
        iconContainer.setPadding(false);
        iconContainer.setMargin(false);
        iconContainer.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        iconContainer.setAlignItems(FlexComponent.Alignment.CENTER);
        iconContainer.setWidth("50px");
        iconContainer.setHeight("50px");
        iconContainer.addClassNames(LumoUtility.BorderRadius.LARGE, bgThemeClass);

        // Thiết kế text chứa Số liệu phía bên phải
        VerticalLayout textContainer = new VerticalLayout();
        textContainer.setPadding(false);
        textContainer.setSpacing(false);

        Span labelSpan = new Span(title);
        labelSpan.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

        Span valueSpan = new Span(value);
        valueSpan.addClassNames(LumoUtility.FontSize.XXLARGE, LumoUtility.FontWeight.BOLD);

        textContainer.add(labelSpan, valueSpan);

        card.add(iconContainer, textContainer);
        return card;
    }

    // Đổ dữ liệu danh sách công việc vào Vaadin Grid
    private Component createRecentTasksGrid(DashboardResponse data) {
        Grid<DashboardTaskItem> grid = new Grid<>(DashboardTaskItem.class, false);
        
        // Cấu hình các cột hiển thị cụ thể
        grid.addColumn(DashboardTaskItem::getTaskName).setHeader("Tên công việc").setSortable(true).setAutoWidth(true);
        grid.addColumn(DashboardTaskItem::getProjectName).setHeader("Thuộc dự án").setSortable(true).setAutoWidth(true);
        grid.addColumn(DashboardTaskItem::getTeamName).setHeader("Nhóm phụ trách").setAutoWidth(true);
        grid.addColumn(DashboardTaskItem::getDeadline).setHeader("Hạn chót").setSortable(true).setAutoWidth(true);

        // Tùy biến cột Trạng thái để hiển thị Badge màu sắc bắt mắt thay vì chữ thường
        grid.addComponentColumn(item -> {
            Span badge = new Span(item.getStatus());
            badge.getElement().getThemeList().add("badge"); // Sử dụng theme Badge có sẵn của Lumo
            
            // Định màu sắc tương ứng với từng trạng thái nhiệm vụ
            switch (item.getStatus()) {
                case "TODO" -> badge.getElement().getThemeList().add("contrast");
                case "IN_PROGRESS" -> badge.getElement().getThemeList().add("primary");
                case "PENDING_APPROVAL" -> badge.getElement().getThemeList().add("warning");
                case "DONE" -> badge.getElement().getThemeList().add("success");
                default -> badge.getElement().getThemeList().add("error");
            }
            return badge;
        }).setHeader("Trạng thái").setAutoWidth(true);

        // Cột hiển thị mức độ ưu tiên
        grid.addColumn(DashboardTaskItem::getPriority).setHeader("Độ ưu tiên").setAutoWidth(true);

        // Truyền List data DTO nhận được từ Service vào Grid
        grid.setItems(data.getRecentTasks());
        
        // Hiệu ứng zebra dải màu xen kẽ giữa các dòng cho dễ nhìn
        grid.setAllRowsVisible(true);
        grid.addClassName(LumoUtility.Border.ALL);

        return grid;
    }
}