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

@Route(value = "member/dashboard", layout = MainLayout.class)
@RolesAllowed("MEMBER")
public class MemberDashboardView extends VerticalLayout {

    // Tiêm DashboardService vào thông qua Constructor
    public MemberDashboardView(DashboardService dashboardService) {
        // Cấu hình Layout tổng thể
        setPadding(true);
        setSpacing(true);
        setSizeFull();

        // 1. Lấy dữ liệu Dashboard đã được filter riêng cho Member hiện tại
        DashboardResponse data = dashboardService.getDashboardData();

        // 2. Render các Component lên UI
        add(createHeader());
        add(createMetricsLayout(data.getMetrics()));
        add(new H3("Nhiệm vụ cá nhân sắp đến hạn"));
        add(createMyTasksGrid(data));
    }

    // Tiêu đề chào mừng Member
    private Component createHeader() {
        VerticalLayout header = new VerticalLayout();
        header.setPadding(false);
        header.setSpacing(false);

        H2 title = new H2("Chào mừng Thành viên quay trở lại!");
        title.addClassNames(LumoUtility.Margin.Bottom.NONE, LumoUtility.TextColor.PRIMARY);

        Paragraph subtitle = new Paragraph("Xem tổng quan các nhiệm vụ cá nhân được giao và cập nhật tiến độ công việc.");
        subtitle.addClassNames(LumoUtility.Margin.Top.XSMALL, LumoUtility.TextColor.SECONDARY);

        header.add(title, subtitle);
        return header;
    }

    // Khối hiển thị 3 thẻ thống kê Task cá nhân
    private Component createMetricsLayout(Map<String, Long> metrics) {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.setSpacing(true);

        // Đọc dữ liệu từ Map dựa trên các key cá nhân được định nghĩa ở DashboardService
        long myTodoTasks = metrics.getOrDefault("myTodoTasks", 0L);
        long myInProgressTasks = metrics.getOrDefault("myInProgressTasks", 0L);
        long myDoneTasks = metrics.getOrDefault("myDoneTasks", 0L);

        // Thêm các thẻ trạng thái (To-Do, In Progress, Done)
        layout.add(createCard("Nhiệm vụ cần làm", String.valueOf(myTodoTasks), VaadinIcon.LIST_UL, "bg-contrast-5"));
        layout.add(createCard("Nhiệm vụ đang chạy", String.valueOf(myInProgressTasks), VaadinIcon.PROGRESSBAR, "bg-primary-10"));
        layout.add(createCard("Nhiệm vụ hoàn thành", String.valueOf(myDoneTasks), VaadinIcon.CHECK_CIRCLE, "bg-success-10"));

        return layout;
    }

    // Helper: Tạo cấu trúc giao diện chuẩn cho một Card chỉ số
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

        // Khối bọc Icon
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

        // Khối chứa chữ & số liệu thống kê
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

    // Grid hiển thị danh sách các task cá nhân phục vụ cho Vaadin View
    private Component createMyTasksGrid(DashboardResponse data) {
        Grid<DashboardTaskItem> grid = new Grid<>(DashboardTaskItem.class, false);

        // Định nghĩa các cột
        grid.addColumn(DashboardTaskItem::getTaskName).setHeader("Tên công việc").setSortable(true).setAutoWidth(true);
        grid.addColumn(DashboardTaskItem::getProjectName).setHeader("Thuộc dự án").setSortable(true).setAutoWidth(true);
        grid.addColumn(DashboardTaskItem::getTeamName).setHeader("Nhóm công việc").setAutoWidth(true);
        grid.addColumn(DashboardTaskItem::getDeadline).setHeader("Hạn chót").setSortable(true).setAutoWidth(true);

        // Tùy biến cột Trạng thái thành các Badge màu sắc Lumo của Vaadin
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

        // Hiển thị độ ưu tiên
        grid.addColumn(DashboardTaskItem::getPriority).setHeader("Độ ưu tiên").setAutoWidth(true);

        // Đổ danh sách 5 task cá nhân khẩn cấp nhất vào Grid
        grid.setItems(data.getRecentTasks());

        // Cấu hình UI Grid
        grid.setAllRowsVisible(true);
        grid.addClassName(LumoUtility.Border.ALL);

        return grid;
    }
}