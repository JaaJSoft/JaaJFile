package dev.jaaj.file.watchservice;

import dev.jaaj.file.event.FileChangedListener;
import dev.jaaj.file.event.FileCreatedListener;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class PathWatchServiceTest {
    private final PathWatchService pathWatchService = new PathWatchService();
    private boolean triggerCreated;
    private boolean triggerModified;
    private boolean triggerDeleted;
    private final Thread thread;

    public PathWatchServiceTest() throws IOException {
        thread = new Thread(pathWatchService);

    }

    @Before
    public void setUp() throws Exception {
        thread.start();
    }

    @After
    public void tearDown() throws Exception {
        thread.interrupt();
    }

    @Test
    public void testFileCreated() throws IOException {
        triggerCreated = false;
        FileCreatedListener fileCreatedListener = event -> {
            System.out.println("fileCreated : " + event.getSource());
            triggerCreated = true;
        };
        pathWatchService.addListener(Path.of("."), fileCreatedListener);
        Files.createFile(Path.of("./test"));
        pathWatchService.removeListener(Path.of("."), fileCreatedListener);
        Files.delete(Path.of("./test"));
        assertTrue(triggerCreated);
    }

    @Test
    public void testFileModified() throws IOException {
        triggerCreated = false;
        FileChangedListener fileChangedListener = event -> {
            System.out.println("fileChanged : " + event.getSource());
            triggerModified = true;
        };
        Path dir = Path.of("./");
        Path modifiedFile = Path.of("./file");
        Files.createFile(modifiedFile);
        pathWatchService.addListener(modifiedFile, fileChangedListener);
        BufferedWriter writer = Files.newBufferedWriter(modifiedFile);
        writer.write("AAAAAAAAAAAAA");
        writer.close();
        pathWatchService.removeListener(modifiedFile, fileChangedListener);
        Files.delete(modifiedFile);
        assertTrue(triggerModified);
    }


    @Test
    public void test() throws IOException {
        FileCreatedListener fileCreatedListener = event -> {
            System.out.println("fileCreated : " + event.getSource());
            triggerCreated = true;
        };
        pathWatchService.addListener(Path.of("./test"), fileCreatedListener);

        while (true) ;
    }
}
