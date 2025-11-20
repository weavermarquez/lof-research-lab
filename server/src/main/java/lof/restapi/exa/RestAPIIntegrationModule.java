package lof.restapi.exa;

import java.io.IOException;
import java.util.List;

import com.rpl.rama.*;
import com.rpl.rama.integration.*;
import com.rpl.rama.module.*;
import com.rpl.rama.ops.*;
import org.asynchttpclient.*;
import org.asynchttpclient.netty.NettyResponse;
import lof.restapi.exa.data.SearchRequest;

/*
 * This module demonstrates integrating Rama with an external service, in this case a REST API.
 *
 * See the test class RestApiIntegrationModuleTest for how a client interacts with this module.
 */
public class RestAPIIntegrationModule implements RamaModule {

  //   This defines a "task global" object, which when used with declareObject (as shown below), creates a value that
  // can be referenced on all tasks in both ETLs and query topologies. This interface specializes the object on each
  // task with lifecycle methods "prepareForTask" and "close". This interface can be used for anything from creating
  // task-specific caches to clients to external systems. The latter use case is demonstrated here by creating an HTTP
  // client and managing its lifecycle through this interface.
  //   Many external client interfaces can be shared on the same thread, or if thread-safe can be shared among all
  // threads in the same worker. The documentation for this API explores how to manage resources like that, and the
  // rama-kafka project is a real-world example of doing so. Links:
  //  - https://redplanetlabs.com/docs/~/integrating.html
  //  - https://github.com/redplanetlabs/rama-kafka
  //   This example is using AsyncHttpClient as a demonstration of integrating with any Java API. From this example you can
  // see how you'd interact with external databases, monitoring systems, or other tools as well.
  public static class AsyncHttpClientTaskGlobal implements TaskGlobalObject {
    public AsyncHttpClient client;

    @Override
    public void prepareForTask(int taskId, TaskGlobalContext context) {
      client = Dsl.asyncHttpClient();
    }

    @Override
    public void close() throws IOException {
      client.close();
    }
  }

  // This method is the entry point to all modules. It defines all depots, ETLs, PStates, and query topologies.
  @Override
  public void define(Setup setup, Topologies topologies) {
    // This depot takes in URL strings. The second argument is a "depot partitioner" that controls how
    // appended data is partitioned across the depot, affecting on which task each piece of data begins
    // processing in ETLs.
    setup.declareDepot("*getDepot", Depot.hashBy(Ops.IDENTITY));
    setup.declareDepot("*postDepot", Depot.hashBy((SearchRequest req) -> req.query));
    // This declares a task global with the given value. Since AsyncHttpClientTaskGlobal implements the TaskGlobalObject
    // interface, the value is specialized per task. Accessing the variable "*httpClient" in topologies always accesses the
    // value local to the task where the topology event is running.
    setup.declareObject("*httpClient", new AsyncHttpClientTaskGlobal());

    // Stream topologies process appended data within a few milliseconds and guarantee all data will be fully processed.
    StreamTopology s = topologies.stream("getHttp");
    //   PStates are durable and replicated datastores and are represented as an arbitrary combination of data structures. Reads
    // and writes to PStates go to disk and are not purely in-memory operations.
    //   This PState stores the latest response for each URL, a map from a URL to the body of the HTTP response.
    s.pstate("$$responses", PState.mapSchema(String.class, String.class));
    // This subscribes the ETL to "*getDepot", binding all URLs to the variable "*url". Because of the depot partitioner
    // on "*getDepot", computation starts on the same task where registration info is stored for that URL in
    // the "$$responses" PState.
    s.source("*getDepot").out("*url")
     // eachAsync integrates arbitrary asynchronous work represented by a CompletableFuture within a topology. It ties
     // the success/failure of the asynchronous task with the success/failure of the topology. So if the asynchronous
     // work fails or times out, the topology will fail as well and the depot record will be retried. eachAsync is a
     // non-blocking operation.
     .eachAsync((AsyncHttpClientTaskGlobal client, String url) ->
                 client.client.prepareGet(url).execute().toCompletableFuture(),
                "*httpClient", "*url").out("*response")
     .each((RamaFunction1<NettyResponse, String>) NettyResponse::getResponseBody, "*response").out("*body")
     // This records the latest response in the PState.
     .localTransform("$$responses", Path.key("*url").termVal("*body"));

    // postDepot triggers a POST search to Exa based on the provided query.
    s.source("*postDepot").out("*searchRequest")
     .each((SearchRequest req) -> req.query, "*searchRequest").out("*query")
     // eachAsync integrates arbitrary asynchronous work represented by a CompletableFuture within a topology. It ties
     // the success/failure of the asynchronous task with the success/failure of the topology. So if the asynchronous
     // work fails or times out, the topology will fail as well and the depot record will be retried. eachAsync is a
     // non-blocking operation.
     .eachAsync((AsyncHttpClientTaskGlobal client, SearchRequest req) -> {
                 String apiKey = System.getenv("EXA_API_KEY");
                 if(apiKey == null || apiKey.isEmpty()) {
                   throw new IllegalStateException("EXA_API_KEY env var must be set for Exa search");
                 }
                 BoundRequestBuilder request = client.client.preparePost("https://api.exa.ai/search")
                   .setHeader("accept", "application/json")
                   .setHeader("content-type", "application/json")
                   .setHeader("x-api-key", apiKey)
                   .setBody(buildRequestBody(req));
                 return request.execute().toCompletableFuture();
               },
               "*httpClient", "*searchRequest").out("*response")
     .each((RamaFunction1<Response, String>) Response::getResponseBody, "*response").out("*body")
     // This records the latest response in the PState.
     .localTransform("$$responses", Path.key("*query").termVal("*body"));
  }

