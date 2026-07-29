package com.forge.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String refreshToken;
    private String tokenType;
    private UserInfo user;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private String id;
        private String username;
        private String displayName;
        private String email;
        private String leetcodeUsername;
        private Integer targetLevel;
        private String preferredAnalysisTime;
        private Integer dailyGenerationsUsed;
        private String lastGenerationDate;
    }
}
