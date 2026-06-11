    package com.chun.backend.model;

    import jakarta.persistence.*;
    import lombok.Getter;
    import lombok.Setter;

    import java.time.LocalDateTime;

    @Entity
    @Getter
    @Setter
    @Table(name = "users")
    public class User {

        @Id
        @GeneratedValue(strategy= GenerationType.IDENTITY)
        private Long id;

        @Column(nullable = false, unique = true, length = 50)
        private String username;

        @Column(nullable = false, length = 255)
        private String passwordHash;

        @Column(nullable = false)
        private LocalDateTime createdAt;

        @Column(name = "is_deleted", nullable = false)
        private boolean isDeleted;

        @Column
        private LocalDateTime deletedAt;

        @PrePersist
        protected void onCreate() {
            createdAt = LocalDateTime.now();
            isDeleted = false;
        }
    }
