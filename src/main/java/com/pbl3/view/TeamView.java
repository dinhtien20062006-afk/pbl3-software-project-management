package com.pbl3.view;

import java.time.format.DateTimeFormatter;

import com.pbl3.dto.request.TeamRequest;
import com.pbl3.dto.response.ProjectResponse;
import com.pbl3.dto.response.ShowInfoResponse;
import com.pbl3.dto.response.TeamResponse;
import com.pbl3.entity.ProjectTeam;
import com.pbl3.service.ProjectService;
import com.pbl3.service.ProjectTeamService;
import com.pbl3.service.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.data.binder.Binder;
import jakarta.annotation.security.PermitAll;


@Route(value = "team", layout = MainLayout.class)
@PageTitle("Chi tiết dự án & Nhóm")
@PermitAll
public class TeamView extends VerticalLayout implements HasUrlParameter<Long> {

    private final ProjectTeamService teamService;
    private final ProjectService projectService;
    private final UserService userService;
    private final AuthenticationContext authContext;

    private Long projectId;
    private boolean isProjectManager;
    
    // UI Components
    private final VerticalLayout projectInfoSection = new VerticalLayout();
    private final Grid<TeamResponse> teamGrid = new Grid<>(TeamResponse.class, false);
    private final Button addTeamBtn = new Button("Thêm nhóm mới", VaadinIcon.PLUS.create());
    private final Button ProjectProgressBtn = new Button("Báo cáo tiến độ dự án", VaadinIcon.BAR_CHART_H.create());
    private final Button HistoryBtn = new Button("Lịch sử hoạt động", VaadinIcon.CLOCK.create());
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");

    public TeamView(ProjectTeamService teamService, ProjectService projectService, 
                    UserService userService, AuthenticationContext authContext) {
        this.teamService = teamService;
        this.projectService = projectService;
        this.userService = userService;
        this.authContext = authContext;

        setSizeFull();
        setupProjectHeader();
        setupTeamGrid();
    }

    @Override
    public void setParameter(BeforeEvent event, Long parameter) {
        this.projectId = parameter;
        refreshUI();
    }

    private void refreshUI() {
        projectInfoSection.removeAll();
        ProjectResponse project = projectService.getProjectById(projectId);
        
        // Kiểm tra quyền: User hiện tại có phải là Manager của Project này không?
        String currentUsername = authContext.getPrincipalName().orElse("");
        isProjectManager = project.getManagerUsername().equals(currentUsername);

        // Header thông tin dự án
        H3 title = new H3("Dự án: " + project.getProjectName());
        Span desc = new Span("Mô tả: " + (project.getDescription() != null ? project.getDescription() : "Không có"));
        Span manager = new Span("Quản lý dự án: " + project.getManagerName());
        manager.getStyle().set("font-weight", "bold").set("color", "var(--lumo-primary-color)");

        projectInfoSection.add(title, desc, manager);
        
        addTeamBtn.setVisible(isProjectManager);
        addTeamBtn.addClickListener(e -> openTeamDialog(null));

        ProjectProgressBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        ProjectProgressBtn.addClickListener(e -> {
            UI.getCurrent().navigate(ProjectProgressView.class, projectId);
        });
        ProjectProgressBtn.setVisible(isProjectManager);

        HistoryBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        HistoryBtn.addClickListener(e -> {
            UI.getCurrent().navigate(AuditLogView.class, projectId);
        });
        HistoryBtn.setVisible(isProjectManager);

        teamGrid.setItems(teamService.getTeamsByProject(projectId));
    }

    private void setupProjectHeader() {
        projectInfoSection.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
        projectInfoSection.getStyle().set("border-radius", "8px");
        projectInfoSection.setPadding(true);
        add(projectInfoSection);
    }

