package com.pbl3.view;

import com.pbl3.dto.response.ShowInfoResponse;
import com.pbl3.entity.User;
import com.pbl3.service.UserService;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "admin/users", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class UserManagementView extends VerticalLayout {

    private final Grid<ShowInfoResponse> grid = new Grid<>(ShowInfoResponse.class, false);

    public UserManagementView(UserService userService) {
        setSizeFull();
        add(new H2("Quản lý tài khoản người dùng"));

        grid.addColumn(ShowInfoResponse::getUsername).setHeader("Tên đăng nhập").setAutoWidth(true);
        grid.addColumn(ShowInfoResponse::getFullName).setHeader("Họ tên").setAutoWidth(true);
        grid.addColumn(ShowInfoResponse::getPhoneNumber).setHeader("SĐT").setAutoWidth(true);
        
        // Cột thay đổi quyền hạn
        grid.addComponentColumn(userDto -> {
            ComboBox<User.Role> roleBox = new ComboBox<>();
            roleBox.setItems(User.Role.values());
            roleBox.setValue(User.Role.valueOf(userDto.getRole()));
            
            roleBox.addValueChangeListener(event -> {
                try {
            
                    Long userId = userService.findByUsername(userDto.getUsername()).getId();
                    userService.updateUserRole(userId, event.getValue());
                    Notification.show("Cập nhật quyền thành công")
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } catch (Exception ex) {
                    roleBox.setValue(event.getOldValue()); // Reset nếu lỗi
                    Notification.show(ex.getMessage())
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });
            return roleBox;
        }).setHeader("Quyền hạn (Edit)");

        grid.setItems(userService.getAllUsers());
        add(grid);
    }
}