package com.forge.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileRequest {

    private String displayName;
    private String email;
    private String leetcodeUsername;
    private String avatarUrl;
    private Integer targetLevel;
}
