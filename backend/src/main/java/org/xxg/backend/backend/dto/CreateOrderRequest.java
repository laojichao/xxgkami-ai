package org.xxg.backend.backend.dto;

import lombok.Data;

@Data
public class CreateOrderRequest {
    private Integer userId;
    private String username;
    private String cardType;
    private String cardSpec;
    private Integer quantity = 1;
    private String paymentMethod;
    private String email;
    private Integer pricingId;
}
