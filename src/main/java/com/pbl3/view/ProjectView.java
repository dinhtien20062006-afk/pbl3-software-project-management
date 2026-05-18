package com.pbl3.view;

import java.time.format.DateTimeFormatter;

import com.pbl3.dto.request.ProjectRequest;
import com.pbl3.dto.response.ProjectResponse;
import com.pbl3.entity.Project;
import com.pbl3.service.ProjectService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.data.binder.Binder;

import jakarta.annotation.security.PermitAll;

import java.time.LocalDateTime;

@Route(value = "projects", layout = MainLayout.class)
@PageTitle("Quản lý dự án")
@PermitAll
public class ProjectView extends VerticalLayout {

    private final ProjectService projectService;
    private final AuthenticationContext authContext;
    private final Grid<ProjectResponse> grid = new Grid<>(ProjectResponse.class, false);
    private final TextField searchField = new TextField();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");

    public ProjectView(ProjectService projectService, AuthenticationContext authContext) {
        this.projectService = projectService;
        this.authContext = authContext;

        setSizeFull();
        setupHeader();
        setupGrid();
        refreshGrid();
    }

    private void setupHeader() {
        H2 title = new H2("Danh sách dự án");

        searchField.setPlaceholder("Tìm theo tên dự án...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.addValueChangeListener(e -> refreshGrid());

        Button addBtn = new Button("Tạo dự án mới", VaadinIcon.PLUS.create(), e -> openProjectDialog(null));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        // Chỉ hiển thị nút "Tạo mới" cho PROJECT_MANAGER hoặc ADMIN
        boolean canCreate = authContext.getAuthenticatedUser(org.springframework.security.core.userdetails.User.class)
                .map(u -> u.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_PROJECT_MANAGER") || a.getAuthority().equals("ROLE_ADMIN")))
                .orElse(false);
        addBtn.setVisible(canCreate);

        HorizontalLayout toolbar = new HorizontalLayout(searchField, addBtn);
        toolbar.setWidthFull();
        toolbar.expand(searchField);

        add(title, toolbar);
    }

    private void setupGrid() {
        grid.addColumn(ProjectResponse::getProjectName).setHeader("Tên dự án").setSortable(true).setResizable(true);
        grid.addColumn(ProjectResponse::getManagerName).setHeader("Người quản lý").setResizable(true);
        
        // Hiển thị Status kèm màu sắc (Badge)
        grid.addComponentColumn(project -> {
            Span status = new Span(project.getStatus().toString());
            status.getElement().getThemeList().add("badge");
            if (project.getStatus() == Project.ProjectStatus.COMPLETED) status.getElement().getThemeList().add("success");
            if (project.getStatus() == Project.ProjectStatus.IN_PROGRESS) status.getElement().getThemeList().add("contrast");
            return status;
        }).setHeader("Trạng thái");

        grid.addColumn(project -> project.getStartDate() != null ? project.getStartDate().format(dateFormatter) : "-").setHeader("Ngày bắt đầu");
        grid.addColumn(project -> project.getEndDate() != null ? project.getEndDate().format(dateFormatter) : "-").setHeader("Hạn chót");

        grid.addComponentColumn(project -> {
            HorizontalLayout actions = new HorizontalLayout();

            // 1. Chi tiết: Chuyển hướng sang trang Team/Task
            Button detailBtn = new Button(VaadinIcon.SEARCH.create(), e -> {
                getUI().ifPresent(ui -> ui.navigate(TeamView.class, project.getId()));
            });
            detailBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_SMALL);
            detailBtn.setTooltipText("Xem chi tiết dự án");

            // 2. Sửa & Xóa: Chỉ hiện nếu là Manager dự án hoặc ADMIN
            boolean hasPermission = isOwnerOrAdmin(project);

            Button editBtn = new Button(VaadinIcon.EDIT.create(), e -> openProjectDialog(project));
            editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            editBtn.setVisible(hasPermission);

            Button deleteBtn = new Button(VaadinIcon.TRASH.create(), e -> {
                // Thêm xác nhận trước khi xóa
                confirmAndDelete(project);
            });
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            deleteBtn.setVisible(hasPermission);

            actions.add(detailBtn, editBtn, deleteBtn);
            return actions;
        }).setHeader("Thao tác").setAutoWidth(true);

        grid.setSizeFull();
        add(grid);
    }

    private void refreshGrid() {
        String filter = searchField.getValue();
        try {
            if (filter == null || filter.isEmpty()) {
                grid.setItems(projectService.getAllProjects());
            } else {
                grid.setItems(projectService.getProjectsByName(filter));
            }
        } catch (Exception ex) {
            Notification.show("Lỗi khi tải dữ liệu: " + ex.getMessage(), 3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private boolean isOwnerOrAdmin(ProjectResponse project) {
        return authContext.getAuthenticatedUser(org.springframework.security.core.userdetails.User.class)
                .map(u -> {
                    boolean isAdmin = u.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                    // Logic khớp với ProjectService: Manager của project hoặc Admin
                    return isAdmin || u.getUsername().equals(project.getManagerUsername());
                }).orElse(false);
    }

        private void openProjectDialog(ProjectResponse project) {
        boolean isEdit = (project != null);
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(isEdit ? "Cập nhật dự án" : "Tạo dự án mới");

        // --- Khởi tạo các Fields ---
        TextField nameField = new TextField("Tên dự án");
        TextArea descField = new TextArea("Mô tả");
        DateTimePicker startPicker = new DateTimePicker("Ngày bắt đầu");
        DateTimePicker endPicker = new DateTimePicker("Ngày kết thúc");
        ComboBox<Project.ProjectStatus> statusBox = new ComboBox<>("Trạng thái");
        statusBox.setItems(Project.ProjectStatus.values());
        statusBox.setVisible(isEdit);

        startPicker.setMin(LocalDateTime.now());
        endPicker.setMin(LocalDateTime.now());

        // --- Cấu hình Binder ---
        // Sử dụng UpdateProjectRequest làm object trung gian để lưu dữ liệu từ form
        Binder<ProjectRequest> binder = new Binder<>(ProjectRequest.class);

        // Validate Tên dự án
        binder.forField(nameField)
            .asRequired("Tên dự án không được để trống")
            .withValidator(name -> name.length() >= 3, "Tên dự án phải có ít nhất 3 ký tự")
            .bind(ProjectRequest::getProjectName, ProjectRequest::setProjectName);

        // Validate Mô tả (không bắt buộc nhưng giới hạn độ dài)
        binder.forField(descField)
            .withValidator(desc -> desc == null || desc.length() <= 500, "Mô tả không quá 500 ký tự")
            .bind(ProjectRequest::getDescription, ProjectRequest::setDescription);

        // Validate Ngày bắt đầu
        binder.forField(startPicker)
            .asRequired("Phải chọn ngày bắt đầu")
            .bind(ProjectRequest::getStartDate, ProjectRequest::setStartDate);

        // Validate Ngày kết thúc (Phải sau ngày bắt đầu)
        binder.forField(endPicker)
            .asRequired("Phải chọn hạn chót")
            .withValidator(endDate -> {
                LocalDateTime startDate = startPicker.getValue();
                return startDate == null || endDate == null || !endDate.isBefore(startDate);
            }, "Ngày kết thúc không được trước ngày bắt đầu")
            .bind(ProjectRequest::getEndDate, ProjectRequest::setEndDate);

        // Bind Status
        binder.forField(statusBox)
            .bind(ProjectRequest::getStatus, ProjectRequest::setStatus);

        // --- Khởi tạo dữ liệu cho Binder ---
        ProjectRequest requestObject = new ProjectRequest();
        if (isEdit) {
            // Đổ dữ liệu từ ProjectResponse sang requestObject
            requestObject.setProjectName(project.getProjectName());
            requestObject.setDescription(project.getDescription());
            requestObject.setStartDate(project.getStartDate());
            requestObject.setEndDate(project.getEndDate());
            requestObject.setStatus(project.getStatus());
        }
        binder.readBean(requestObject); // Đưa dữ liệu vào các field trên UI

        // --- Xử lý nút Lưu ---
        Button saveBtn = new Button("Lưu", e -> {
            // writeBean sẽ kiểm tra tất cả validators, nếu OK mới nạp dữ liệu vào requestObject
            if (binder.writeBeanIfValid(requestObject)) {
                try {
                    if (!isEdit) {
                        ProjectRequest createReq = new ProjectRequest();
                        createReq.setProjectName(requestObject.getProjectName());
                        createReq.setDescription(requestObject.getDescription());
                        createReq.setStartDate(requestObject.getStartDate());
                        createReq.setEndDate(requestObject.getEndDate());
                        projectService.createProject(createReq);
                    } else {
                        projectService.updateProject(project.getId(), requestObject);
                    }
                    
                    refreshGrid();
                    dialog.close();
                    Notification.show("Lưu thành công", 2000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } catch (Exception ex) {
                    Notification.show(ex.getMessage(), 5000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            } else {
                Notification.show("Vui lòng kiểm tra lại thông tin nhập liệu", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        VerticalLayout dialogLayout = new VerticalLayout(nameField, descField, new HorizontalLayout(startPicker, endPicker), statusBox);
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(true);
        dialog.add(dialogLayout);
        dialog.getFooter().add(new Button("Hủy", ev -> dialog.close()), saveBtn);
        dialog.open();
    }

    private void confirmAndDelete(ProjectResponse project) {
        Dialog confirmDialog = new Dialog();
        confirmDialog.setHeaderTitle("Xác nhận xóa");
        confirmDialog.add(new Span("Bạn có chắc chắn muốn xóa dự án '" + project.getProjectName() + "'?"));
        
        Button deleteBtn = new Button("Xóa", e -> {
            try {
                projectService.deleteProject(project.getId());
                refreshGrid();
                confirmDialog.close();
                Notification.show("Đã xóa dự án");
            } catch (Exception ex) {
                Notification.show(ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        
        confirmDialog.getFooter().add(new Button("Hủy", e -> confirmDialog.close()), deleteBtn);
        confirmDialog.open();
    }
}