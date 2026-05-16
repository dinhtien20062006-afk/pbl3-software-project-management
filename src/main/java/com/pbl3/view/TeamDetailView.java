package com.pbl3.view;

import com.pbl3.dto.request.TeamMemberRequest;
import com.pbl3.dto.response.ShowInfoResponse;
import com.pbl3.dto.response.TeamMemberResponse;
import com.pbl3.dto.response.TeamResponse;
import com.pbl3.service.ProjectTeamService;
import com.pbl3.service.TeamMemberService;
import com.pbl3.service.UserService;
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
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "team-details", layout = MainLayout.class)
@PageTitle("Thành viên nhóm")
@PermitAll
public class TeamDetailView extends VerticalLayout implements HasUrlParameter<Long> {

    private final TeamMemberService teamMemberService;
    private final ProjectTeamService projectTeamService;
    private final UserService userService;
    private final AuthenticationContext authContext;

    private Long teamId;
    private String currentUsername;

    // UI Components
    private final VerticalLayout teamInfoSection = new VerticalLayout();
    private final Grid<TeamMemberResponse> memberGrid = new Grid<>(TeamMemberResponse.class, false);
    private final Button addMemberBtn = new Button("Thêm thành viên", VaadinIcon.PLUS.create());
    private final Button leaveTeamBtn = new Button("Rời khỏi nhóm", VaadinIcon.SIGN_OUT.create());
    private final Button manageTasksBtn = new Button("Quản lý Task", VaadinIcon.TASKS.create());
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private boolean isLeader;

    public TeamDetailView(TeamMemberService teamMemberService, ProjectTeamService projectTeamService,
                          UserService userService, AuthenticationContext authContext) {
        this.teamMemberService = teamMemberService;
        this.projectTeamService = projectTeamService;
        this.userService = userService;
        this.authContext = authContext;

        setSizeFull();
        setupTeamHeaderSection();
        setupMemberGrid();
    }

    @Override
    public void setParameter(BeforeEvent event, Long parameter) {
        this.teamId = parameter;
        this.currentUsername = authContext.getPrincipalName().orElse("");
        refreshUI();
    }

