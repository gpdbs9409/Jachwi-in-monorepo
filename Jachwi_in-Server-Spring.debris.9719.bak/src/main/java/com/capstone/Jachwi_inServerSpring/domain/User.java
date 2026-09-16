package com.capstone.Jachwi_inServerSpring.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.transaction.annotation.Transactional;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Transactional
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String nickname;

    @Column
    private String school;

    @Builder
    /*빌더 패턴으로 객체를 생성할 수 있도록 합니다.
    빌더 패턴은 일반적으로 setter나 객체 생성 시 값을 넣어주는 것과 다르게 .userName("name") 같은 방식으로
    값을 넣을 수 있습니다. 생성자로 객체를 생성하면 순서를 맞춰야하고,
    setter는 권장하지 않기 때문에 빌더 패턴을 사용하면 좋습니다.*/
    public User(Long id, String email, String password,
                String name, String nickname, String school){
        this.id = id;
        this.email = email;
        this.password = password;
        this.name = name;
        this.nickname = nickname;
        this.school = school;
    }
}

