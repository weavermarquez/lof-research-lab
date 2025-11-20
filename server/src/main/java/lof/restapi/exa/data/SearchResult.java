package lof.restapi.exa.data;

import java.util.List;
import java.util.Map;

import com.rpl.rama.RamaSerializable;

public class SearchResult implements RamaSerializable {
  public String title;
  public String url;
  public String publishedDate;
  public String author;
  public String id;
  public String image;
  public String favicon;
  public String text;
  public List<String> highlights;
  public List<Double> highlightScores;
  public String summary;
  public List<SearchResult> subpages;
  public Map<String, Object> extras;

  public SearchResult() {}

  public SearchResult(String title, String url) {
    this.title = title;
    this.url = url;
  }
}
