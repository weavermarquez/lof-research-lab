package lof.restapi.exa.data;

import java.util.List;

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
  public Boolean contentsText;

  public SearchRequest(String query) {
    this.query = query;
  }
}
