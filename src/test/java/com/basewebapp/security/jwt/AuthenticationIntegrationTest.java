package com.basewebapp.security.jwt;

import com.basewebapp.config.ApplicationProperties;
import com.basewebapp.config.SecurityConfiguration;
import com.basewebapp.config.SecurityJwtConfiguration;
import com.basewebapp.config.WebConfigurer;
import com.basewebapp.management.SecurityMetersService;
import com.basewebapp.security.LoginRateLimiter;
import com.basewebapp.web.rest.AuthenticateController;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import tech.jhipster.config.JHipsterProperties;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(
    properties = {
        "jhipster.security.authentication.jwt.base64-secret=fd54a45s65fds737b9aafcb3412e07ed99b267f33413274720ddbb7f6c5e64e9f14075f2d7ed041592f0b7657baf8",
        "jhipster.security.authentication.jwt.token-validity-in-seconds=60000",
    },
    // Contesto "a fette": niente component scan, quindi ogni collaboratore di
    // SecurityConfiguration va elencato qui. ApplicationProperties e LoginRateLimiter servono dal
    // rate limit di B2b: senza di loro il contesto non parte (vedi B2c1).
    classes = {
        JHipsterProperties.class,
        ApplicationProperties.class,
        WebConfigurer.class,
        SecurityConfiguration.class,
        SecurityJwtConfiguration.class,
        SecurityMetersService.class,
        LoginRateLimiter.class,
        AuthenticateController.class,
        JwtAuthenticationTestUtils.class,
    }
)
public @interface AuthenticationIntegrationTest {}
