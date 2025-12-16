package com.itdg.common.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DbConnectionRequest {
    @NotBlank(message = "Database URL is required")
    private String url;

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    // Driver class name can be optional (auto-detected) or required. Let's make it
    // optional for now or require it.
    // Given the logic usually requires it or defaults it.
    private String driverClassName;
}
