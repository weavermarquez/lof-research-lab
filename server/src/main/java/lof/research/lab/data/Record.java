package lof.research.lab.data;

import com.rpl.rama.RamaSerializable;
import java.util.List;
import java.util.ArrayList;

public class Record implements RamaSerializable {
  public String canonicalId;
  public List<String> authors;
  public String title;
  public Integer year;
  public String doi;
  public String url;

  public Record() {
    // Default constructor required for RamaSerializable
  }

  public Record(List<String> authors, String title, Integer year, String doi, String url) {
    this.authors = authors != null ? new ArrayList<>(authors) : new ArrayList<>();
    this.title = title;
    this.year = year;
    this.doi = doi;
    this.url = url;
    this.canonicalId = generateCanonicalId();
  }

  /**
   * Generate a canonical ID from (firstAuthor, year, titleSlug).
   * Format: "lastname-year-first-three-title-words"
   * Example: "voros-2023-cradle-things"
   */
  public String generateCanonicalId() {
    StringBuilder id = new StringBuilder();

    // 1. First author's last name (or "unknown")
    String authorPart = "unknown";
    if (authors != null && !authors.isEmpty()) {
      String firstAuthor = authors.get(0);
      // Extract last name (assume "LastName, FirstName" or "FirstName LastName" format)
      String[] parts = firstAuthor.split(",");
      if (parts.length > 0) {
        authorPart = parts[0].trim();
      } else {
        String[] spaceParts = firstAuthor.split(" ");
        if (spaceParts.length > 0) {
          authorPart = spaceParts[spaceParts.length - 1];
        }
      }
      authorPart = normalize(authorPart);
    }
    id.append(authorPart);

    // 2. Year (or "nodate")
    id.append("-");
    if (year != null) {
      id.append(year);
    } else {
      id.append("nodate");
    }

    // 3. First three words of title (or "notitle")
    id.append("-");
    if (title != null && !title.isEmpty()) {
      String[] words = title.split("\\s+");
      int wordCount = Math.min(3, words.length);
      for (int i = 0; i < wordCount; i++) {
        if (i > 0) id.append("-");
        id.append(normalize(words[i]));
      }
    } else {
      id.append("notitle");
    }

    return id.toString();
  }

  /**
   * Normalize a string: lowercase, remove punctuation, trim
   */
  private String normalize(String s) {
    if (s == null) return "";
    return s.toLowerCase()
            .replaceAll("[^a-z0-9]", "")
            .trim();
  }

  @Override
  public String toString() {
    return String.format("Record{id='%s', title='%s', authors=%s, year=%d, doi='%s', url='%s'}",
            canonicalId,
            title != null ? title.substring(0, Math.min(50, title.length())) + "..." : "null",
            authors,
            year != null ? year : 0,
            doi != null ? doi : "null",
            url != null ? url : "null");
  }
}
