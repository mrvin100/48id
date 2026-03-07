package io.k48.fortyeightid.auth.internal;

import io.k48.fortyeightid.identity.User;
import io.k48.fortyeightid.identity.UserQueryService;
import io.k48.fortyeightid.identity.UserStatus;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
class AuthService {

    private final UserQueryService userQueryService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final JwtConfig jwtConfig;

    LoginResponse login(LoginRequest request) {
        var userOpt = userQueryService.findByMatricule(request.matricule());

        if (userOpt.isEmpty()) {
            throw new BadCredentialsException("Matricule or password is incorrect.");
        }

        var user = userOpt.get();

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new DisabledException("Your account has been suspended. Contact K48 administration.");
        }

        if (user.getStatus() == UserStatus.PENDING_ACTIVATION) {
            throw new DisabledException("Please activate your account. Check your email for an activation link.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Matricule or password is incorrect.");
        }

        var principal = new UserPrincipal(user);
        var accessToken = jwtTokenService.generateAccessToken(principal, user);
        var refreshToken = refreshTokenService.createRefreshToken(user.getId());

        var roles = user.getRoles().stream()
                .map(r -> r.getName())
                .collect(Collectors.joining(","));

        var userInfo = new LoginResponse.UserInfo(
                user.getId().toString(),
                user.getMatricule(),
                user.getName(),
                roles,
                user.getBatch(),
                user.getSpecialization()
        );

        return new LoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtConfig.getAccessTokenExpiry(),
                userInfo
        );
    }
}
