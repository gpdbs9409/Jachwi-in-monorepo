package com.capstone.auth.domain.dto;

import com.capstone.auth.domain.User;
import lombok.Getter;

@Getter
public class UserInfoDto {
    private final Long id;
    private final String email;
    private final String name;
    private final String nickname;
    private final String school;

    public UserInfoDto(User user) {
        this.id       = user.getId();
        this.email    = user.getEmail();
        this.name     = user.getName();
        this.nickname = user.getNickname();
        this.school   = user.getSchool();
    }
}
