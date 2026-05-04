package com.pbl3.view;

import com.pbl3.dto.request.UpdateRequest;
import com.pbl3.dto.response.ShowInfoResponse;
import com.pbl3.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;

@Route(value = "profile/edit", layout = MainLayout.class)
@PermitAll
public class EditProfileView extends VerticalLayout {

    public EditProfileView(UserService userService, AuthenticationContext authContext) {
        String currentUsername = authContext.getPrincipalName().orElse("");
        ShowInfoResponse userInfo = userService.getUserInfo(currentUsername);

        TextField userName = new TextField("Tên đăng nhập");
        userName.setValue(userInfo.getUsername());

        TextField phone = new TextField("Số điện thoại");
        phone.setValue(userInfo.getPhoneNumber() != null ? userInfo.getPhoneNumber() : "");

        TextField location = new TextField("Địa chỉ");
        location.setValue(userInfo.getLocation() != null ? userInfo.getLocation() : "");

        TextArea description = new TextArea("Mô tả bản thân");
        description.setValue(userInfo.getDescription() != null ? userInfo.getDescription() : "");

        Button save = new Button("Lưu thay đổi", e -> {
            try {
                UpdateRequest request = UpdateRequest.builder()
                        .username(userName.getValue())
                        .phoneNumber(phone.getValue())
                        .location(location.getValue())
                        .description(description.getValue())
                        .build();
                
                userService.updateCurrentUserProfile(currentUsername, request);
                Notification.show("Cập nhật thành công!");
                getUI().ifPresent(ui -> ui.navigate(ProfileView.class));
            } catch (Exception ex) {
                Notification.show("Lỗi: " + ex.getMessage());
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(userName, phone, location, description, save);
    }
}