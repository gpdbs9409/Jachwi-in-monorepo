package com.capstone.Jachwi_inServerSpring.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;

@RequiredArgsConstructor
@Service //util도 service의 종류로, 좀더 일반적인 service를 util이라 생각하면 편하다.
public class RedisUtil {
    private final StringRedisTemplate template;
/*    Spring Data Redis에서 Redis의 String 데이터 타입을 다루기 위해 사용되는 클래스
    이를 통해 Redis와 상호작용하여 String 데이터를 다룰 수 있습니다.
    이를 활용하여 간단하게 Redis의 String 데이터를 조회, 저장, 수정, 삭제하는 작업을 수행할 수 있습니다.*/

    //해당 생성자 안쓰려면 @RequiredArgsConstructor를 써주면 된다.
/*    public RedisUtil(StringRedisTemplate template){
        this.template = template;
    }*/

    //ValueOperations는 redis에서 키에 대한 값을 읽거나 쓰는 기능을 제공하는 메서드를 가진다.
    //key값의 value를 리턴해준다. 즉, 이메일의 code값을 알려준다.
    public String getData(String email) {
        ValueOperations<String, String> valueOperations = template.opsForValue();
        return valueOperations.get(email);
    }

    //중복확인 기능인데, 이거는 레디스에서 하지 말고 db연결해서 db에서 찾아야 한다.
    public boolean existData(String eamil) {
        return template.hasKey(eamil);
    }


    public void deleteData(String email) {
        template.delete(email);
    }


    //인증번호 저장
    //Duration는 자바에서 제공하는 시간을 나타내는 객체이다.
    public void saveAuthCode(String email, String ePw){
        ValueOperations<String, String> valueOperations = template.opsForValue();  //인증번호 담을 객체 생성.
        valueOperations.set(email, ePw, Duration.ofMinutes(5));  //인증번호 저장, 만료시간 5분.
    }
}
