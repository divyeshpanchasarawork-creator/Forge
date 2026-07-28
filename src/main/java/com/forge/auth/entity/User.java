package com.forge.auth.entity;

import com.forge.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column
    private String password;

    @Column(length = 100)
    private String email;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "leetcode_username", length = 50)
    private String leetcodeUsername;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;
}
