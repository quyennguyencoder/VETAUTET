package com.nguyenquyen.vetautet.ddd.infrastructure.persistence.dataobject;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transaction")
@Data
public class PaymentTransactionDO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", unique = true)
    private String paymentId;

    @Column(name = "order_number")
    private String orderNumber;

    @Column(name = "user_id")
    private Integer userId;

    private BigDecimal amount;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "payment_status")
    private Integer paymentStatus;

    @Column(name = "gateway_transaction_id")
    private String gatewayTransactionId;

    @Column(name = "payment_url")
    private String paymentUrl;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
