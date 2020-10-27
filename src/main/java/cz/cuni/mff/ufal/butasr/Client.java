package cz.cuni.mff.ufal.butasr;

import javax.annotation.PreDestroy;
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
import java.util.concurrent.*;
import javax.annotation.PostConstruct;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class Client {
    @Resource
    private ManagedExecutorService executorService;

    @Inject
    @ConfigProperty(name="but.asr.host", defaultValue = "localhost")
    private String host;

    @Inject
    @ConfigProperty(name="but.asr.port", defaultValue = "5050")
    private int port;

    private Socket socket;
    private OutputStream outputStream;
    private InputStream inputStream;

    @PostConstruct
    private void open(){
        try {
            socket = new Socket(host, port);
            outputStream = socket.getOutputStream();
            inputStream = socket.getInputStream();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @PreDestroy
    private void close(){
        if(socket != null){
            try {
                socket.close();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public CompletableFuture<String> getTranscriptOfWav(Path path) {
        return getTranscriptOfWav(path.toFile());
    }

    public CompletableFuture<String> getTranscriptOfWav(File file){
        if(socket.isConnected()){
            open();
        }
        final CompletableFuture<Void> sendAudio = CompletableFuture.runAsync(() -> {
            try(final AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file)){
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
        return CompletableFuture.allOf(sendAudio, receivedTranscript).thenApply( ignored -> receivedTranscript.join());
    }
}
