package com.quocnva.easymall.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.Arrays;
import java.util.Locale;

@Configuration
public class I18nConfig implements WebMvcConfigurer {

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver();
        
        // Set Default Locale to Vietnamese
        localeResolver.setDefaultLocale(Locale.of("vi"));
        
        // Supported Locales
        localeResolver.setSupportedLocales(Arrays.asList(
                Locale.of("vi"),
                Locale.ENGLISH
        ));
        
        return localeResolver;
    }
}
