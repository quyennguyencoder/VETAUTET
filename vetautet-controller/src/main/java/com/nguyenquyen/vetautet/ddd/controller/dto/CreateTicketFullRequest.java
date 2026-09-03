package com.nguyenquyen.vetautet.ddd.controller.dto;

import jakarta.validation.Valid;
import lombok.Data;

@Data
public class CreateTicketFullRequest {
    @Valid
    private CreateTicketRequest ticket;

    @Valid
    private CreateTicketDetailRequest detail;
}
