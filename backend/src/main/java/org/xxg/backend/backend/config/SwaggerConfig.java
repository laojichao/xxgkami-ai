package org.xxg.backend.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * SpringDoc OpenAPI (Swagger) 配置。
 * <p>生成交互式 API 文档，支持在线测试接口。</p>
 * <p>访问地址：</p>
 * <ul>
 *   <li>Swagger UI: <a href="/api/swagger-ui.html">/api/swagger-ui.html</a></li>
 *   <li>OpenAPI JSON: <a href="/api/v3/api-docs">/api/v3/api-docs</a></li>
 * </ul>
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("小小怪卡密验证系统 Pro - API 文档")
                        .version("1.0.2")
                        .description("全栈卡密验证系统的 REST API 文档，包含认证、卡密管理、订单、钱包、用户管理等接口。")
                        .contact(new Contact()
                                .name("xxgkami")
                                .url("https://github.com/user/xxgkami-ai")))
                .tags(List.of(
                        new Tag().name("认证接口").description("登录、注册、Token 刷新、TOTP、OAuth"),
                        new Tag().name("卡密管理").description("卡密生成、验证、查询、启停用"),
                        new Tag().name("订单管理").description("订单创建、查询、状态更新"),
                        new Tag().name("钱包管理").description("余额查询、充值、交易记录"),
                        new Tag().name("用户管理").description("用户 CRUD、个人资料、密码修改"),
                        new Tag().name("API Key 管理").description("API 密钥的增删改查"),
                        new Tag().name("系统设置").description("系统配置、定价管理"),
                        new Tag().name("安全管理").description("IP 黑名单、访问日志"),
                        new Tag().name("系统监控").description("系统状态、在线用户")
                ))
                .addSecurityItem(new SecurityRequirement().addList("cookieAuth"))
                .components(new Components()
                        .addSecuritySchemes("cookieAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.COOKIE)
                                        .name("access_token")
                                        .description("JWT Access Token (httpOnly Cookie)")));
    }
}
