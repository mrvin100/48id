package io.k48.fortyeightid.admin.internal;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class AdminDashboardControllerTest {

    @Mock
    private AdminDashboardService adminDashboardService;

    @InjectMocks
    private AdminDashboardController adminDashboardController;

    @Test
    void getDashboardMetrics_returnsMetricsFromService() {
        // Given: Dashboard metrics exist
        var expectedMetrics = new DashboardMetricsResponse(
                1500L,  // totalUsers
                1200L,  // activeUsers
                150L,   // activeSessions
                50L,    // pendingActivations
                25L,    // suspendedUsers
                "operational" // systemHealth
        );
        when(adminDashboardService.getDashboardMetrics()).thenReturn(expectedMetrics);

        // When: Admin requests dashboard metrics
        ResponseEntity<DashboardMetricsResponse> response = adminDashboardController.getDashboardMetrics();

        // Then: Returns metrics from service
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(expectedMetrics);
        verify(adminDashboardService, times(1)).getDashboardMetrics();
    }

    @Test
    void getLoginActivity_returnsActivityFromService() {
        // Given: Login activity data exists
        var expectedActivity = new LoginActivityResponse(
                List.of(
                        new LoginActivityData("Mon", 45L),
                        new LoginActivityData("Tue", 52L),
                        new LoginActivityData("Wed", 38L)
                )
        );
        when(adminDashboardService.getLoginActivity()).thenReturn(expectedActivity);

        // When: Admin requests login activity
        ResponseEntity<LoginActivityResponse> response = adminDashboardController.getLoginActivity();

        // Then: Returns activity from service
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(expectedActivity);
        verify(adminDashboardService, times(1)).getLoginActivity();
    }

    @Test
    void getRecentActivity_returnsActivityFromService() {
        // Given: Recent activity data exists
        var expectedActivity = new RecentActivityResponse(
                List.of(
                        new RecentActivityData("LOGIN_SUCCESS", "user123", "192.168.1.1", java.time.Instant.now()),
                        new RecentActivityData("PASSWORD_RESET", "user456", "192.168.1.2", java.time.Instant.now())
                )
        );
        when(adminDashboardService.getRecentActivity()).thenReturn(expectedActivity);

        // When: Admin requests recent activity
        ResponseEntity<RecentActivityResponse> response = adminDashboardController.getRecentActivity();

        // Then: Returns activity from service
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(expectedActivity);
        verify(adminDashboardService, times(1)).getRecentActivity();
    }
}