package net.korsakova.nastya.hw11;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Пчелы по натуре филосовы (реперы)
 */
@Slf4j
@RequiredArgsConstructor
public class BroadcastBeehive implements Closeable {
    private final static int HIVE_SLEEP_DELAY = 5;
    private final static int ITERATION_SPEECH_PERCENT_QUOTE = 3;

    private final Map<String, Bee> hive;

    private final int port;
    private final InetAddress address;

    private volatile boolean closed = false;
    private Thread executingThread;
    private Thread receiveThread;

    @RequiredArgsConstructor
    public static class Bee implements Closeable {
        private final String name;
        private final Socket connection;

        /**
         * o - r
         * 1 - rw
         */
        private final int mode;

        /**
         * Сказать громко - то есть чтобы услышали все пчелки
         *
         * @param text сообщение
         */
        @SneakyThrows
        public void multicast(@NonNull String text) {
            if (mode >= 1) {
                sendMessage("[multicast]" + text);
            }
        }

        /**
         * Сказать тихо - определенной пчелке
         *
         * @param text сообщение
         */
        @SneakyThrows
        public void unicast(@NonNull String anotherBeeName, @NonNull String text) {
            if (mode >= 1) {
                sendMessage("[unicast:" + anotherBeeName + "]" + text);
            }
        }

        @SneakyThrows
        public void iAmQuiteBitches() {
            sendMessage("[quite]");
        }

        @SneakyThrows
        public void listen() {
            if (mode >= 0) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                if (reader.ready()) {
                    final String message = reader.readLine();

                    if (message != null)
                        log.info("[BEE LISTEN] Bee {} get message: {}", name, message);
                }
            }
        }

        @Override
        public void close() throws IOException {
            connection.close();
        }

        @SneakyThrows
        private void sendMessage(@NonNull String text){
            final var writer = new PrintWriter(
                    connection.getOutputStream(), true);

            writer.println(text);
        }
    }

    @SneakyThrows
    public static @NonNull BroadcastBeehive hive(int port) {
        return new BroadcastBeehive(new ConcurrentHashMap<>(), port, InetAddress.getLocalHost());
    }

    public void start() {
        (this.executingThread = new Thread((Unchecked) () -> {
            final RapQuoteBook[] quotes = RapQuoteBook.values();
            while (!closed) {
                final List<Bee> bees = new ArrayList<>(hive.values().stream().filter(bee -> bee.mode >= 1).collect(Collectors.toList()));
                final int count = bees.size();
                if (count > 0) {
                    final int iterations = Math.max(1, count / ITERATION_SPEECH_PERCENT_QUOTE);

                    for (int i = 0; i < iterations; i++) {
                        final Bee bee = bees.get((int) (Math.random() * count));
                        final String quote = quotes[(int) (Math.random() * quotes.length)].getQuote();

                        bee.multicast(String.format("'%s' (c) %s", quote, bee.name));
                    }

                    log.debug("[HIVE] The hive goes to sleep for {} seconds - bees have to get rest", HIVE_SLEEP_DELAY);
                    TimeUnit.SECONDS.sleep(HIVE_SLEEP_DELAY);
                }
            }
        })).start();
        (this.receiveThread = new Thread((Unchecked) () -> {
            while (!closed)
                hive.values().forEach(Bee::listen);
        })).start();
    }

    @SneakyThrows
    public @NonNull Bee newBee(@NonNull String name, int mode) {
        final Socket connection = new Socket(address, port);

        final Bee bee = new Bee(name, connection, mode);
        hive.put(name, bee);

        final var writer = new PrintWriter(
                connection.getOutputStream(), true);

        writer.println(name);

        return bee;
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            for (Bee bee : hive.values()) {
                bee.close();
            }

            if (executingThread != null)
                executingThread.interrupt();

            if (receiveThread != null)
                receiveThread.interrupt();

            this.closed = true;
        }
    }
}
