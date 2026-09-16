//
//package com.capstone.Jachwi_inServerSpring.config;
//
//import com.capstone.Jachwi_inServerSpring.repository.UserRepository;
//import com.capstone.Jachwi_inServerSpring.repository.UserRepositoryImpl;
//import com.capstone.Jachwi_inServerSpring.service.UserService;
//import jakarta.persistence.EntityManager;
//import jakarta.persistence.EntityManagerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//
//@Configuration
//public class UserConfig {
//
///*  entityManager 객체 em이 repository에 쓰임.
//      entityManager는 bean 객체가 아니기에 여기서 객체 만들어서 연결 해주고
//      @Autowired 해줘야 한다.
//*/
//
//    //UserConfig에 entityManager 주입하기.
//
//    private final EntityManager em;
//    @Autowired
//    public UserConfig(EntityManager em){
//        this.em = em;
//    }
//
//    @Bean
//    public EntityManager entityManager(EntityManagerFactory entityManagerFactory) {
//        return entityManagerFactory.createEntityManager();
//    }
//
//    @Bean
//    public UserService userService(){
//        return new UserService(userRepository());
//    }
//
//    @Bean
//    public UserRepository userRepository(){
//        return new UserRepositoryImpl(em);
//    }
//
//}
