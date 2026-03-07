package io.k48.fortyeightid.auth.internal;

import io.k48.fortyeightid.identity.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class UserDetailsServiceImpl implements UserDetailsService {

    private final UserQueryService userQueryService;

    @Override
    public UserDetails loadUserByUsername(String matricule) throws UsernameNotFoundException {
        var user = userQueryService.findByMatricule(matricule)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + matricule));

        var principal = new UserPrincipal(user);

        if (!principal.isEnabled()) {
            throw new DisabledException("Account is suspended: " + matricule);
        }

        return principal;
    }
}
