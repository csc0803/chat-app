package com.chun.backend.repository;

import com.chun.backend.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TestEntityManager em;

    @Test
    public void findByUsername_pass() {

        User user = new User();
        user.setUsername("user1");
        user.setPasswordHash("password1");

        em.persist(user);
        em.flush();

        Optional<User> result = userRepository.findByUsername("user1");

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("user1");

    }

    @Test
    public void findByUsername_fail() {

        Optional<User> result = userRepository.findByUsername("nobody");

        assertThat(result).isEmpty();

    }
}
