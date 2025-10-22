package dev.corveric;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import java.net.InetSocketAddress;
import java.util.ArrayList;

public class GameServer extends WebSocketServer {

    private ArrayList<String> playerdata = new ArrayList<String>();
    private ArrayList<String> playerids = new ArrayList<String>();

    public GameServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("New connection: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Closed: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        //System.out.println(message);

        String[] clientdata = message.split("\\$");
        if(playerids.contains(clientdata[0])){
            playerdata.set(playerids.indexOf(clientdata[0]), message);
        }
        else{
            playerdata.add(message);
            playerids.add(clientdata[0]);
        }
        conn.send(String.join("&", playerdata));
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("Server started!");
    }

    public static void main(String[] args) {
        new GameServer(777).start();
    }
}