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

@Route(value = "manager/dashboard", layout = MainLayout.class)
@RolesAllowed("PROJECT_MANAGER")
public class ManagerDashboardView extends VerticalLayout {

    // Tiêm DashboardService qua Constructor
    public ManagerDashboardView(DashboardService dashboardService) {
        // Cấu hình tổng quan cho Giao diện
        setPadding(true);
        setSpacing(true);
        setSizeFull();

        // 1. Lấy dữ liệu Dashboard phân quyền từ Service
        DashboardResponse data = dashboardService.getDashboardData();

        // 2. Thêm các thành phần giao diện
        add(createHeader());
        add(createMetricsLayout(data.getMetrics()));
        add(new H3("Danh sách công việc sắp đến hạn trong dự án"));
        add(createRecentTasksGrid(data));
    }

    // Tiêu đề trang chào mừng PM
    private Component createHeader() {
        VerticalLayout header = new VerticalLayout();
        header.setPadding(false);
        header.setSpacing(false);

        H2 title = new H2("Chào mừng Quản lý dự án quay trở lại!");
        title.addClassNames(LumoUtility.Margin.Bottom.NONE, LumoUtility.TextColor.PRIMARY);

        Paragraph subtitle = new Paragraph("Theo dõi tiến độ, quản lý nhóm và phê duyệt các công việc được bàn giao.");
        subtitle.addClassNames(LumoUtility.Margin.Top.XSMALL, LumoUtility.TextColor.SECONDARY);

        header.add(title, subtitle);
        return header;
    }

    // Khối hiển thị 4 thẻ Metrics đo lường hiệu suất của PM
    private Component createMetricsLayout(Map<String, Long> metrics) {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.setSpacing(true);

        // Đọc dữ liệu từ Map theo các Key cấu hình trong DashboardService dành cho PM
        long myProjects = metrics.getOrDefault("myProjects", 0L);
        long myTeams = metrics.getOrDefault("myTeams", 0L);
        long totalMembers = metrics.getOrDefault("totalMembersInCharge", 0L);
        long pendingApprovalTasks = metrics.getOrDefault("pendingApprovalTasks", 0L);

        // Thẻ số 4 (Cần duyệt) sẽ đổi màu nền cảnh báo nếu số lượng task tồn đọng > 0
        String taskCardBg = pendingApprovalTasks > 0 ? "bg-warning-10" : "bg-contrast-5";

        layout.add(createCard("Dự án quản lý", String.valueOf(myProjects), VaadinIcon.ARCHIVES, "bg-primary-10"));
        layout.add(createCard("Nhóm phụ trách", String.valueOf(myTeams), VaadinIcon.SUITCASE, "bg-success-10"));
        layout.add(createCard("Nhân sự trong dự án", String.valueOf(totalMembers), VaadinIcon.USERS, "bg-contrast-5"));
        layout.add(createCard("Công việc chờ duyệt", String.valueOf(pendingApprovalTasks), VaadinIcon.CLIPBOARD_CHECK, taskCardBg));

        return layout;
    }

    // Helper: Định dạng UI cho một Card chỉ số
    private Component createCard(String title, String value, VaadinIcon iconType, String bgThemeClass) {
        HorizontalLayout card = new HorizontalLayout();
        card.setPadding(true);
        card.setSpacing(true);
        card.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        card.setWidthFull();

        card.addClassNames(
                LumoUtility.Border.ALL,
                LumoUtility.BorderColor.CONTRAST_10,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Padding.MEDIUM,
                LumoUtility.Background.BASE
        );

        // Khối tròn chứa Icon
        Icon icon = iconType.create();
        icon.setSize("24px");
        VerticalLayout iconContainer = new VerticalLayout(icon);
        iconContainer.setPadding(false);
        iconContainer.setMargin(false);
        iconContainer.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        iconContainer.setAlignItems(FlexComponent.Alignment.CENTER);
        iconContainer.setWidth("44px");
        iconContainer.setHeight("44px");
        iconContainer.addClassNames(LumoUtility.BorderRadius.LARGE, bgThemeClass);

        // Khối chứa văn bản & số liệu
        VerticalLayout textContainer = new VerticalLayout();
        textContainer.setPadding(false);
        textContainer.setSpacing(false);

        Span labelSpan = new Span(title);
        labelSpan.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

        Span valueSpan = new Span(value);
        valueSpan.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.FontWeight.BOLD);

        textContainer.add(labelSpan, valueSpan);

        card.add(iconContainer, textContainer);
        return card;
    }

    // Grid hiển thị danh sách Task phục vụ cho Vaadin
    private Component createRecentTasksGrid(DashboardResponse data) {
        Grid<DashboardTaskItem> grid = new Grid<>(DashboardTaskItem.class, false);

        grid.addColumn(DashboardTaskItem::getTaskName).setHeader("Tên công việc").setSortable(true).setAutoWidth(true);
        grid.addColumn(DashboardTaskItem::getProjectName).setHeader("Dự án").setSortable(true).setAutoWidth(true);
        grid.addColumn(DashboardTaskItem::getTeamName).setHeader("Nhóm phụ trách").setAutoWidth(true);
        grid.addColumn(DashboardTaskItem::getDeadline).setHeader("Hạn chót").setSortable(true).setAutoWidth(true);

        // Biến đổi cột Trạng thái thành dạng Badge màu sắc trực quan
        grid.addComponentColumn(item -> {
            Span badge = new Span(item.getStatus());
            badge.getElement().getThemeList().add("badge");

            switch (item.getStatus()) {
                case "TODO" -> badge.getElement().getThemeList().add("contrast");
                case "IN_PROGRESS" -> badge.getElement().getThemeList().add("primary");
                case "PENDING_APPROVAL" -> badge.getElement().getThemeList().add("warning");
                case "DONE" -> badge.getElement().getThemeList().add("success");
                case "CHANGE_REQUESTED", "EXTENSION_REQUESTED" -> badge.getElement().getThemeList().add("error");
                default -> badge.getElement().getThemeList().add("normal");
            }
            return badge;
        }).setHeader("Trạng thái").setAutoWidth(true);

        // Cột hiển thị mức độ ưu tiên (Priority)
        grid.addColumn(DashboardTaskItem::getPriority).setHeader("Độ ưu tiên").setAutoWidth(true);

        // Đổ List dữ liệu thu được từ Service vào Grid
        grid.setItems(data.getRecentTasks());
        
        // Cấu hình hiển thị layout đẹp mắt cho bảng dữ liệu
        grid.setAllRowsVisible(true);
        grid.addClassName(LumoUtility.Border.ALL);

        return grid;
    }
}