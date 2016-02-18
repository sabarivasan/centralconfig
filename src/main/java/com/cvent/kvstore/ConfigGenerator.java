package com.cvent.kvstore;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

   public File generateToTmpFile(Document document, DocumentType docType) throws IOException {
      File tmp = File.createTempFile("out", docType.name());
      try (FileOutputStream os = new FileOutputStream(tmp)) {
          generate(document, docType, os);
          os.write("\n".getBytes());
      }
      return tmp;
   }

   public void generate(Document document, DocumentType docType, OutputStream os) throws IOException {
      Iterator<String> it = document.iterateKeys();
      Map<String, String> keyValuesFromDb = new HashMap<>();

      // Since KeySet need not have all leaf nodes, defined, first, retrieve the hierarchies that we need.
      // At the end of this loop, we will have a map containing all leaf nodes
      while (it.hasNext()) {
         String key = it.next();
         if (!keyValuesFromDb.containsKey(key)) {
            keyValuesFromDb.putAll(kvStore.getHierarchyAt(key));
         }
      }

      // Generate a tree representation of the set of keys requested
      TreeNode root = TreeNode.generateTree(keyValuesFromDb);

      // Traverse the tree from left to right of each node to generate the config
      JsonFactory factory = DocumentType.YAML == docType ? new YAMLFactory() : new JsonFactory();
      JsonGenerator jg = factory.createGenerator(os);
      traverseTree(root, jg);
   }

   private void traverseTree(TreeNode node, JsonGenerator jg) throws IOException {
      if (node.key.length() > 0 && node.parent.dataType != JsonNodeType.ARRAY) {
         jg.writeFieldName(node.propertyName);
      }
      switch (node.dataType) {
         case OBJECT: jg.writeStartObject(); break;
         case ARRAY: jg.writeStartArray(); break;
         case STRING: jg.writeString((String) node.value); break;
         case BOOLEAN: jg.writeBoolean((Boolean) node.value);  break;
         case NUMBER: jg.writeNumber((Float) node.value); break;
      }

      jg.flush();
      for (TreeNode child: node.children) {
         traverseTree(child, jg);
      }

      switch (node.dataType) {
         case OBJECT: jg.writeEndObject(); break;
         case ARRAY: jg.writeEndArray(); break;
      }
      jg.flush();
   }

   // A tree representation of a Json/Yaml document with:
   // - Leaves and only leaves representing values in the form of primitive values
   // - All intermediate nodes are parents of hierarchies
   // - Each node has a data type
   // - The children of a node are ordered from left to right (does this matter (apart from for arrays)?)
   private static class TreeNode {
      private String propertyName; // Represents the property name of parent
      private String key;
      private JsonNodeType dataType;
      private Object value;
      private TreeNode parent;
      private List<TreeNode> children = new LinkedList<>();

      private TreeNode(String key, JsonNodeType dataType, String propertyName) {
         this(key, dataType, propertyName, null);
      }

      private TreeNode(String key, JsonNodeType dataType, String propertyName, Object value) {
         this.propertyName = propertyName;
         this.key = key;
         this.dataType = dataType;
         this.value = value;
      }

      private static TreeNode generateTree(Map<String, String> keyValues) {
         // Sort the keys so they are in document order.
         Set<String> sortedKeys = Document.sort(keyValues.keySet());

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
                           parts[n + 1].startsWith(KVStore.ARRAY_PREFIX) ? JsonNodeType.ARRAY : JsonNodeType.OBJECT,
                           parts[n]);
                  } else {
                     String val = keyValues.get(nodePath);
                     if (val.startsWith("\"")) {
                        node = new TreeNode(nodePath, JsonNodeType.STRING, parts[n], val.substring(1, val.length() - 1));
                     } else if (KVStore.BOOLEAN_VALUES.contains(val)) {
                        node = new TreeNode(nodePath, JsonNodeType.BOOLEAN, parts[n], Boolean.valueOf(val));
                     } else {
                        node = new TreeNode(nodePath, JsonNodeType.NUMBER, parts[n], Float.parseFloat(val));
                     }

                  }
                  nodesByKey.put(nodePath, node);
                  node.setParent(prev);
                  prev.addChild(node);
               }
               prev = node;
            }
         }
         return root;
      }

      private static TreeNode createRootNode(Set<String> sortedKeys) {
         String[] parts = sortedKeys.iterator().next().split(KVStore.HIERARCHY_SEPARATOR);
         if (parts[0].startsWith(KVStore.ARRAY_PREFIX)) {
            return new TreeNode("", JsonNodeType.ARRAY, null);
         } else {
            return new TreeNode("", JsonNodeType.OBJECT, null);
         }
      }

      public void setParent(TreeNode parent) {
         this.parent = parent;
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

   }
}
