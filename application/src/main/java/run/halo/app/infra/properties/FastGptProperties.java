package run.halo.app.infra.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * FastGPT配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "halo.fastgpt")
public class FastGptProperties {

    /**
     * 是否启用API Key认证
     */
    private boolean authEnabled = true;

    /**
     * 允许的API Keys列表
     */
    private Set<String> apiKeys = new HashSet<>();

    /**
     * 是否允许匿名访问（向后兼容）
     */
    private boolean allowAnonymous = false;
}