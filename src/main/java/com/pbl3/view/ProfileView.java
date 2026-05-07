package com.pbl3.view;

import com.pbl3.dto.request.UpdateRequest;
import com.pbl3.dto.response.ShowInfoResponse;
import com.pbl3.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;

@Route(value = "profile", layout = MainLayout.class)
@PageTitle("Thông tin cá nhân")
@PermitAll
public class ProfileView extends VerticalLayout {

    private final UserService userService;
    private final String currentUsername;

    // Components hiển thị ngoài màn hình chính
    private final Span lblFullName = new Span();
    private final Span lblUsername = new Span();
    private final Span lblRole = new Span();
    private final Span lblPhone = new Span();
    private final Span lblLocation = new Span();
    private final Span lblDescription = new Span();

    public ProfileView(UserService userService, AuthenticationContext authContext) {
        this.userService = userService;
        this.currentUsername = authContext.getPrincipalName().orElse("");

        setAlignItems(Alignment.CENTER);
        setPadding(true);

        // Header
        H2 header = new H2("Hồ sơ cá nhân");
        
        // Layout thông tin chi tiết
        FormLayout infoForm = new FormLayout();
        infoForm.addFormItem(lblFullName, "Họ và tên:");
        infoForm.addFormItem(lblUsername, "Tên đăng nhập:");
        infoForm.addFormItem(lblRole, "Quyền hạn:");
        infoForm.addFormItem(lblPhone, "Số điện thoại:");
        infoForm.addFormItem(lblLocation, "Địa chỉ:");
        infoForm.addFormItem(lblDescription, "Mô tả:");
        infoForm.setMaxWidth("600px");

        // Nút mở Dialog
        Button openDialogBtn = new Button("Chỉnh sửa thông tin", VaadinIcon.EDIT.create(), 
                e -> openEditDialog());
        openDialogBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(header, infoForm, openDialogBtn);
        
        refreshData();
    }

    private void refreshData() {
        ShowInfoResponse userInfo = userService.getUserInfo(currentUsername);
        lblFullName.setText(userInfo.getFullName());
        lblUsername.setText(userInfo.getUsername());
        lblRole.setText(userInfo.getRole());
        lblPhone.setText(userInfo.getPhoneNumber() != null ? userInfo.getPhoneNumber() : "---");
        lblLocation.setText(userInfo.getLocation() != null ? userInfo.getLocation() : "---");
        lblDescription.setText(userInfo.getDescription() != null ? userInfo.getDescription() : "---");
    }

    private void openEditDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Cập nhật thông tin");

        // Lấy dữ liệu hiện tại để fill vào form
        ShowInfoResponse userInfo = userService.getUserInfo(currentUsername);

        TextField txtUsername = new TextField("Tên đăng nhập");
        txtUsername.setValue(userInfo.getUsername());
        txtUsername.setWidthFull();

        TextField txtPhone = new TextField("Số điện thoại");
        txtPhone.setValue(userInfo.getPhoneNumber() != null ? userInfo.getPhoneNumber() : "");
        txtPhone.setWidthFull();

        TextField txtLocation = new TextField("Địa chỉ");
        txtLocation.setValue(userInfo.getLocation() != null ? userInfo.getLocation() : "");
        txtLocation.setWidthFull();

        TextArea txtDescription = new TextArea("Mô tả bản thân");
        txtDescription.setValue(userInfo.getDescription() != null ? userInfo.getDescription() : "");
        txtDescription.setWidthFull();

        VerticalLayout dialogLayout = new VerticalLayout(txtUsername, txtPhone, txtLocation, txtDescription);
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(true);
        dialog.add(dialogLayout);

        // Buttons trong Footer của Dialog
        Button saveBtn = new Button("Lưu", e -> {
            try {
                UpdateRequest request = UpdateRequest.builder()
                        .username(txtUsername.getValue())
                        .phoneNumber(txtPhone.getValue())
                        .location(txtLocation.getValue())
                        .description(txtDescription.getValue())
                        .build();

                userService.updateCurrentUserProfile(currentUsername, request);
                
                Notification.show("Cập nhật thành công!")
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                
                refreshData(); // Cập nhật lại UI chính
                dialog.close();
            } catch (Exception ex) {
                Notification.show("Lỗi: " + ex.getMessage())
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Hủy", e -> dialog.close());

        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }
}