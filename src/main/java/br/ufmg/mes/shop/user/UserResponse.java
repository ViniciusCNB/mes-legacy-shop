package br.ufmg.mes.shop.user;

public class UserResponse {

    private final Long id;
    private final String name;
    private final String email;
    private final UserRole role;

    public UserResponse(AppUser user) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.role = user.getRole();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public UserRole getRole() {
        return role;
    }
}
