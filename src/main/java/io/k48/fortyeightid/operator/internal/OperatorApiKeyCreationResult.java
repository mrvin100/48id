package io.k48.fortyeightid.operator.internal;

import io.k48.fortyeightid.auth.ApiKey;

record OperatorApiKeyCreationResult(String rawKey, ApiKey apiKey) {}
