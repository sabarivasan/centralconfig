package com.cvent.kvstore;

import com.cvent.kvstore.consul.ConsulKVDaoEcwid;
import com.cvent.kvstore.dw.ConsulKVStoreConfig;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Test for ConsulKVStore
 * Created by sviswanathan on 2/10/16.
 */
public class KVStoreTest {
   private static ConsulKVStoreConfig config;
   private static KVSStoreDao dao;
   private static KVStore defaultKVStore;
   private static KVStore kvStore;
   private static String region;
   private static String author;
   private static Random random = new Random();

   @BeforeClass
   public static void setup() {
      config = new ConsulKVStoreConfig();
      config.setConsulEndpoint("dev-wiz-01:8500");
      region = "test-" + random.nextLong();
      dao = new ConsulKVDaoEcwid(config);
//      kvStore = new ConsulKVStoreOrbitz(region, config);
      defaultKVStore = SimpleKVStore.kvStoreForDefaultRegion(dao);
      author = randomString("author-", "");
      kvStore = SimpleKVStore.forRegion(region, dao);
   }

   @Before
   public void before() {
      defaultKVStore.destroy();
      kvStore.destroy();
   }

   //@After
   public void teardown() {
      defaultKVStore.destroy();
      kvStore.destroy();
   }

   private static String randomString(String prefix, String suffix) {
      return prefix + random.nextLong() + suffix;
   }

   private static KeyValue randomKeyValue() {
      return KeyValue.from(randomString("key", ""), randomString("val", ""));
   }

   @Test
   public void testWriteRead() throws KVStoreException {
      KeyValue kv1 = randomKeyValue();
      // Have to insert value into default region first
      boolean exceptionThrown = false;
      try {
         kvStore.put(kv1, "sabari", false);
      } catch (KVStoreException e) {
         TestCase.assertEquals(KVStoreException.Reason.KEY_ABSENT_IN_DEFAULT, e.getReason());
         exceptionThrown = true;
      }
      TestCase.assertTrue(exceptionThrown);

      // Insert value into default region alone
      defaultKVStore.put(kv1, "sabari", false);
      TestCase.assertEquals(kv1.value(), kvStore.getValueAt(kv1.key()).get());
      TestCase.assertEquals(kv1.value(), defaultKVStore.getValueAt(kv1.key()).get());

      // Insert same value into actual region without forcing (should be no-op)
      kvStore.put(kv1, "sabari", false);
      TestCase.assertEquals(kv1.value(), kvStore.getValueAt(kv1.key()).get());
      TestCase.assertEquals(kv1.value(), defaultKVStore.getValueAt(kv1.key()).get());

      // Insert different value into actual region without forcing (should be no-op)
      KeyValue kv2 = KeyValue.from(kv1.key(), randomString("val", ""));
      kvStore.put(kv2, "sabari", false);
      TestCase.assertEquals(kv2.value(), kvStore.getValueAt(kv1.key()).get());
      TestCase.assertEquals(kv1.value(), defaultKVStore.getValueAt(kv1.key()).get());

      kvStore.destroy();
      TestCase.assertEquals(kv1.value(), defaultKVStore.getValueAt(kv1.key()).get());
      defaultKVStore.destroy();
      TestCase.assertFalse(defaultKVStore.getValueAt(kv1.key()).isPresent());
   }

   @Test
   public void testCanonical() throws IOException {
      Map<String, KVStore> kvStoreProvider = new HashMap<String, KVStore>() {{
         put(KVStore.DEFAULT_REGION, defaultKVStore);
         put("region1", kvStore);
         put(kvStore.region(), kvStore);
      }};
      Map<String, Map<String, String>> keyValuesByRegion = createConicalData(kvStoreProvider, "canonical_doc.properties");


      Map<String, String> defaultKVs = keyValuesByRegion.get(KVStore.DEFAULT_REGION);
      keyValuesByRegion.keySet().forEach(region -> {
         KVStore kvStore = kvStoreProvider.get(region);
         Map<String, String> regionKVs = keyValuesByRegion.get(region);

         Set<String> hierarchiesTested = new HashSet<>();
         defaultKVs.keySet().forEach(k -> {
            // Test getValueAt()
            TestCase.assertEquals(regionKVs.containsKey(k) ? regionKVs.get(k) : defaultKVs.get(k),
                  kvStore.getValueAt(k).get());

            // Test getHierarchyAt()
            Set<String> hierarchiesToTest = generateHierarchies(k, hierarchiesTested);
            for (String key : hierarchiesToTest) {
               Set<String> children = defaultKVs.keySet().stream().filter(k2 -> k2.startsWith(key)).collect(Collectors.toSet());
               Map<String, String> actualHierarchy = kvStore.getHierarchyAt(key);
               TestCase.assertEquals(children, actualHierarchy.keySet());
               for (String k2 : children) {
                  String expected = regionKVs.containsKey(k2) ? regionKVs.get(k2) : defaultKVs.get(k2);
                  TestCase.assertEquals(expected, actualHierarchy.get(k2));
               }
            }
            hierarchiesTested.addAll(hierarchiesToTest);
         });
      });
   }

