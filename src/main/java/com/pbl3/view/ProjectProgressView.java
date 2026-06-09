package com.pbl3.view;

import com.pbl3.dto.response.ProjectProgressResponse;
import com.pbl3.service.ProgressService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Hr;
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

@Route(value = "project-progress", layout = MainLayout.class)
@PageTitle("Tiến độ tổng thể dự án")
@PermitAll
public class ProjectProgressView extends VerticalLayout implements HasUrlParameter<Long> {

    private final ProgressService progressService;
    private Long projectId;

    // Header components
    private final H2 title = new H2("Tổng quan dự án: -");
    private final Button backBtn = new Button("Quay lại dự án", VaadinIcon.ARROW_LEFT.create());

    // Stats components
    private final Span txtTotalMembers = new Span("0");
    private final Span txtTotalTasks = new Span("0");
    private final Span txtOverallRate = new Span("0.0%");
    private final ProgressBar projectProgressBar = new ProgressBar();

    // Grid hiển thị danh sách các Team
    private final Grid<ProjectProgressResponse.TeamProgressItem> teamGrid = 
            new Grid<>(ProjectProgressResponse.TeamProgressItem.class, false);

    public ProjectProgressView(ProgressService progressService) {
        this.progressService = progressService;
        setSizeFull();
        setSpacing(true);
        setPadding(true);

        setupHeader();
        setupProjectDashboard();
        setupTeamGrid();
    }

    @Override
    public void setParameter(BeforeEvent event, Long parameter) {
        this.projectId = parameter;
        loadProjectData();
    }

    private void setupHeader() {
        backBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        backBtn.addClickListener(e -> UI.getCurrent().navigate(TeamView.class, projectId));

        HorizontalLayout header = new HorizontalLayout(backBtn, title);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        add(header);
    }

    private void setupProjectDashboard() {
        HorizontalLayout dashboard = new HorizontalLayout();
        dashboard.setWidthFull();
        dashboard.setSpacing(true);

        // Tạo các thẻ thống kê lớn (Kpi Cards)
        VerticalLayout card1 = createKpiCard("Nhân sự toàn dự án", txtTotalMembers, VaadinIcon.USERS);
        VerticalLayout card2 = createKpiCard("Tổng khối lượng công việc", txtTotalTasks, VaadinIcon.LIST_OL);
        VerticalLayout card3 = createKpiCard("Tiến độ hoàn thành", txtOverallRate, VaadinIcon.CHART_LINE);

        dashboard.add(card1, card2, card3);

        // Progress bar tổng của cả dự án
        VerticalLayout progressSection = new VerticalLayout();
        progressSection.setPadding(false);
        progressSection.setSpacing(false);
        
        Span label = new Span("Chỉ số hoàn thành mục tiêu dự án:");
        label.getStyle().set("font-size", "var(--lumo-font-size-s)").set("margin-bottom", "5px");
        
        projectProgressBar.setWidthFull();
        projectProgressBar.setHeight("15px");
        
        progressSection.add(label, projectProgressBar);
        add(dashboard, progressSection, new Hr());
    }

    private VerticalLayout createKpiCard(String label, Span value, VaadinIcon icon) {
        VerticalLayout card = new VerticalLayout();
        card.setAlignItems(FlexComponent.Alignment.CENTER);
        card.setPadding(true);
        card.getStyle()
            .set("border", "1px solid var(--lumo-contrast-10pct)")
            .set("border-radius", "12px")
            .set("background-color", "var(--lumo-base-color)");

        HorizontalLayout titleBox = new HorizontalLayout(icon.create(), new Span(label));
        titleBox.setAlignItems(FlexComponent.Alignment.CENTER);
        titleBox.getStyle().set("color", "var(--lumo-secondary-text-color)");

        value.getStyle().set("font-size", "2rem").set("font-weight", "bold").set("color", "var(--lumo-primary-color)");

        card.add(titleBox, value);
        return card;
    }

    private void setupTeamGrid() {
        teamGrid.setSizeFull();
        teamGrid.getStyle().set("border-radius", "8px");

        teamGrid.addColumn(ProjectProgressResponse.TeamProgressItem::getTeamName)
                .setHeader("Tên nhóm")
                .setSortable(true)
                .setFlexGrow(1);

        teamGrid.addColumn(ProjectProgressResponse.TeamProgressItem::getLeaderName)
                .setHeader("Trưởng nhóm (Leader)")
                .setSortable(true);

        teamGrid.addColumn(ProjectProgressResponse.TeamProgressItem::getTotalMembers)
                .setHeader("Thành viên")
                .setTextAlign(com.vaadin.flow.component.grid.ColumnTextAlign.CENTER);

        // Hiển thị thanh tiến độ cho từng Team
        teamGrid.addComponentColumn(team -> {
            VerticalLayout layout = new VerticalLayout();
            layout.setPadding(false);
            layout.setSpacing(false);
            layout.setWidthFull();

            ProgressBar bar = new ProgressBar();
            bar.setValue(team.getCompletionRate() / 100.0);
            
            // Set màu theo mức độ hoàn thành
            if (team.getCompletionRate() >= 100) bar.getElement().setAttribute("theme", "success");
            else if (team.getCompletionRate() < 30) bar.getElement().setAttribute("theme", "error");

            Span percent = new Span(team.getCompletionRate() + "%");
            percent.getStyle().set("font-size", "var(--lumo-font-size-xs)").set("font-weight", "600");

            layout.add(bar, percent);
            layout.setHorizontalComponentAlignment(FlexComponent.Alignment.END, percent);
            return layout;
        }).setHeader("Tiến độ nhóm").setWidth("200px");

        // Nút hành động: Xem chi tiết nhóm
        teamGrid.addComponentColumn(team -> {
            Button detailBtn = new Button("Chi tiết", VaadinIcon.SEARCH.create());
            detailBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            detailBtn.addClickListener(e -> UI.getCurrent().navigate(TaskManagementView.class, team.getTeamId()));
            return detailBtn;
        }).setHeader("Hành động");

        add(new Span("Phân tích tiến độ theo từng đơn vị nhóm:"), teamGrid);
    }

    private void loadProjectData() {
        try {
            ProjectProgressResponse response = progressService.getProjectProgress(projectId);
            
            title.setText("Tổng quan dự án: " + response.getProjectName());
            txtTotalMembers.setText(String.valueOf(response.getTotalMembers()));
            txtTotalTasks.setText(String.valueOf(response.getTotalTasks()));
            txtOverallRate.setText(response.getOverallCompletionRate() + "%");
            
            projectProgressBar.setValue(response.getOverallCompletionRate() / 100.0);
            teamGrid.setItems(response.getTeamProgresses());
        } catch (Exception e) {
            title.setText("Không có quyền truy cập hoặc lỗi dữ liệu");
            teamGrid.setItems(new ArrayList<>());
        }
    }
}