package cz.cuni.mff.ufal.butasr.control;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.net.Socket;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.concurrent.*;
import java.util.logging.Logger;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ButSoundClient {

    private static Logger log = Logger.getLogger(ButSoundClient.class.getName());

    @Resource
    private ManagedExecutorService executorService;

    @Inject
    Config config;

    @Inject
    @ConfigProperty(name="but.asr.default.host")
    private String defaultHost;

    @Inject
    @ConfigProperty(name="but.asr.default.port")
    private int defaultPort;

    private Socket open(String serviceName){
        int port;
        String host;
        try {
            port = config.getValue("but." + serviceName + ".port", Integer.class);
            host = config.getValue("but." + serviceName + ".host", String.class);
        }catch(NoSuchElementException e){
            host = defaultHost;
            port = defaultPort;
        }
        try {
           return new Socket(host, port);
        } catch (IOException e) {
            log.severe(String.format("Failed to open socket to '%s:%s'", host, port));
            throw new IllegalStateException(e);
        }
    }

    public CompletableFuture<String> processWav(String serviceName, Path path) {
        return processWav(serviceName, path.toFile());
    }

    public CompletableFuture<String> processWav(String serviceName, File file){
        OutputStream outputStream;
        InputStream inputStream;
        Socket socket = open(serviceName);
        try {
            outputStream = socket.getOutputStream();
            inputStream = socket.getInputStream();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        final CompletableFuture<Void> sendAudio = CompletableFuture.runAsync(() -> {
            try(final AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
            ){
                audioInputStream.transferTo(outputStream);
            } catch (IOException | UnsupportedAudioFileException e) {
                throw new IllegalStateException(e);
            }
        }, executorService);

        final CompletableFuture<String> receivedTranscript = CompletableFuture.supplyAsync(() -> {
            try {
                return new String(inputStream.readAllBytes()).replaceAll("[^\n]*\\r", "");
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            //input.transferTo(System.out);
        }, executorService);
        return CompletableFuture.allOf(sendAudio, receivedTranscript).thenApply( ignored -> {
            String transcript = receivedTranscript.join();
            try {
                socket.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return transcript;
        });
    }
}
