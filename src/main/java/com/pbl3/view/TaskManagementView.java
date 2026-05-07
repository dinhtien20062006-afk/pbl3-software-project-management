package com.pbl3.view;

import com.pbl3.dto.request.TaskCreateRequest;
import com.pbl3.dto.request.TaskUpdateRequest;
import com.pbl3.dto.response.ProjectMemberResponse;
import com.pbl3.dto.response.ProjectResponse;
import com.pbl3.dto.response.ShowTaskResponse;
import com.pbl3.entity.Task;
import com.pbl3.service.ProjectMemberService;
import com.pbl3.service.ProjectService;
import com.pbl3.service.TaskService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
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

import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "project-tasks", layout = MainLayout.class)
@PageTitle("Quản lý nhiệm vụ")
@PermitAll
public class TaskManagementView extends VerticalLayout implements HasUrlParameter<Long> {

    private final TaskService taskService;
    private final ProjectService projectService;
    private final ProjectMemberService memberService;
    private final AuthenticationContext authContext;

    private Long projectId;
    private boolean isManager;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // UI Components
    private final Grid<ShowTaskResponse> taskGrid = new Grid<>(ShowTaskResponse.class, false);
    private final ComboBox<Task.TaskStatus> statusFilter = new ComboBox<>("Trạng thái");
    private final ComboBox<ProjectMemberResponse> assigneeFilter = new ComboBox<>("Người thực hiện");
    private final Button addTaskBtn = new Button("Tạo nhiệm vụ", VaadinIcon.PLUS.create());

    public TaskManagementView(TaskService taskService, ProjectService projectService, 
                              ProjectMemberService memberService, AuthenticationContext authContext) {
        this.taskService = taskService;
        this.projectService = projectService;
        this.memberService = memberService;
        this.authContext = authContext;

        setSizeFull();
        setupFilterArea();
        setupGrid();
    }

    @Override
    public void setParameter(BeforeEvent event, Long parameter) {
        this.projectId = parameter;
        checkRoleAndInitFilters();
        refreshGrid();
    }

    private void checkRoleAndInitFilters() {
        String currentUsername = authContext.getPrincipalName().orElse("");
        ProjectResponse project = projectService.getProjectById(projectId);
        this.isManager = project.getManagerUsername().equals(currentUsername);

        addTaskBtn.setVisible(isManager);
        
        // Load danh sách thành viên vào bộ lọc assignee
        List<ProjectMemberResponse> members = memberService.getMembers(projectId);
        assigneeFilter.setItems(members);
        assigneeFilter.setItemLabelGenerator(ProjectMemberResponse::getFullName);
    }

    private void setupFilterArea() {
        HorizontalLayout filterLayout = new HorizontalLayout();
        filterLayout.setAlignItems(Alignment.END);

        statusFilter.setItems(Task.TaskStatus.values());
        statusFilter.setClearButtonVisible(true);
        statusFilter.addValueChangeListener(e -> refreshGrid());

        assigneeFilter.setClearButtonVisible(true);
        assigneeFilter.addValueChangeListener(e -> refreshGrid());

        Button backBtn = new Button("Quay lại dự án", VaadinIcon.ARROW_LEFT.create(),
                e -> getUI().ifPresent(ui -> ui.navigate(ProjectDetailView.class, projectId)));

        addTaskBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addTaskBtn.addClickListener(e -> openTaskDialog(null));

        filterLayout.add(backBtn, statusFilter, assigneeFilter, addTaskBtn);
        add(new H3("Quản lý nhiệm vụ dự án"), filterLayout);
    }

    private void setupGrid() {
        taskGrid.addColumn(ShowTaskResponse::getTaskName).setHeader("Tên nhiệm vụ");
        taskGrid.addColumn(t -> t.getAssigneeUsername()).setHeader("Người thực hiện");
        
        taskGrid.addComponentColumn(t -> {
            Span status = new Span(t.getStatus().name());
            status.getElement().getThemeList().add("badge " + getStatusTheme(t.getStatus()));
            return status;
        }).setHeader("Trạng thái");

        taskGrid.addColumn(t -> t.getDeadLine() != null ? t.getDeadLine().format(dateFormatter) : "-")
                .setHeader("Hạn chót");

        taskGrid.addComponentColumn(this::createActionButtons).setHeader("Thao tác");

        taskGrid.setSizeFull();
        add(taskGrid);
    }

