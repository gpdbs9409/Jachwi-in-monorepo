package com.capstone.Jachwi_inServerSpring.domain.dto;

import lombok.*;

@Data
@NoArgsConstructor //기본생성자를 자동으로 생성.
@AllArgsConstructor // 필드를 매개변수로 하는 생성자를 만들어준다.
@ToString // DTO 객체가 가지고 있는 필드값을 출력할때, string 타입으로 자동으로 만들어준다.

public class UserJoinDto {

    private String email;
    private String name;
    private String nickname;
    private String password;
    private String school;

}
