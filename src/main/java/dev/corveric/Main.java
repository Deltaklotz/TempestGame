package dev.corveric;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.system.AppSettings;

import java.util.Hashtable;
import java.util.Scanner;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main extends SimpleApplication {
    public static Main instance; // static reference for networking thread

    private BulletAppState bulletAppState;
    private CharacterControl player;
    private Vector3f walkDirection = new Vector3f();
    private boolean left, right, forward, backward;

    public static Hashtable<String, Node> playerEntities = new Hashtable<>();
    public static Hashtable<String, String> playerData = new Hashtable<>();
    public static Spatial hand;
    private static Vector3f handOffset;
    private static Node handNode;
    private static Node playerView;

    public static String serverAdress;
    public static String clientID;
    public static boolean useInstancing;

    public static void main(String[] args) throws Exception{

        //Logic for connecting to Server
        Scanner scanner = new Scanner(System.in);

        System.out.println("Enter IP Adress of Server:");
        serverAdress = scanner.nextLine();
        while(true) {
            System.out.println("Enter Username:");
            clientID = scanner.nextLine();
            if(clientID.contains("$") || clientID.contains("§") || clientID.contains("&")){
                System.out.println("Invalid Usrename, cannot contain Symbols: [§, $, &]");
            }
            else {
                break;
            }
        }
        System.out.println("Use Instancing? 'no' for no, otherwise: yes");
        String s = scanner.nextLine();
        useInstancing = !s.equalsIgnoreCase("no");

        NetworkThread client = new NetworkThread(serverAdress, 777);
        client.connect();

        Main app = new Main();
        AppSettings settings = new AppSettings(true);
        settings.setResolution(1920, 1080); // set to your monitor resolution
        settings.setFullscreen(true);      // true = fullscreen, false = windowed
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }

    // Called by networking thread
    public Vector3f getPlayerPosition() {
        if (player == null) {
            return new Vector3f(0,0,0); // or skip sending
        }
        return player.getPhysicsLocation().clone(); // clone to avoid threading issues
    }

    public float getPlayerRotationY() {
        if (player == null) {
            return 0f;
        }

        Quaternion camRot = cam.getRotation();
        float[] angles = camRot.toAngles(null); // returns [X, Y, Z] in radians
        float yaw = angles[1]; // Yaw around Y-axis in radians
        return yaw * FastMath.RAD_TO_DEG;
    }
    public float getPlayerRotationX() {
        if (player == null) {
            return 0f;
        }

        Quaternion camRot = cam.getRotation();
        float[] angles = camRot.toAngles(null); // returns [X, Y, Z] in radians
        float pitch = angles[0]; // Yaw around Y-axis in radians
        return pitch * FastMath.RAD_TO_DEG;
    }

    public void simpleInitApp(){
        instance = this;

        // Physics
        bulletAppState = new BulletAppState();
        //bulletAppState.setDebugEnabled(true);
        stateManager.attach(bulletAppState);

        //Lighting
        DirectionalLight sun = new DirectionalLight();
        sun.setColor(ColorRGBA.White);
        sun.setDirection(new Vector3f(-.5f,-.5f,-.5f).normalizeLocal());
        rootNode.addLight(sun);

        AmbientLight ambient = new AmbientLight();
        ambient.setColor(ColorRGBA.White.mult(0.5f)); // 50% intensity
        rootNode.addLight(ambient);

        final int SHADOWMAP_SIZE=1024;
        DirectionalLightShadowRenderer dlsr = new DirectionalLightShadowRenderer(assetManager, SHADOWMAP_SIZE, 3);
        dlsr.setLight(sun);
        viewPort.addProcessor(dlsr);

        //Sky
        viewPort.setBackgroundColor(ColorRGBA.fromRGBA255(64, 223, 255, 255));

        // Character (first-person)
        CapsuleCollisionShape capsule = new CapsuleCollisionShape(1f, 2f);
        player = new CharacterControl(capsule, 0.05f);
        player.setPhysicsLocation(new Vector3f(0, 2, 0));
        bulletAppState.getPhysicsSpace().add(player);

        // Camera
        flyCam.setMoveSpeed(1f);
        cam.setLocation(player.getPhysicsLocation());

        // Input
        initKeys();
    }

    private void initKeys() {
        inputManager.addMapping("Left",     new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Right",    new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Forward",  new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Backward", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Jump",     new KeyTrigger(KeyInput.KEY_SPACE));

        inputManager.addListener(actionListener,
                "Left", "Right", "Forward", "Backward", "Jump");
    }

    private final ActionListener actionListener = new ActionListener() {
        public void onAction(String binding, boolean isPressed, float tpf) {
            switch (binding) {
                case "Left":     left = isPressed; break;
                case "Right":    right = isPressed; break;
                case "Forward":  forward = isPressed; break;
                case "Backward": backward = isPressed; break;
                case "Jump":
                    if (isPressed) {
                        player.jump();
                    }
                    break;
            }
        }
    };

    @Override
    public void simpleUpdate(float tpf){

        walkDirection.set(0, 0, 0);
        if (left) walkDirection.addLocal(cam.getLeft());
        if (right) walkDirection.addLocal(cam.getLeft().negate());
        if (forward) walkDirection.addLocal(cam.getDirection());
        if (backward) walkDirection.addLocal(cam.getDirection().negate());

        walkDirection.y = 0; // keep horizontal
        player.setWalkDirection(walkDirection.mult(0.25f));
        player.setJumpSpeed(12f); // lower jump
        player.setFallSpeed(30f);
        player.setGravity(30f);
        cam.setLocation(player.getPhysicsLocation().add(0, 1.5f, 0));
    }

    @Override
    public void destroy() {
        super.destroy();
        // cleanup logic here
        System.out.println("Window closed. Stopping code.");
        System.exit(0); // guarantees JVM exit
    }

}