  private static String buildRequestBody(SearchRequest req) {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    appendField(sb, "query", req.query, true);
    appendField(sb, "type", req.type, false);
    appendField(sb, "category", req.category, false);
    appendField(sb, "userLocation", req.userLocation, false);
    appendNumberField(sb, "numResults", req.numResults);
    appendStringArray(sb, "includeDomains", req.includeDomains);
    appendStringArray(sb, "excludeDomains", req.excludeDomains);
    appendField(sb, "startCrawlDate", req.startCrawlDate, false);
    appendField(sb, "endCrawlDate", req.endCrawlDate, false);
    appendField(sb, "startPublishedDate", req.startPublishedDate, false);
    appendField(sb, "endPublishedDate", req.endPublishedDate, false);
    appendStringArray(sb, "includeText", req.includeText);
    appendStringArray(sb, "excludeText", req.excludeText);
    appendBooleanField(sb, "context", req.context);
    appendBooleanField(sb, "moderation", req.moderation);

    boolean includeTextVal = req.contentsText == null ? true : req.contentsText;
    sb.append("\"contents\":{");
    sb.append("\"text\":").append(includeTextVal);
    sb.append("}");

    sb.append("}");
    return sb.toString();
  }

  private static void appendField(StringBuilder sb, String key, String value, boolean first) {
    if(value == null) return;
    if(!first) sb.append(",");
    sb.append("\"").append(key).append("\":\"").append(escape(value)).append("\"");
  }

  private static void appendNumberField(StringBuilder sb, String key, Number value) {
    if(value == null) return;
    sb.append(",\"").append(key).append("\":").append(value);
  }

  private static void appendBooleanField(StringBuilder sb, String key, Boolean value) {
    if(value == null) return;
    sb.append(",\"").append(key).append("\":").append(value);
  }

  private static void appendStringArray(StringBuilder sb, String key, List<String> vals) {
    if(vals == null || vals.isEmpty()) return;
    sb.append(",\"").append(key).append("\":[");
    for(int i=0; i<vals.size(); i++) {
      if(i>0) sb.append(",");
      sb.append("\"").append(escape(vals.get(i))).append("\"");
    }
    sb.append("]");
  }

  private static String escape(String val) {
    return val.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
