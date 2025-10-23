package dev.corveric;

import com.jme3.math.Vector3f;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class NetworkThread extends WebSocketClient {

    private volatile boolean running = true;
    private Thread senderThread;

    public NetworkThread(String serverAddress, int port) throws Exception {
        super(new URI("ws://" + serverAddress + ":" + port));
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("Connected to server");

        // start the continuous sender loop
        senderThread = new Thread(() -> {
            while (running) {
                try {
                    if (Main.instance != null) {
                        Vector3f pos = Main.instance.getPlayerPosition();
                        float rotY = Main.instance.getPlayerRotationY();
                        String playerData = "1" + Main.clientID + ":" + rotY + ";" + pos.x + ";" + pos.y + ";" + pos.z;
                        send(playerData); // this is WebSocketClient.send(...)
                    }
                    Thread.sleep(33); // 10 per second
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        senderThread.setDaemon(true);
        senderThread.start();
    }

    @Override
    public void onMessage(String message) {
        // same parsing logic you had
        String[] players = message.split("§");
        for (String player : players) {
            String[] parts = player.split(":");
            if (parts.length == 2) {
                String name = parts[0];
                String data = parts[1];
                if (!name.equals(Main.clientID)) {
                    Main.playerData.put(name, data);
                }
            }
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Disconnected: " + reason);
        running = false;
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
    }

    public void stopRunning() {
        running = false;
        close();
    }
}
