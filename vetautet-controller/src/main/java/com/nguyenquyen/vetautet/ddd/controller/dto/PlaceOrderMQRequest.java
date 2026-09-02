package com.nguyenquyen.vetautet.ddd.controller.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PlaceOrderMQRequest {

    @NotNull
    private Long ticketId;

    @Min(1)
    private int quantity;
}