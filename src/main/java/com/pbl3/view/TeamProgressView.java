package com.pbl3.view;

import com.pbl3.dto.response.TeamProgressResponse;
import com.pbl3.service.ProgressService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.util.ArrayList;

@Route(value = "team-progress", layout = MainLayout.class)
@PageTitle("Tiến độ công việc nhóm")
@PermitAll
public class TeamProgressView extends VerticalLayout implements HasUrlParameter<Long> {

    private final ProgressService progressService;
    private Long teamId;

    // UI Components đầu trang
    private final H2 viewTitle = new H2("Tiến độ nhóm: -");
    private final Button backBtn = new Button("Quay lại", VaadinIcon.ARROW_LEFT.create());
    
    // Thẻ Dashboard trực quan (Stats Summary Cards)
    private final Span txtTotalMembers = new Span("0");
    private final Span txtTotalTasks = new Span("0");
    private final Span txtCompletionRate = new Span("0.0%");
    private final ProgressBar overallProgressBar = new ProgressBar();

    // Bảng dữ liệu tiến độ thành viên
    private final Grid<TeamProgressResponse.MemberProgressItem> memberGrid = 
            new Grid<>(TeamProgressResponse.MemberProgressItem.class, false);

    public TeamProgressView(ProgressService progressService) {
        this.progressService = progressService;
        
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        setupHeader();
        setupSummaryDashboard();
        setupMemberGrid();
    }

    @Override
    public void setParameter(BeforeEvent event, Long parameter) {
        this.teamId = parameter;
        loadTeamProgressData();
    }

