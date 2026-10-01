package com.kgt.facility_access_management.access;

import com.kgt.facility_access_management.access.domain.AccessRequest;
import com.kgt.facility_access_management.access.mapper.AccessRequestMapper;
import com.kgt.facility_access_management.user.domain.User;
import com.kgt.facility_access_management.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static com.kgt.facility_access_management.access.domain.AccessRequestStatus.PENDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class AccessHttpIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired AccessRequestMapper requests;
    MockMvc mvc;

    @BeforeEach void setUp() { mvc = MockMvcBuilders.webAppContextSetup(context).build(); }

    private MockHttpSession session(long id, UserRole role) {
        User user = new User(); user.setId(id); user.setRole(role); user.setActive(true);
        MockHttpSession session = new MockHttpSession(); session.setAttribute("LOGIN_USER", user);
        return session;
    }

    @ParameterizedTest
    @ValueSource(strings = {"/facilities", "/admin/access-requests", "/admin/access-logs"})
    void anonymousRequestsAreBlockedByActualInterceptorRegistration(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/admin/access-requests", "/admin/access-logs"})
    void ordinaryUsersCannotReadAdministrativeData(String path) throws Exception {
        mvc.perform(get(path).session(session(1L, UserRole.USER))).andExpect(status().isForbidden());
    }

    @Test void ordinaryUserCannotApproveByCallingUrlDirectly() throws Exception {
        mvc.perform(post("/admin/access-requests/999/approve").session(session(1L, UserRole.USER)))
                .andExpect(status().isForbidden());
    }

    @Test void applicantComesFromSessionEvenWhenBodyContainsAnotherUserId() throws Exception {
        mvc.perform(post("/access-requests").session(session(1L, UserRole.USER))
                .contentType("application/json").content("""
                    {"userId":3,"facilityId":1,"requestReason":"Session ownership test",
                     "accessStartAt":"2030-01-01T09:00:00","accessEndAt":"2030-01-01T18:00:00"}
                    """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));
        assertThat(requests.findByUserId(1L)).anySatisfy(request ->
                assertThat(request.getRequestReason()).isEqualTo("Session ownership test"));
        assertThat(requests.findByUserId(3L)).noneSatisfy(request ->
                assertThat(request.getRequestReason()).isEqualTo("Session ownership test"));
    }

    @Test void adminRoleDoesNotPermitSelfApproval() throws Exception {
        AccessRequest request = new AccessRequest();
        request.setUserId(2L); request.setFacilityId(1L); request.setStatus(PENDING);
        request.setAccessStartAt(LocalDateTime.of(2030, 1, 1, 9, 0));
        request.setAccessEndAt(LocalDateTime.of(2030, 1, 1, 18, 0));
        requests.save(request);
        mvc.perform(post("/admin/access-requests/{id}/approve", request.getId())
                .session(session(2L, UserRole.ADMIN)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("본인의 접근요청은 직접 승인할 수 없습니다."));
        assertThat(requests.findById(request.getId()).getStatus()).isEqualTo(PENDING);
    }
}
