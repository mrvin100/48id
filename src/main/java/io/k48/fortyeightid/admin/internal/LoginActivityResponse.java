package io.k48.fortyeightid.admin.internal;

import java.util.List;

record LoginActivityResponse(
        List<LoginActivityData> data
) {
}

record LoginActivityData(
        String day,
        Long logins
) {
}