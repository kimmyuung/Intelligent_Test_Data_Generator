package com.itdg.analyzer.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DbConnectionRequest {
    private String url;
    private String username;
    private String password;
    private String driverClassName;
}
