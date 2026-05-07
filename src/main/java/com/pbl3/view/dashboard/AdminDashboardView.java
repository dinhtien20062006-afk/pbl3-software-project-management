package com.pbl3.view.dashboard;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import com.pbl3.view.MainLayout;

@Route(value = "admin/dashboard", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class AdminDashboardView extends VerticalLayout {
    public  AdminDashboardView() {
        add(new H2("Chào mừng Quản trị viên quay trở lại!"));
        add(new Paragraph("Bạn có thể quản lý các dự án và theo dõi tiến độ thành viên tại đây."));
        setPadding(true);
    }
}
