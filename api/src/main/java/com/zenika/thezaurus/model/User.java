package com.zenika.thezaurus.model;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class User {

    private String name;
    private String email;
    private String role; // "admin" or "membre"

    public User(String email, String role) {
        this.email = email;
        this.role = role;
    }

    @Builder
    private User(String name, String email, String role) {
        this.name = name;
        this.email = email;
        this.role = role;
    }
}