    private HorizontalLayout createActionButtons(ShowTaskResponse task) {
        HorizontalLayout actions = new HorizontalLayout();
        String currentUsername = authContext.getPrincipalName().orElse("");

        // 1. Nút Bắt đầu (Chỉ hiện cho Assignee và khi task đang ở trạng thái TODO)
        if (currentUsername.equals(task.getAssigneeUsername()) && task.getStatus() == Task.TaskStatus.TODO) {
            Button startBtn = new Button("Bắt đầu", VaadinIcon.PLAY.create(), e -> {
                try {
                    taskService.startTask(task.getId());
                    refreshGrid();
                    Notification.show("Nhiệm vụ đã chuyển sang trạng thái Đang thực hiện")
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } catch (Exception ex) {
                    Notification.show(ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });
            startBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            actions.add(startBtn);
        }
        // 2. Nút nộp bài (Chỉ hiện cho Assignee và khi task đang IN_PROGRESS)
        if (currentUsername.equals(task.getAssigneeUsername()) && task.getStatus() == Task.TaskStatus.IN_PROGRESS) {
            Button submitBtn = new Button("Nộp bài", e -> {
                taskService.submitTask(task.getId());
                refreshGrid();
                Notification.show("Đã nộp nhiệm vụ!");
            });
            submitBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
            actions.add(submitBtn);
        }

        // 3. Nhóm nút của Manager
        if (isManager) {
            // Nút Duyệt/Từ chối
            if (task.getStatus() == Task.TaskStatus.PENDING_APPROVAL) {
                Button approveBtn = new Button(VaadinIcon.CHECK.create(), e -> {
                    taskService.reviewTask(task.getId(), true);
                    refreshGrid();
                });
                approveBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_TERTIARY);
                
                Button rejectBtn = new Button(VaadinIcon.CLOSE.create(), e -> {
                    taskService.reviewTask(task.getId(), false);
                    refreshGrid();
                });
                rejectBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
                
                actions.add(approveBtn, rejectBtn);
            }

            // Nút Chỉnh sửa
            Button editBtn = new Button(VaadinIcon.EDIT.create(), e -> openTaskDialog(task));
            editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            actions.add(editBtn);
        }

        return actions;
    }

    private void openTaskDialog(ShowTaskResponse taskResponse) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(taskResponse == null ? "Tạo nhiệm vụ" : "Chỉnh sửa nhiệm vụ");

        TextField nameField = new TextField("Tên nhiệm vụ");
        TextArea descField = new TextArea("Mô tả");
        DatePicker deadlinePicker = new DatePicker("Hạn chót");
        ComboBox<Task.TaskPriority> priorityBox = new ComboBox<>("Mức độ ưu tiên", Task.TaskPriority.values());
        ComboBox<Task.TaskStatus> statusBox = new ComboBox<>("Trạng thái", Task.TaskStatus.values());
        ComboBox<ProjectMemberResponse> assigneeBox = new ComboBox<>("Giao cho");
        assigneeBox.setItems(memberService.getMembers(projectId));
        assigneeBox.setItemLabelGenerator(ProjectMemberResponse::getFullName);

        if (taskResponse != null) {
            nameField.setValue(taskResponse.getTaskName());
            descField.setValue(taskResponse.getDescription() != null ? taskResponse.getDescription() : "");
            deadlinePicker.setValue(taskResponse.getDeadLine());
            priorityBox.setValue(taskResponse.getPriority());
            statusBox.setValue(taskResponse.getStatus());
            // Tìm assignee tương ứng trong list member
            assigneeBox.setPlaceholder(taskResponse.getAssigneeUsername());
        }

        Button saveBtn = new Button("Lưu", e -> {
            try {
                Long assigneeId = assigneeBox.getValue() != null ? assigneeBox.getValue().getUserId() : null;
                if (taskResponse == null) {
                    TaskCreateRequest req = new TaskCreateRequest();
                    req.setProjectId(projectId);
                    req.setTaskName(nameField.getValue());
                    req.setDescription(descField.getValue());
                    req.setDeadline(deadlinePicker.getValue());
                    req.setPriority(priorityBox.getValue());
                    req.setAssigneeId(assigneeId);
                    taskService.createTask(req);
                } else {
                    TaskUpdateRequest req = new TaskUpdateRequest();
                    req.setTaskName(nameField.getValue());
                    req.setDescription(descField.getValue());
                    req.setDeadline(deadlinePicker.getValue());
                    req.setPriority(priorityBox.getValue());
                    req.setAssigneeId(assigneeId);
                    req.setStatus(statusBox.getValue());
                    taskService.updateTask(taskResponse.getId(), req);
                }
                refreshGrid();
                dialog.close();
                Notification.show("Thành công").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show(ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        VerticalLayout layout = new VerticalLayout(nameField, descField, deadlinePicker, priorityBox, statusBox, assigneeBox);
        dialog.add(layout);
        dialog.getFooter().add(new Button("Hủy", e -> dialog.close()), saveBtn);
        dialog.open();
    }

    private void refreshGrid() {
        if (projectId == null) return;
        Long assigneeId = assigneeFilter.getValue() != null ? assigneeFilter.getValue().getUserId() : null;
        
        List<ShowTaskResponse> tasks = taskService.filterTasks(
                projectId, 
                statusFilter.getValue(), 
                null, 
                assigneeId
        );
        taskGrid.setItems(tasks);
    }

    private String getStatusTheme(Task.TaskStatus status) {
        return switch (status) {
            case TODO -> "contrast";
            case IN_PROGRESS -> "primary";
            case PENDING_APPROVAL -> "warning";
            case DONE -> "success";
            case CHANGE_REQUESTED -> "error";
            case EXTENSION_REQUESTED -> "tertiary";
        };
    }
}