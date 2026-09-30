package com.kgt.facility_access_management.auth;

import com.kgt.facility_access_management.user.domain.User;
import com.kgt.facility_access_management.user.domain.UserRole;
import com.kgt.facility_access_management.user.mapper.UserMapper;
import org.assertj.core.api.Assert;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class UserMapperTest {

    @Autowired
    UserMapper userMapper;

    @Test
    void findByLoginIdUSER() {
        //given
        String loginId = "user3";

        //when
        User user3 = userMapper.findByLoginId(loginId);

        //then
        assertThat(loginId).isEqualTo(user3.getLoginId());
        assertThat(UserRole.USER).isEqualTo(user3.getRole());
    }

    @Test
    void findByLoginIdADMIN() {
        //given
        String loginId = "admin2";

        //when
        User admin2 = userMapper.findByLoginId(loginId);

        //then
        assertThat(loginId).isEqualTo(admin2.getLoginId());
        assertThat(UserRole.ADMIN).isEqualTo(admin2.getRole());
    }


}
