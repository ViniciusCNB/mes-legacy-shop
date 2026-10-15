package br.ufmg.mes.shop.user;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    private final AppUserService users;

    public UserController(AppUserService users) {
        this.users = users;
    }

    @GetMapping("/api/me")
    public UserResponse me(@AuthenticationPrincipal UserDetails user) {
        return users.profile(user.getUsername());
    }
}
