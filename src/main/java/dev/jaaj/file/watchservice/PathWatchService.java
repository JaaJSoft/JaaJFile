package dev.jaaj.file.watchservice;

import dev.jaaj.event.EventInvoker;
import dev.jaaj.file.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.StandardWatchEventKinds.*;

public class PathWatchService implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(PathWatchService.class);

    private final WatchService watchService;

    private final HashMap<Path, WatchKey> pathWatched = new HashMap<>();
    private final Map<Path, EventInvoker<FileChangedEvent>> eventInvokersFileChanged = new HashMap<>();
    private final Map<Path, EventInvoker<FileDeletedEvent>> eventInvokersFileDeleted = new HashMap<>();
    private final Map<Path, EventInvoker<FileCreatedEvent>> eventInvokersFileCreated = new HashMap<>();

    public PathWatchService() throws IOException {
        watchService = FileSystems.getDefault().newWatchService();
    }

    public PathWatchService(FileSystem fileSystem) throws IOException {
        watchService = fileSystem.newWatchService();
    }

    public PathWatchService(WatchService watchService) {
        this.watchService = watchService;
    }

    @Override
    public void run() {
        WatchKey key;
        while (!Thread.currentThread().isInterrupted()) {
            try {
                if ((key = watchService.take()) != null) {
                    for (WatchEvent<?> event : key.pollEvents()) {
                        //because the path returned by context is broken
                        Path file = ((Path) key.watchable()).resolve(((WatchEvent<Path>) event).context());
                        Path parentFile = file.getParent();
                        if (event.kind() == ENTRY_MODIFY) {
                            EventInvoker<FileChangedEvent> dirEventInvoker = eventInvokersFileChanged.get(parentFile);
                            logger.info("MODIFY -> " + dirEventInvoker);
                            if (dirEventInvoker != null) {
                                dirEventInvoker.invoke(new FileChangedEvent(file));
                            }
                            EventInvoker<FileChangedEvent> fileEventInvoker = eventInvokersFileChanged.get(file);
                            logger.info("MODIFY -> " + fileEventInvoker);
                            if (fileEventInvoker != null) {
                                fileEventInvoker.invoke(new FileChangedEvent(file));
                            }
                        } else if (event.kind() == ENTRY_DELETE) {
                            EventInvoker<FileDeletedEvent> dirEventInvoker = eventInvokersFileDeleted.get(parentFile);
                            logger.info("DELETE -> " + dirEventInvoker);
                            if (dirEventInvoker != null) {
                                dirEventInvoker.invoke(new FileDeletedEvent(file));
                                //unregister(file);
                            }
                            EventInvoker<FileDeletedEvent> fileEventInvoker = eventInvokersFileDeleted.get(file);
                            logger.info("DELETE -> " + fileEventInvoker);
                            if (fileEventInvoker != null) {
                                fileEventInvoker.invoke(new FileDeletedEvent(file));
                                //unregister(file);
                            }
                        } else if (event.kind() == ENTRY_CREATE) {
                            EventInvoker<FileCreatedEvent> dirEventInvoker = eventInvokersFileCreated.get(parentFile);
                            logger.info("CREATE -> " + dirEventInvoker);
                            if (dirEventInvoker != null) {
                                dirEventInvoker.invoke(new FileCreatedEvent(file));
                            }
                            EventInvoker<FileCreatedEvent> fileEventInvoker = eventInvokersFileCreated.get(file);
                            logger.info("CREATE -> " + fileEventInvoker);
                            if (fileEventInvoker != null) {
                                fileEventInvoker.invoke(new FileCreatedEvent(file));
                            }
                        }
                    }
                    key.reset();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized void registerIfNot(Path pathToWatch) throws IOException {
        Path dirToWatch;
        if (Files.isDirectory(pathToWatch)) {
            dirToWatch = pathToWatch;
        } else {
            dirToWatch = pathToWatch.getParent();
        }
        if (!pathWatched.containsKey(dirToWatch)) {
            WatchKey key = dirToWatch.register(watchService, ENTRY_MODIFY, ENTRY_DELETE, ENTRY_CREATE);
            pathWatched.put(dirToWatch, key);
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
            if (!Files.isDirectory(pathToUnWatch)
                    && eventInvokersFileCreated.get(pathToUnWatch.getParent()) == null
                    && eventInvokersFileDeleted.get(pathToUnWatch.getParent()) == null
                    && eventInvokersFileChanged.get(pathToUnWatch.getParent()) == null) {
                return unregister(pathToUnWatch.getParent());
            } else {
                return unregister(pathToUnWatch);
            }
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
