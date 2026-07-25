package com.airtribe.meditrack.interfaces;

public interface Searchable {

    String getSearchableText();

    default boolean matches(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        return getSearchableText().toLowerCase().contains(keyword.toLowerCase().trim());
    }

    default boolean matchesAll(String... keywords) {
        if (keywords == null || keywords.length == 0) {
            return true;
        }
        for (String keyword : keywords) {
            if (!matches(keyword)) {
                return false;
            }
        }
        return true;
    }

    default boolean matchesAny(String... keywords) {
        if (keywords == null || keywords.length == 0) {
            return true;
        }
        for (String keyword : keywords) {
            if (matches(keyword)) {
                return true;
            }
        }
        return false;
    }
}
