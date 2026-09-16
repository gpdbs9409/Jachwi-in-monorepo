package com.capstone.Jachwi_inServerSpring.domain.dto;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class EmailAuthDto {
    private String email;
    private String ePw;

    public String getEmail() {
        return email;
    }

    public String getePw() {
        return ePw;
    }
}