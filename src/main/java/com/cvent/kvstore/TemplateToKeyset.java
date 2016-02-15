package com.cvent.kvstore;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This class is responsible for analyzing a template JSON/YAML document
 * and deriving at the optimal set of keys required for representing the document.
 *
 * Created by sviswanathan on 2/12/16.
 */
public class TemplateToKeyset {

   public static final String PARENT_CONFIG_FILE_PROP_NAME = "parentConfigurationFile";

   public static KeySet templateToKeySet(File document) throws IOException {
      ObjectMapper mapper = new ObjectMapper(document.getName().endsWith("yaml")?new YAMLFactory():new JsonFactory());
      JsonNode rootNode = mapper.readValue(document, JsonNode.class);

      Set<String> keySet = new HashSet<>();
      visit("", rootNode, keySet, "", document);

      return KeySet.from(keySet);
   }


   // Each node is responsible for:
   // - printing its own information
   // - determining its children and calling visit for each child and passing
   //    - the child's name (since we cannot get that from JsonNode), the path INCLUDING the child's name
   private static void visit(String parentName, JsonNode parent, Set<String> keySet, String path, File parentDoc) throws IOException {
      if (parentName.length() > 0) {
         // Ignore root node
//         System.out.println(path + ":" + parent.getNodeType());
         keySet.add(path);
      }
      Iterator<Map.Entry<String, JsonNode>> it = parent.fields();
      while (it.hasNext()) {
         Map.Entry<String, JsonNode> entry = it.next();
         String name = entry.getKey();
         JsonNode node = entry.getValue();
         switch (node.getNodeType()) {
            case OBJECT:
               visit(name, node, keySet, path + KVStore.HIERARCHY_SEPARATOR + name, parentDoc);
               break;

            case ARRAY:
               for (int n = 0; n < node.size(); n++) {
                  visit("..." + n, node.get(n), keySet, path + KVStore.HIERARCHY_SEPARATOR + name + KVStore.HIERARCHY_SEPARATOR + "..." + n, parentDoc);
               }
               break;

            case STRING:
               if (PARENT_CONFIG_FILE_PROP_NAME.equals(name)) {
                  File parentRef = new File(parentDoc.getParentFile(), node.asText());
                  if (!parentRef.exists()) {
                     parentRef = new File(parentDoc.getParentFile().getParentFile(), node.asText());
                  }
                  if (!parentRef.exists()) {
                     throw new FileNotFoundException(name);
                  }
                  keySet.addAll(templateToKeySet(parentRef).keys());
                  break;
               }
            case NUMBER:
            case NULL:
            case BOOLEAN:
            case BINARY:
               visit(name, node, keySet, path + KVStore.HIERARCHY_SEPARATOR + name, parentDoc);
               break;

         }

      }
   }

   public static void main(String[] args) throws IOException {
//      TemplateToKeyset.templateToKeySet(new File("/Users/sviswanathan/work/projects/LearnHogan/auth-service/auth-service-web/configs/alpha.yaml"));
      TemplateToKeyset.templateToKeySet(new File("/Users/sviswanathan/work/projects/CentralConfig/CentralConfig/canonical.json"));
   }

}
