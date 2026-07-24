package main.java.com.airtribe.meditrack.interfaces;


public interface Searchable {
    /**
     * Checks if the entity matches the given keyword.
     * @param keyword the string to search for
     * @return true if it matches, false otherwise
     */
    boolean matches(String keyword);
}