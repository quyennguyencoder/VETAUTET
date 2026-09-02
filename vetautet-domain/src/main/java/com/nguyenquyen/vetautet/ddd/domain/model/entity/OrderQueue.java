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
@Table(name = "order_queue")
public class OrderQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 64)
    private String token;

    private int ticketId;
    private int quantity;
    private int userId;

    // 0=PENDING, 1=SUCCESS, 2=FAILED
    private int status;

    private String orderNumber;
    private String message;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}