   @Test
   public void testConfigGenerator() throws IOException {
      Map<String, KVStore> kvStoreProvider = new HashMap<String, KVStore>() {{
         put(KVStore.DEFAULT_REGION, defaultKVStore);
         put("region1", kvStore);
         put(kvStore.region(), kvStore);
      }};
      Map<String, Map<String, String>> keyValuesByRegion = createConicalData(kvStoreProvider, "canonical_doc.properties");

      ConfigGenerator configGenerator = new ConfigGenerator(defaultKVStore);
      KeySet keySet = TemplateToKeyset.templateToKeySet(new File("/Users/sviswanathan/work/projects/CentralConfig/CentralConfig/canonical.json"));
      configGenerator.generate(keySet, DocumentType.JSON, new FileOutputStream("/Users/sviswanathan/work/projects/CentralConfig/CentralConfig/testout.json"));
      configGenerator.generate(keySet, DocumentType.YAML, new FileOutputStream("/Users/sviswanathan/work/projects/CentralConfig/CentralConfig/testout.yaml"));
   }

   @Test
   public void testConfigGeneratorAuth() throws IOException {

      KVStore p2KVStore = SimpleKVStore.forRegion("p2", dao);
      KVStore region1KVStore = SimpleKVStore.forRegion("region1", dao);
      try {
         Map<String, KVStore> kvStoreProvider = new HashMap<String, KVStore>() {{
            put(KVStore.DEFAULT_REGION, defaultKVStore);
            put("region1", region1KVStore);
            put(region1KVStore.region(), region1KVStore);
//         put("alpha", alphaKVStore);
//         put(alphaKVStore.region(), alphaKVStore);
            put("p2", p2KVStore);
            put(p2KVStore.region(), p2KVStore);
         }};
         createConicalData(kvStoreProvider, "canonical_doc.properties");
         createConicalData(kvStoreProvider, "canonical_doc_auth.properties");

//      ConfigGenerator configGenerator = new ConfigGenerator(defaultKVStore);
//      KeySet keySet = TemplateToKeyset.templateToKeySet(new File("/Users/sviswanathan/work/projects/CentralConfig/CentralConfig/canonical.json"));
//      configGenerator.generate(keySet, DocumentType.JSON, new FileOutputStream("/Users/sviswanathan/work/projects/CentralConfig/CentralConfig/testout.json"));
//      configGenerator.generate(keySet, DocumentType.YAML, new FileOutputStream("/Users/sviswanathan/work/projects/CentralConfig/CentralConfig/testout.yaml"));

      } finally {
//         p2KVStore.destroy();
//         region1KVStore.destroy();
      }
   }

   private Set<String> generateHierarchies(String key, Set<String> hierarchiesTested) {
      String[] parts = key.split(KVStore.HIERARCHY_SEPARATOR);
      StringBuilder sb = new StringBuilder(64);
      Set<String> toTest = new HashSet<>();
      for (int n = 0; n < parts.length; n++) {
         if (n > 0) sb.append(KVStore.HIERARCHY_SEPARATOR);
         sb.append(parts[n]);
         String fullPath = sb.toString();
         if (!hierarchiesTested.contains(fullPath)) {
            toTest.add(fullPath);
         }
      }
      return toTest;
   }


   private Map<String, Map<String, String>> createConicalData(Map<String, KVStore> kvStoreProvider, String canonicalDataFile) throws IOException {
      Map<String, Map<String, String>> keyValuesByRegion = new HashMap<>();
      Properties props = new Properties();
      props.load(this.getClass().getResourceAsStream(canonicalDataFile));
      // Makes the assumption that the default region appears before other regions
      props.stringPropertyNames().stream().sorted().forEach(k -> {
         try {
            int ind = k.indexOf("/");
            KVStore kvStore = kvStoreProvider.get(k.substring(0, ind));
            String value = props.getProperty(k);
            kvStore.put(k.substring(ind + 1), value, author, false);
            if (!keyValuesByRegion.containsKey(kvStore.region())) {
               keyValuesByRegion.put(kvStore.region(), new HashMap<>());
            }
            keyValuesByRegion.get(kvStore.region()).put(k.substring(ind + 1), value);
         } catch (KVStoreException e) {
            throw new RuntimeException("Could not write to kv store", e);
         }
      });
      return keyValuesByRegion;
   }

}
