import com.ibm.websphere.jaxrs20.multipart.IAttachment;
import com.ibm.websphere.jaxrs20.multipart.IMultipartBody;
import cz.cuni.mff.ufal.butasr.Client;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    public CompletableFuture<String> getTranscript(@PathParam("lang") String lang, IMultipartBody body){
        if(body == null){
            throw new WebApplicationException("No multipart body", Response.Status.BAD_REQUEST);
        }
        try {
            //System.out.println(file.toAbsolutePath());
            final IAttachment attachment = body.getAttachment("file");
            if(attachment == null){
                throw new WebApplicationException("The form field with file should be named 'file'," +
                        " eg. Content-Disposition: form-data; name=\"file\"; filename=\"example.wav\"",
                        Response.Status.BAD_REQUEST);
            }
            try (InputStream inputStream = attachment.getDataHandler().getInputStream()) {
                java.nio.file.Path file = null;
                try {
                    file = Files.createTempFile("asr_lid_fileupload_", null);
                    // AudioSystem has issues recognizing the wav when passed as InputStream, saving to a tmp file
                    // There'll probably be some preprocessing in the future
                    Files.copy(inputStream, file, StandardCopyOption.REPLACE_EXISTING);
                    return client.getTranscriptOfWav(file);
                } finally {
                    if(file != null) {
                        file.toFile().delete();
                    }
                }
            }
        } catch (IOException e) {
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
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
