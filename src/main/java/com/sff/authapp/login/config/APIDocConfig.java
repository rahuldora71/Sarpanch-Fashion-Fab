package com.sff.authapp.login.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(
                title = "Auth App API",
                version = "1.0",
                description = "API documentation for the Auth App",
                contact = @Contact(
                        name = "Your Name",
                        email = "rahuldoraksp02@gmail.com",
                        url = "https://substringtechnologies.com"

                ),
                summary = "This app is very usefull for sarpanch Fashion FAb backend"



        ),
        security = {
                @SecurityRequirement(
                        name = "bearerAuth"

                )
        }

)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)


@Configuration
public class APIDocConfig {


}
