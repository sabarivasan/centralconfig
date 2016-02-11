package com.cvent.kvstore.consul;

import com.cvent.kvstore.KVSStoreDao;
import com.cvent.kvstore.KVStoreException;
import com.cvent.kvstore.KeyValue;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.kv.model.GetValue;
import com.google.common.base.Optional;
import com.sun.jersey.core.util.Base64;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A Consul client from Ecwid
 * https://github.com/Ecwid/consul-api
 *
 * Created by sviswanathan on 2/11/16.
 */
public class ConsulKVDaoEcwid implements KVSStoreDao {
   private ConsulKVStoreConfig config;
   private ConsulClient client;

   public ConsulKVDaoEcwid(ConsulKVStoreConfig config) {
      this.config = config;
      int ind = config.getConsulEndpoint().lastIndexOf(":");
      client = new ConsulClient(config.getConsulEndpoint().substring(0, ind),
                     Integer.valueOf(config.getConsulEndpoint().substring(ind + 1)));
   }

   @Override
   public void put(String key, String value) throws KVStoreException {
      if (!client.setKVValue(key, value).getValue()) {
         throw KVStoreException.writeFailed(key);
      }
   }

   @Override
   public Optional<String> getValueAt(String key) {
      Response<GetValue> val = client.getKVValue(key);
      return val.getValue() != null?Optional.of(Base64.base64Decode(val.getValue().getValue())):Optional.absent();
   }

   @Override
   public Optional<Collection<KeyValue>> getHierarchyAt(String key) {
      Response<List<GetValue>> vals = client.getKVValues(key);
      if (vals.getValue() != null) {
         return Optional.of(vals.getValue().stream().map(gv -> KeyValue.from(gv.getKey(), gv.getValue())).collect(Collectors.toList()));
      } else {
         return Optional.absent();
      }
   }

   @Override
   public Map<String, String> getHierarchyAsMap(String key) {
      Response<List<GetValue>> vals = client.getKVValues(key);
      if (vals.getValue() != null) {
         return vals.getValue().stream().collect(Collectors.toMap(GetValue::getKey, GetValue::getValue));
      } else {
         return Collections.emptyMap();
      }
   }

   @Override
   public Optional<Collection<String>> getKeysAt(String key) {
      Response<List<String>> keys = client.getKVKeysOnly(key);
      return keys.getValue() != null?Optional.of(keys.getValue()):Optional.absent();
   }

   @Override
   public void deleteKey(String key) {
      client.deleteKVValue(key);
   }

   @Override
   public void deleteHierarchyAt(String key) {
      client.deleteKVValues(key);
   }


}
