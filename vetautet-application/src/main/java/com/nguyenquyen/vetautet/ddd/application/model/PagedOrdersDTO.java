package com.nguyenquyen.vetautet.ddd.application.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PagedOrdersDTO {
    private List<OrderDTO> items;
    private Long nextCursor;
    private boolean hasMore;
}
