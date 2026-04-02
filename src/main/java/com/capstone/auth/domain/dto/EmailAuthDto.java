package com.capstone.auth.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EmailAuthDto {
    private String email;
    private String ePw;
}
