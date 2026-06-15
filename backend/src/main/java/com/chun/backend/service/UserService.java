package com.chun.backend.service;

import com.chun.backend.exception.AuthenticationFailedException;
import com.chun.backend.exception.UsernameAlreadyExistsException;
import com.chun.backend.model.User;
import com.chun.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User register(String username, String password) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new UsernameAlreadyExistsException(username);
        }

        User user = new User();
        user.setUsername(username);
        String hashed = passwordEncoder.encode(password);
        user.setPasswordHash(hashed);

        return userRepository.save(user);
    }

    public User login(String username, String password) {

        // 1. 用 userRepository 查 username，查無就丟例外
        User user = userRepository.findByUsername(username)
                .orElseThrow(AuthenticationFailedException::new);

        // 2. 用 passwordEncoder.matches(password, user.getPasswordHash()) 驗密碼，不符就丟例外
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new AuthenticationFailedException();
        }


        // 3. 回傳 user
        return user;
    }


}