    private void setupHeader() {
        viewTitle.getStyle().set("margin", "0");
        
        backBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        backBtn.addClickListener(e -> UI.getCurrent().navigate(TaskManagementView.class, teamId));

        HorizontalLayout header = new HorizontalLayout(backBtn, viewTitle);
        header.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, backBtn, viewTitle);
        header.setSpacing(true);
        add(header);
    }

    private void setupSummaryDashboard() {
        // Tạo các Card nhỏ tổng quan số liệu
        HorizontalLayout cardsLayout = new HorizontalLayout();
        cardsLayout.setWidthFull();
        cardsLayout.setSpacing(true);

        VerticalLayout cardMembers = createSummaryCard("Tổng số thành viên", txtTotalMembers, "badge success");
        VerticalLayout cardTasks = createSummaryCard("Tổng số công việc", txtTotalTasks, "badge primary");
        VerticalLayout cardRate = createSummaryCard("Tỷ lệ hoàn thành nhóm", txtCompletionRate, "badge");
        cardRate.getStyle().set("background-color", "var(--lumo-primary-color-10pct)");

        cardsLayout.add(cardMembers, cardTasks, cardRate);

        // Thanh tiến độ chung toàn nhóm (đặt dưới các thẻ)
        VerticalLayout progressWrapper = new VerticalLayout();
        progressWrapper.setPadding(false);
        progressWrapper.setSpacing(false);
        progressWrapper.getStyle().set("margin-top", "10px");
        
        Span progressLabel = new Span("Thước đo tiến độ tổng thể của nhóm:");
        progressLabel.getStyle().set("font-size", "var(--lumo-font-size-s)").set("color", "var(--lumo-secondary-text-color)");
        
        overallProgressBar.setWidthFull();
        overallProgressBar.setMin(0);
        overallProgressBar.setMax(100);

        progressWrapper.add(progressLabel, overallProgressBar);

        add(cardsLayout, progressWrapper, new com.vaadin.flow.component.html.Hr());
    }

    private VerticalLayout createSummaryCard(String title, Span valueSpan, String themeBadge) {
        VerticalLayout card = new VerticalLayout();
        card.setPadding(true);
        card.setSpacing(false);
        card.getStyle()
            .set("border", "1px solid var(--lumo-contrast-20pct)")
            .set("border-radius", "var(--lumo-border-radius-m)")
            .set("box-shadow", "var(--lumo-box-shadow-xs)");

        Span titleSpan = new Span(title);
        titleSpan.getStyle().set("font-size", "var(--lumo-font-size-s)").set("color", "var(--lumo-secondary-text-color)");
        
        valueSpan.getStyle().set("font-size", "var(--lumo-font-size-xxl)").set("font-weight", "bold");
        if (!themeBadge.isBlank()) {
            valueSpan.getElement().getThemeList().add(themeBadge);
        }

        card.add(titleSpan, valueSpan);
        return card;
    }

    private void setupMemberGrid() {
        memberGrid.setSizeFull();
        
        // 1. Cột Tên thành viên
        memberGrid.addColumn(TeamProgressResponse.MemberProgressItem::getFullName)
                .setHeader("Họ và tên")
                .setSortable(true)
                .setResizable(true);
        
        memberGrid.addColumn(TeamProgressResponse.MemberProgressItem::getUsername)
                .setHeader("Tài khoản")
                .setSortable(true);

        // 2. Cột Số việc được giao
        memberGrid.addColumn(TeamProgressResponse.MemberProgressItem::getAssignedTasks)
                .setHeader("Tổng số việc")
                .setSortable(true);

        // 3. Cột Tỷ lệ phân mảnh công việc (Dạng chuỗi: "Số việc xong / Tổng số việc")
        memberGrid.addColumn(TeamProgressResponse.MemberProgressItem::getProgressDisplay)
                .setHeader("Đã hoàn thành")
                .getStyle().set("font-weight", "bold");

        // 4. Đồ họa trực quan: ProgressBar cho từng nhân sự
        memberGrid.addComponentColumn(item -> {
            HorizontalLayout layout = new HorizontalLayout();
            layout.setWidthFull();
            layout.setAlignItems(FlexComponent.Alignment.CENTER);

            ProgressBar bar = new ProgressBar();
            bar.setValue(item.getCompletionRate() / 100.0); // Quy đổi về khoảng 0.0 - 1.0
            bar.setWidthFull();

            // Đổi màu thanh dựa trên phần trăm tiến độ
            if (item.getCompletionRate() >= 80.0) {
                bar.getElement().setAttribute("theme", "success");
            } else if (item.getCompletionRate() < 40.0) {
                bar.getElement().setAttribute("theme", "error");
            }

            Span percentText = new Span(item.getCompletionRate() + "%");
            percentText.getStyle().set("font-size", "var(--lumo-font-size-s)").set("font-weight", "bold");

            layout.add(bar, percentText);
            layout.setFlexGrow(1, bar);
            return layout;
        }).setHeader("Tiến độ cá nhân").setWidth("250px").setResizable(true);

        // 5. Cột Số việc Trễ Hạn (Cảnh báo đặc biệt)
        memberGrid.addComponentColumn(item -> {
            Span overdueBadge = new Span(String.valueOf(item.getOverdueTasks()));
            overdueBadge.getElement().getThemeList().add("badge");
            
            if (item.getOverdueTasks() > 0) {
                overdueBadge.getElement().getThemeList().add("error");
                overdueBadge.getStyle().set("font-weight", "bold");
            } else {
                overdueBadge.getElement().getThemeList().add("contrast");
            }
            return overdueBadge;
        }).setHeader("Việc trễ hạn").setSortable(true);

        add(memberGrid);
    }

    private void loadTeamProgressData() {
        try {
            TeamProgressResponse data = progressService.getTeamProgress(teamId);
            
            // Đổ dữ liệu text tổng quan
            viewTitle.setText("Tiến độ nhóm: " + data.getTeamName());
            txtTotalMembers.setText(String.valueOf(data.getTotalMembers()));
            txtTotalTasks.setText(String.valueOf(data.getTotalTasks()));
            txtCompletionRate.setText(data.getOverallCompletionRate() + "%");
            
            // Cập nhật thanh tiến độ lớn đầu trang
            overallProgressBar.setValue(data.getOverallCompletionRate() / 100.0);
            if (data.getOverallCompletionRate() >= 80.0) {
                overallProgressBar.getElement().setAttribute("theme", "success");
            } else if (data.getOverallCompletionRate() < 40.0) {
                overallProgressBar.getElement().setAttribute("theme", "error");
            } else {
                overallProgressBar.getElement().removeAttribute("theme");
            }

            // Đổ danh sách vào Grid
            memberGrid.setItems(data.getMemberProgresses());
            
        } catch (Exception e) {
            viewTitle.setText("Lỗi: Không thể tải thông tin tiến độ hoặc bạn không có quyền xem!");
            memberGrid.setItems(new ArrayList<>());
        }
    }
}