package io.k48.fortyeightid.operator.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.k48.fortyeightid.admin.DashboardQueryPort;
import io.k48.fortyeightid.admin.DashboardQueryPort.DashboardSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class OperatorDashboardControllerTest {

    @Mock
    private DashboardQueryPort dashboardQueryPort;

    @InjectMocks
    private OperatorDashboardController operatorDashboardController;

    @Test
    void getDashboardMetrics_returnsSnapshotFromPort() {
        // Given: A dashboard snapshot exists
        var snapshot = new DashboardSnapshot(1500L, 1200L, 150L, 50L, 25L);
        when(dashboardQueryPort.getDashboardSnapshot()).thenReturn(snapshot);

        // When: Operator requests dashboard metrics
        ResponseEntity<OperatorDashboardResponse> response = operatorDashboardController.getDashboardMetrics();

        // Then: Returns mapped snapshot with 200 OK
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().totalUsers()).isEqualTo(1500L);
        assertThat(response.getBody().activeUsers()).isEqualTo(1200L);
        assertThat(response.getBody().activeSessions()).isEqualTo(150L);
        assertThat(response.getBody().pendingActivations()).isEqualTo(50L);
        assertThat(response.getBody().suspendedUsers()).isEqualTo(25L);
        verify(dashboardQueryPort, times(1)).getDashboardSnapshot();
    }
}