    private void refreshUI() {
        teamInfoSection.removeAll();

        // 1. Lấy thông tin nhóm và danh sách tất cả các nhóm để xác định quyền PM
        // (Do ProjectTeamService đã có các hàm check quyền nội bộ, ta tận dụng dữ liệu mapping)
        List<TeamResponse> allTeams = projectTeamService.getAllTeams();
        TeamResponse currentTeam = allTeams.stream()
                .filter(t -> t.getTeamId().equals(teamId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dữ liệu nhóm hoặc bạn không có quyền xem"));

        // Xác định quyền: Nếu user sở hữu nhóm này trong danh sách getAllTeams() với tư cách PM/Leader
        // Hoặc so sánh tên Trưởng nhóm trực tiếp để cấp quyền Leader
        boolean isLeader = currentTeam.getLeaderName() != null && 
                userService.getAllUsers().stream()
                        .filter(u -> u.getFullName().equals(currentTeam.getLeaderName()))
                        .anyMatch(u -> u.getUsername().equals(currentUsername));
        this.isLeader = isLeader;

        // 2. Kết xuất thông tin chung của nhóm (Bố cục phía trên)
        H3 teamTitle = new H3("Nhóm: " + currentTeam.getTeamName());
        Span leaderInfo = new Span("Trưởng nhóm: " + currentTeam.getLeaderName());
        leaderInfo.getStyle().set("font-weight", "bold").set("color", "var(--lumo-primary-color)");
        
        Span descInfo = new Span("Mô tả: " + (currentTeam.getDescription() != null ? currentTeam.getDescription() : "Không có"));
        Span deadlineInfo = new Span("Hạn chót nhóm: " + (currentTeam.getDeadline() != null ? currentTeam.getDeadline().format(dateFormatter) : "-"));
        Span statusSpan = new Span("Trạng thái: " + currentTeam.getStatus());
        statusSpan.getElement().getThemeList().add("badge");

        teamInfoSection.add(teamTitle, leaderInfo, descInfo, deadlineInfo, statusSpan);

        // 3. Phân quyền ẩn hiện nút chức năng chính
        addMemberBtn.setVisible(isLeader);
        
        // Nút tự rời nhóm: Chỉ ẩn đối với Leader (Theo logic nghiệp vụ tầng Service)
        leaveTeamBtn.setVisible(!isLeader);

        // 4. Cập nhật dữ liệu lưới thành viên
        List<TeamMemberResponse> members = teamMemberService.getMembersByTeam(teamId);
        memberGrid.setItems(members);
        
        // Kiểm tra nếu User hiện tại thậm chí không có trong danh sách thành viên và cũng không phải PM/Admin thì ẩn nút rời nhóm
        boolean isActualMember = members.stream().anyMatch(m -> m.getUsername().equals(currentUsername));
        if (!isActualMember) {
            leaveTeamBtn.setVisible(false);
        }
    }

    private void setupTeamHeaderSection() {
        teamInfoSection.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "8px")
                .set("margin-bottom", "15px");
        teamInfoSection.setPadding(true);
        add(teamInfoSection);
    }

    private void setupMemberGrid() {
        memberGrid.addColumn(TeamMemberResponse::getFullName).setHeader("Họ và tên").setSortable(true);
        memberGrid.addColumn(TeamMemberResponse::getUsername).setHeader("Tài khoản");
        
        memberGrid.addComponentColumn(member -> {
            Span roleBadge = new Span(member.getRole());
            roleBadge.getElement().getThemeList().add("badge contrast");
            if ("LEADER".equalsIgnoreCase(member.getRole())) {
                roleBadge.getElement().getThemeList().add("success");
            }
            return roleBadge;
        
           
        }).setHeader("Vai trò trong nhóm");

        memberGrid.addColumn(m -> m.getJoinedAt() != null ? m.getJoinedAt().format(dateFormatter) : "-").setHeader("Ngày tham gia");

        
        // Cột Thao tác: Xóa thành viên (Chỉ hiển thị cho người có quyền quản lý và không được tự xóa Leader)
        memberGrid.addComponentColumn(member -> {
            HorizontalLayout actions = new HorizontalLayout(); 

            Button removeBtn = new Button(VaadinIcon.MINUS.create(), e -> confirmRemoveMember(member));
            removeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            removeBtn.setTooltipText("Xóa thành viên khỏi nhóm");
            
            // Chỉ PM/Leader mới thấy nút xóa và không được xóa tài khoản mang vai trò LEADER tại đây
            removeBtn.setVisible(isLeader && !"LEADER".equalsIgnoreCase(member.getRole()));

            actions.add(removeBtn);
            return actions;
        }).setHeader("Thao tác").setAutoWidth(true);

        // Thanh công cụ thao tác Grid
        addMemberBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addMemberBtn.addClickListener(e -> openAddMemberDialog());

        leaveTeamBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        leaveTeamBtn.addClickListener(e -> confirmLeaveTeam());

        manageTasksBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        manageTasksBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(TaskManagementView.class, teamId)));

        HorizontalLayout toolbar = new HorizontalLayout(new H3("Danh sách thành viên"), addMemberBtn, leaveTeamBtn, manageTasksBtn);
        toolbar.setVerticalComponentAlignment(Alignment.CENTER, addMemberBtn, leaveTeamBtn, manageTasksBtn);

