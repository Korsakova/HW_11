package net.korsakova.nastya.hw11;

import lombok.SneakyThrows;

public interface Unchecked extends Runnable {

    @SneakyThrows
    default void run() {
        unchecked();
    }

    void unchecked() throws Exception;
}