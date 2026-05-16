package com.pbl3.view;

import com.pbl3.dto.request.TeamCreateRequest;
import com.pbl3.dto.request.TeamUpdateRequest;
import com.pbl3.dto.response.ProjectResponse;
import com.pbl3.dto.response.ShowInfoResponse;
import com.pbl3.dto.response.TeamResponse;
import com.pbl3.entity.ProjectTeam;
import com.pbl3.service.ProjectService;
import com.pbl3.service.ProjectTeamService;
import com.pbl3.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
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
import jakarta.annotation.security.PermitAll;

import java.util.List;

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
        teamGrid.addColumn(TeamResponse::getDeadline).setHeader("Hạn chót");

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

        add(new HorizontalLayout(new H3("Danh sách nhóm thực hiện"), addTeamBtn), teamGrid);
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
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(team == null ? "Tạo nhóm mới" : "Cập nhật nhóm");

        FormLayout formLayout = new FormLayout();
        TextField nameField = new TextField("Tên nhóm");
        TextArea descField = new TextArea("Mô tả công việc");
        DatePicker deadlinePicker = new DatePicker("Hạn chót");
        
        // Thay đổi ComboBox để nhận ShowInfoResponse
        ComboBox<ShowInfoResponse> leaderPicker = new ComboBox<>("Chọn trưởng nhóm");

        // Đổ dữ liệu từ UserService vào
        leaderPicker.setItems(userService.getAllUsers());

        // Hiển thị tên đầy đủ của User trên danh sách chọn
        leaderPicker.setItemLabelGenerator(ShowInfoResponse::getFullName);

        if (team != null) {
            nameField.setValue(team.getTeamName());
            descField.setValue(team.getDescription() != null ? team.getDescription() : "");
            deadlinePicker.setValue(team.getDeadline());
            
            List<ShowInfoResponse> allUsers = userService.getAllUsers();
            allUsers.stream()
                .filter(u -> u.getFullName().equals(team.getLeaderName()))
                .findFirst()
                .ifPresent(leaderPicker::setValue);
                }

        formLayout.add(nameField, leaderPicker, deadlinePicker, descField);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Button saveBtn = new Button("Lưu", e -> {
            try {
                ShowInfoResponse selectedLeader = leaderPicker.getValue();
                if (selectedLeader == null) {
                    Notification.show("Vui lòng chọn trưởng nhóm").addThemeVariants(NotificationVariant.LUMO_WARNING);
                    return;
                }
                if (team == null) {
                    TeamCreateRequest req = new TeamCreateRequest();
                    req.setProjectId(this.projectId);
                    req.setTeamName(nameField.getValue());
                    req.setDescription(descField.getValue());
                    req.setDeadline(deadlinePicker.getValue());
                    req.setLeaderId(selectedLeader.getUserId());
                    teamService.createTeam(req);
                } else {
                    TeamUpdateRequest req = new TeamUpdateRequest();
                    req.setTeamName(nameField.getValue());
                    req.setDescription(descField.getValue());
                    req.setDeadline(deadlinePicker.getValue());
                    req.setLeaderId(selectedLeader.getUserId());
                    teamService.updateTeam(team.getTeamId(), req);
                }
                refreshUI();
                dialog.close();
                Notification.show("Thành công").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show(ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.add(formLayout);
        dialog.getFooter().add(new Button("Hủy", e -> dialog.close()), saveBtn);
        dialog.open();
    }
}