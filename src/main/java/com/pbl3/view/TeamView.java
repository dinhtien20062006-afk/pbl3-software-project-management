package com.pbl3.view;

import java.time.format.DateTimeFormatter;

import com.pbl3.dto.request.TeamRequest;
import com.pbl3.dto.response.ProjectResponse;
import com.pbl3.dto.response.ShowInfoResponse;
import com.pbl3.dto.response.TeamResponse;
import com.pbl3.entity.Project;
import com.pbl3.entity.ProjectTeam;
import com.pbl3.service.ProjectService;
import com.pbl3.service.ProjectTeamService;
import com.pbl3.service.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
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
    private String currentUsername;
    private boolean isProjectManager;
    private boolean currentProjectOnHold;
    private boolean currentProjectReadOnly;
    
    // UI Components
    private final VerticalLayout projectInfoSection = new VerticalLayout();
    private final Grid<TeamResponse> teamGrid = new Grid<>(TeamResponse.class, false);
    private final Button backBtn = new Button("Quay lại", VaadinIcon.ARROW_LEFT.create());
    private final Button addTeamBtn = new Button("Thêm nhóm mới", VaadinIcon.PLUS.create());
    private final Button projectProgressBtn = new Button("Báo cáo tiến độ dự án", VaadinIcon.BAR_CHART_H.create());
    private final Button historyBtn = new Button("Lịch sử hoạt động", VaadinIcon.CLOCK.create());
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");

    public TeamView(ProjectTeamService teamService, ProjectService projectService, 
                    UserService userService, AuthenticationContext authContext) {
        this.teamService = teamService;
        this.projectService = projectService;
        this.userService = userService;
        this.authContext = authContext;

        setSizeFull();
        setupNavigationHeader();
        setupProjectHeader();
        setupTeamGrid();
    }

    @Override
    public void setParameter(BeforeEvent event, Long parameter) {
        this.projectId = parameter;
        this.currentUsername = authContext.getPrincipalName().orElse("");
        refreshUI();
    }

    private void refreshUI() {
        projectInfoSection.removeAll();
        ProjectResponse project = projectService.getProjectById(projectId);
        isProjectManager = project.getManagerUsername().equals(currentUsername);
        currentProjectOnHold = project.getStatus() == Project.ProjectStatus.ON_HOLD;
        currentProjectReadOnly = currentProjectOnHold || project.getStatus() == Project.ProjectStatus.COMPLETED;

        H3 title = new H3("Dự án: " + project.getProjectName());
        Span desc = new Span("Mô tả: " + (project.getDescription() != null ? project.getDescription() : "Không có"));
        Span manager = new Span("Quản lý dự án: " + project.getManagerName());
        manager.getStyle().set("font-weight", "bold").set("color", "var(--lumo-primary-color)");
        Span projectStatus = new Span("Trạng thái dự án: " + project.getStatus());
        projectStatus.getStyle().set("font-weight", "bold");
        if (currentProjectOnHold) {
            projectStatus.getStyle().set("color", "var(--lumo-error-color)");
        } else if (project.getStatus() == Project.ProjectStatus.COMPLETED) {
            projectStatus.getStyle().set("color", "var(--lumo-success-color)");
        }

        projectInfoSection.add(title, desc, manager, projectStatus);
        
        addTeamBtn.setVisible(isProjectManager);
        addTeamBtn.setEnabled(!currentProjectReadOnly);
        addTeamBtn.setTooltipText(currentProjectReadOnly
                ? (currentProjectOnHold ? "Dự án đang tạm dừng nên không thể tạo nhóm mới" : "Dự án đã hoàn thành nên không thể tạo nhóm mới")
                : "Thêm nhóm mới");
        projectProgressBtn.setVisible(isProjectManager);
        historyBtn.setVisible(isProjectManager);

        teamGrid.setItems(teamService.getTeamsByProject(projectId));
    }

    private void setupNavigationHeader() {
        backBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        backBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(ProjectView.class)));
        HorizontalLayout topBar = new HorizontalLayout(backBtn);
        topBar.setWidthFull();
        add(topBar);
    }

    private void setupProjectHeader() {
        projectInfoSection.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
        projectInfoSection.getStyle().set("border-radius", "8px");
        projectInfoSection.setPadding(true);
        add(projectInfoSection);
    }

    private void setupTeamGrid() {
        addTeamBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addTeamBtn.addClickListener(e -> {
            if (currentProjectReadOnly) {
                Notification.show(currentProjectOnHold ? "Dự án đang tạm dừng, không thể tạo nhóm mới" : "Dự án đã hoàn thành, không thể tạo nhóm mới")
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            openTeamDialog(null);
        });

        projectProgressBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        projectProgressBtn.addClickListener(e -> UI.getCurrent().navigate(ProjectProgressView.class, projectId));

        historyBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        historyBtn.addClickListener(e -> UI.getCurrent().navigate(AuditLogView.class, projectId));

        teamGrid.addColumn(TeamResponse::getTeamName).setHeader("Tên nhóm").setSortable(true);
        teamGrid.addColumn(TeamResponse::getLeaderName).setHeader("Trưởng nhóm");
        teamGrid.addColumn(TeamResponse::getStatus).setHeader("Trạng thái");
        teamGrid.addColumn(team -> team.getDeadline() != null ? team.getDeadline().format(dateFormatter) : "-").setHeader("Hạn chót");
        teamGrid.addColumn(team -> team.getRequestedDeadline() != null
                ? "Chờ duyệt: " + team.getRequestedDeadline().format(dateFormatter)
                : "-").setHeader("Yêu cầu đổi hạn");

        teamGrid.addComponentColumn(team -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(true);
            boolean isLeaderOfThisTeam = currentUsername != null && currentUsername.equals(team.getLeaderUsername());
            boolean hasDeadlineRequest = team.getRequestedDeadline() != null;
            
            Button editBtn = new Button(VaadinIcon.EDIT.create(), e -> openTeamDialog(team));
            editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            editBtn.setVisible(isProjectManager && !currentProjectReadOnly);
            editBtn.setTooltipText(currentProjectReadOnly ? "Dự án đang tạm dừng/đã hoàn thành, không thể chỉnh sửa nhóm" : "Chỉnh sửa nhóm");

            Button deleteBtn = new Button(VaadinIcon.TRASH.create(), e -> confirmDelete(team));
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            deleteBtn.setVisible(isProjectManager && !currentProjectReadOnly);
            deleteBtn.setTooltipText(currentProjectReadOnly ? "Dự án đang tạm dừng/đã hoàn thành, không thể xóa nhóm" : "Xóa nhóm");

            if (!currentProjectReadOnly && team.getStatus() == ProjectTeam.TeamStatus.PLANNING && (isProjectManager || isLeaderOfThisTeam)) {
                Button startBtn = new Button("Bắt đầu", e -> {
                    try {
                        teamService.startTeam(team.getTeamId());
                        Notification.show("Đã bắt đầu nhóm").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                        refreshUI();
                    } catch (Exception ex) {
                        Notification.show("Lỗi: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                });
                startBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS);
                actions.add(startBtn);
            }

            if (!currentProjectReadOnly && team.getStatus() == ProjectTeam.TeamStatus.IN_PROGRESS && (isProjectManager || isLeaderOfThisTeam)) {
                Button doneBtn = new Button("Xong", e -> confirmCompleteTeam(team));
                doneBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS);
                doneBtn.setTooltipText("Chỉ hoàn thành nhóm khi tất cả task trong nhóm đã xong");
                actions.add(doneBtn);
            }

            Button membersBtn = new Button(VaadinIcon.USERS.create(), e -> {
                getUI().ifPresent(ui -> ui.navigate(TeamDetailView.class, team.getTeamId()));
            });
            membersBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_CONTRAST);
            membersBtn.setTooltipText("Quản lý thành viên nhóm");

            if (!currentProjectReadOnly && isLeaderOfThisTeam && !isProjectManager && !hasDeadlineRequest) {
                Button requestDeadlineBtn = new Button("Xin đổi hạn", VaadinIcon.CLOCK.create(), e -> openDeadlineRequestDialog(team));
                requestDeadlineBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
                requestDeadlineBtn.setTooltipText("Gửi yêu cầu đổi deadline cho Project Manager");
                actions.add(requestDeadlineBtn);
            }

            if (hasDeadlineRequest) {
                Button viewRequestBtn = new Button(VaadinIcon.EYE.create(), e -> openDeadlineRequestInfoDialog(team));
                viewRequestBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
                viewRequestBtn.setTooltipText("Xem yêu cầu đổi deadline");
                actions.add(viewRequestBtn);

                if (!currentProjectReadOnly && isProjectManager) {
                    Button approveBtn = new Button(VaadinIcon.CHECK.create(), e -> resolveDeadlineRequest(team, true));
                    approveBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_ICON);
                    approveBtn.setTooltipText("Chấp nhận yêu cầu deadline");

                    Button declineBtn = new Button(VaadinIcon.CLOSE.create(), e -> resolveDeadlineRequest(team, false));
                    declineBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_ICON);
                    declineBtn.setTooltipText("Từ chối yêu cầu deadline");
                    actions.add(approveBtn, declineBtn);
                }
            }

            actions.add(editBtn, deleteBtn, membersBtn);
            return actions;
        }).setHeader("Thao tác").setAutoWidth(true);

        add(new HorizontalLayout(new H3("Danh sách nhóm thực hiện"), addTeamBtn, projectProgressBtn, historyBtn), teamGrid);
        teamGrid.setSizeFull();
    }


    private void confirmCompleteTeam(TeamResponse team) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Xác nhận hoàn thành nhóm");
        dialog.setText("Nhóm chỉ được hoàn thành khi tất cả task trong nhóm đã xong. Bạn muốn tiếp tục?");
        dialog.setCancelable(true);
        dialog.setCancelText("Hủy");
        dialog.setConfirmText("Xong");
        dialog.setConfirmButtonTheme("success primary");
        dialog.addConfirmListener(e -> {
            try {
                teamService.completeTeam(team.getTeamId());
                Notification.show("Đã chuyển nhóm sang COMPLETED", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                refreshUI();
            } catch (Exception ex) {
                Notification.show("Lỗi: " + ex.getMessage(), 4000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        dialog.open();
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

        leaderPicker.setItems(userService.getAllUsers());
        leaderPicker.setItemLabelGenerator(ShowInfoResponse::getFullName);

        ProjectResponse project = projectService.getProjectById(this.projectId);
        deadlinePicker.setMin(project.getStartDate());
        deadlinePicker.setMax(project.getEndDate());

        Binder<TeamRequest> binder = new Binder<>(TeamRequest.class);
        TeamRequest requestData = new TeamRequest();

        binder.forField(nameField)
                .asRequired("Tên nhóm không được để trống")
                .bind(TeamRequest::getTeamName, TeamRequest::setTeamName);

        binder.forField(leaderPicker)
                .asRequired("Vui lòng chọn trưởng nhóm")
                .bind(src -> null,
                    (target, value) -> target.setLeaderId(value != null ? value.getUserId() : null));

        binder.forField(deadlinePicker)
                .asRequired("Hạn chót là bắt buộc")
                .bind(TeamRequest::getDeadline, TeamRequest::setDeadline);

        binder.bind(descField, TeamRequest::getDescription, TeamRequest::setDescription);

        if (isEdit) {
            requestData.setTeamName(team.getTeamName());
            requestData.setDescription(team.getDescription());
            requestData.setDeadline(team.getDeadline());
            userService.getAllUsers().stream()
                    .filter(u -> u.getUserId().equals(team.getLeaderId()) || u.getUsername().equals(team.getLeaderUsername()))
                    .findFirst()
                    .ifPresent(leaderPicker::setValue);
            binder.readBean(requestData);
        }

        formLayout.add(nameField, leaderPicker, deadlinePicker, descField);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Button saveBtn = new Button("Lưu", e -> {
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

    private void openDeadlineRequestDialog(TeamResponse team) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Yêu cầu đổi deadline nhóm");
        dialog.setWidth("500px");

        TextArea reasonArea = new TextArea("Lý do yêu cầu");
        reasonArea.setWidthFull();
        reasonArea.setRequired(true);

        DateTimePicker requestedDeadlinePicker = new DateTimePicker("Deadline mới mong muốn");
        requestedDeadlinePicker.setWidthFull();
        if (team.getDeadline() != null) {
            requestedDeadlinePicker.setMin(team.getDeadline());
        }
        ProjectResponse project = projectService.getProjectById(team.getProjectId());
        if (project.getEndDate() != null) {
            requestedDeadlinePicker.setMax(project.getEndDate());
        }

        Button submitBtn = new Button("Gửi yêu cầu", e -> {
            if (reasonArea.isEmpty() || requestedDeadlinePicker.isEmpty()) {
                Notification.show("Vui lòng nhập lý do và deadline mong muốn")
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            try {
                teamService.requestDeadlineChange(team.getTeamId(), reasonArea.getValue(), requestedDeadlinePicker.getValue());
                Notification.show("Đã gửi yêu cầu đổi deadline cho Project Manager")
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                refreshUI();
                dialog.close();
            } catch (Exception ex) {
                Notification.show("Lỗi: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.add(new VerticalLayout(reasonArea, requestedDeadlinePicker));
        dialog.getFooter().add(new Button("Hủy", e -> dialog.close()), submitBtn);
        dialog.open();
    }

    private void openDeadlineRequestInfoDialog(TeamResponse team) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Chi tiết yêu cầu đổi deadline");
        dialog.setWidth("500px");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.add(new Span("Nhóm: " + team.getTeamName()));
        layout.add(new Span("Leader gửi yêu cầu: " + team.getLeaderName()));
        layout.add(new Span("Deadline hiện tại: " + (team.getDeadline() != null ? team.getDeadline().format(dateFormatter) : "-")));
        layout.add(new Span("Deadline mong muốn: " + (team.getRequestedDeadline() != null ? team.getRequestedDeadline().format(dateFormatter) : "-")));

        TextArea reasonArea = new TextArea("Lý do");
        reasonArea.setValue(team.getDeadlineRequestReason() != null ? team.getDeadlineRequestReason() : "Không có lý do");
        reasonArea.setReadOnly(true);
        reasonArea.setWidthFull();
        reasonArea.setHeight("140px");
        layout.add(reasonArea);

        dialog.add(layout);
        dialog.getFooter().add(new Button("Đóng", e -> dialog.close()));
        dialog.open();
    }

    private void resolveDeadlineRequest(TeamResponse team, boolean approved) {
        try {
            teamService.resolveDeadlineChangeRequest(team.getTeamId(), approved);
            Notification.show(approved ? "Đã chấp nhận yêu cầu đổi deadline" : "Đã từ chối yêu cầu đổi deadline")
                    .addThemeVariants(approved ? NotificationVariant.LUMO_SUCCESS : NotificationVariant.LUMO_WARNING);
            refreshUI();
        } catch (Exception ex) {
            Notification.show("Lỗi: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
