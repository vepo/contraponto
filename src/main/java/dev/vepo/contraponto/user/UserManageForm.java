package dev.vepo.contraponto.user;

import java.util.List;

import jakarta.ws.rs.FormParam;

public class UserManageForm {

    @FormParam("id")
    private Long id;

    @FormParam("username")
    private String username;

    @FormParam("name")
    private String name;

    @FormParam("email")
    private String email;

    @FormParam("password")
    private String password;

    @FormParam("newPassword")
    private String newPassword;

    @FormParam("active")
    private String active;

    @FormParam("roles")
    private List<String> roles;

    public String getEmail() {
        return email;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public String getPassword() {
        return password;
    }

    public List<String> getRoles() {
        return roles;
    }

    public String getUsername() {
        return username;
    }

    public boolean isActive() {
        return active != null;
    }
}
