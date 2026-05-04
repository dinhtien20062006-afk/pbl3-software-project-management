package com.pbl3.view;

import com.pbl3.dto.request.CreateProjectRequest;
import com.pbl3.dto.request.UpdateProjectRequest;
import com.pbl3.dto.response.ProjectResponse;
import com.pbl3.entity.Project;
import com.pbl3.service.ProjectService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
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
import jakarta.annotation.security.PermitAll;


@Route(value = "projects", layout = MainLayout.class)
@PageTitle("Quản lý dự án")
@PermitAll
public class ProjectView extends VerticalLayout {

    private final ProjectService projectService;
    private final AuthenticationContext authContext;
    private final Grid<ProjectResponse> grid = new Grid<>(ProjectResponse.class, false);
    private final TextField searchField = new TextField();

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
        searchField.addValueChangeListener(e -> refreshGrid());

        Button addBtn = new Button("Tạo dự án mới", VaadinIcon.PLUS.create(), e -> openProjectDialog(null));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        // Chỉ hiển thị nút "Tạo mới" cho Project Manager
        boolean isManager = authContext.getAuthenticatedUser(org.springframework.security.core.userdetails.User.class)
                .map(u -> u.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PROJECT_MANAGER")))
                .orElse(false);
        addBtn.setVisible(isManager);

        HorizontalLayout toolbar = new HorizontalLayout(searchField, addBtn);
        toolbar.setWidthFull();
        toolbar.expand(searchField);

        add(title, toolbar);
    }

    private void setupGrid() {
        grid.addColumn(ProjectResponse::getProjectName).setHeader("Tên dự án").setSortable(true);
        grid.addColumn(ProjectResponse::getManagerName).setHeader("Người quản lý");
        grid.addColumn(ProjectResponse::getStatus).setHeader("Trạng thái");
        grid.addColumn(ProjectResponse::getStartDate).setHeader("Ngày bắt đầu");
        grid.addColumn(ProjectResponse::getEndDate).setHeader("Hạn chót");

        // Cột hành động (Sửa/Xóa)
        grid.addComponentColumn(project -> {
            HorizontalLayout actions = new HorizontalLayout();

            // 1. Nút Chi tiết (Mọi người đều có thể nhấn để xem)
            Button detailBtn = new Button(VaadinIcon.SEARCH.create(), e -> {
                // Điều hướng sang trang chi tiết với ID dự án
                getUI().ifPresent(ui -> ui.navigate(ProjectDetailView.class, project.getId()));
            });
            detailBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_SMALL);
            detailBtn.setTooltipText("Xem chi tiết dự án");

            Button editBtn = new Button(VaadinIcon.EDIT.create(), e -> openProjectDialog(project));
            editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            editBtn.setVisible(isOwner(project));

            Button deleteBtn = new Button(VaadinIcon.TRASH.create(), e -> {
                try {
                    projectService.deleteProject(project.getId());
                    refreshGrid();
                    Notification.show("Đã xóa dự án");
                } catch (Exception ex) {
                    Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            deleteBtn.setVisible(isOwner(project));

            actions.add(detailBtn, editBtn, deleteBtn);
            
            return actions;
        }).setHeader("Thao tác").setAutoWidth(true);

        grid.setSizeFull();
        add(grid);
    }

    private void refreshGrid() {
        String filter = searchField.getValue();
        if (filter == null || filter.isEmpty()) {
            grid.setItems(projectService.getAllProjects());
        } else {
            grid.setItems(projectService.searchProjectByName(filter));
        }
    }

    private boolean isOwner(ProjectResponse project) {
        // Lưu ý: ProjectResponse của bạn cần trả về Username của Manager để so sánh chính xác hơn FullName
        // Ở đây tạm dùng logic nếu role là MANAGER thì cho hiện (Service sẽ check quyền lần 2 khi gọi hàm)
        return authContext.getAuthenticatedUser(org.springframework.security.core.userdetails.User.class)
                .map(u -> u.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PROJECT_MANAGER")))
                .orElse(false);
    }

    // --- Dialog Thêm/Sửa dự án ---
    private void openProjectDialog(ProjectResponse project) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(project == null ? "Tạo dự án mới" : "Cập nhật dự án");

        TextField nameField = new TextField("Tên dự án");
        TextArea descField = new TextArea("Mô tả");
        DatePicker startPicker = new DatePicker("Ngày bắt đầu");
        DatePicker endPicker = new DatePicker("Ngày kết thúc");
        ComboBox<Project.ProjectStatus> statusBox = new ComboBox<>("Trạng thái");
        statusBox.setItems(Project.ProjectStatus.values());

        if (project != null) {
            nameField.setValue(project.getProjectName());
            descField.setValue(project.getDescription() != null ? project.getDescription() : "");
            startPicker.setValue(project.getStartDate()); 
            endPicker.setValue(project.getEndDate());
            statusBox.setValue(project.getStatus());
        }

        Button saveBtn = new Button("Lưu", e -> {
            try {
                if (project == null) {
                    CreateProjectRequest req = new CreateProjectRequest();
                    req.setProjectName(nameField.getValue());
                    req.setDescription(descField.getValue());
                    req.setStartDate(startPicker.getValue());
                    req.setEndDate(endPicker.getValue());
                    projectService.createProject(req);
                } else {
                    UpdateProjectRequest req = new UpdateProjectRequest();
                    req.setProjectName(nameField.getValue());
                    req.setDescription(descField.getValue());
                    req.setStartDate(startPicker.getValue());
                    req.setEndDate(endPicker.getValue());
                    req.setStatus(statusBox.getValue());
                    projectService.updateProject(project.getId(), req);
                }
                refreshGrid();
                dialog.close();
                Notification.show("Thành công!").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show(ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        VerticalLayout dialogLayout = new VerticalLayout(nameField, descField, startPicker, endPicker, statusBox);
        dialog.add(dialogLayout);
        dialog.getFooter().add(new Button("Hủy", e -> dialog.close()), saveBtn);
        dialog.open();
    }
}