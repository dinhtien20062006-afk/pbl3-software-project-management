package com.pbl3.view;

import com.pbl3.dto.request.TaskCreateRequest;
import com.pbl3.dto.request.TaskUpdateRequest;
import com.pbl3.dto.response.*;
import com.pbl3.entity.Task;
import com.pbl3.service.TaskService;
import com.pbl3.service.TeamMemberService;
import com.pbl3.service.ProjectTeamService; // Cần thêm Service để lấy thông tin Team/Project
import com.pbl3.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "tasks", layout = MainLayout.class)
@PageTitle("Quản lý công việc (Tasks)")
@PermitAll
public class TaskManagementView extends VerticalLayout implements HasUrlParameter<Long> {

    private final TaskService taskService;
    private final TeamMemberService teamMemberService;
    private final UserService userService;
    private final ProjectTeamService projectTeamService;
    private final AuthenticationContext authContext;

    private Long teamId;
    private String currentUsername;
    private boolean isManagerOrLeader;

    // UI Components
    private final Grid<ShowTaskResponse> taskGrid = new Grid<>(ShowTaskResponse.class, false);
    private final Button createTaskBtn = new Button("Thêm công việc", VaadinIcon.PLUS.create());
    private final Button backToTeamBtn = new Button("Xem thông tin Nhóm", VaadinIcon.ARROW_LEFT.create());
    private final Button viewProgressBtn = new Button("Xem tiến độ nhóm", VaadinIcon.CHART.create());

    // Filter Components
    private final ComboBox<Task.TaskStatus> statusFilter = new ComboBox<>("Trạng thái");
    private final ComboBox<Task.TaskPriority> priorityFilter = new ComboBox<>("Mức độ ưu tiên");
    private final ComboBox<TeamMemberResponse> assigneeFilter = new ComboBox<>("Người thực hiện");

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public TaskManagementView(TaskService taskService, TeamMemberService teamMemberService,
                              UserService userService, ProjectTeamService projectTeamService, AuthenticationContext authContext) {
        this.taskService = taskService;
        this.teamMemberService = teamMemberService;
        this.userService = userService;
        this.projectTeamService = projectTeamService;
        this.authContext = authContext;

        setSizeFull();
        setupHeader();
        setupFilterBar();
        setupTaskGrid();
    }

