package com.quocnva.easymall.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

public class SlugUtils {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern EDGESDHASHES = Pattern.compile("(^-|-$)");
    private static final Pattern MULTIDASH = Pattern.compile("-{2,}");

    private SlugUtils() {
        // Private constructor for utility class
    }

    public static String toSlug(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        
        // Convert to lower case first
        String lowerInput = input.toLowerCase();
        
        // Replace Vietnamese characters
        lowerInput = lowerInput.replaceAll("đ", "d");
        
        String nowhitespace = WHITESPACE.matcher(lowerInput).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        slug = MULTIDASH.matcher(slug).replaceAll("-");
        slug = EDGESDHASHES.matcher(slug).replaceAll("");
        return slug;
    }
}
