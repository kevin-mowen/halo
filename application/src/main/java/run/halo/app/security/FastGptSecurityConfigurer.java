package run.halo.app.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationEntryPointFailureHandler;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.security.oauth2.server.resource.web.server.BearerTokenServerAuthenticationEntryPoint;
import run.halo.app.infra.properties.FastGptProperties;
import run.halo.app.security.authentication.SecurityConfigurer;
import run.halo.app.security.fastgpt.FastGptApiKeyAuthenticationConverter;
import run.halo.app.security.fastgpt.FastGptApiKeyAuthenticationManager;

/**
 * Security configuration for FastGPT API endpoints.
 * This configurer supports both API Key authentication and anonymous access (configurable).
 */
@Configuration
@RequiredArgsConstructor
public class FastGptSecurityConfigurer {

    private final FastGptProperties fastGptProperties;

    @Bean
    @Order(-1) // 设置为最高优先级，在其他配置之前执行
    SecurityConfigurer fastGptAuthorizationConfigurer() {
        return http -> {
            // 如果启用了API Key认证，添加认证过滤器
            if (fastGptProperties.isAuthEnabled() && !fastGptProperties.getApiKeys().isEmpty()) {
                var authenticationManager = new FastGptApiKeyAuthenticationManager(fastGptProperties.getApiKeys());
                var authenticationFilter = new AuthenticationWebFilter(authenticationManager);
                authenticationFilter.setServerAuthenticationConverter(new FastGptApiKeyAuthenticationConverter());
                authenticationFilter.setRequiresAuthenticationMatcher(
                    ServerWebExchangeMatchers.pathMatchers(HttpMethod.POST, "/api/fastgpt/**")
                );
                
                // 设置认证失败处理器
                var entryPoint = new BearerTokenServerAuthenticationEntryPoint();
                var failureHandler = new ServerAuthenticationEntryPointFailureHandler(entryPoint);
                authenticationFilter.setAuthenticationFailureHandler(failureHandler);
                
                http.addFilterBefore(authenticationFilter, SecurityWebFiltersOrder.AUTHORIZATION);
            }

            // 配置授权规则
            http.authorizeExchange(spec -> {
                if (fastGptProperties.isAllowAnonymous() || !fastGptProperties.isAuthEnabled()) {
                    // 允许匿名访问（向后兼容）
                    spec.pathMatchers(HttpMethod.POST, "/api/fastgpt/**").permitAll();
                } else {
                    // 需要FASTGPT_API角色
                    spec.pathMatchers(HttpMethod.POST, "/api/fastgpt/**").hasRole("FASTGPT_API");
                }
            });
        };
    }
}