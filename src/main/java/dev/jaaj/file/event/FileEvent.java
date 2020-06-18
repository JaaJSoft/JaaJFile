package dev.jaaj.file.event;

import dev.jaaj.event.JaaJEvent;

import java.nio.file.Path;

public abstract class FileEvent extends JaaJEvent<Path> {

    public FileEvent(Path source) {
        super(source);
    }
}
