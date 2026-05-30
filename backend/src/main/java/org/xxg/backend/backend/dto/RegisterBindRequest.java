package org.xxg.backend.backend.dto;

import lombok.Data;

@Data
public class RegisterBindRequest {
    private Integer userId;
    private String token;
}
