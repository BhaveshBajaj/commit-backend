package com.commit.commit.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.commit.commit.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findAllByIdIn(List<Long> ids);
    Optional<User> findByEmail(String email);
    Optional<User> findByFirebaseUid(String firebaseUid);
}
