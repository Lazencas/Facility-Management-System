package com.kgt.facility_access_management.auth;

import com.kgt.facility_access_management.auth.service.AuthService;
import com.kgt.facility_access_management.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AuthServiceTest {

    @Autowired
    AuthService authService;

    @Test
    void 정상로그인성공() {
        String loginId = "user1";
        String password = "1234";
        Optional<User> loginUser = authService.login(loginId, password);
        assertThat(loginUser).isPresent();
        assertThat(loginUser.get().getLoginId()).isEqualTo(loginId);
    }

    @Test
    void 없는ID실패() {
        String loginId = "userss";
        String password = "1234";
        Optional<User> loginUser = authService.login(loginId, password);
        assertThat(loginUser).isEmpty();
    }

    @Test
    void 비밀번호불일치실패(){
        String loginId = "user1";
        String password = "12345";
        Optional<User> loginUser = authService.login(loginId, password);
        assertThat(loginUser).isEmpty();
    }

    @Test
    void 비활성화사용자실패() {
        String loginId = "user4";
        String password = "1234";
        Optional<User> loginUser = authService.login(loginId, password);
        assertThat(loginUser).isEmpty();
    }


}
