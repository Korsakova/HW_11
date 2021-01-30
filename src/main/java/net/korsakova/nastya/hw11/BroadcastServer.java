package net.korsakova.nastya.hw11;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
public class BroadcastServer implements Closeable {

    // сервер
    private final ServerSocket server;
    // подписчики
    private final Map<String, Socket> broadcast;

    // потоки исполняюшие активные действия
    private Thread acceptThread;
    private Thread executingThread;

    @SneakyThrows
    public static BroadcastServer concurrentBroadcastServer(int port) {
        return new BroadcastServer(new ServerSocket(port), new ConcurrentHashMap<>());
    }

    public void start() {
        (this.acceptThread = new Thread((Unchecked) () -> {
            // поток принимает соединение и оформляет подписку
            while (!server.isClosed()) {
                final Socket accept = server.accept();

                final BufferedReader is = reader(accept);
                final String name = is.readLine();

                log.info("[SERVER] client connected to server: {}", name);
                broadcast.put(name, accept);
            }
        })).start();
        (this.executingThread = new Thread((Unchecked) () -> {
            // поток принимает сообщения, подписывает и рассылает на всех
            final Set<String> detached = new HashSet<>();

            while (!server.isClosed()) {
                final Map<String, Socket> listeners = new HashMap<>(this.broadcast);

                for (final String name : listeners.keySet()) {
                    final Socket socket = listeners.get(name);

                    if(isConnectionClosed(name, socket, detached))
                        break;

                    final BufferedReader reader = reader(socket);

                    if(reader.ready()) {
                        final String input = reader.readLine();

                        if (input != null || !input.isEmpty()) {
                            log.info("[SERVER] client [{}] send message = {}", name, input);

                            final boolean isQuiteMessage = "[quite]".equals(input);

                            if(isQuiteMessage){
                                log.info("[SERVER] client disconnected from server: {}", name);
                                detached.add(name);
                                socket.close();
                            }

                            final boolean isMulticastMessage = input.startsWith("[multicast]");

                            if (isMulticastMessage || isQuiteMessage) {

                                final String message = isQuiteMessage ?
                                        String.format("%s say: Im out bitches. User %s left the chat...", name, name) :
                                        String.format("%s say: %s", name, input.substring(11));

                                listeners.keySet().stream()
                                        .filter(key -> !name.equals(key) && !detached.contains(key))
                                        .forEach(key -> {
                                            try {
                                                Socket listener = listeners.get(key);

                                                if (!isConnectionClosed(key, listener, detached)) {
                                                    BufferedWriter writer = writer(listener);
                                                    writer.write(message);
                                                    writer.newLine();
                                                    writer.flush();
                                                }
                                            } catch (Exception exception) {
                                                log.warn("Send message by [" + key + "] exception", exception);
                                            }
                                        });
                            }

                            final boolean isUnicastMessage = input.startsWith("[unicast:");

                            if(isUnicastMessage){
                                String inputWithoutType = input.substring(9);
                                int delimiter = StringUtils.indexOf(inputWithoutType, ']');
                                String unicastReceiverName = inputWithoutType.substring(0, delimiter);

                                final String message = String.format("%s say: %s", name, inputWithoutType.substring(delimiter + 1));

                                final Socket receiver = broadcast.get(unicastReceiverName);

                                if(!isConnectionClosed(unicastReceiverName, receiver, detached)){
                                    BufferedWriter writer = writer(receiver);
                                    writer.write(message);
                                    writer.newLine();
                                    writer.flush();
                                }
                            }

                        }
                    }
                }

                detached.forEach(broadcast::remove);
                detached.clear();
            }
        })).start();
    }

    private boolean isConnectionClosed(String name, Socket socket, Set<String> detachedNames) {
        boolean detached;
        if(detached = socket.isClosed()) {
            log.debug("[SERVER] client detached: " + name);
            detachedNames.add(name);
        }

        return detached;
    }

    @SneakyThrows
    private static BufferedReader reader(Socket socket) {
        return new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    @SneakyThrows
    private static BufferedWriter writer(Socket socket) {
        return new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    @Override
    public void close() throws IOException {
        server.close();

        if(acceptThread != null)
            acceptThread.interrupt();

        if(executingThread != null)
            executingThread.interrupt();
    }
}
