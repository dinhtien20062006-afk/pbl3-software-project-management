package com.pbl3.view.admin;

import com.pbl3.dto.response.AuditLogResponse;
import com.pbl3.dto.response.DashboardResponse;
import com.pbl3.service.DashboardService;
import com.pbl3.view.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.Map;
import java.util.List;

@Route(value = "admin/dashboard", layout = MainLayout.class)
@RolesAllowed("ADMIN")
@PageTitle("Admin Dashboard | Hệ thống Quản lý Dự án")
public class AdminDashboardView extends VerticalLayout {

    public AdminDashboardView(DashboardService dashboardService) {
        addClassName("admin-dashboard-view");
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // 1. Lấy dữ liệu từ Service
        DashboardResponse data = dashboardService.getDashboardData();

        // 2. Header
        add(new H2("Bảng điều khiển hệ thống"));

        // 3. Phần Statistics Cards (Thẻ con số)
        add(createStatsLayout(data.getStats()));

        // 4. Phần Content Middle (Biểu đồ & Danh sách)
        HorizontalLayout contentLayout = new HorizontalLayout();
        contentLayout.setSizeFull();
        contentLayout.setSpacing(true);

        // Bên trái: Biểu đồ trạng thái (Giả lập bằng danh sách trực quan nếu chưa cài Vaadin Charts)
        Component chartSection = createStatusSummary(data.getProjectDistribution(), "Thống kê dự án");
        
        // Bên phải: Hoạt động gần đây
        Component activitySection = createActivityGrid(data.getRecentActivities());

        contentLayout.add(chartSection, activitySection);
        contentLayout.setFlexGrow(1, chartSection);
        contentLayout.setFlexGrow(2, activitySection);

        add(contentLayout);
    }

    // --- CÁC THÀNH PHẦN UI CHI TIẾT ---

    private Component createStatsLayout(DashboardResponse.Statistics stats) {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.setSpacing(true);

        layout.add(
            createCard("Người dùng", String.valueOf(stats.getTotalUsers()), VaadinIcon.USERS, "blue"),
            createCard("Dự án", String.valueOf(stats.getTotalProjects()), VaadinIcon.BRIEFCASE, "green"),
            createCard("Công việc", String.valueOf(stats.getTotalTasks()), VaadinIcon.TASKS, "orange"),
            createCard("Hoàn thành", String.valueOf(stats.getCompletedTasks()), VaadinIcon.CHECK_CIRCLE, "purple")
        );

        return layout;
    }

    private Component createCard(String title, String value, VaadinIcon icon, String color) {
        VerticalLayout card = new VerticalLayout();
        card.setPadding(true);
        card.setSpacing(false);
        card.setAlignItems(FlexComponent.Alignment.CENTER);
        card.getStyle().set("border", "1px solid #e2e2e2")
                     .set("border-radius", "8px")
                     .set("background-color", "white")
                     .set("box-shadow", "0 2px 4px rgba(0,0,0,0.1)");

        Icon vIcon = icon.create();
        vIcon.getStyle().set("color", color);
        
        Span titleSpan = new Span(title);
        titleSpan.getStyle().set("color", "#666").set("font-size", "0.9em");
        
        Span valueSpan = new Span(value);
        valueSpan.getStyle().set("font-size", "1.8em").set("font-weight", "bold");

        card.add(vIcon, valueSpan, titleSpan);
        card.setWidth("25%");
        return card;
    }

    private Component createStatusSummary(Map<String, Long> distribution, String title) {
        VerticalLayout layout = new VerticalLayout();
        layout.add(new H4(title));
        layout.getStyle().set("border", "1px solid #e2e2e2").set("border-radius", "8px");

        distribution.forEach((status, count) -> {
            HorizontalLayout row = new HorizontalLayout();
            row.setWidthFull();
            row.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
            
            Span statusName = new Span(status);
            Span countVal = new Span(count.toString());
            countVal.getStyle().set("font-weight", "bold");
            
            row.add(statusName, countVal);
            layout.add(row);
        });

        return layout;
    }

    private Component createActivityGrid(List<AuditLogResponse> activities) {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.add(new H4("Hoạt động hệ thống gần đây"));

        Grid<AuditLogResponse> grid = new Grid<>(AuditLogResponse.class, false);
        grid.addColumn(AuditLogResponse::getTime).setHeader("Thời gian").setAutoWidth(true);
        grid.addColumn(AuditLogResponse::getExecutor).setHeader("Người thực hiện").setAutoWidth(true);
        grid.addColumn(AuditLogResponse::getAction).setHeader("Hành động").setAutoWidth(true);
        grid.addColumn(AuditLogResponse::getTarget).setHeader("Đối tượng").setAutoWidth(true);

        grid.setItems(activities);
        grid.setAllRowsVisible(true); // Hiển thị hết 10 dòng không cần scroll grid riêng

        layout.add(grid);
        layout.getStyle().set("border", "1px solid #e2e2e2").set("border-radius", "8px");
        return layout;
    }
}