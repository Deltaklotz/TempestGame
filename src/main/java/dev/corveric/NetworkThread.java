package dev.corveric;

import com.jme3.math.Vector3f;
import dev.corveric.spellObjects.Projectile;
import dev.corveric.spellObjects.Stationary;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class NetworkThread extends WebSocketClient {

    private volatile boolean running = true;
    private Thread senderThread;

    public NetworkThread(String serverAddress, int port) throws Exception {
        super(new URI("ws://" + serverAddress + ":" + port));
    }

    public void cast(String spell){
        if(spell.equals("plasmaball")){
            Vector3f origVec = Main.instance.getPlayerPosition();
            origVec.y += 1.5f;
            Vector3f dirVec = Main.instance.getPlayerRotation();
            send("2"+Main.clientID+"§"+spell+"§"+origVec.x+":"+origVec.y+":"+origVec.z+";"+dirVec.x+":"+dirVec.y+":"+dirVec.z);
        }
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
                        float rotY = Main.instance.getPlayerRotation().y;
                        String playerData = "1" + Main.clientID + ":" + rotY + ";" + pos.x + ";" + pos.y + ";" + pos.z + ";" + Main.animState;
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
        char mti = message.charAt(0);
        String data = message.substring(1);

        if (mti == '1') {
            String[] players = data.split("§");
            for (String player : players) {
                String[] parts = player.split(":");
                if (parts.length == 2) {
                    String name = parts[0];
                    String Sdata = parts[1];
                    if (!name.equals(Main.clientID)) {
                        Main.playerData.put(name, Sdata);
                    }
                }
            }
        }
        else if (mti == '2'){
            String[] dataList = data.split("§");
            String[] paramList = dataList[2].split(";");
            if (dataList[1].equals("plasmaball")){
                String[] origVecParam = paramList[0].split(":");
                String[] dirVecParam = paramList[1].split(":");
                Vector3f origVec = new Vector3f(Float.parseFloat(origVecParam[0]), Float.parseFloat(origVecParam[1]), Float.parseFloat(origVecParam[2]));
                Vector3f dirVec = new Vector3f(Float.parseFloat(dirVecParam[0])+180f,Float.parseFloat(dirVecParam[1]),Float.parseFloat(dirVecParam[2]));
                Projectile newPlas = new Projectile(Main.instance.getAssetManager(), dataList[0], "plasmaball", origVec, dirVec, 0f, 22f, 100f, 10f);
                Main.projectiles.add(newPlas);
            }
            else if (dataList[1].equals("firemolly")){
                String[] posVecParam = paramList[0].split(":");
                Vector3f posVec = new Vector3f(Float.parseFloat(posVecParam[0]), Float.parseFloat(posVecParam[1]), Float.parseFloat(posVecParam[2]));
                Stationary newStat = new Stationary(Main.instance.getAssetManager(), "firemolly", posVec, 0f, 10f, 10f);
                Main.stationaries.add(newStat);
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
