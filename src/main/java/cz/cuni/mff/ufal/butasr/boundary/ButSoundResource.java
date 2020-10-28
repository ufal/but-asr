package cz.cuni.mff.ufal.butasr.boundary;

import com.ibm.websphere.jaxrs20.multipart.IAttachment;
import com.ibm.websphere.jaxrs20.multipart.IMultipartBody;
import cz.cuni.mff.ufal.butasr.control.ButSoundClient;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Metered;
import org.eclipse.microprofile.metrics.annotation.Timed;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutionException;

@Path("/")
public class ButSoundResource {

    @Inject
    ButSoundClient client;

    @Timed(name="asrProcessingTime")
    @Counted(name="asrAccessCount")
    @Metered(name="asrMeter")
    @Path("/asr/{lang}")
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    public String getTranscript(@PathParam("lang") String lang, IMultipartBody body) {
        return process("asr." + lang, body);
    }

    @Timed(name="lidProcessingTime")
    @Counted(name="lidAccessCount")
    @Metered(name="lidMeter")
    @Path("/lid")
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    public String lid(IMultipartBody body) {
        return process("lid", body);
    }

    @Timed(name="processProcessingTime")
    @Counted(name="processAccessCount")
    @Metered(name="processMeter")
    private String process(String serviceName, IMultipartBody body){
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
                    return client.processWav(serviceName, file).get();
                } finally {
                    if(file != null) {
                        file.toFile().delete();
                    }
                }
            }
        } catch (IOException | InterruptedException | ExecutionException e) {
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Timed(name="testProcessingTime")
    @Counted(name="testAccessCount")
    @Metered(name="testMeter")
    @GET
    @Path("/ping")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_PLAIN)
    public String ping(){
        return "pong";
    }
}
