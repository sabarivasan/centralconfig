package com.cvent.kvstore.consul;

import com.cvent.kvstore.KVSStoreDao;
import com.cvent.kvstore.KVStore;
import com.cvent.kvstore.KVStoreException;
import com.cvent.kvstore.KeyValue;
import com.google.common.base.Optional;
import junit.framework.TestCase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Random;

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
   private static Random random = new Random();

   @BeforeClass
   public static void setup() {
      config = new ConsulKVStoreConfig();
      config.setConsulEndpoint("dev-wiz-01:8500");
      region = "test-" + random.nextLong();
      dao = new ConsulKVDaoEcwid(config);
//      kvStore = new ConsulKVStoreOrbitz(region, config);
      defaultKVStore = SimpleKVStore.kvStoreForDefaultRegion(dao);
      kvStore = new SimpleKVStore(region, dao);
   }

   @AfterClass
   public static void teardown() {
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
   public void testHierarchy() {

   }

}
