package com.nguyenquyen.vetautet.ddd.domain.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;

    private String socialId;

    private String role;

    private String avatarUrl;

    @Column(insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
