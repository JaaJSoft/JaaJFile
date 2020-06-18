package dev.jaaj.file.event;

import dev.jaaj.event.JaaJEvent;

import java.nio.file.Path;

public class FileChangedEvent extends FileEvent {

    public FileChangedEvent(Path source) {
        super(source);
    }
}
