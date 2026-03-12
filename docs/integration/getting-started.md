# Integration Guide

How to integrate your application with 48ID.

## Overview

48ID provides two authentication methods for external applications:

1. **JWT Bearer Token** - For user-facing operations (forwarding user tokens)
2. **API Key** - For server-to-server operations

## Getting Started

### 1. Request API Key

Contact K48 administration to request an API key for your application.

Provide:
- Application name
- Application description
- Expected usage

### 2. Store API Key Securely

```bash
# Environment variable (recommended)
export FORTYEIGHTID_API_KEY="your-api-key-here"

# Or in configuration file (ensure file is not committed)
FORTYEIGHTID_API_KEY=your-api-key-here
```

**Never:**
- Commit API keys to version control
- Expose API keys in client-side code
- Share API keys publicly

### 3. Test Connection

```bash
curl -X GET http://localhost:8080/api/v1/users/K48-2024-001/exists \
  -H "X-API-Key: your-api-key-here"
```

Expected response:
```json
{
  "exists": true,
  "status": "ACTIVE"
}
```

## Authentication Flows

### Flow 1: Server-Side Rendering (Recommended)

Forward user's JWT to 48ID to retrieve identity data.

```
┌─────────┐     ┌──────────┐     ┌──────┐
│  User   │────▶│ Your App │────▶│ 48ID │
│         │◀────│          │◀────│      │
└─────────┘ JWT └──────────┘ JWT └──────┘
```

**Steps:**

1. User logs in via 48ID, receives JWT
2. User's browser sends JWT to your application
3. Your backend forwards JWT to `GET /api/v1/me`
4. 48ID returns verified identity data
5. Your application renders page with user data

**Example (Node.js/Express):**

```javascript
app.get('/profile', async (req, res) => {
  const jwt = req.headers.authorization?.replace('Bearer ', '');
  
  const response = await fetch('http://localhost:8080/api/v1/me', {
    headers: {
      'Authorization': `Bearer ${jwt}`
    }
  });
  
  const user = await response.json();
  res.render('profile', { user });
});
```

**No API key required** - uses user's JWT.

---

### Flow 2: Token Verification (Server-to-Server)

Verify user's JWT validity without user context.

```
┌─────────┐     ┌──────────┐     ┌──────┐
│  User   │────▶│ Your App │────▶│ 48ID │
│         │     │          │     │      │
└─────────┘ JWT └──────────┘ JWT └──────┘
                          API Key
```

**Steps:**

1. User presents JWT to your application
2. Your backend calls `POST /api/v1/auth/verify-token` with API key
3. 48ID returns validity status and identity claims
4. Your application proceeds based on validity

**Example (Node.js/Express):**

```javascript
app.post('/verify', async (req, res) => {
  const jwt = req.body.token;
  
  const response = await fetch('http://localhost:8080/api/v1/auth/verify-token', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-API-Key': process.env.FORTYEIGHTID_API_KEY
    },
    body: JSON.stringify({ token: jwt })
  });
  
  const result = await response.json();
  
  if (result.valid) {
    res.json({ status: 'verified', user: result.user });
  } else {
    res.status(401).json({ status: 'invalid', reason: result.reason });
  }
});
```

---

### Flow 3: Identity Lookup by ID

Retrieve public identity information by user UUID.

**Steps:**

1. Your application has user's UUID
2. Your backend calls `GET /api/v1/users/{id}/identity` with API key
3. 48ID returns public identity fields

**Example:**

```javascript
async function getUserIdentity(userId) {
  const response = await fetch(
    `http://localhost:8080/api/v1/users/${userId}/identity`,
    {
      headers: {
        'X-API-Key': process.env.FORTYEIGHTID_API_KEY
      }
    }
  );
  
  return await response.json();
}
```

---

### Flow 4: Matricule Validation

Check if a matricule exists before authentication.

**Steps:**

1. User enters matricule
2. Your backend calls `GET /api/v1/users/{matricule}/exists` with API key
3. 48ID returns existence status

**Example:**

```javascript
async function validateMatricule(matricule) {
  const response = await fetch(
    `http://localhost:8080/api/v1/users/${matricule}/exists`,
    {
      headers: {
        'X-API-Key': process.env.FORTYEIGHTID_API_KEY
      }
    }
  );
  
  const result = await response.json();
  return result.exists;
}
```

## Error Handling

### API Key Errors

**403 Forbidden** - Invalid API Key
```json
{
  "type": "https://48id.k48.io/errors/invalid-api-key",
  "title": "Invalid API Key",
  "status": 403,
  "detail": "The provided API key is not valid or has been revoked."
}
```

**Action:** Contact K48 administration to obtain new API key.

### Rate Limiting

**429 Too Many Requests**
```json
{
  "type": "https://48id.k48.io/errors/rate-limit-exceeded",
  "title": "Too Many Requests",
  "status": 429,
  "detail": "Rate limit exceeded. Please retry after the specified time.",
  "instance": "/api/v1/auth/login"
}
```

**Headers:**
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1705320600
Retry-After: 60
```

