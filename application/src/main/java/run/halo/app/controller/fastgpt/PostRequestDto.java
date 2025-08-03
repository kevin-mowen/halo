package run.halo.app.controller.fastgpt;

import lombok.Data;

@Data
public class PostRequestDto {
    private String title;
    private String content;

    // 一定要加 getter/setter
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}