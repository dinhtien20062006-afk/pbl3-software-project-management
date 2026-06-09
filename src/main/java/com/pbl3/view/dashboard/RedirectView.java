package com.pbl3.view.dashboard;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;
import com.pbl3.view.MainLayout;

@Route(value = "", layout = MainLayout.class)
@PermitAll
public class RedirectView extends VerticalLayout implements BeforeEnterObserver {
    private final AuthenticationContext authContext;

    public RedirectView(AuthenticationContext authContext) {
        this.authContext = authContext;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String role = authContext.getAuthenticatedUser(org.springframework.security.core.userdetails.User.class)
                .map(user -> user.getAuthorities().stream().findFirst().get().getAuthority())
                .orElse("");

        if (role.equals("ROLE_ADMIN")) {
            event.forwardTo(AdminDashboardView.class);
        } else if (role.equals("ROLE_PROJECT_MANAGER")) {
            event.forwardTo(ManagerDashboardView.class);
        } else {
            event.forwardTo(MemberDashboardView.class);
        }
    }
}