package im.bigs.pg.api.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("나노바나나 페이먼츠 결제 API")
                    .version("v1.0")
                    .description("결제 도메인 서버 API 문서")
                    .contact(
                        Contact()
                            .name("개발팀")
                            .email("dev@nanobananapayments.com")
                    )
            )
            .servers(
                listOf(
                    Server()
                        .url("http://localhost:8080")
                        .description("Local Server")
                )
            )
    }

}