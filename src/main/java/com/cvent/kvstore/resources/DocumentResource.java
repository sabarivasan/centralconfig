package com.cvent.kvstore.resources;

import com.cvent.kvstore.ConfigGenerator;
import com.cvent.kvstore.Document;
import com.cvent.kvstore.DocumentType;
import com.cvent.kvstore.KVStore;
import com.cvent.kvstore.KVStoreException;
import com.cvent.kvstore.SimpleKVStore;
import com.cvent.kvstore.TemplateToDocument;
import com.cvent.kvstore.consul.ConsulKVDaoEcwid;
import com.cvent.kvstore.dw.ConsulKVStoreConfig;
import com.google.common.base.Optional;
import org.apache.commons.io.IOUtils;
import org.hibernate.validator.constraints.NotEmpty;
import retrofit.http.Body;

import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Random;

/**
 * CRUD operations for a document.
 * A document is just a KeySet.
 * <p/>
 * Created by sviswanathan on 2/15/16.
 */

@Path("/document")
@Consumes({MediaType.APPLICATION_JSON, "text/yaml"})
@Produces({MediaType.APPLICATION_JSON, "text/yaml"})
public class DocumentResource {

    private ConsulKVStoreConfig config;
    private KVStore docKVStore;

    public DocumentResource(ConsulKVStoreConfig config) {
        this.config = config;
        docKVStore = SimpleKVStore.forRegion(KVStore.DOCUMENT_REGION, new ConsulKVDaoEcwid(config));
    }

    @POST
    @Path("/{name}")
    /**
     * Can create a document by passing in the body a yaml or json file
     */
    public Response createDoc(@NotNull @PathParam("name") String name,
                              @NotNull @Body InputStream file,
                              @NotNull @NotEmpty @QueryParam("author") String author,
                              @NotNull @HeaderParam("Content-Type") String contentType) throws IOException, KVStoreException {

        String key = name;
        if (docKVStore.getValueAt(key).isPresent()) {
            throw new IllegalArgumentException(String.format("Document named %s already exists. Did you mean to use PUT to update it?", name));
        }
        if (file.available() == 0) {
            throw new IllegalArgumentException("Request Body is empty");
        }

        DocumentType docType;
        if (MediaType.APPLICATION_JSON.equals(contentType)) {
            docType = DocumentType.JSON;
        } else {
            docType = DocumentType.YAML;
        }
        Document document = TemplateToDocument.from(file, docType);
        docKVStore.put(key, document.serialize(), author, true);
        return Response.ok().build();
    }

    @GET
    @Path("{name}")
    public Response getDoc(@NotNull @PathParam("name") String name) {
        Optional<String> serializedDoc = docKVStore.getValueAt(name);
        if (!serializedDoc.isPresent()) {
//         throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(Document.deserialize(serializedDoc.get()).keys()).build();
    }

    @POST
    @Path("/{name}/checkout")
    public Response checkout(@NotNull @PathParam("name") String name,
                             @NotNull @NotEmpty @QueryParam("author") String author,
                             @NotNull @NotEmpty @QueryParam("region") String region) throws IOException {
        if (region == null || author == null) {
            throw new IllegalArgumentException("author and region are required");
        }
        Optional<String> serializedDoc = docKVStore.getValueAt(name);
        if (!serializedDoc.isPresent()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        Document document = Document.deserialize(serializedDoc.get());
        ConfigGenerator configGenerator = new ConfigGenerator(SimpleKVStore.forRegion(region,
              new ConsulKVDaoEcwid(config)));

        String stashRepoPath = "/Users/sviswanathan/work/projects/CentralConfig/centralconfigchanges";
        File out = new File(stashRepoPath,
              String.format("%s-%s-%d.yaml", name, region, new Random().nextInt(1000)));
        try (FileOutputStream os = new FileOutputStream(out)) {
            configGenerator.generate(document, DocumentType.YAML, os);
        }

        String branch = String.format("%s-%s-%s-%d", author, name, region, System.currentTimeMillis());
        createBranch(stashRepoPath, branch);

        add(stashRepoPath, out.getName());

        String commitMessage = String.format("Config changes for %s-%s", name, region);
        commit(stashRepoPath, commitMessage);

        push(stashRepoPath, branch);
//        String pullRequestOutput = createPullRequest(stashRepoPath, branch, commitMessage);

        String url = "https://stash/users/sviswanathan/repos/centralconfigchanges/browse?at=refs%2Fheads%2F" + branch + "\n";
        return Response.ok(url).build();
    }

    private static String add(String stashRepoPath, String filePath) throws IOException {
        String[] command = {"git", "add", filePath};
        return executeCommand(stashRepoPath, command);
    }

    private static String commit(String stashRepoPath, String message) throws IOException {
        String[] command = {"git", "commit", "-m", "\"" + message + "\""};
        return executeCommand(stashRepoPath, command);
    }

    private static String createBranch(String stashRepoPath, String branch) throws IOException {
        String[] command = {"git", "checkout", "-b", branch};
        return executeCommand(stashRepoPath, command);
    }

    private static String push(String stashRepoPath, String branch) throws IOException {
        String[] command = {"git", "push", "-u", "origin", branch};
        return executeCommand(stashRepoPath, command);
    }

    private static String createPullRequest(String stashRepoPath, String branch, String title) throws IOException {
        String[] command = {"stash", "pull-request", branch, "master", "-T", String.format("\"%s\"", title)};
        return executeCommand(stashRepoPath, command);
    }

    private static String executeCommand(String stashRepoPath, String[] command) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(stashRepoPath));
        pb.redirectOutput(new File("stash_output.log"));
        pb.redirectError(new File("stash_error.log"));
        Process process = pb.start();

        try {
            process.waitFor();
        } catch (InterruptedException e) {

        }

        System.out.println("command " + Arrays.toString(command) + " exit value = " + process.exitValue());
        if (process.exitValue() != 0) {
            throw new RuntimeException("command " + Arrays.toString(command) + " failed ");
        }

        return IOUtils.toString(new FileInputStream("stash_output.log"));
    }


//    StashMagic client =
//          new RetrofitClientProvider<>("https://stash/rest/api/1.0/projects/~sviswanathan/repos/centralconfigchanges",
//                StashMagic.class,
//                new JacksonConverter(JsonSerializer.getObjectMapper())).getClient();
//    String branches = client.getBranches();

    private interface StashMagic {

        @retrofit.http.GET("/branches")
        String getBranches();

    }

    public static void main(String[] args) throws IOException {
        String stashRepoPath = "/Users/sviswanathan/work/projects/CentralConfig/centralconfigchanges";

        String branch = "blahblahblah" + System.currentTimeMillis();
        System.out.println(createBranch(stashRepoPath, branch));

        add(stashRepoPath, "auth-region1.yaml");

        String commitMessage = String.format("Config changes for %s-%s", "auth", "region1");
        System.out.println(commit(stashRepoPath, commitMessage));

        System.out.println(push(stashRepoPath, branch));
    }


}
