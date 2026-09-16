//package com.capstone.Jachwi_inServerSpring.repository;
//
//import com.capstone.Jachwi_inServerSpring.domain.User;
//import jakarta.persistence.EntityManager;
//import jakarta.persistence.NoResultException;
//import jakarta.persistence.TypedQuery;
//import jakarta.transaction.Transactional;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//import java.util.Optional;
//
//이건 JPA로 구현하는 법
//@Transactional
//@Repository
//public class UserRepositoryImpl implements UserRepository{
//    private final EntityManager em; //JPA는 entityManager로 작동한다.
//    public UserRepositoryImpl(EntityManager em){
//        this.em = em;
//    }
//
//    @Override //저장 기능
//    public User save(User user) {
//        em.persist(user);
//        return user;
//    }
//
//    @Override  //PK같은 경우는 이런 식으로 할 수 있다. 다른 값은 유일값이더라도 이걸로 안된다.
//    public Optional<User> findById(Long id) {
//        User user = em.find(User.class, id);
//        return Optional.ofNullable(user);
//    }
//
//    @Override
//    public Optional<User> findByNickname(String nickname){
//        User user = em.find(User.class, nickname);
//        return Optional.ofNullable(user);
//    }
//
//    @Override
//    public Optional<User> findByEmail(String email){
//
//        TypedQuery<User> query = em.createNamedQuery("select m from user table m where m.email=:email", User.class);
//        query.setParameter("email", email);
//        try {
//            return Optional.ofNullable(query.getSingleResult());
//        } catch (NoResultException e) {
//            return Optional.empty();
//        }
////        User user = em.find(User.class, email);
////        return Optional.ofNullable(user);
//    }
//
//    @Override
//    public Optional<User> findByName(String userName) {
//        List<User> result = em.createQuery("select m from User m where m.name=:name", User.class)
//                .setParameter("name", userName)
//                .getResultList();
//        return result.stream().findAny();
//    }
//
//    @Override
//    public List<User> findAll() {
//        return em.createQuery("select u from User u", User.class)
//                .getResultList();
//    }
//
//    public void clearStore(){
//        em.clear();
//    }
//}
