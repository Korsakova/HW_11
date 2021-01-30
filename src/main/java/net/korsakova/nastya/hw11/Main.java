package net.korsakova.nastya.hw11;

import lombok.extern.slf4j.Slf4j;
import net.korsakova.nastya.hw11.BroadcastBeehive.Bee;

@Slf4j
public class Main {

    /**
     * Разработать приложение - многопользовательский чат, в котором участвует произвольное количество клиентов.
     * Каждый клиент после запуска отправляет свое имя серверу. После чего начинает отправлять ему сообщения.
     * Каждое сообщение сервер подписывает именем клиента и рассылает всем клиентам (broadcast).
     * <p>
     *
     * Реализуем самы простой вариант => будем держать соединия
     */
    public static void main(String[] args) {
        final int port = 8080;

        try (
                BroadcastServer server = BroadcastServer.concurrentBroadcastServer(port);
                BroadcastBeehive hive = BroadcastBeehive.hive(port)
        ) {
            server.start();
            hive.start();

            hive.newBee("Пчелка Би", 0);
            hive.newBee("Летающий полосатик", 0);
            Bee beeNamedTail = hive.newBee("Реактивный хвостик", 1);
            hive.newBee("Старче Би", 0);
            Bee beeNamedSullenWing = hive.newBee("Угрюмое крыло", 0);

            Thread.sleep(1000);

            beeNamedTail.unicast("Старче Би", "Ты пес, а не пчела!");

            Thread.sleep(1000);

            beeNamedSullenWing.iAmQuiteBitches();

            Thread.sleep(60000);
        } catch (Exception e) {
            log.warn("server stopped with error", e);
        }
    }


}
