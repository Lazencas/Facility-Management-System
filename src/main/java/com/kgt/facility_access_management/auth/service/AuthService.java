package com.kgt.facility_access_management.auth.service;

import com.kgt.facility_access_management.user.domain.User;
import com.kgt.facility_access_management.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final UserMapper userMapper;

    public AuthService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public Optional<User> login(String loginId, String password) {

        User byLoginId = userMapper.findByLoginId(loginId);
        //해당 아이디가 존재하는지 체크
        if (byLoginId == null) {
            return Optional.empty();
        }

        //비밀번호 체크
        if (!byLoginId.getPasswordHash().equals(password)) {
            return Optional.empty();
        }

        //active 확인
        if (!byLoginId.isActive()) {
            return Optional.empty();
        }

        return Optional.of(byLoginId);
    }


}
