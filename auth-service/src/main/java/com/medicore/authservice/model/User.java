package com.medicore.authservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * A user who can log in to MediCore.
 * <p>
 * Stored in the {@code users} table ("user" is a reserved word in PostgreSQL).
 * The password is always a BCrypt hash, never the plain-text password.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotBlank
    @Email
    @Size(max = 255)
    @Column(nullable = false, unique = true)
    private String email;

    /** BCrypt hash of the password (e.g. "$2a$10$..."), never plain text. */
    @NotBlank
    @Column(nullable = false)
    private String password;

    /** Authorization role, e.g. "ADMIN" or "USER". Added to the JWT as a claim. */
    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String role;

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
