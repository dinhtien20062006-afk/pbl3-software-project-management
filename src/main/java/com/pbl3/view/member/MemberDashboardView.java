package com.pbl3.view.member;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import com.pbl3.view.MainLayout;

@Route(value = "member/dashboard", layout = MainLayout.class)
@RolesAllowed("MEMBER")
public class MemberDashboardView extends VerticalLayout {
    public MemberDashboardView() {
        add(new H2("Chào mừng Thành viên quay trở lại!"));
        add(new Paragraph("Xem các nhiệm vụ và tiến độ cá nhân của bạn tại đây."));
        setPadding(true);
    }
}
