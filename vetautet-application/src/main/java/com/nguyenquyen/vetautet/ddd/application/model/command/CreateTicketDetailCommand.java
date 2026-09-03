package com.nguyenquyen.vetautet.ddd.application.model.command;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateTicketDetailCommand {

    private String name;
    private String description;

    private Integer stockInitial;
    private Integer stockAvailable;

    private BigDecimal priceOriginal;
    private BigDecimal priceFlash;

    private LocalDateTime saleStartTime;
    private LocalDateTime saleEndTime;

    private Boolean stockPrepared;
}
