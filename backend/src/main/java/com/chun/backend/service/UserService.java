package com.chun.backend.service;

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
        if(userRepository.findByUsername(username).isPresent()){
            throw new UsernameAlreadyExistsException(username);
        }

        User user = new User();
        user.setUsername(username);
        String hashed = passwordEncoder.encode(password);
        user.setPasswordHash(hashed);

        return userRepository.save(user);
    }


}
