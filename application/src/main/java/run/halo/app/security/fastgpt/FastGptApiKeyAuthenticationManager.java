package run.halo.app.security.fastgpt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * FastGPT API Key认证管理器
 */
@Slf4j
public class FastGptApiKeyAuthenticationManager implements ReactiveAuthenticationManager {

    private final Set<String> validApiKeys;

    public FastGptApiKeyAuthenticationManager(Set<String> validApiKeys) {
        this.validApiKeys = validApiKeys;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        if (!(authentication instanceof FastGptApiKeyAuthenticationToken)) {
            return Mono.empty();
        }

        FastGptApiKeyAuthenticationToken token = (FastGptApiKeyAuthenticationToken) authentication;
        String apiKey = token.getApiKey();

        if (!StringUtils.hasText(apiKey)) {
            return Mono.error(new BadCredentialsException("API Key不能为空"));
        }

        // 验证API Key
        if (!validApiKeys.contains(apiKey)) {
            log.warn("无效的FastGPT API Key: {}", apiKey.substring(0, Math.min(8, apiKey.length())) + "***");
            return Mono.error(new BadCredentialsException("无效的API Key"));
        }

        log.debug("FastGPT API Key验证成功: {}", apiKey.substring(0, Math.min(8, apiKey.length())) + "***");
        
        // 创建已认证的token，principal设为"fastgpt"
        return Mono.just(FastGptApiKeyAuthenticationToken.authenticated(apiKey, "fastgpt"));
    }
}