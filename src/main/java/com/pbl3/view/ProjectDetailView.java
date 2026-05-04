package com.pbl3.view;

import com.pbl3.dto.request.ProjectMemberRequest;
import com.pbl3.dto.response.ProjectMemberResponse;
import com.pbl3.dto.response.ProjectResponse;
import com.pbl3.dto.response.ShowInfoResponse;
import com.pbl3.service.ProjectMemberService;
import com.pbl3.service.ProjectService;
import com.pbl3.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "project-detail", layout = MainLayout.class)
@PageTitle("Chi tiết dự án")
@PermitAll
public class ProjectDetailView extends VerticalLayout implements HasUrlParameter<Long> {

    private final ProjectService projectService;
    private final ProjectMemberService memberService;
    private final UserService userService;
    private final AuthenticationContext authContext;

    private Long projectId;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Components thông tin
    private final Span nameLabel = new Span();
    private final Span statusLabel = new Span();
    private final Span managerLabel = new Span();
    private final Span dateLabel = new Span();
    private final Grid<ProjectMemberResponse> memberGrid = new Grid<>(ProjectMemberResponse.class, false);
    
    // Buttons điều khiển
    private final Button addMemberBtn = new Button("Thêm thành viên", VaadinIcon.PLUS.create());
    private final Button leaveProjectBtn = new Button("Rời dự án", VaadinIcon.EXIT.create());
    private final Button viewTasksBtn = new Button("Xem nhiệm vụ", VaadinIcon.TASKS.create());

    public ProjectDetailView(ProjectService projectService, ProjectMemberService memberService, 
                             UserService userService, AuthenticationContext authContext) {
        this.projectService = projectService;
        this.memberService = memberService;
        this.userService = userService;
        this.authContext = authContext;

        setSizeFull();
        setupProjectInfoSection();
        setupMemberSection();
    }

    @Override
    public void setParameter(BeforeEvent event, Long parameter) {
        this.projectId = parameter;
        refreshData();
    }

