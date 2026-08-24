package com.talkweb.security;

import com.talkweb.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.Principal;
import java.util.Collection;
import java.util.Collections;

@Getter
@Builder
@AllArgsConstructor
public class UserPrincipal implements UserDetails, Principal {

    private final Long id;
    private final String username;
    private final String password;
    private final String nickname;
    private final boolean admin;
    private final boolean mustChangePassword;
    private final String status;
    private final Collection<? extends GrantedAuthority> authorities;

    public static UserPrincipal create(User user) {
        String role = Boolean.TRUE.equals(user.getIsAdmin()) ? "ROLE_ADMIN" : "ROLE_USER";
        return UserPrincipal.builder()
                .id(user.getId())
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .nickname(user.getNickname())
                .admin(Boolean.TRUE.equals(user.getIsAdmin()))
                .mustChangePassword(Boolean.TRUE.equals(user.getMustChangePassword()))
                .status(user.getStatus())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority(role)))
                .build();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !"DISABLED".equalsIgnoreCase(status);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "ACTIVE".equalsIgnoreCase(status);
    }

    @Override
    public String getName() {
        return username;
    }
}

