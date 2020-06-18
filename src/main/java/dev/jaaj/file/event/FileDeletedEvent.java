package dev.jaaj.file.event;

import dev.jaaj.event.JaaJEvent;

import java.nio.file.Path;

public class FileDeletedEvent extends FileEvent {
    public FileDeletedEvent(Path source) {
        super(source);
    }
}