    private void setupProjectInfoSection() {
        H3 title = new H3("Thông tin dự án");
        FormLayout infoLayout = new FormLayout();
        infoLayout.addFormItem(nameLabel, "Tên dự án:");
        infoLayout.addFormItem(statusLabel, "Trạng thái:");
        infoLayout.addFormItem(managerLabel, "Người quản lý:");
        infoLayout.addFormItem(dateLabel, "Thời gian:");

        Button backBtn = new Button("Quay lại", VaadinIcon.ARROW_LEFT.create(), 
                e -> getUI().ifPresent(ui -> ui.navigate(ProjectView.class)));
        
        viewTasksBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        viewTasksBtn.addClickListener(e -> {
            getUI().ifPresent(ui -> ui.navigate(TaskManagementView.class, projectId));
        });
        leaveProjectBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        leaveProjectBtn.addClickListener(e -> showLeaveConfirmation());
        Button logBtn = new Button("Nhật ký dự án", VaadinIcon.TIME_BACKWARD.create());
        logBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(AuditLogView.class, projectId)));

        HorizontalLayout actions = new HorizontalLayout(backBtn, viewTasksBtn, leaveProjectBtn, logBtn);
        add(actions, title, infoLayout);
    }

    private void setupMemberSection() {
        H3 memberTitle = new H3("Danh sách thành viên");
        
        addMemberBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        addMemberBtn.addClickListener(e -> openAddMemberDialog());

        memberGrid.addColumn(ProjectMemberResponse::getFullName).setHeader("Họ tên");
        memberGrid.addColumn(ProjectMemberResponse::getUsername).setHeader("Username");
        memberGrid.addColumn(ProjectMemberResponse::getRole).setHeader("Vai trò");
        
        // Định dạng ngày tham gia d/m/y
        memberGrid.addColumn(member -> member.getJoinedAt() != null ? 
                member.getJoinedAt().format(dateFormatter) : "-")
                .setHeader("Ngày tham gia");

        memberGrid.addComponentColumn(member -> {
            HorizontalLayout rowActions = new HorizontalLayout();

            Button removeBtn = new Button(VaadinIcon.MINUS.create(), e -> {
                try {
                    memberService.removeMember(projectId, member.getUserId());
                    refreshData();
                    Notification.show("Đã xóa thành viên");
                } catch (Exception ex) {
                    Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });
            removeBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            
            // Logic ẩn/hiện nút xóa trong từng dòng
            String currentUsername = authContext.getPrincipalName().orElse("");
            ProjectResponse project = projectService.getProjectById(projectId);
            boolean isProjectManager = project.getManagerUsername().equals(currentUsername);
            
            removeBtn.setVisible(isProjectManager && !member.getUsername().equals(currentUsername));
            
            rowActions.add(removeBtn);
            return rowActions;
        }).setHeader("Thao tác");

        add(memberTitle, addMemberBtn, memberGrid);
    }

    private void openAddMemberDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Thêm thành viên mới");

        ComboBox<ShowInfoResponse> userSearchBox = new ComboBox<>("Tìm người dùng");
        userSearchBox.setPlaceholder("Nhập username...");
        userSearchBox.setItemLabelGenerator(u -> u.getUsername() + " (" + u.getFullName() + ")");
        
        userSearchBox.setItems(query -> {
            String filter = query.getFilter().orElse("");
            List<ShowInfoResponse> result = userService.searchUserByUsername(filter);
            return result.stream().skip(query.getOffset()).limit(query.getLimit());
        });

        TextField roleField = new TextField("Vai trò trong dự án");
        roleField.setPlaceholder("VD: Developer, Tester...");

        Button saveBtn = new Button("Thêm", e -> {
            if (userSearchBox.getValue() == null) return;
            try {
                ProjectMemberRequest req = new ProjectMemberRequest();
                req.setUserId(userSearchBox.getValue().getUserId());
                req.setRole(roleField.getValue());
                
                memberService.addMember(projectId, req);
                refreshData();
                dialog.close();
                Notification.show("Đã thêm thành viên!").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show(ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        VerticalLayout layout = new VerticalLayout(userSearchBox, roleField);
        dialog.add(layout);
        dialog.getFooter().add(new Button("Hủy", e -> dialog.close()), saveBtn);
        dialog.open();
    }

    private void showLeaveConfirmation() {
        Dialog confirmDialog = new Dialog();
        confirmDialog.setHeaderTitle("Xác nhận rời dự án");
        confirmDialog.add(new Span("Bạn có chắc chắn muốn rời khỏi dự án này không? Thao tác này không thể hoàn tác."));

        Button confirmBtn = new Button("Xác nhận rời", e -> {
            try {
                memberService.leaveProject(projectId);
                Notification.show("Bạn đã rời khỏi dự án");
                getUI().ifPresent(ui -> ui.navigate(ProjectView.class));
                confirmDialog.close();
            } catch (Exception ex) {
                Notification.show(ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);

        confirmDialog.getFooter().add(new Button("Hủy", e -> confirmDialog.close()), confirmBtn);
        confirmDialog.open();
    }

    private void refreshData() {
        if (projectId == null) return;
        try {
            ProjectResponse project = projectService.getProjectById(projectId);
            String currentUsername = authContext.getPrincipalName().orElse("");
            boolean isProjectManager = project.getManagerUsername().equals(currentUsername);

            // Cập nhật text thông tin dự án + định dạng ngày
            nameLabel.setText(project.getProjectName());
            statusLabel.setText(project.getStatus().toString());
            managerLabel.setText(project.getManagerName());
            
            String startStr = project.getStartDate() != null ? project.getStartDate().format(dateFormatter) : "N/A";
            String endStr = project.getEndDate() != null ? project.getEndDate().format(dateFormatter) : "Chưa xác định";
            dateLabel.setText(startStr + " - " + endStr);

            // Phân quyền nút bấm chính
            addMemberBtn.setVisible(isProjectManager);
            leaveProjectBtn.setVisible(!isProjectManager); // Manager không được tự rời theo cách này

            // Cập nhật danh sách thành viên
            List<ProjectMemberResponse> members = memberService.getMembers(projectId);
            memberGrid.setItems(members);
            
        } catch (Exception e) {
            Notification.show("Không thể tải dữ liệu dự án").addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}