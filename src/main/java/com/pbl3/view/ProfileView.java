package com.pbl3.view;

import com.pbl3.dto.response.ShowInfoResponse;
import com.pbl3.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;

@Route(value = "profile", layout = MainLayout.class)
@PageTitle("Thông tin cá nhân")
@PermitAll
public class ProfileView extends VerticalLayout {

    public ProfileView(UserService userService, AuthenticationContext authContext) {
        String username = authContext.getPrincipalName().orElse("");
        ShowInfoResponse userInfo = userService.getUserInfo(username);

        H2 header = new H2("Hồ sơ của tôi");
        
        FormLayout formLayout = new FormLayout();
        formLayout.addFormItem(new Span(userInfo.getFullName()), "Họ và tên:");
        formLayout.addFormItem(new Span(userInfo.getUsername()), "Tên đăng nhập:");
        formLayout.addFormItem(new Span(userInfo.getRole()), "Quyền hạn:");
        formLayout.addFormItem(new Span(userInfo.getPhoneNumber()), "Số điện thoại:");
        formLayout.addFormItem(new Span(userInfo.getLocation()), "Địa chỉ:");
        formLayout.addFormItem(new Span(userInfo.getDescription()), "Mô tả bản thân:");

        Button editBtn = new Button("Chỉnh sửa thông tin", VaadinIcon.EDIT.create(), 
            e -> getUI().ifPresent(ui -> ui.navigate(EditProfileView.class)));
        editBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(header, formLayout, editBtn);
        setPadding(true);
    }
}