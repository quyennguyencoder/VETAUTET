package com.nguyenquyen.vetautet.ddd.infrastructure.mq;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CancelOrderMQMessage {
    private String orderNumber;
    private String yearMonth;
    private Integer ticketId;
    private Integer quantity;
    private Long timestamp;
}