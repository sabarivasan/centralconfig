package com.cvent.kvstore;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.jsonschema.JsonSerializableSchema;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Generates a config for a region in either YAML/JSON based on a set of keys.
 * The set of keys passed as input need not be leaf nodes. If they stop short of the leaf,
 * the entire subtree is automatically included.
 *
 * Created by sviswanathan on 2/14/16.
 */
public class ConfigGenerator {
   private KVStore kvStore;

   public ConfigGenerator(KVStore kvStore) {
      this.kvStore = kvStore;
   }

   public void generate(String region, KeySet keySet, DocumentType docType, OutputStream os) throws IOException {
      JsonFactory factory = DocumentType.YAML == docType ? new YAMLFactory() : new JsonFactory();
      JsonGenerator jg = factory.createGenerator(os);
      Iterator<String> it = keySet.iterateKeys();
      Map<String, String> keyValuesFromDb = new HashMap<>();

      // Since KeySet need not have all leaf nodes, defined, first, retrieve the hierarchies that we need.
      // At the end of this loop, we will have a map containing all leaf nodes
      while (it.hasNext()) {
         String key = it.next();
         if (!keyValuesFromDb.containsKey(key)) {
            keyValuesFromDb.putAll(kvStore.getHierarchyAt(key));
         }
      }

      // Sort the keys so they are in document order. We need this because we are going to use a
      // streaming document generator
      List<String> currentNode = null;
      jg.writeStartObject();


      for (String key : sortedKeys) {
         int writeFrom = 0;
         List<String> parts = Arrays.asList(key.split(KVStore.HIERARCHY_SEPARATOR));
         if (currentNode == null) {
            writeFrom = 0;
            currentNode = parts;
         } else {
            // Find the greatest common ancestor
            for (writeFrom = parts.size() - 1; writeFrom >= 0 && parts.subList(0, writeFrom).equals(currentNode.subList(0, writeFrom)); writeFrom--)
               ;
         }

         // Close previous intermediate levels upto greatest common ancestor

         // Write start() for all intermediate levels
         for (int l = writeFrom; l <= parts.size() - 2; l++) {
            if (parts.get(l + 1).startsWith(KVStore.ARRAY_PREFIX)) {
               jg.writeStartArray();
            }
         }

         // Write the leaf node itself
      }
      jg.writeEndObject();

   }

   // A tree representation of a Json/Yaml document with-
   // - Leaves and only leaves representing values in the form of primitive values
   // - All intermediate nodes are parents of hierarchies
   // - Each node has a data type
   // - The children of a node are ordered from left to right (does this matter (apart from for arrays)?)
   private static class TreeNode {
      private String key;
      private JsonNodeType dataType;
      private Object value;

      private List<TreeNode> children = new LinkedList<>();

      private TreeNode(String key, JsonNodeType dataType) {
         this(key, dataType, null);
      }

      private TreeNode(String key, JsonNodeType dataType, Object value) {
         this.key = key;
         this.dataType = dataType;
         this.value = value;
      }

      private static TreeNode generateTree(Map<String, String> keyValues) {
         // Sort the keys so they are in document order.
         Set<String> sortedKeys = keyValues.keySet().stream().sorted(KeySet.keySortComparator()).collect(Collectors.toSet());

         TreeNode root = createRootNode(sortedKeys);
         Map<String, TreeNode> nodesByKey = new HashMap<>();
         StringBuilder sb = new StringBuilder(64);
         for (String key : sortedKeys) {
//            List<String> parts = Arrays.asList(key.split(KVStore.HIERARCHY_SEPARATOR));
            String[] parts = key.split(KVStore.HIERARCHY_SEPARATOR);
            sb.setLength(0);
            TreeNode prev = root;
            for (int n = 0; n < parts.length; n++) {
               if (n > 0) sb.append(KVStore.HIERARCHY_SEPARATOR);
               sb.append(parts[n]);
               String nodePath = sb.toString();


               TreeNode node = nodesByKey.get(nodePath);
               if (node == null) {
                  if (n < parts.length - 1) {
                     node = new TreeNode(nodePath,
                           parts[n + 1].startsWith(KVStore.ARRAY_PREFIX) ? JsonNodeType.ARRAY : JsonNodeType.OBJECT);
                  } else {
                     String val = keyValues.get(nodePath);
                     if (val.startsWith("\"")) {
                        node = new TreeNode(nodePath, JsonNodeType.STRING, val.substring(1, val.length() - 1));
                     } else if (KVStore.BOOLEAN_VALUES.contains(val)) {
                        node = new TreeNode(nodePath, JsonNodeType.BOOLEAN, Boolean.valueOf(val));
                     } else {
                        node = new TreeNode(nodePath, JsonNodeType.NUMBER, Float.parseFloat(val));
                     }

                  }
                  nodesByKey.put(nodePath, node);
                  prev.addChild(node);
               }
               prev = node;
            }
         }
      }

      private static TreeNode createRootNode(Set<String> sortedKeys) {
         String[] parts = sortedKeys.iterator().next().split(KVStore.HIERARCHY_SEPARATOR);
         if (parts[0].startsWith(KVStore.ARRAY_PREFIX)) {
            return new TreeNode("", JsonNodeType.ARRAY);
         } else {
            return new TreeNode("", JsonNodeType.OBJECT);
         }
      }

      private void addChild(TreeNode node) {
         // Check to see if this child already exists
         for (TreeNode child: children) {
            if (node.key.equals(child.key)) {
               return;
            }
         }
         children.add(node);
      }

/*
      private static JsonNodeType deduceDataType(Map<String, String> keyValues, String[] parts, int n, String nodePath) {
         JsonNodeType dataType;
         if (n < parts.length - 1) {
            dataType = parts[n + 1].startsWith(KVStore.ARRAY_PREFIX)?JsonNodeType.ARRAY:JsonNodeType.OBJECT;
         } else {
            String val = keyValues.get(nodePath);
            if (val.startsWith("\"")) {
               dataType = JsonNodeType.STRING;
            } else if (KVStore.BOOLEAN_VALUES.contains(val)) {
               dataType = JsonNodeType.BOOLEAN;
            } else {
               dataType = JsonNodeType.NUMBER;
            }
         }
         return dataType;
      }
*/
   }
}
