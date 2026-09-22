package com.nguyenquyen.vetautet.ddd.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleLoginRequest {
    @NotBlank
    private String idToken;
}
