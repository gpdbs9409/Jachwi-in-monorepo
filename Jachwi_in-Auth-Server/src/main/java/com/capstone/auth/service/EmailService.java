package com.capstone.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.io.UnsupportedEncodingException;

public interface EmailService {
    MimeMessage createMessage(String to) throws MessagingException, UnsupportedEncodingException;
    String sendSimpleMessage(String to) throws Exception;
}