    @Override
    public void setParameter(BeforeEvent event, Long parameter) {
        this.teamId = parameter;
        this.currentUsername = authContext.getPrincipalName().orElse("");
        
        // Nạp dữ liệu vào bộ lọc thành viên nhóm
        List<TeamMemberResponse> teamMembers = teamMemberService.getMembersByTeam(teamId);
        assigneeFilter.setItems(teamMembers);
        assigneeFilter.setItemLabelGenerator(TeamMemberResponse::getFullName);

        List<TeamResponse> allTeams = projectTeamService.getAllTeams();
        TeamResponse currentTeam = allTeams.stream()
            .filter(t -> t.getTeamId().equals(teamId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhóm"));

        // Check Leader
        boolean isLeader = currentTeam.getLeaderName() != null &&
                userService.getAllUsers().stream()
                        .filter(u -> u.getFullName().equals(currentTeam.getLeaderName()))
                        .anyMatch(u -> u.getUsername().equals(currentUsername));

        // Check Project Manager
        boolean isManager = currentTeam.getManagerName() != null &&
                userService.getAllUsers().stream()
                        .filter(u -> u.getFullName().equals(currentTeam.getManagerName()))
                        .anyMatch(u -> u.getUsername().equals(currentUsername));

        isManagerOrLeader = isManager || isLeader;
        createTaskBtn.setVisible(isManagerOrLeader);
        viewProgressBtn.setVisible(isManager);

        refreshGridData();
    }

    private void refreshGridData() {
        Long filteredAssigneeId = assigneeFilter.getValue() != null ? assigneeFilter.getValue().getUserId() : null;
        
        // Gọi hàm filter từ Service của bạn
        List<ShowTaskResponse> tasks = taskService.filterTeamTasks(
                teamId, 
                statusFilter.getValue(), 
                priorityFilter.getValue(), 
                filteredAssigneeId
        );
        taskGrid.setItems(tasks);
    }

    private void setupHeader() {
        H2 title = new H2("Bảng quản lý công việc");
        title.getStyle().set("margin", "0");

        createTaskBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createTaskBtn.addClickListener(e -> openCreateTaskDialog());

        backToTeamBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        backToTeamBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(TeamDetailView.class, teamId)));

        viewProgressBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        viewProgressBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(TeamProgressView.class, teamId)));
        
        HorizontalLayout headerLayout = new HorizontalLayout(backToTeamBtn, title, createTaskBtn, viewProgressBtn);
        headerLayout.setWidthFull();
        headerLayout.setFlexGrow(1, title);
        headerLayout.setVerticalComponentAlignment(Alignment.CENTER, backToTeamBtn, title, createTaskBtn, viewProgressBtn);

        add(headerLayout);
    }

    private void setupFilterBar() {
        statusFilter.setItems(Task.TaskStatus.values());
        statusFilter.setClearButtonVisible(true);
        statusFilter.addValueChangeListener(e -> refreshGridData());

        priorityFilter.setItems(Task.TaskPriority.values());
        priorityFilter.setClearButtonVisible(true);
        priorityFilter.addValueChangeListener(e -> refreshGridData());

        assigneeFilter.setClearButtonVisible(true);
        assigneeFilter.addValueChangeListener(e -> refreshGridData());

        HorizontalLayout filterLayout = new HorizontalLayout(statusFilter, priorityFilter, assigneeFilter);
        filterLayout.setWidthFull();
        filterLayout.setSpacing(true);
        add(filterLayout);
    }

    private void setupTaskGrid() {
        taskGrid.addColumn(ShowTaskResponse::getTaskName).setHeader("Tên công việc").setSortable(true).setResizable(true);
        taskGrid.addColumn(ShowTaskResponse::getDescription).setHeader("Mô tả").setResizable(true);
        
        taskGrid.addComponentColumn(task -> {
            Span statusBadge = new Span(task.getStatus().name());
            statusBadge.getElement().getThemeList().add("badge");
            switch (task.getStatus()) {
                case TODO -> statusBadge.getElement().getThemeList().add("contrast");
                case IN_PROGRESS -> statusBadge.getElement().getThemeList().add("primary");
                case PENDING_APPROVAL -> statusBadge.getElement().getThemeList().add("warning");
                case DONE -> statusBadge.getElement().getThemeList().add("success");
                case CHANGE_REQUESTED, EXTENSION_REQUESTED -> statusBadge.getElement().getThemeList().add("error");
            }
            return statusBadge;
        }).setHeader("Trạng thái").setSortable(true);

        taskGrid.addColumn(ShowTaskResponse::getPriority).setHeader("Độ ưu tiên").setSortable(true);
        taskGrid.addColumn(task -> task.getDeadLine() != null ? task.getDeadLine().format(dateFormatter) : "-").setHeader("Hạn chót");
        taskGrid.addColumn(ShowTaskResponse::getAssigneeUsername).setHeader("Người phụ trách");

        // Cột Hành động thông minh tự động thay đổi dựa trên phân quyền và trạng thái Task
        taskGrid.addComponentColumn(task -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(true);

            boolean isAssignee = currentUsername.equals(task.getAssigneeUsername());

            // 1. Phân hệ nút bấm dành cho Người thực hiện (Member / Assignee)
            if (isAssignee) {
                if (task.getStatus() == Task.TaskStatus.TODO) {
                    Button startBtn = new Button("Bắt đầu", VaadinIcon.PLAY.create(), e -> {
                        try {
                            taskService.startTask(task.getId());
                            Notification.show("Đã bắt đầu công việc").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                            refreshGridData();
                        } catch (Exception ex) {
                            Notification.show(ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
                        }
                    });
                    startBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
                    actions.add(startBtn);
                }

                if (task.getStatus() == Task.TaskStatus.IN_PROGRESS) {
                    Button submitBtn = new Button("Nộp bài", VaadinIcon.CHECK.create(), e -> {
                        try {
                            taskService.submitTask(task.getId());
                            Notification.show("Đã gửi phê duyệt").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                            refreshGridData();
                        } catch (Exception ex) {
                            Notification.show(ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
                        }
                    });
                    submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

                    Button reqExtensionBtn = new Button("Gia hạn", e -> openReasonDialog("Yêu cầu gia hạn", reason -> {
                        taskService.requestExtension(task.getId(), reason);
                        refreshGridData();
                    }));
                    reqExtensionBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

                    Button reqChangeBtn = new Button("Đổi việc", e -> openReasonDialog("Yêu cầu đổi task", reason -> {
                        taskService.requestChangeTask(task.getId(), reason);
                        refreshGridData();
                    }));
                    reqChangeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);

                    actions.add(submitBtn, reqExtensionBtn, reqChangeBtn);
                }
            }

            // 2. Phân hệ nút bấm dành cho Quản lý (Project Manager / Team Leader)
            if (isManagerOrLeader) {
                // Nút Chỉnh sửa thông tin Task chung
                Button editBtn = new Button(VaadinIcon.EDIT.create(), e -> openUpdateTaskDialog(task));
                editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                actions.add(editBtn);

                // Nếu Task có yêu cầu cần duyệt (Gia hạn, đổi việc)
            if (task.getStatus() == Task.TaskStatus.CHANGE_REQUESTED || 
                task.getStatus() == Task.TaskStatus.EXTENSION_REQUESTED) {
                
                // NÚT MỚI: Xem nội dung lý do chi tiết qua Popup Dialog
                if (task.getRequestReason() != null && !task.getRequestReason().isBlank()) {
                    Button viewReasonBtn = new Button(VaadinIcon.EYE.create(), e -> openViewReasonPopup(task.getTaskName(), task.getRequestReason()));
                    viewReasonBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
                    viewReasonBtn.setTooltipText("Xem lý do giải trình");
                    refreshGridData(); // Cập nhật lại dữ liệu để đảm bảo requestReason mới nhất được hiển thị
                    actions.add(viewReasonBtn);
                }
            }
                // Nếu task đang chờ duyệt 
                if (task.getStatus() == Task.TaskStatus.PENDING_APPROVAL ) {
                    
                    Button approveBtn = new Button(VaadinIcon.THUMBS_UP.create(), e -> {
                        taskService.reviewTask(task.getId(), true);
                        Notification.show("Đã phê duyệt").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                        refreshGridData();
                    });
                    approveBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL);

                    Button rejectBtn = new Button(VaadinIcon.THUMBS_DOWN.create(), e -> {
                        taskService.reviewTask(task.getId(), false);
                        Notification.show("Đã từ chối / Yêu cầu chỉnh sửa").addThemeVariants(NotificationVariant.LUMO_WARNING);
                        refreshGridData();
                    });
                    rejectBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL);

                    actions.add(approveBtn, rejectBtn);
                }
            }

            return actions;
        }).setHeader("Thao tác nghiệp vụ").setAutoWidth(true);

        add(taskGrid);
        taskGrid.setSizeFull();
    }

    // --- DIALOG THAO TÁC NGHIỆP VỤ ---

    private void openCreateTaskDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Tạo công việc mới");

        FormLayout formLayout = new FormLayout();
        TextField nameField = new TextField("Tên công việc");
        TextArea descField = new TextArea("Mô tả chi tiết");
        DatePicker deadlinePicker = new DatePicker("Hạn chót");
        ComboBox<Task.TaskPriority> priorityCombo = new ComboBox<>("Độ ưu tiên", Task.TaskPriority.values());
        ComboBox<TeamMemberResponse> assigneeCombo = new ComboBox<>("Giao cho nhân sự");

        assigneeCombo.setItems(teamMemberService.getMembersByTeam(teamId));
        assigneeCombo.setItemLabelGenerator(TeamMemberResponse::getFullName);

        formLayout.add(nameField, priorityCombo, deadlinePicker, assigneeCombo, descField);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Binder<TaskCreateRequest> binder = new Binder<>(TaskCreateRequest.class);
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTeamId(this.teamId);
        
        binder.forField(nameField).asRequired("Tên công việc không được để trống").bind(TaskCreateRequest::getTaskName, TaskCreateRequest::setTaskName);
        binder.forField(deadlinePicker).asRequired("Vui lòng chọn hạn chót").bind(TaskCreateRequest::getDeadline, TaskCreateRequest::setDeadline);
        binder.forField(priorityCombo).asRequired("Chọn mức độ ưu tiên").bind(TaskCreateRequest::getPriority, TaskCreateRequest::setPriority);
        binder.forField(assigneeCombo)
                .bind(src -> src.getAssigneeId() != null ? teamMemberService.getMembersByTeam(teamId).stream().filter(m -> m.getUserId().equals(src.getAssigneeId())).findFirst().orElse(null) : null,
                      (dest, val) -> dest.setAssigneeId(val != null ? val.getUserId() : null));
        binder.bind(descField, TaskCreateRequest::getDescription, TaskCreateRequest::setDescription);

        binder.readBean(request);

        Button saveBtn = new Button("Tạo mới", e -> {
            try {
                if (binder.writeBeanIfValid(request)) {
                    taskService.createTask(request);
                    Notification.show("Thêm Task thành công").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    refreshGridData();
                    dialog.close();
                }
            } catch (Exception ex) {
                Notification.show("Lỗi: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.add(formLayout);
        dialog.getFooter().add(new Button("Hủy", ce -> dialog.close()), saveBtn);
        dialog.open();
    }

    private void openUpdateTaskDialog(ShowTaskResponse taskResponse) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Cập nhật thông tin công việc");

        FormLayout formLayout = new FormLayout();
        TextField nameField = new TextField("Tên công việc");
        TextArea descField = new TextArea("Mô tả chi tiết");
        DatePicker deadlinePicker = new DatePicker("Hạn chót");
        ComboBox<Task.TaskPriority> priorityCombo = new ComboBox<>("Độ ưu tiên", Task.TaskPriority.values());
        ComboBox<Task.TaskStatus> statusCombo = new ComboBox<>("Trạng thái", Task.TaskStatus.values());
        ComboBox<TeamMemberResponse> assigneeCombo = new ComboBox<>("Người phụ trách");

        assigneeCombo.setItems(teamMemberService.getMembersByTeam(teamId));
        assigneeCombo.setItemLabelGenerator(TeamMemberResponse::getFullName);

        formLayout.add(nameField, priorityCombo, statusCombo, deadlinePicker, assigneeCombo, descField);

        Binder<TaskUpdateRequest> binder = new Binder<>(TaskUpdateRequest.class);
        TaskUpdateRequest request = new TaskUpdateRequest();

        binder.bind(nameField, TaskUpdateRequest::getTaskName, TaskUpdateRequest::setTaskName);
        binder.bind(descField, TaskUpdateRequest::getDescription, TaskUpdateRequest::setDescription);
        binder.bind(deadlinePicker, TaskUpdateRequest::getDeadline, TaskUpdateRequest::setDeadline);
        binder.bind(priorityCombo, TaskUpdateRequest::getPriority, TaskUpdateRequest::setPriority);
        binder.bind(statusCombo, TaskUpdateRequest::getStatus, TaskUpdateRequest::setStatus);
        binder.forField(assigneeCombo)
                .bind(src -> src.getAssigneeId() != null ? teamMemberService.getMembersByTeam(teamId).stream().filter(m -> m.getUserId().equals(src.getAssigneeId())).findFirst().orElse(null) : null,
                      (dest, val) -> dest.setAssigneeId(val != null ? val.getUserId() : null));

        // Điền dữ liệu cũ hiện tại vào Form
        request.setTaskName(taskResponse.getTaskName());
        request.setDescription(taskResponse.getDescription());
        request.setDeadline(taskResponse.getDeadLine());
        request.setPriority(taskResponse.getPriority());
        request.setStatus(taskResponse.getStatus());
        teamMemberService.getMembersByTeam(teamId).stream()
                .filter(m -> m.getUsername().equals(taskResponse.getAssigneeUsername()))
                .findFirst().ifPresent(m -> {
                    request.setAssigneeId(m.getUserId());
                    assigneeCombo.setValue(m);
                });

        binder.readBean(request);

        Button updateBtn = new Button("Cập nhật", e -> {
            try {
                if (binder.writeBeanIfValid(request)) {
                    taskService.updateTask(taskResponse.getId(), request);
                    Notification.show("Cập nhật Task thành công").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    refreshGridData();
                    dialog.close();
                }
            } catch (Exception ex) {
                Notification.show("Lỗi: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        updateBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.add(formLayout);
        dialog.getFooter().add(new Button("Hủy", ce -> dialog.close()), updateBtn);
        dialog.open();
    }

    private void openReasonDialog(String title, java.util.function.Consumer<String> action) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(title);

        TextArea reasonArea = new TextArea("Lý do gửi yêu cầu");
        reasonArea.setWidthFull();
        reasonArea.setRequired(true);

        Button submitBtn = new Button("Gửi đi", e -> {
            if (!reasonArea.isEmpty()) {
                try {
                    action.accept(reasonArea.getValue());
                    Notification.show("Đã gửi yêu cầu thành công").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    dialog.close();
                } catch (Exception ex) {
                    Notification.show(ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            } else {
                reasonArea.setInvalid(true);
                reasonArea.setErrorMessage("Vui lòng nhập lý do");
            }
        });
        submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.add(reasonArea);
        dialog.getFooter().add(new Button("Hủy", ce -> dialog.close()), submitBtn);
        dialog.open();
    }

    private void openViewReasonPopup(String taskName, String reasonContent) {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle("Chi tiết lý do giải trình");
    dialog.setWidth("450px");

    VerticalLayout dialogLayout = new VerticalLayout();
    dialogLayout.setPadding(false);
    dialogLayout.setSpacing(true);

    Span taskLabel = new Span("Công việc: " + taskName);
    taskLabel.getStyle().set("font-weight", "bold");

    // Dùng TextArea đặt chế độ Read-Only để hiển thị văn bản dài có thanh cuộn mượt mà
    TextArea reasonTextArea = new TextArea("Nội dung thành viên gửi:");
    reasonTextArea.setValue(reasonContent);
    reasonTextArea.setReadOnly(true);
    reasonTextArea.setWidthFull();
    reasonTextArea.setHeight("180px");

    dialogLayout.add(taskLabel, reasonTextArea);
    dialog.add(dialogLayout);

    Button closeBtn = new Button("Đóng", e -> dialog.close());
    dialog.getFooter().add(closeBtn);
    
    dialog.open();
    }
}