package run.halo.app.security.fastgpt;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * FastGPT API Key认证转换器
 * 从HTTP请求中提取API Key并创建认证对象
 */
public class FastGptApiKeyAuthenticationConverter implements ServerAuthenticationConverter {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        return Mono.fromCallable(() -> {
            String apiKey = extractApiKey(exchange);
            if (StringUtils.hasText(apiKey)) {
                return new FastGptApiKeyAuthenticationToken(apiKey);
            }
            return null;
        });
    }

    private String extractApiKey(ServerWebExchange exchange) {
        // 1. 尝试从X-API-Key头部获取
        String apiKey = exchange.getRequest().getHeaders().getFirst(API_KEY_HEADER);
        if (StringUtils.hasText(apiKey)) {
            return apiKey;
        }

        // 2. 尝试从Authorization头部获取Bearer token
        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authorization) && authorization.startsWith(BEARER_PREFIX)) {
            return authorization.substring(BEARER_PREFIX.length());
        }

        return null;
    }
}