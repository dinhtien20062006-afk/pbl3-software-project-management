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
            if (project.getStatus() == Project.ProjectStatus.ON_HOLD) status.getElement().getThemeList().add("error");
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
            boolean projectOnHold = project.getStatus() == Project.ProjectStatus.ON_HOLD;
            boolean projectCompleted = project.getStatus() == Project.ProjectStatus.COMPLETED;

            Button editBtn = new Button(VaadinIcon.EDIT.create(), e -> openProjectDialog(project));
            editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            editBtn.setVisible(hasPermission && !projectOnHold && !projectCompleted);
            editBtn.setTooltipText(projectCompleted ? "Dự án đã hoàn thành, chỉ được xem thông tin"
                    : (projectOnHold ? "Dự án đang tạm dừng, không thể chỉnh sửa" : "Chỉnh sửa dự án"));

            Button doneBtn = new Button("Xong", e -> confirmAndComplete(project));
            doneBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS);
            doneBtn.setTooltipText("Đánh dấu dự án hoàn thành khi tất cả task đã xong");
            doneBtn.setVisible(hasPermission && project.getStatus() == Project.ProjectStatus.IN_PROGRESS);

            Button holdBtn = new Button("Tạm dừng", e -> confirmAndHold(project));
            holdBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            holdBtn.setTooltipText("Tạm dừng dự án nếu dự án chưa thể tiếp tục");
            holdBtn.setVisible(hasPermission && project.getStatus() != Project.ProjectStatus.COMPLETED
                    && project.getStatus() != Project.ProjectStatus.ON_HOLD);

            Button resumeBtn = new Button("Tiếp tục", e -> openResumeProjectDialog(project));
            resumeBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
            resumeBtn.setTooltipText("Tiếp tục dự án đang tạm dừng");
            resumeBtn.setVisible(hasPermission && project.getStatus() == Project.ProjectStatus.ON_HOLD);

            Button deleteBtn = new Button(VaadinIcon.TRASH.create(), e -> {
                // Thêm xác nhận trước khi xóa
                confirmAndDelete(project);
            });
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            deleteBtn.setVisible(hasPermission && !projectCompleted);
            deleteBtn.setTooltipText(projectCompleted ? "Dự án đã hoàn thành, chỉ được xem thông tin"
                    : (projectOnHold ? "Xóa dự án đang tạm dừng" : "Xóa dự án"));

            actions.add(detailBtn, doneBtn, holdBtn, resumeBtn, editBtn, deleteBtn);
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
        boolean isInProgressEdit = isEdit && project.getStatus() == Project.ProjectStatus.IN_PROGRESS;
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(isEdit ? "Cập nhật dự án" : "Tạo dự án mới");

        TextField nameField = new TextField("Tên dự án");
        TextArea descField = new TextArea("Mô tả");
        DateTimePicker startPicker = new DateTimePicker("Ngày bắt đầu");
        DateTimePicker endPicker = new DateTimePicker("Ngày kết thúc");

        if (!isEdit) {
            startPicker.setMin(LocalDateTime.now());
            endPicker.setMin(LocalDateTime.now());
        }

        if (isInProgressEdit) {
            startPicker.setEnabled(false);
            startPicker.setTooltipText("Dự án đang thực hiện nên không thể sửa ngày bắt đầu");
        }

        Binder<ProjectRequest> binder = new Binder<>(ProjectRequest.class);

        binder.forField(nameField)
            .asRequired("Tên dự án không được để trống")
            .withValidator(name -> name.length() >= 3, "Tên dự án phải có ít nhất 3 ký tự")
            .bind(ProjectRequest::getProjectName, ProjectRequest::setProjectName);

        binder.forField(descField)
            .withValidator(desc -> desc == null || desc.length() <= 500, "Mô tả không quá 500 ký tự")
            .bind(ProjectRequest::getDescription, ProjectRequest::setDescription);

        binder.forField(startPicker)
            .asRequired("Phải chọn ngày bắt đầu")
            .bind(ProjectRequest::getStartDate, ProjectRequest::setStartDate);

        // Nếu chọn hạn chót trước ngày bắt đầu thì hệ thống sẽ tự chỉnh lại cho hợp lý,
        // không bắt lỗi cứng như trước.
        binder.forField(endPicker)
            .asRequired("Phải chọn hạn chót")
            .bind(ProjectRequest::getEndDate, ProjectRequest::setEndDate);

        startPicker.addValueChangeListener(ev -> {
            LocalDateTime startDate = ev.getValue();
            LocalDateTime endDate = endPicker.getValue();
            if (startDate != null) {
                endPicker.setMin(startDate);
                if (endDate != null && endDate.isBefore(startDate)) {
                    endPicker.setValue(startDate);
                }
            }
        });

        endPicker.addValueChangeListener(ev -> {
            LocalDateTime startDate = startPicker.getValue();
            LocalDateTime endDate = ev.getValue();
            if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
                endPicker.setValue(startDate);
            }
        });

        ProjectRequest requestObject = new ProjectRequest();
        if (isEdit) {
            requestObject.setProjectName(project.getProjectName());
            requestObject.setDescription(project.getDescription());
            requestObject.setStartDate(project.getStartDate());
            requestObject.setEndDate(project.getEndDate());
        }
        binder.readBean(requestObject);

        Button saveBtn = new Button("Lưu", e -> {
            if (binder.writeBeanIfValid(requestObject)) {
                try {
                    if (requestObject.getStartDate() != null && requestObject.getEndDate() != null
                            && requestObject.getEndDate().isBefore(requestObject.getStartDate())) {
                        requestObject.setEndDate(requestObject.getStartDate());
                    }

                    if (!isEdit) {
                        ProjectRequest createReq = new ProjectRequest();
                        createReq.setProjectName(requestObject.getProjectName());
                        createReq.setDescription(requestObject.getDescription());
                        createReq.setStartDate(requestObject.getStartDate());
                        createReq.setEndDate(requestObject.getEndDate());
                        projectService.createProject(createReq);
                    } else {
                        ProjectRequest updateReq = new ProjectRequest();
                        updateReq.setProjectName(requestObject.getProjectName());
                        updateReq.setDescription(requestObject.getDescription());
                        updateReq.setEndDate(requestObject.getEndDate());

                        // Dự án IN_PROGRESS không gửi startDate xuống Service để tránh sửa ngày bắt đầu.
                        if (!isInProgressEdit) {
                            updateReq.setStartDate(requestObject.getStartDate());
                        }

                        projectService.updateProject(project.getId(), updateReq);
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
        VerticalLayout dialogLayout = new VerticalLayout(nameField, descField, new HorizontalLayout(startPicker, endPicker));
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(true);
        dialog.add(dialogLayout);
        dialog.getFooter().add(new Button("Hủy", ev -> dialog.close()), saveBtn);
        dialog.open();
    }


    private void confirmAndComplete(ProjectResponse project) {
        Dialog confirmDialog = new Dialog();
        confirmDialog.setHeaderTitle("Xác nhận hoàn thành dự án");
        confirmDialog.add(new Span("Dự án chỉ được hoàn thành khi tất cả task đã xong. Bạn muốn tiếp tục?"));

        Button doneBtn = new Button("Xong", e -> {
            try {
                projectService.completeProject(project.getId());
                refreshGrid();
                confirmDialog.close();
                Notification.show("Đã chuyển dự án sang COMPLETED")
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Lỗi: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        doneBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        confirmDialog.getFooter().add(new Button("Hủy", e -> confirmDialog.close()), doneBtn);
        confirmDialog.open();
    }

    private void confirmAndHold(ProjectResponse project) {
        Dialog confirmDialog = new Dialog();
        confirmDialog.setHeaderTitle("Xác nhận tạm dừng dự án");
        confirmDialog.add(new Span("Dự án sẽ được chuyển sang trạng thái tạm dừng. Bạn có chắc chắn không?"));

        Button holdBtn = new Button("Tạm dừng", e -> {
            try {
                projectService.putProjectOnHold(project.getId());
                refreshGrid();
                confirmDialog.close();
                Notification.show("Đã tạm dừng dự án")
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);
            } catch (Exception ex) {
                Notification.show("Lỗi: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        holdBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

        confirmDialog.getFooter().add(new Button("Hủy", e -> confirmDialog.close()), holdBtn);
        confirmDialog.open();
    }


    private void openResumeProjectDialog(ProjectResponse project) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Cập nhật thông tin và tiếp tục dự án");
        dialog.setWidth("650px");

        TextField nameField = new TextField("Tên dự án");
        TextArea descField = new TextArea("Mô tả");
        DateTimePicker startPicker = new DateTimePicker("Ngày bắt đầu");
        DateTimePicker endPicker = new DateTimePicker("Ngày kết thúc");

        nameField.setWidthFull();
        descField.setWidthFull();
        startPicker.setWidthFull();
        endPicker.setWidthFull();

        Span note = new Span("Dự án đang tạm dừng. Bạn có thể chỉnh lại thông tin trước khi tiếp tục.");
        note.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Binder<ProjectRequest> binder = new Binder<>(ProjectRequest.class);
        ProjectRequest requestObject = new ProjectRequest();
        requestObject.setProjectName(project.getProjectName());
        requestObject.setDescription(project.getDescription());
        requestObject.setStartDate(project.getStartDate());
        requestObject.setEndDate(project.getEndDate());

        binder.forField(nameField)
                .asRequired("Tên dự án không được để trống")
                .withValidator(name -> name.length() >= 3, "Tên dự án phải có ít nhất 3 ký tự")
                .bind(ProjectRequest::getProjectName, ProjectRequest::setProjectName);

        binder.forField(descField)
                .withValidator(desc -> desc == null || desc.length() <= 500, "Mô tả không quá 500 ký tự")
                .bind(ProjectRequest::getDescription, ProjectRequest::setDescription);

        binder.forField(startPicker)
                .asRequired("Phải chọn ngày bắt đầu")
                .bind(ProjectRequest::getStartDate, ProjectRequest::setStartDate);

        binder.forField(endPicker)
                .asRequired("Phải chọn hạn chót")
                .bind(ProjectRequest::getEndDate, ProjectRequest::setEndDate);

        startPicker.addValueChangeListener(ev -> {
            LocalDateTime startDate = ev.getValue();
            LocalDateTime endDate = endPicker.getValue();
            if (startDate != null) {
                endPicker.setMin(startDate);
                if (endDate != null && endDate.isBefore(startDate)) {
                    endPicker.setValue(startDate);
                }
            }
        });

        endPicker.addValueChangeListener(ev -> {
            LocalDateTime startDate = startPicker.getValue();
            LocalDateTime endDate = ev.getValue();
            if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
                endPicker.setValue(startDate);
            }
        });

        binder.readBean(requestObject);

        Button resumeBtn = new Button("Lưu và tiếp tục", e -> {
            if (!binder.writeBeanIfValid(requestObject)) {
                Notification.show("Vui lòng kiểm tra lại thông tin nhập liệu", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            try {
                if (requestObject.getStartDate() != null && requestObject.getEndDate() != null
                        && requestObject.getEndDate().isBefore(requestObject.getStartDate())) {
                    requestObject.setEndDate(requestObject.getStartDate());
                }

                projectService.resumeProject(project.getId(), requestObject);
                refreshGrid();
                dialog.close();
                Notification.show("Đã cập nhật thông tin và tiếp tục dự án")
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Lỗi: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        resumeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        VerticalLayout layout = new VerticalLayout(
                note,
                nameField,
                descField,
                new HorizontalLayout(startPicker, endPicker)
        );
        layout.setPadding(false);
        layout.setSpacing(true);
        layout.setWidthFull();

        dialog.add(layout);
        dialog.getFooter().add(new Button("Hủy", e -> dialog.close()), resumeBtn);
        dialog.open();
    }


    private void confirmAndResume(ProjectResponse project) {
        Dialog confirmDialog = new Dialog();
        confirmDialog.setHeaderTitle("Xác nhận tiếp tục dự án");
        confirmDialog.add(new Span("Dự án đang tạm dừng sẽ được mở lại. Nếu đã đến ngày bắt đầu, dự án và các nhóm đang lập kế hoạch sẽ chuyển sang đang thực hiện."));

        Button resumeBtn = new Button("Tiếp tục", e -> {
            try {
                projectService.resumeProject(project.getId());
                refreshGrid();
                confirmDialog.close();
                Notification.show("Đã tiếp tục dự án")
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Lỗi: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        resumeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        confirmDialog.getFooter().add(new Button("Hủy", e -> confirmDialog.close()), resumeBtn);
        confirmDialog.open();
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