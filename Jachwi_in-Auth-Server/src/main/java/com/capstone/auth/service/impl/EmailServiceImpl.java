package com.capstone.auth.service.impl;

import com.capstone.auth.service.EmailService;
import com.capstone.auth.service.RedisUtil;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender emailSender;
    private final RedisUtil redisUtil;

    private String ePw;

    @Override
    public MimeMessage createMessage(String to) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = emailSender.createMimeMessage();
        message.addRecipients(Message.RecipientType.TO, to);
        message.setSubject("자취인 회원가입 이메일 인증");

        String msg = "<div style='margin:auto;background-color:#13b3af;width:600px;padding:40px'>" +
                "<div style='background:#fff;padding:30px'>" +
                "<h2>자취인 이메일 인증</h2>" +
                "<p>아래 인증번호를 입력해주세요. (5분간 유효)</p>" +
                "<h1 style='color:#13b3af;letter-spacing:8px'>" + ePw + "</h1>" +
                "</div></div>";

        message.setText(msg, "utf-8", "html");
        message.setFrom(new InternetAddress("yongwoo1207@naver.com", "Jachwi-in admin"));

        redisUtil.saveAuthCode(to, ePw);
        return message;
    }

    @Override
    public String sendSimpleMessage(String email) throws Exception {
        if (redisUtil.existData(email)) {
            redisUtil.deleteData(email);
        }
        ePw = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
        MimeMessage message = createMessage(email);

        try {
            emailSender.send(message);
        } catch (MailException e) {
            e.printStackTrace();
            throw new IllegalAccessException();
        }
        return ePw;
    }

    public boolean verifyEmailCode(String email, String code) {
        String saved = redisUtil.getData(email);
        if (saved == null) return false;
        return saved.equals(code);
    }
}
