package com.cvent.kvstore;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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
   private File document;
   private DocumentType docType;
   private JsonNode rootNode;

   private TemplateToKeyset(File document) throws IOException {
      this.document = document;
      docType = document.getName().endsWith("yaml")?DocumentType.YAML:DocumentType.JSON;
      ObjectMapper mapper = new ObjectMapper(docType.isYAML()?new YAMLFactory():new JsonFactory());
      rootNode = mapper.readValue(document, JsonNode.class);
   }

   private TemplateToKeyset(InputStream is, DocumentType docType) throws IOException {
      this.docType = docType;
      ObjectMapper mapper = new ObjectMapper(docType.isYAML()?new YAMLFactory():new JsonFactory());
      rootNode = mapper.readValue(is, JsonNode.class);
   }

   private KeySet generate() throws IOException {
      Set<String> keySet = new HashSet<>();
      visit("", rootNode, keySet, "");
      return KeySet.from(keySet);
   }

   public static KeySet from(InputStream is, DocumentType documentType) throws IOException {
      TemplateToKeyset toKeyset = new TemplateToKeyset(is, documentType);
      return toKeyset.generate();
   }

   public static KeySet from(File document) throws IOException {
      TemplateToKeyset toKeyset = new TemplateToKeyset(document);
      return toKeyset.generate();
   }


   // Each node is responsible for:
   // - printing its own information
   // - determining its children and calling visit for each child and passing
   //    - the child's name (since we cannot get that from JsonNode), the path INCLUDING the child's name
   private void visit(String parentName, JsonNode parent, Set<String> keySet, String path) throws IOException {
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
               visit(name, node, keySet, path + KVStore.HIERARCHY_SEPARATOR + name);
               break;

            case ARRAY:
               for (int n = 0; n < node.size(); n++) {
                  visit("..." + n, node.get(n), keySet, path + KVStore.HIERARCHY_SEPARATOR + name + KVStore.HIERARCHY_SEPARATOR + "..." + n);
               }
               break;

            case STRING:
               if (PARENT_CONFIG_FILE_PROP_NAME.equals(name)) {
                  if (document == null) {
                     throw new IllegalArgumentException("Cannot resolve parentConfigurationFile");
                  }
                  File parentRef = new File(document.getParentFile(), node.asText());
                  if (!parentRef.exists()) {
                     parentRef = new File(document.getParentFile().getParentFile(), node.asText());
                  }
                  if (!parentRef.exists()) {
                     throw new FileNotFoundException(name);
                  }
                  keySet.addAll(from(parentRef).keys());
                  break;
               }
            case NUMBER:
            case NULL:
            case BOOLEAN:
            case BINARY:
               visit(name, node, keySet, path + KVStore.HIERARCHY_SEPARATOR + name);
               break;

         }

      }
   }

   public static void main(String[] args) throws IOException {
//      TemplateToKeyset.from(new File("/Users/sviswanathan/work/projects/LearnHogan/auth-service/auth-service-web/configs/alpha.yaml"));
      TemplateToKeyset.from(new File("/Users/sviswanathan/work/projects/CentralConfig/CentralConfig/canonical.json"));
   }

}
