package com.solo_leveling.bank_of_cli_backend.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {
    public static final String API_PREFIX = "/api";

    private static final String BASE_PACKAGE = "com.solo_leveling.bank_of_cli_backend";

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // Both conditions must hold. HandlerTypePredicate would match on either one, which also
        // prefixes springdoc's own @RestController endpoints and breaks Swagger UI.
        configurer.addPathPrefix(API_PREFIX, type ->
                type.isAnnotationPresent(RestController.class) && type.getPackageName().startsWith(BASE_PACKAGE));
    }
}
