import cz.cuni.mff.ufal.butasr.Client;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Path("asr")
public class Asr {

    @Inject
    Client client;

    @Path("{lang}")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String getTranscript(@PathParam("lang") String lang, InputStream inputStream){
        try {
            final java.nio.file.Path file = Files.createTempFile("example", "wav");
            System.out.println(file.toAbsolutePath());
            Files.copy(inputStream, file, StandardCopyOption.REPLACE_EXISTING);
            return client.getTranscriptOfWav(file).get();
        } catch (IOException | InterruptedException | ExecutionException e) {
            return "err";
        }
    }

    @GET
    @Path("/")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_PLAIN)
    public String test(){
        return "test";
    }
}