    private void setupTeamGrid() {
        teamGrid.addColumn(TeamResponse::getTeamName).setHeader("Tên nhóm").setSortable(true);
        teamGrid.addColumn(TeamResponse::getLeaderName).setHeader("Trưởng nhóm");
        teamGrid.addColumn(TeamResponse::getStatus).setHeader("Trạng thái");
        teamGrid.addColumn(team -> team.getDeadline() != null ? team.getDeadline().format(dateFormatter) : "-").setHeader("Hạn chót");

        teamGrid.addComponentColumn(team -> {
            HorizontalLayout actions = new HorizontalLayout();
            
            // Nút Sửa
            Button editBtn = new Button(VaadinIcon.EDIT.create(), e -> openTeamDialog(team));
            editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            editBtn.setVisible(isProjectManager);

            // Nút Xóa với xác nhận
            Button deleteBtn = new Button(VaadinIcon.TRASH.create(), e -> confirmDelete(team));
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            deleteBtn.setVisible(isProjectManager);

            // Nút Start Team (Nếu đang Planning)
            if (team.getStatus() == ProjectTeam.TeamStatus.PLANNING) {
                Button startBtn = new Button("Bắt đầu", e -> {
                    teamService.startTeam(team.getTeamId());
                    refreshUI();
                });
                startBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS);
                actions.add(startBtn);
            }

            // Nút Xem thành viên (Mới)
            Button membersBtn = new Button(VaadinIcon.USERS.create(), e -> {
                getUI().ifPresent(ui -> ui.navigate(TeamDetailView.class, team.getTeamId()));
            });
            membersBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_CONTRAST);
            membersBtn.setTooltipText("Quản lý thành viên nhóm");

            actions.add(editBtn, deleteBtn, membersBtn);
            return actions;
        }).setHeader("Thao tác").setAutoWidth(true);

        add(new HorizontalLayout(new H3("Danh sách nhóm thực hiện"), addTeamBtn, ProjectProgressBtn, HistoryBtn), teamGrid);
        teamGrid.setSizeFull();
    }

    private void confirmDelete(TeamResponse team) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Xác nhận xóa");
        dialog.setText("Bạn có chắc chắn muốn xóa nhóm '" + team.getTeamName() + "' không? Hành động này không thể hoàn tác.");
        
        dialog.setCancelable(true);
        dialog.setCancelText("Hủy");
        
        dialog.setConfirmText("Xóa");
        dialog.setConfirmButtonTheme("error primary");
        
        dialog.addConfirmListener(e -> {
            try {
                teamService.deleteTeam(team.getTeamId());
                Notification.show("Đã xóa nhóm thành công", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                refreshUI();
            } catch (Exception ex) {
                Notification.show("Lỗi: " + ex.getMessage(), 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        dialog.open();
    }

        private void openTeamDialog(TeamResponse team) {
        boolean isEdit = (team != null);
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(isEdit ? "Cập nhật nhóm" : "Tạo nhóm mới");

        FormLayout formLayout = new FormLayout();
        TextField nameField = new TextField("Tên nhóm");
        TextArea descField = new TextArea("Mô tả công việc");
        DateTimePicker deadlinePicker = new DateTimePicker("Hạn chót");
        ComboBox<ShowInfoResponse> leaderPicker = new ComboBox<>("Chọn trưởng nhóm");

        // Đổ dữ liệu cho ComboBox
        leaderPicker.setItems(userService.getAllUsers());
        leaderPicker.setItemLabelGenerator(ShowInfoResponse::getFullName);

        // Cấu hình DateTimePicker dựa trên thời gian dự án
        ProjectResponse project = projectService.getProjectById(this.projectId);
        deadlinePicker.setMin(project.getStartDate());
        deadlinePicker.setMax(project.getEndDate());

        // --- KHỞI TẠO BINDER ---
        Binder<TeamRequest> binder = new Binder<>(TeamRequest.class);
        TeamRequest requestData = new TeamRequest();

        // Ràng buộc Tên nhóm
        binder.forField(nameField)
                .asRequired("Tên nhóm không được để trống")
                .bind(TeamRequest::getTeamName, TeamRequest::setTeamName);

        // Ràng buộc Trưởng nhóm (Chuyển đổi giữa UserID và Object trong ComboBox)
        binder.forField(leaderPicker)
                .asRequired("Vui lòng chọn trưởng nhóm")
                .bind(src -> null, // Getter không dùng trực tiếp từ requestData cho ComboBox này
                    (target, value) -> target.setLeaderId(value != null ? value.getUserId() : null));

        // Ràng buộc Hạn chót
        binder.forField(deadlinePicker)
                .asRequired("Hạn chót là bắt buộc")
                .bind(TeamRequest::getDeadline, TeamRequest::setDeadline);

        // Ràng buộc Mô tả
        binder.bind(descField, TeamRequest::getDescription, TeamRequest::setDescription);

        // --- NẠP DỮ LIỆU KHI EDIT ---
        if (isEdit) {
            requestData.setTeamName(team.getTeamName());
            requestData.setDescription(team.getDescription());
            requestData.setDeadline(team.getDeadline());
            
            // Tìm và set leader hiện tại vào ComboBox UI
            userService.getAllUsers().stream()
                    .filter(u -> u.getFullName().equals(team.getLeaderName()))
                    .findFirst()
                    .ifPresent(leaderPicker::setValue);
            
            binder.readBean(requestData);
        }

        formLayout.add(nameField, leaderPicker, deadlinePicker, descField);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Button saveBtn = new Button("Lưu", e -> {
            // writeBean kiểm tra Validations trước khi lưu
            if (binder.writeBeanIfValid(requestData)) {
                try {
                    if (!isEdit) {
                        requestData.setProjectId(this.projectId);
                        teamService.createTeam(requestData);
                    } else {
                        TeamRequest updateReq = new TeamRequest();
                        updateReq.setTeamName(requestData.getTeamName());
                        updateReq.setDescription(requestData.getDescription());
                        updateReq.setDeadline(requestData.getDeadline());
                        updateReq.setLeaderId(requestData.getLeaderId());
                        teamService.updateTeam(team.getTeamId(), updateReq);
                    }
                    refreshUI();
                    dialog.close();
                    Notification.show("Thành công").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } catch (Exception ex) {
                    Notification.show("Lỗi: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            }
        });

        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.add(formLayout);
        dialog.getFooter().add(new Button("Hủy", ev -> dialog.close()), saveBtn);
        dialog.open();
    }
}