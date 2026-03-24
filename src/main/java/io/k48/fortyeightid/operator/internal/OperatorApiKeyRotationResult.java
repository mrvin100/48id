package io.k48.fortyeightid.operator.internal;

import java.time.Instant;

record OperatorApiKeyRotationResult(String rawKey, String applicationName, Instant rotatedAt) {}