        add(toolbar, memberGrid);
        memberGrid.setSizeFull();
    }

    private void openAddMemberDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Thêm thành viên vào nhóm");

        FormLayout formLayout = new FormLayout();
        ComboBox<ShowInfoResponse> userComboBox = new ComboBox<>("Chọn nhân sự");
        ComboBox<String> roleComboBox = new ComboBox<>("Vai trò đảm nhiệm");

        // Đổ dữ liệu cấu hình ComboBox
        userComboBox.setItems(userService.getAllUsers());
        userComboBox.setItemLabelGenerator(u -> u.getFullName() + " (" + u.getUsername() + ")");
        
        roleComboBox.setItems("DEVELOPER", "TESTER", "DESIGNER", "BUSINESS_ANALYST");
        roleComboBox.setAllowCustomValue(true);

        formLayout.add(userComboBox, roleComboBox);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        // Tích hợp Vaadin Binder quản lý dữ liệu DTO chuyên nghiệp
        Binder<TeamMemberRequest> binder = new Binder<>(TeamMemberRequest.class);
        TeamMemberRequest requestData = new TeamMemberRequest();

        binder.forField(userComboBox)
                .asRequired("Vui lòng chọn nhân sự cần thêm")
                .bind(src -> src.getUserId() != null ? userService.getAllUsers().stream().filter(u -> u.getUserId().equals(src.getUserId())).findFirst().orElse(null) : null,
                      (dest, value) -> dest.setUserId(value != null ? value.getUserId() : null));

        binder.forField(roleComboBox)
                .asRequired("Vui lòng chỉ định vai trò nghiệp vụ")
                .bind(TeamMemberRequest::getRole, TeamMemberRequest::setRole);

        binder.readBean(requestData);

        Button saveBtn = new Button("Thêm mới", e -> {
            try {
                if (binder.writeBeanIfValid(requestData)) {
                    teamMemberService.addMemberToTeam(teamId, requestData);
                    Notification.show("Đã thêm thành viên vào nhóm thành công", 3000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    refreshUI();
                    dialog.close();
                } else {
                    Notification.show("Vui lòng điền đầy đủ thông tin mẫu lỗi").addThemeVariants(NotificationVariant.LUMO_WARNING);
                }
            } catch (Exception ex) {
                Notification.show("Lỗi: " + ex.getMessage(), 4000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.add(formLayout);
        dialog.getFooter().add(new Button("Hủy", e -> dialog.close()), saveBtn);
        dialog.open();
    }

    private void confirmRemoveMember(TeamMemberResponse member) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Trục xuất thành viên");
        dialog.setText("Bạn có chắc chắn muốn xóa thành viên '" + member.getFullName() + "' ra khỏi nhóm? Hệ thống sẽ gỡ phân công toàn bộ Task hiện tại của nhân sự này trong nhóm.");
        
        dialog.setCancelable(true);
        dialog.setCancelText("Hủy bỏ");
        dialog.setConfirmText("Xác nhận xóa");
        dialog.setConfirmButtonTheme("error primary");

        dialog.addConfirmListener(e -> {
            try {
                teamMemberService.removeMemberFromTeam(teamId, member.getUserId());
                Notification.show("Đã xóa thành viên khỏi nhóm thành công").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                refreshUI();
            } catch (Exception ex) {
                Notification.show("Lỗi: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        dialog.open();
    }

    private void confirmLeaveTeam() {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Xác nhận rời nhóm");
        dialog.setText("Bạn có chắc chắn muốn tự xin rút khỏi nhóm làm việc này? Mọi tác vụ (Task) đang được giao cho bạn tại nhóm này sẽ chuyển về trạng thái trống (Unassigned).");

        dialog.setCancelable(true);
        dialog.setCancelText("Ở lại");
        dialog.setConfirmText("Rời nhóm");
        dialog.setConfirmButtonTheme("error primary");

        dialog.addConfirmListener(e -> {
            try {
                teamMemberService.leaveTeam(teamId);
                Notification.show("Bạn đã rời khỏi nhóm thành công").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                // Sau khi rời nhóm thì không còn quyền xem chi tiết sâu nữa, điều hướng về danh sách dự án chính
                getUI().ifPresent(ui -> ui.navigate(ProjectView.class));
            } catch (Exception ex) {
                Notification.show("Lỗi: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        dialog.open();
    }
}