package com.kgt.facility_access_management;

import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MyBatisConnectionTest {

    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    @Test
    void testConnection() throws Exception {

        try (var sqlSession = sqlSessionFactory.openSession()) {

            Connection connection = sqlSession.getConnection();

            assertThat(connection).isNotNull();
            assertThat(connection.isValid(1)).isTrue();
        }
    }


}