**Action:** Implement exponential backoff.

## Best Practices

### 1. Cache Identity Data

Cache user identity data to reduce API calls:

```javascript
const cache = new Map();

async function getCachedIdentity(userId, jwt) {
  if (cache.has(userId)) {
    return cache.get(userId);
  }
  
  const identity = await fetchIdentity(userId, jwt);
  cache.set(userId, identity);
  
  // Cache for 5 minutes
  setTimeout(() => cache.delete(userId), 5 * 60 * 1000);
  
  return identity;
}
```

### 2. Handle Token Expiry

Check token expiry and refresh as needed:

```javascript
function isTokenExpired(jwt) {
  const payload = JSON.parse(atob(jwt.split('.')[1]));
  return payload.exp * 1000 < Date.now();
}

async function authenticatedRequest(jwt, refreshToken) {
  if (isTokenExpired(jwt)) {
    const newTokens = await refreshTokens(refreshToken);
    jwt = newTokens.access_token;
    refreshToken = newTokens.refresh_token;
  }
  
  return makeRequest(jwt);
}
```

### 3. Implement Retry Logic

```javascript
async function retryableRequest(fn, maxRetries = 3) {
  for (let i = 0; i < maxRetries; i++) {
    try {
      return await fn();
    } catch (error) {
      if (error.status === 429 && i < maxRetries - 1) {
        const waitTime = Math.pow(2, i) * 1000;
        await sleep(waitTime);
        continue;
      }
      throw error;
    }
  }
}
```

### 4. Log API Key Usage

Track API key usage for monitoring:

```javascript
async function logApiUsage(endpoint, statusCode) {
  // Log to your monitoring system
  console.log(`API Call: ${endpoint} - ${statusCode}`);
}
```

## Example Integrations

### React Frontend

```javascript
// src/services/48id.js
class FortyEightIdService {
  constructor(apiBaseUrl) {
    this.apiBaseUrl = apiBaseUrl;
    this.accessToken = null; // Store in memory, not localStorage
  }

  async login(matricule, password) {
    const response = await fetch(`${this.apiBaseUrl}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ matricule, password })
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.detail);
    }

    const data = await response.json();
    // Store access token in memory (not localStorage for security)
    this.accessToken = data.access_token;
    // Refresh token should be set as HttpOnly cookie by server
    // Do NOT store refresh token in localStorage
    
    return data;
  }

  async getCurrentUser() {
    if (!this.accessToken) {
      throw new Error('Not authenticated');
    }
    
    const response = await fetch(`${this.apiBaseUrl}/me`, {
      headers: { 'Authorization': `Bearer ${this.accessToken}` }
    });

    if (!response.ok) {
      throw new Error('Not authenticated');
    }

    return await response.json();
  }
}

export const fortyEightId = new FortyEightIdService('http://localhost:8080/api/v1');
```

**Security Note:** Access tokens are stored in memory (lost on page refresh) for security. For production, implement token refresh via HttpOnly cookies or use a backend-for-frontend pattern.

### Spring Boot Backend

```java
// src/main/java/com/example/service/FortyEightIdClient.java
@Service
public class FortyEightIdClient {

    @Value("${fortyeightid.api.base-url}")
    private String apiBaseUrl;

    @Value("${fortyeightid.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean verifyToken(String jwt) {
        var request = new VerifyTokenRequest(jwt);
        var headers = new HttpHeaders();
        headers.set("X-API-Key", apiKey);
        headers.set("Content-Type", "application/json");

        var response = restTemplate.exchange(
            apiBaseUrl + "/auth/verify-token",
            HttpMethod.POST,
            new HttpEntity<>(request, headers),
            VerifyTokenResponse.class
        );

        return response.getBody().isValid();
    }

    public UserIdentity getUserIdentity(String userId) {
        var headers = new HttpHeaders();
        headers.set("X-API-Key", apiKey);

        var response = restTemplate.exchange(
            apiBaseUrl + "/users/{id}/identity",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            UserIdentity.class,
            userId
        );

        return response.getBody();
    }
}
```

## Next Steps

- [API Key Authentication](api-keys.md) - API key management
- [SDK Examples](examples.md) - Code examples in multiple languages
- [Security Best Practices](../security/overview.md) - Security guidelines
