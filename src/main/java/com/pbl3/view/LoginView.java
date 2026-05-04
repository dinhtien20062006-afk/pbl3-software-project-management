package com.pbl3.view;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("login")
@PageTitle("Đăng nhập hệ thống quản lý dự án")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm login = new LoginForm();

    public LoginView() {
        addClassName("login-view");
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        // Cấu hình action của form để Spring Security bắt được (mặc định là /login)
        login.setAction("login"); 

        add(new H1("Hệ thống PBL3"), login, new RouterLink("Chưa có tài khoản? Đăng ký ngay", RegisterView.class));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        // Thông báo lỗi nếu đăng nhập thất bại (URL có query parameter 'error')
        if (beforeEnterEvent.getLocation()
                .getQueryParameters()
                .getParameters()
                .containsKey("error")) {
            login.setError(true);
        }
    }
}