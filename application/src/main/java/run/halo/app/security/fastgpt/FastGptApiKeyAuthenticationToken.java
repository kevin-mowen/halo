package run.halo.app.security.fastgpt;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

/**
 * FastGPT API Key认证Token
 */
public class FastGptApiKeyAuthenticationToken extends AbstractAuthenticationToken {

    private final String apiKey;
    private final String principal;

    public FastGptApiKeyAuthenticationToken(String apiKey) {
        super(null);
        this.apiKey = apiKey;
        this.principal = null;
        setAuthenticated(false);
    }

    public FastGptApiKeyAuthenticationToken(String apiKey, String principal, 
                                           Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.apiKey = apiKey;
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return apiKey;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    public String getApiKey() {
        return apiKey;
    }

    public static FastGptApiKeyAuthenticationToken authenticated(String apiKey, String principal) {
        return new FastGptApiKeyAuthenticationToken(apiKey, principal, 
            List.of(new SimpleGrantedAuthority("ROLE_FASTGPT_API")));
    }
}