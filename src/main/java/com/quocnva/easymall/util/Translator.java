package com.quocnva.easymall.util;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Helper component for resolving i18n messages from {@link MessageSource}.
 * <p>
 * Uses a static reference so it can be called from any context,
 * including enums (e.g., {@code ErrorCode}) that cannot use Spring DI.
 * </p>
 */
@Component
public class Translator {

    private static MessageSource messageSource;

    public Translator(MessageSource messageSource) {
        Translator.messageSource = messageSource;
    }

    /**
     * Resolves the message for the given key using the current request's locale.
     *
     * @param msgCode the message key defined in {@code messages.properties}
     * @param args    optional arguments to interpolate into the message
     * @return the resolved message string
     */
    public static String toLocale(String msgCode, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(msgCode, args, locale);
    }
}
