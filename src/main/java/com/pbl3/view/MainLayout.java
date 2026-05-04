package com.pbl3.view;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.springframework.security.core.GrantedAuthority;

import com.pbl3.view.admin.*;
import com.pbl3.view.manager.*;
import com.pbl3.view.member.*;

@PageTitle("Hệ thống quản lý dự án PBL3")
public class MainLayout extends AppLayout {

    private final AuthenticationContext authContext;

    public MainLayout(AuthenticationContext authContext) {
        this.authContext = authContext;
        
        createHeader();
        createDrawer();
        createFooter();
    }

    private void createHeader() {
        H1 logo = new H1("PBL3 MANAGEMENT");
        logo.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.MEDIUM);

        // Nút Logout
        Button logout = new Button("Đăng xuất", VaadinIcon.SIGN_OUT.create(), e -> authContext.logout());
        logout.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);

        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), logo, logout);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(logo); // Đẩy nút logout sang bên phải
        header.setWidthFull();
        header.addClassNames(LumoUtility.Padding.Vertical.NONE, LumoUtility.Padding.Horizontal.MEDIUM);

        addToNavbar(header);
    }

    private void createDrawer() {
        VerticalLayout nav = new VerticalLayout();
        nav.setSpacing(false);
        nav.setPadding(false);

        // Lấy role của user hiện tại
        String role = authContext.getAuthenticatedUser(org.springframework.security.core.userdetails.User.class)
                .map(user -> user.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .findFirst().orElse(""))
                .orElse("");

        // Render Menu dựa trên Role
        if (role.equals("ROLE_ADMIN")) {
            addAdminMenu(nav);
        } else if (role.equals("ROLE_PROJECT_MANAGER")) {
            addManagerMenu(nav);
        } else {
            addMemberMenu(nav);
        }

        addToDrawer(new Scroller(nav));
    }


    private void addAdminMenu(VerticalLayout nav) {
        nav.add(new H4("ADMIN PANEL"));
        nav.add(createNavItem("Tổng quan", VaadinIcon.DASHBOARD, AdminDashboardView.class));
        nav.add(createNavItem("Quản lý tài khoản", VaadinIcon.USERS, UserManagementView.class));
        nav.add(createNavItem("Thông tin cá nhân", VaadinIcon.USER_CARD, ProfileView.class));
        nav.add(createNavItem("Dự án hệ thống", VaadinIcon.RECORDS, ProjectView.class));
        nav.add(createNavItem("Lịch sử hệ thống", VaadinIcon.TIME_BACKWARD, AuditLogView.class));
    }

    private void addManagerMenu(VerticalLayout nav) {
        nav.add(new H4("PROJECT MANAGER"));
        nav.add(createNavItem("Tổng quan", VaadinIcon.DASHBOARD, ManagerDashboardView.class));
        nav.add(createNavItem("Thông tin cá nhân", VaadinIcon.USER_CARD, ProfileView.class));
        nav.add(createNavItem("Quản lý dự án", VaadinIcon.GROUP, ProjectView.class));
    }

    private void addMemberMenu(VerticalLayout nav) {
        nav.add(new H4("MEMBER PANEL"));
        nav.add(createNavItem("Tổng quan", VaadinIcon.DASHBOARD, MemberDashboardView.class));
        nav.add(createNavItem("Thông tin cá nhân", VaadinIcon.USER_CARD, ProfileView.class));
        nav.add(createNavItem("Dự án tham gia", VaadinIcon.WORKPLACE, ProjectView.class));
    }

    private RouterLink createNavItem(String label, VaadinIcon icon, Class viewClass) {
        RouterLink link = new RouterLink();
        link.setRoute(viewClass);
        HorizontalLayout layout = new HorizontalLayout(icon.create(), new Span(label));
        layout.setSpacing(true);
        layout.addClassNames(LumoUtility.Padding.SMALL, LumoUtility.Margin.Horizontal.MEDIUM);
        link.add(layout);
        link.addClassNames(LumoUtility.Display.BLOCK, LumoUtility.TextColor.BODY);
        link.getStyle().set("text-decoration", "none");
        
        return link;
    }

    private void createFooter() {
        Footer footer = new Footer(new Span("2026 PBL3 Project Management System. All rights reserved."));
        footer.addClassNames(LumoUtility.Display.FLEX, LumoUtility.JustifyContent.CENTER, 
                           LumoUtility.Padding.MEDIUM, LumoUtility.Background.CONTRAST_5);
        addToDrawer(footer); // Hoặc add vào dưới cùng trang
    }
}