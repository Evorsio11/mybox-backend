package com.evorsio.mybox.auth.internal.service.impl;

import com.evorsio.mybox.auth.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;

@Slf4j
@Service
@Profile("dev")
@RequiredArgsConstructor
public class LogEmailService implements EmailService {

    private final TemplateEngine templateEngine;

    @Value("${mybox.common.cors.allowed-origins}")
    private List<String> frontendOrigins;

    @Override
    public void sendPasswordResetEmail(String to, String token) {
        log.info("====== [DEV] 模拟发送密码重置邮件 ======");
        log.info("收件人: {}", to);
        log.info("");

        // 使用 Thymeleaf 渲染模板
        String emailContent = renderEmailTemplate(to, token);

        // 打印 HTML 内容（开发环境）
        log.info("邮件 HTML 内容:");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info(emailContent);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("");
        log.info("重置链接:");
        for (String url : frontendOrigins) {
            String link = url.trim() + "/reset-password?token=" + token;
            log.info("  - {}", link);
        }
        log.info("======================================");
    }

    /**
     * 使用 Thymeleaf 渲染邮件模板
     */
    private String renderEmailTemplate(String to, String token) {
        Context context = new Context();
        context.setVariable("resetUrl", getResetUrl(token));
        context.setVariable("token", token);
        context.setVariable("userName", to); // 可以替换为真实的用户名

        return templateEngine.process("email/password-reset", context);
    }

    /**
     * 获取重置 URL（使用第一个配置的前端地址）
     */
    private String getResetUrl(String token) {
        String frontendUrl = frontendOrigins != null && !frontendOrigins.isEmpty()
                ? frontendOrigins.get(0).trim()
                : "http://localhost:3000";
        return frontendUrl + "/reset-password?token=" + token;
    }
}
