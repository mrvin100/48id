package io.k48.fortyeightid.admin.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestGetters.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.k48.fortyeightid.audit.internal.AuditLog;
import io.k48.fortyeightid.audit.internal.AuditLogRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminAuditController.class)
@Import(SecurityConfig.class)
class AdminAuditControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditLogRepository auditLogRepository;

    @Test
    @WithMockUser(roles = "USER")
    void getAuditLog_returnsForbiddenForNonAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-log"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAuditLog_returnsOkForAdmin() throws Exception {
        when(auditLogRepository.findWithFilters(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/admin/audit-log"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAuditLog_forwardsFiltersToRepository() throws Exception {
        var userId = UUID.randomUUID();
        var page = new PageImpl<>(List.of());
        when(auditLogRepository.findWithFilters(
                eq("LOGIN_SUCCESS"),
                eq(userId),
                any(),
                any(),
                any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/audit-log")
                .param("userId", userId.toString())
                .param("eventType", "LOGIN_SUCCESS"))
                .andExpect(status().isOk());

        verify(auditLogRepository, times(1)).findWithFilters(
                eq("LOGIN_SUCCESS"),
                eq(userId),
                any(),
                any(),
                any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getLoginHistory_usesLoginHistoryRepositoryMethod() throws Exception {
        var userId = UUID.randomUUID();
        var page = new PageImpl<>(List.of());
        when(auditLogRepository.findLoginHistory(eq(userId), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/audit-log/login-history")
                .param("userId", userId.toString()))
                .andExpect(status().isOk());

        verify(auditLogRepository, times(1)).findLoginHistory(
                eq(userId),
                any(),
                any(),
                any(Pageable.class));
    }
}
