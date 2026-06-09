package com.pbl3.view;

import com.pbl3.dto.response.AuditLogResponse;
import com.pbl3.service.AuditLogService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Route(value = "audit-logs", layout = MainLayout.class)
@PageTitle("Lịch sử hoạt động")
@PermitAll
public class AuditLogView extends VerticalLayout implements HasUrlParameter<Long> {

    private final AuditLogService auditLogService;
    
    private final Grid<AuditLogResponse> grid = new Grid<>(AuditLogResponse.class, false);
    private final TextField filterText = new TextField();
    private final Button backBtn = new Button("Quay lại", VaadinIcon.ARROW_LEFT.create());
    private Long projectId;
    private List<AuditLogResponse> allLogs = new ArrayList<>();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");

    public AuditLogView(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;

        setSizeFull();
        addClassName("audit-log-view");

        setupHeader();
        setupGrid();
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter Long parameter) {
        this.projectId = parameter;
        backBtn.setVisible(projectId != null);
        refreshData();
    }

    private void setupHeader() {
        H2 title = new H2("Nhật ký hoạt động hệ thống");
        title.getStyle().set("margin", "0");

        backBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        backBtn.setVisible(false);
        backBtn.addClickListener(e -> {
            if (projectId != null) {
                getUI().ifPresent(ui -> ui.navigate(TeamView.class, projectId));
            }
        });

        filterText.setPlaceholder("Lọc theo người thực hiện hoặc hành động...");
        filterText.setPrefixComponent(VaadinIcon.SEARCH.create());
        filterText.setClearButtonVisible(true);
        filterText.setWidth("350px");
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
        filterText.addValueChangeListener(e -> updateList());

        HorizontalLayout header = new HorizontalLayout(backBtn, title, filterText);
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        
        add(header);
    }

    private void setupGrid() {
        // Cột thời gian
        grid.addColumn(log -> log.getTime().format(formatter))
                .setHeader("Thời gian")
                .setAutoWidth(true)
                .setSortable(true);

        // Cột người thực hiện
        grid.addColumn(AuditLogResponse::getExecutor)
                .setHeader("Người thực hiện")
                .setAutoWidth(true);

        // Cột hành động (Sử dụng Badge màu sắc)
        grid.addComponentColumn(log -> {
            Span badge = new Span(log.getAction());
            badge.getElement().getThemeList().add("badge");
            applyActionTheme(badge, log.getAction());
            return badge;
        }).setHeader("Hành động").setAutoWidth(true);

        // Cột đối tượng tác động
        grid.addColumn(AuditLogResponse::getTarget)
                .setHeader("Đối tượng")
                .setFlexGrow(1);

        grid.setSizeFull();
        add(grid);
    }

    private void applyActionTheme(Span badge, String action) {
        if (action.contains("Tạo")) badge.getElement().getThemeList().add("success");
        else if (action.contains("Xóa") || action.contains("Gỡ")) badge.getElement().getThemeList().add("error");
        else if (action.contains("Duyệt")) badge.getElement().getThemeList().add("primary");
        else if (action.contains("Cập nhật") || action.contains("Sửa")) badge.getElement().getThemeList().add("contrast");
        else badge.getElement().getThemeList().add("pill");
    }

    private void refreshData() {
        try {
            // Logic phân quyền: Nếu có projectId thì lấy log project, ngược lại lấy toàn bộ (dành cho Admin)
            if (projectId != null) {
                allLogs = auditLogService.getLogsByProject(projectId);
            } else {
                allLogs = auditLogService.getAllSystemLogs();
            }
            updateList();
        } catch (Exception e) {
            // Có thể thêm Notification báo lỗi phân quyền ở đây
            grid.setItems(new ArrayList<>());
        }
    }

    private void updateList() {
        String filter = filterText.getValue().toLowerCase();
        List<AuditLogResponse> filtered = allLogs.stream()
                .filter(log -> log.getExecutor().toLowerCase().contains(filter) || 
                               log.getAction().toLowerCase().contains(filter) ||
                               (log.getTarget() != null && log.getTarget().toLowerCase().contains(filter)))
                .toList();
        grid.setItems(filtered);
    }
}