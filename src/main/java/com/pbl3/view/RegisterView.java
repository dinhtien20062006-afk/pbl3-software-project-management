package com.pbl3.view;

import com.pbl3.dto.request.RegisterRequest;
import com.pbl3.service.AuthService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("register")
@PageTitle("Đăng ký | PBL3")
@AnonymousAllowed
public class RegisterView extends VerticalLayout {

    public RegisterView(AuthService authService) {
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setSizeFull();

        TextField username = new TextField("Tên đăng nhập");
        TextField fullName = new TextField("Họ và tên");
        PasswordField password = new PasswordField("Mật khẩu");
        PasswordField confirmPassword = new PasswordField("Xác nhận mật khẩu");

        Button registerButton = new Button("Đăng ký", e -> {
            if (!password.getValue().equals(confirmPassword.getValue())) {
                Notification.show("Mật khẩu không khớp!").addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            try {
                RegisterRequest request = new RegisterRequest();
                request.setUsername(username.getValue());
                request.setFullName(fullName.getValue());
                request.setPassword(password.getValue());

                authService.registerNewUser(request);
                
                Notification.show("Đăng ký thành công!").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                getUI().ifPresent(ui -> ui.navigate("login"));
            } catch (Exception ex) {
                Notification.show(ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        registerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(username, fullName, password, confirmPassword, registerButton);
    }
}