package com.airtribe.meditrack.interfaces;

/**
 * Contract for any entity that can be located by a free-text keyword.
 *
 * <p>Demonstrates interface {@code default} methods: implementers only need to supply
 * {@link #getSearchableText()}, and inherit keyword matching for free. This keeps the
 * matching rule in one place (Single Responsibility) while letting each entity decide
 * which of its fields are searchable (Open/Closed).</p>
 *
 * @author Varun (Core Entities, OOP and Factory)
 */
public interface Searchable {

    /**
     * Returns the concatenated text this entity should be searched over.
     * Implementations typically join id, name and any domain-specific fields.
     *
     * @return searchable text, never {@code null}
     */
    String getSearchableText();

    /**
     * Checks whether this entity matches the given keyword.
     *
     * <p>A {@code null} or blank keyword matches everything, which makes
     * "search with no filter" behave like "list all".</p>
     *
     * @param keyword the string to search for
     * @return {@code true} if it matches, {@code false} otherwise
     */
    default boolean matches(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        return getSearchableText().toLowerCase().contains(keyword.toLowerCase().trim());
    }

    /**
     * Checks whether this entity matches <em>every</em> supplied keyword.
     *
     * @param keywords the keywords that must all match
     * @return {@code true} only if all keywords match
     */
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

    /**
     * Checks whether this entity matches <em>at least one</em> supplied keyword.
     *
     * @param keywords the candidate keywords
     * @return {@code true} if any keyword matches
     */
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
