package transparent.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Represents the filters applied when retrieving content for the library view.
 * The query supports keyword, category and tag filtering.
 */
public final class ContentQuery {
    private String keyword = "";
    private String category = "";
    private final List<String> tags = new ArrayList<>();

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword == null ? "" : keyword.trim();
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category == null ? "" : category.trim();
    }

    public List<String> getTags() {
        return Collections.unmodifiableList(tags);
    }

    public void setTags(List<String> newTags) {
        tags.clear();
        if (newTags == null) {
            return;
        }
        for (String tag : newTags) {
            if (tag == null) {
                continue;
            }
            String normalised = tag.trim().toLowerCase(Locale.ROOT);
            if (!normalised.isBlank() && !tags.contains(normalised)) {
                tags.add(normalised);
            }
        }
    }

    public boolean hasKeyword() {
        return !keyword.isBlank();
    }

    public boolean hasCategory() {
        return !category.isBlank();
    }

    public boolean hasTags() {
        return !tags.isEmpty();
    }
}
