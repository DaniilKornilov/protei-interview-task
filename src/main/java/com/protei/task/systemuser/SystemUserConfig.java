package com.protei.task.systemuser;

import com.protei.task.systemuser.enumeration.SystemUserStatus;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SystemUserConfig {
    @Bean
    CommandLineRunner commandLineRunner(SystemUserRepository repository) {
        return args -> {
            SystemUser alex = new SystemUser(
                    "Alex",
                    "alex@yandex.ru",
                    "+7 903 333 33 33",
                    SystemUserStatus.OFFLINE
            );
            SystemUser maria = new SystemUser(
                    "Maria",
                    "maria@gmail.com",
                    "+7 903 333 33 34",
                    SystemUserStatus.OFFLINE
            );
            repository.saveAll(List.of(alex, maria));
        };
    }
}
