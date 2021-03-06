package org.infinispan.server.functional;

import static org.infinispan.server.security.Common.sync;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.infinispan.client.rest.RestCacheClient;
import org.infinispan.client.rest.RestClient;
import org.infinispan.client.rest.RestMetricsClient;
import org.infinispan.client.rest.RestResponse;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.server.test.InfinispanServerRule;
import org.infinispan.server.test.InfinispanServerTestMethodRule;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author anistor@redhat.com
 * @since 10.0
 */
public class RestMetricsResource {

   @ClassRule
   public static InfinispanServerRule SERVERS = ClusteredIT.SERVERS;

   @Rule
   public InfinispanServerTestMethodRule SERVER_TEST = new InfinispanServerTestMethodRule(SERVERS);

   private final ObjectMapper mapper = new ObjectMapper();

   @Test
   public void testOpenMetrics() {
      RestMetricsClient metricsClient = SERVER_TEST.rest().create().metrics();

      RestResponse response = sync(metricsClient.metrics(true));

      assertEquals(200, response.getStatus());
      assertEquals(MediaType.TEXT_PLAIN, response.contentType());

      String metricsText = response.getBody();
      assertTrue(metricsText.contains("# TYPE vendor_Cache_Statistics_stores gauge\n"));

      response = sync(metricsClient.metrics("vendor/Cache_Statistics_stores", true));
      assertEquals(200, response.getStatus());

      metricsText = response.getBody();
      assertTrue(metricsText.contains("# TYPE vendor_Cache_Statistics_stores gauge\n"));
   }

   @Test
   public void testBaseAndVendor() throws Exception {
      RestMetricsClient metricsClient = SERVER_TEST.rest().create().metrics();

      RestResponse response = sync(metricsClient.metrics("base/classloader.loadedClasses.count"));
      assertEquals(200, response.getStatus());

      JsonNode loadedClassesCountNode = mapper.readTree(response.getBody());
      assertTrue(loadedClassesCountNode.hasNonNull("classloader.loadedClasses.count"));
      int loadedClassesCount = loadedClassesCountNode.get("classloader.loadedClasses.count").asInt();
      assertTrue(loadedClassesCount > 0);

      response = sync(metricsClient.metrics("vendor/memoryPool.Metaspace.usage"));
      assertEquals(200, response.getStatus());

      JsonNode memoryPoolMetaspaceUsageNode = mapper.readTree(response.getBody());
      assertTrue(memoryPoolMetaspaceUsageNode.hasNonNull("memoryPool.Metaspace.usage"));
      int metaspaceUsage = memoryPoolMetaspaceUsageNode.get("memoryPool.Metaspace.usage").asInt();
      assertTrue(metaspaceUsage > 0);
   }

   @Test
   public void testMicroprofileMetrics() throws Exception {
      RestClient client = SERVER_TEST.rest().create();
      RestMetricsClient metricsClient = client.metrics();

      String cacheNameTag = SERVER_TEST.getMethodName() + "(" + CacheMode.DIST_SYNC.toString().toLowerCase() + ")";

      RestResponse response = sync(metricsClient.metrics());

      assertEquals(200, response.getStatus());
      assertEquals(MediaType.APPLICATION_JSON, response.contentType());

      String metricsJson = response.getBody();
      JsonNode node = mapper.readTree(metricsJson);
      assertNotNull(node.get("base"));
      assertNotNull(node.get("vendor"));
      assertNotNull(node.get("application"));
      assertTrue(metricsJson.contains("Cache_Statistics_stores"));

      response = sync(metricsClient.metrics("vendor/Cache_Statistics_stores"));
      assertEquals(200, response.getStatus());

      long totalStoresBefore = streamNodeFields(mapper.readTree(response.getBody()))
            .filter(e -> e.getKey().contains(cacheNameTag))
            .map(e -> e.getValue().asLong())
            .reduce(0L, Long::sum);

      assertEquals(0, totalStoresBefore);

      // put some entries then check that the stats were updated
      RestCacheClient cache = client.cache(SERVER_TEST.getMethodName());
      int NUM_PUTS = 10;
      for (int i = 0; i < NUM_PUTS; i++) {
         RestResponse putResp = sync(cache.put("k" + i, "v" + i));
         assertEquals(204, putResp.getStatus());
      }

      response = sync(metricsClient.metrics("vendor/Cache_Statistics_stores"));
      assertEquals(200, response.getStatus());

      String metricJson = response.getBody();
      long totalStoresAfter = streamNodeFields(mapper.readTree(metricJson))
            .filter(e -> e.getKey().contains(cacheNameTag))
            .map(e -> e.getValue().asLong())
            .reduce(0L, Long::sum);

      assertEquals(NUM_PUTS, totalStoresAfter);
   }

