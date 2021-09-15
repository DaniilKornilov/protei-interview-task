package com.protei.task.systemuser;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemUserRepository extends JpaRepository<SystemUser, Long> {
    Optional<SystemUser> findSystemUserById(Long id);

    Optional<SystemUser> findSystemUserByEmail(String email);

    Optional<SystemUser> findSystemUserByPhoneNumber(String phoneNumber);
}
