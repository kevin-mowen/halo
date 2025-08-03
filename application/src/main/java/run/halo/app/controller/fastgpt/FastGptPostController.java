package run.halo.app.controller.fastgpt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import run.halo.app.content.PostService;
import run.halo.app.content.PostRequest;
import run.halo.app.content.ContentUpdateParam;
import run.halo.app.core.extension.content.Post;
import run.halo.app.extension.Metadata;

import java.time.Instant;
import java.util.Map;
import java.util.HashMap;

@Slf4j
@RestController
@RequestMapping("/api/fastgpt")
@RequiredArgsConstructor
public class FastGptPostController {

    private final PostService postService;

    @PostMapping("/create-post")
    public Mono<ResponseEntity<Map<String, Object>>> createPost(@RequestBody PostRequestDto request, Authentication authentication) {
        try {
            log.info("收到FastGPT博客创建请求");
            log.info("请求参数 - title: {}", request.getTitle());
            log.info("请求参数 - content length: {}", request.getContent() != null ? request.getContent().length() : 0);
            log.info("FastGPT创建博客请求：title={}, content={}", request.getTitle(),
                request.getContent() != null ? request.getContent().substring(0, Math.min(50, request.getContent().length())) + "..." : "null");

            // 输入验证
            if (!StringUtils.hasText(request.getTitle())) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "标题不能为空");
                return Mono.just(ResponseEntity.badRequest().body(errorResponse));
            }

            if (!StringUtils.hasText(request.getContent())) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "内容不能为空");
                return Mono.just(ResponseEntity.badRequest().body(errorResponse));
            }

            // 生成slug（URL友好的标识符）
            String slug = generateSlug(request.getTitle());

            // 创建Post扩展资源
            Post post = createPostExtension(request.getTitle(), slug);

            // 创建内容参数 - 使用HTML格式以避免编辑器插件依赖
            ContentUpdateParam contentParam = new ContentUpdateParam(
                null, // version
                convertMarkdownToHtml(request.getContent()), // raw (HTML)
                convertMarkdownToHtml(request.getContent()), // content (HTML)
                "HTML" // rawType - 使用系统默认支持的HTML格式
            );

            // 创建PostRequest
            PostRequest postRequest = new PostRequest(post, contentParam);

            // 创建草稿并发布
            return postService.draftPost(postRequest)
                .flatMap(draftPost -> {
                    // 设置为已发布
                    draftPost.getSpec().setPublish(true);
                    draftPost.getSpec().setPublishTime(Instant.now());

                    // 发布博客
                    return postService.publish(draftPost);
                })
                .map(publishedPost -> {
                    log.info("博客发布成功：{}", publishedPost.getMetadata().getName());
                    Map<String, Object> successResponse = new HashMap<>();
                    successResponse.put("success", true);
                    successResponse.put("message", "博客发布成功");
                    successResponse.put("postName", publishedPost.getMetadata().getName());
                    successResponse.put("title", publishedPost.getSpec().getTitle());
                    return ResponseEntity.ok(successResponse);
                })
                .onErrorResume(error -> {
                    log.error("创建博客失败", error);
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message", "创建博客失败: " + error.getMessage());
                    return Mono.just(ResponseEntity.status(500).body(errorResponse));
                });

        } catch (Exception e) {
            log.error("处理FastGPT创建博客请求失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "服务器内部错误: " + e.getMessage());
            return Mono.just(ResponseEntity.status(500).body(errorResponse));
        }
    }

    private Post createPostExtension(String title, String slug) {
        Post post = new Post();

        // 设置metadata
        Metadata metadata = new Metadata();
        metadata.setName(slug); // 使用slug作为资源名称
        metadata.setGenerateName("post-");
        post.setMetadata(metadata);

        // 设置spec
        Post.PostSpec spec = new Post.PostSpec();
        spec.setTitle(title);
        spec.setSlug(slug);
        spec.setOwner("admin"); // 默认设置为admin，你可以根据需要修改
        spec.setDeleted(false);
        spec.setPublish(false); // 先创建草稿
        spec.setPinned(false);
        spec.setAllowComment(true);
        spec.setVisible(Post.VisibleEnum.PUBLIC);
        spec.setPriority(0);

        // 设置摘要自动生成
        Post.Excerpt excerpt = new Post.Excerpt();
        excerpt.setAutoGenerate(true);
        spec.setExcerpt(excerpt);

        post.setSpec(spec);

        return post;
    }

    private String generateSlug(String title) {
        if (!StringUtils.hasText(title)) {
            return "post-" + System.currentTimeMillis();
        }

        // 简单的slug生成：移除特殊字符，转为小写，用连字符连接
        return title.trim()
            .replaceAll("[^\\w\\s\\u4e00-\\u9fff]", "") // 保留字母、数字、空格和中文
            .replaceAll("\\s+", "-") // 空格替换为连字符
            .toLowerCase()
            + "-" + System.currentTimeMillis(); // 添加时间戳确保唯一性
    }

    /**
     * 简单的Markdown到HTML转换
     * 支持基本的Markdown语法转换为HTML
     */
    private String convertMarkdownToHtml(String markdown) {
        if (!StringUtils.hasText(markdown)) {
            return "";
        }

        String html = markdown;
        
        // 标题转换
        html = html.replaceAll("(?m)^# (.+)$", "<h1>$1</h1>");
        html = html.replaceAll("(?m)^## (.+)$", "<h2>$1</h2>");
        html = html.replaceAll("(?m)^### (.+)$", "<h3>$1</h3>");
        html = html.replaceAll("(?m)^#### (.+)$", "<h4>$1</h4>");
        html = html.replaceAll("(?m)^##### (.+)$", "<h5>$1</h5>");
        html = html.replaceAll("(?m)^###### (.+)$", "<h6>$1</h6>");
        
        // 粗体和斜体
        html = html.replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>");
        html = html.replaceAll("\\*(.+?)\\*", "<em>$1</em>");
        
        // 代码块
        html = html.replaceAll("```(.*?)\\n([\\s\\S]*?)```", "<pre><code class=\"language-$1\">$2</code></pre>");
        html = html.replaceAll("`(.+?)`", "<code>$1</code>");
        
        // 链接
        html = html.replaceAll("\\[(.+?)\\]\\((.+?)\\)", "<a href=\"$2\">$1</a>");
        
        // 列表项
        html = html.replaceAll("(?m)^- (.+)$", "<li>$1</li>");
        html = html.replaceAll("(?m)^\\* (.+)$", "<li>$1</li>");
        
        // 包装列表项为ul
        html = html.replaceAll("(<li>.*</li>)", "<ul>$1</ul>");
        html = html.replaceAll("</ul>\\s*<ul>", ""); // 合并连续的ul标签
        
        // 引用
        html = html.replaceAll("(?m)^> (.+)$", "<blockquote>$1</blockquote>");
        
        // 换行转换为段落
        html = html.replaceAll("\\n\\n", "</p><p>");
        html = html.replaceAll("\\n", "<br>");
        
        // 包装在段落标签中
        if (!html.trim().isEmpty() && !html.trim().startsWith("<")) {
            html = "<p>" + html + "</p>";
        }
        
        log.debug("Markdown转HTML完成: {} -> {}", 
            markdown.length() > 100 ? markdown.substring(0, 100) + "..." : markdown,
            html.length() > 100 ? html.substring(0, 100) + "..." : html);
        
        return html;
    }
}