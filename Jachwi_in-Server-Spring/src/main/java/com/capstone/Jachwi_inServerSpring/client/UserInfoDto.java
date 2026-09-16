package com.capstone.Jachwi_inServerSpring.client;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserInfoDto {
    private Long id;
    private String email;
    private String name;
    private String nickname;
    private String school;
}
