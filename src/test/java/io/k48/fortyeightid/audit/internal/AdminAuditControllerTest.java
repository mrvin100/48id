package io.k48.fortyeightid.audit.internal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.k48.fortyeightid.identity.Role;
import io.k48.fortyeightid.identity.User;
import io.k48.fortyeightid.identity.UserStatus;
import io.k48.fortyeightid.identity.internal.RoleRepository;
import io.k48.fortyeightid.identity.internal.UserRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminAuditControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;

    private UUID testUserId;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        
        var studentRole = roleRepository.findByName("STUDENT")
                .orElseThrow(() -> new IllegalStateException("STUDENT role not found in test DB"));
        
        var user = User.builder()
                .matricule("K48-2024-TEST")
                .email("test@k48.io")
                .name("Test User")
                .passwordHash("hash")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(studentRole))
                .build();
        testUserId = userRepository.save(user).getId();

        // Create sample audit events
        auditLogRepository.save(AuditLog.builder()
                .userId(testUserId)
                .action("LOGIN_SUCCESS")
                .details("{\"ip\":\"127.0.0.1\"}")
                .ipAddress("127.0.0.1")
                .build());

        auditLogRepository.save(AuditLog.builder()
                .userId(testUserId)
                .action("LOGIN_FAILURE")
                .details("{\"reason\":\"bad password\"}")
                .ipAddress("127.0.0.1")
                .build());

        auditLogRepository.save(AuditLog.builder()
                .userId(UUID.randomUUID())
                .action("ACCOUNT_SUSPENDED")
                .details("{}")
                .build());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAuditLog_returnsAllEvents() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-log")
                        .param("page", "0")
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAuditLog_filtersByEventType() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-log")
                        .param("eventType", "LOGIN_FAILURE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].eventType").value("LOGIN_FAILURE"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAuditLog_filtersByUserId() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-log")
                        .param("userId", testUserId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].userId").value(testUserId.toString()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAuditLog_filtersByDateRange() throws Exception {
        var now = Instant.now();
        var oneHourAgo = now.minusSeconds(3600);
        var twoHoursFromNow = now.plusSeconds(7200);

        mockMvc.perform(get("/api/v1/admin/audit-log")
                        .param("from", oneHourAgo.toString())
                        .param("to", twoHoursFromNow.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void getAuditLog_deniesStudentAccess() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-log"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAuditLog_deniesUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-log"))
                .andExpect(status().isUnauthorized());
    }
}
