package dev.vepo.passport.model;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_users_reset_password_token")
public class ResetPasswordToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Link to User entity
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String token;

    @Column(name = "encoded_password", nullable = false)
    private String encodedPassword;

    private boolean used;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    // Constructors
    public ResetPasswordToken() {}

    public ResetPasswordToken(String token, String encodedPassword, User user) {
        this(null, token, encodedPassword, user, false, Instant.now());
    }

    public ResetPasswordToken(Long id, String token, String encodedPassword, User user, boolean used, Instant requestedAt) {
        this.id = id;
        this.token = Objects.requireNonNull(token, "token is required!");
        this.encodedPassword = Objects.requireNonNull(encodedPassword, "encodedPassword is required!");
        this.user = Objects.requireNonNull(user, "user is required!");
        this.requestedAt = requestedAt;
        this.used = used;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getEncodedPassword() {
        return encodedPassword;
    }

    public void setEncodedPassword(String encodedPassword) {
        this.encodedPassword = encodedPassword;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}