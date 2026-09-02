package com.nguyenquyen.vetautet.ddd.controller.dto;

import jakarta.validation.constraints.*;
import lombok.Data;


/**
 * DTO (Data Transfer Object) cho yêu cầu nhập kho.
 * Lớp này được dùng để truyền dữ liệu từ request body của API vào hệ thống.
 * Nó chứa các validation cơ bản để đảm bảo dữ liệu đầu vào hợp lệ trước khi xử lý.
 */
@Data
public class InventorBucketRequest {

    /**
     * ID của sản phẩm (SKU). Bắt buộc.
     */
    @NotBlank(message = "skuId cannot be blank")
    private String skuId;

    /**
     * ID của người bán. Bắt buộc.
     */
    @NotBlank(message = "sellerId cannot be blank")
    private String sellerId;

    /**
     * Số lượng tồn kho cần nhập. Bắt buộc và phải lớn hơn 0.
     */
    @NotNull(message = "inventoryNum cannot be null")
    @Min(value = 1, message = "inventoryNum must be greater than 0")
    private Integer inventoryNum;

    /**
     * Mã nghiệp vụ duy nhất cho lần nhập kho này (Idempotency Key).
     * Sẽ được gán tự động trong Controller nếu client không cung cấp.
     */
    private String inventorCode;

    /**
     * ID của mẫu cấu hình phân桶, không bắt buộc.
     * Nếu không được cung cấp, hệ thống sẽ sử dụng mẫu mặc định.
     */
    private Long templateId;
}