   @Test
   public void testMicroprofileTimerMetrics() throws Exception {
      RestClient client = SERVER_TEST.rest().create();
      RestMetricsClient metricsClient = client.metrics();

      String cacheNameTag = SERVER_TEST.getMethodName() + "(" + CacheMode.DIST_SYNC.toString().toLowerCase() + ")";

      RestResponse response = sync(metricsClient.metrics("vendor/Cache_Statistics_storeTimes"));
      assertEquals(200, response.getStatus());
      assertEquals(MediaType.APPLICATION_JSON, response.contentType());

      long meanStoreTimesBefore = streamNodeFields(mapper.readTree(response.getBody()).get("Cache_Statistics_storeTimes"))
            .filter(e -> e.getKey().startsWith("mean;") && e.getKey().contains(cacheNameTag))
            .map(e -> e.getValue().asLong())
            .reduce(0L, Long::sum);

      assertEquals(0, meanStoreTimesBefore);

      // put some entries then check that the stats were updated
      RestCacheClient cache = client.cache(SERVER_TEST.getMethodName());
      int NUM_PUTS = 10;
      for (int i = 0; i < NUM_PUTS; i++) {
         RestResponse putResp = sync(cache.put("k" + i, "v" + i));
         assertEquals(204, putResp.getStatus());
      }

      response = sync(metricsClient.metrics("vendor/Cache_Statistics_storeTimes"));
      assertEquals(200, response.getStatus());

      long meanStoreTimesAfter = streamNodeFields(mapper.readTree(response.getBody()).get("Cache_Statistics_storeTimes"))
            .filter(e -> e.getKey().startsWith("mean;") && e.getKey().contains(cacheNameTag))
            .map(e -> e.getValue().asLong())
            .reduce(0L, Long::sum);

      assertTrue(meanStoreTimesAfter > 0);
   }

   @Test
   public void testMicroprofileMetricsMetadata() throws Exception {
      RestMetricsClient metricsClient = SERVER_TEST.rest().create().metrics();
      RestResponse response = sync(metricsClient.metricsMetadata());

      assertEquals(200, response.getStatus());
      assertEquals(MediaType.APPLICATION_JSON, response.contentType());

      String metricsMetadataJson = response.getBody();
      assertTrue(metricsMetadataJson.contains("Cache_Statistics_stores"));

      response = sync(metricsClient.metricsMetadata("vendor/Cache_Statistics_stores"));
      assertEquals(200, response.getStatus());

      JsonNode node = mapper.readTree(response.getBody());
      assertNotNull(node.get("Cache_Statistics_stores"));
      assertEquals("gauge", node.get("Cache_Statistics_stores").get("type").asText());
      assertEquals("stores", node.get("Cache_Statistics_stores").get("displayName").asText());
   }

   /**
    * Stream over the fields of a given JsonNode.
    */
   private static Stream<Map.Entry<String, JsonNode>> streamNodeFields(JsonNode node) {
      return StreamSupport.stream(Spliterators.spliteratorUnknownSize(node.fields(), Spliterator.IMMUTABLE), false);
   }
}
