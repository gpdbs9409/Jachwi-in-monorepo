package com.capstone.auth.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserJoinDto {
    private String email;
    private String name;
    private String nickname;
    private String password;
    private String school;
}
