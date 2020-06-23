package dev.jaaj.file.watchservice;

import dev.jaaj.event.EventInvoker;
import dev.jaaj.file.event.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.nio.file.StandardWatchEventKinds.*;

public class PathWatchService {
    private final WatchService watchService;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final HashMap<Path, WatchKey> pathWatched = new HashMap<>();
    private final Map<Path, EventInvoker<FileChangedEvent>> eventInvokersFileChanged = new HashMap<>();
    private final Map<Path, EventInvoker<FileDeletedEvent>> eventInvokersFileDeleted = new HashMap<>();
    private final Map<Path, EventInvoker<FileCreatedEvent>> eventInvokersFileCreated = new HashMap<>();

    private boolean running = false;

    public PathWatchService() throws IOException {
        watchService = FileSystems.getDefault().newWatchService();
    }

    public PathWatchService(FileSystem fileSystem) throws IOException {
        watchService = fileSystem.newWatchService();
    }

    public PathWatchService(WatchService watchService) {
        this.watchService = watchService;
    }

    private synchronized void registerIfNot(Path pathToWatch) throws IOException {
        if (!pathWatched.containsKey(pathToWatch)) {
            WatchKey key = pathToWatch.register(watchService, ENTRY_MODIFY, ENTRY_DELETE, ENTRY_CREATE);
            pathWatched.put(pathToWatch, key);
        }
    }

    private synchronized boolean unregister(Path pathToUnWatch) {
        WatchKey watchKey = pathWatched.get(pathToUnWatch);
        if (watchKey != null) {
            watchKey.cancel();
            return true;
        } else return false;
    }

    private boolean unregisterIfNoListenerLeft(Path pathToUnWatch) {
        if (eventInvokersFileCreated.get(pathToUnWatch) == null
                && eventInvokersFileDeleted.get(pathToUnWatch) == null
                && eventInvokersFileChanged.get(pathToUnWatch) == null) {
            return unregister(pathToUnWatch);
        }
        return false;
    }

    public void addListener(Path pathToWatch, FileDeletedListener fileDeletedListener) throws IOException {
        EventInvoker<FileDeletedEvent> eventInvoker = eventInvokersFileDeleted.getOrDefault(pathToWatch, new EventInvoker<>());
        eventInvoker.addListener(fileDeletedListener);
        eventInvokersFileDeleted.put(pathToWatch, eventInvoker);
        registerIfNot(pathToWatch);
    }

    public void addListener(Path pathToWatch, FileChangedListener fileChangedListener) throws IOException {
        EventInvoker<FileChangedEvent> eventInvoker = eventInvokersFileChanged.getOrDefault(pathToWatch, new EventInvoker<>());
        eventInvoker.addListener(fileChangedListener);
        eventInvokersFileChanged.put(pathToWatch, eventInvoker);
        registerIfNot(pathToWatch);
    }

    public void addListener(Path pathToWatch, FileCreatedListener fileCreatedListener) throws IOException {
        EventInvoker<FileCreatedEvent> eventInvoker = eventInvokersFileCreated.getOrDefault(pathToWatch, new EventInvoker<>());
        eventInvoker.addListener(fileCreatedListener);
        eventInvokersFileCreated.put(pathToWatch, eventInvoker);
        registerIfNot(pathToWatch);
    }

    public boolean removeListener(Path pathToWatch, FileDeletedListener fileDeletedListener) throws IOException {
        EventInvoker<FileDeletedEvent> eventInvoker = eventInvokersFileDeleted.get(pathToWatch);
        if (eventInvoker == null) {
            return false;
        }
        boolean b = eventInvoker.removeListener(fileDeletedListener);
        if (eventInvoker.hasListener()) {
            eventInvokersFileDeleted.put(pathToWatch, eventInvoker);
        } else {
            eventInvokersFileDeleted.remove(pathToWatch);
            unregisterIfNoListenerLeft(pathToWatch);
        }
        return b;
    }

    public boolean removeListener(Path pathToWatch, FileChangedListener fileChangedListener) throws IOException {
        EventInvoker<FileChangedEvent> eventInvoker = eventInvokersFileChanged.get(pathToWatch);
        if (eventInvoker == null) {
            return false;
        }
        boolean b = eventInvoker.removeListener(fileChangedListener);
        if (eventInvoker.hasListener()) {
            eventInvokersFileChanged.put(pathToWatch, eventInvoker);
        } else {
            eventInvokersFileChanged.remove(pathToWatch);
            unregisterIfNoListenerLeft(pathToWatch);
        }
        return b;
    }

    public boolean removeListener(Path pathToWatch, FileCreatedListener fileCreatedListener) throws IOException {
        EventInvoker<FileCreatedEvent> eventInvoker = eventInvokersFileCreated.get(pathToWatch);
        if (eventInvoker == null) {
            return false;
        }
        boolean b = eventInvoker.removeListener(fileCreatedListener);
        if (eventInvoker.hasListener()) {
            eventInvokersFileCreated.put(pathToWatch, eventInvoker);
        } else {
            eventInvokersFileCreated.remove(pathToWatch);
            unregisterIfNoListenerLeft(pathToWatch);
        }
        return b;
    }
}