package dev.jaaj.file.event;

import dev.jaaj.event.JaaJEvent;

import java.nio.file.Path;

public class FileCreatedEvent extends FileEvent {

    public FileCreatedEvent( Path newFile) {
        super(newFile);
    }
}
