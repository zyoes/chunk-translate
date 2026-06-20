package com.example.chunktranslate.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI / Swagger 文档配置。
 * <p>访问 http://localhost:8080/swagger-ui.html 查看 API 文档。
 * 按业务模块（文档管理/翻译管理/导出管理）组织 Tag。</p>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI智能文档翻译平台 API")
                        .version("1.0.0")
                        .description("支持大文档自动分块翻译、实时查看翻译结果以及导出译文的Web系统")
                        .contact(new Contact()
                                .name("ChunkTranslate Team"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .tags(List.of(
                        new Tag().name("文档管理").description("文档上传、解析、目录树"),
                        new Tag().name("翻译管理").description("翻译任务启动、进度查询、结果查看"),
                        new Tag().name("导出管理").description("译文导出（DOCX/PDF/Markdown/TXT）")
                ));
    }
}
