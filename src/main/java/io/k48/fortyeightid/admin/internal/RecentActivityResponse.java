package io.k48.fortyeightid.admin.internal;

import java.time.Instant;
import java.util.List;

record RecentActivityResponse(
        List<RecentActivityData> activities
) {
}

record RecentActivityData(
        String action,
        String user,
        String ipAddress,
        Instant timestamp
) {
}