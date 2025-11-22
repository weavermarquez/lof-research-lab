package lof.restapi.exa.data;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.rpl.rama.RamaSerializable;

public class SearchRequest implements RamaSerializable {
  public String query;
  public String type;
  public String category;
  public String userLocation;
  public Integer numResults;
  public List<String> includeDomains;
  public List<String> excludeDomains;
  public String startCrawlDate;
  public String endCrawlDate;
  public String startPublishedDate;
  public String endPublishedDate;
  public List<String> includeText;
  public List<String> excludeText;
  public Boolean context;
  public Boolean moderation;
  public Map<String, Object> contents;

  public SearchRequest(String query) {
    this.query = query;
    this.contents = new HashMap<>();
    this.contents.put("text", Boolean.FALSE);
  }
}
