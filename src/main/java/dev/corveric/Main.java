package dev.corveric;

import com.jme3.anim.AnimComposer;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.TextureKey;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.*;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import dev.corveric.spellObjects.Projectile;

import java.util.*;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main extends SimpleApplication {
    public static Main instance; // static reference for networking thread
    public static NetworkThread server;
    private BulletAppState bulletAppState;
    private CharacterControl player;
    private Vector3f walkDirection = new Vector3f();
    private boolean left, right, forward, backward;

    public static Hashtable<String, Node> playerEntities = new Hashtable<>();
    public static Hashtable<String, String> playerData = new Hashtable<>();
    public static ArrayList<Projectile> projectiles = new ArrayList<>();
    public static String animState = "1"; //1 = idle; 2 = walk

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

        server = new NetworkThread(serverAdress, 777);
        server.connect();

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

    public Vector3f getPlayerRotation() {
        if (player == null) {
            return Vector3f.ZERO;
        }

        Quaternion camRot = cam.getRotation();
        float[] angles = camRot.toAngles(null); // returns [X, Y, Z] in radians
        float yaw = angles[1]; // Yaw around Y-axis in radians
        float pitch = angles[0]; // Yaw around X-axis in radians
        float roll = angles[2];
        return new Vector3f(pitch * FastMath.RAD_TO_DEG,yaw * FastMath.RAD_TO_DEG, roll * FastMath.RAD_TO_DEG);
    }

    public int getTextureIndex(String username){
        int sum = 0;
        for (char c : username.toCharArray()){
            sum += (int) c;
        }

        int firstDigit = Integer.toString(sum).charAt(0) - '0';
        return firstDigit;
    }

    public void simpleInitApp(){
        instance = this;

        // Physics
        bulletAppState = new BulletAppState();
        //bulletAppState.setDebugEnabled(true);
        stateManager.attach(bulletAppState);

        //test data for testing entity creation
        playerData.put("batman007", "130;5;2;5;3");
        Projectile p = new Projectile(assetManager, clientID,"plasma", new Vector3f(0,5,0), new Vector3f(90,0,0), 0.5f, 5f, 60f);
        projectiles.add(p);
        rootNode.attachChild(p);

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

        //Level Initialization
        Spatial level = assetManager.loadModel("/models/test_level/test_level.obj");
        level.setLocalScale(3f);
        level.setShadowMode(RenderQueue.ShadowMode.Receive);
        rootNode.attachChild(level);

        //Level Physics
        RigidBodyControl levelPhys = new RigidBodyControl(0.0f); // static
        level.addControl(levelPhys);
        bulletAppState.getPhysicsSpace().add(levelPhys);



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

        if (walkDirection.lengthSquared() > 0.0001f) animState = "2";
        else animState = "1";

        walkDirection.y = 0; // keep horizontal
        player.setWalkDirection(walkDirection.mult(0.25f));
        player.setJumpSpeed(12f); // lower jump
        player.setFallSpeed(30f);
        player.setGravity(30f);
        cam.setLocation(player.getPhysicsLocation().add(0, 1.5f, 0));


        Enumeration<String> playerNames = playerData.keys();
        while (playerNames.hasMoreElements()) {
            String PName = playerNames.nextElement();
            if(playerEntities.containsKey(PName)){
                String[] playerArray = playerData.get(PName).split(";");
                playerEntities.get(PName).setLocalTranslation(new Vector3f(Float.parseFloat(playerArray[1]), Float.parseFloat(playerArray[2]), Float.parseFloat(playerArray[3])));
                playerEntities.get(PName).setLocalRotation(RotationUtil.fromDegrees(0f, Float.parseFloat(playerArray[0]), 0f));
                AnimComposer comp = ((Node) playerEntities.get(PName)).getChild("Armature").getControl(AnimComposer.class);
                if(playerArray[4].equals("1") && comp.getCurrentAction(AnimComposer.DEFAULT_LAYER) != comp.action("idle")) {
                    comp.setCurrentAction("idle", AnimComposer.DEFAULT_LAYER, true);
                }
                else if (playerArray[4].equals("2") && comp.getCurrentAction(AnimComposer.DEFAULT_LAYER) != comp.action("run")) {
                    comp.setCurrentAction("run", AnimComposer.DEFAULT_LAYER, true);
                }
            }
            else{
                Spatial newP = assetManager.loadModel("models/player/player.glb");
                newP.updateModelBound();             // recalculates mesh bounds
                newP.setCullHint(Spatial.CullHint.Never);

                newP.depthFirstTraversal(spatial -> {
                    if (spatial instanceof Geometry geom) {
                        Material body_mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
                        Material jacket_mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
                        Material hat_mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
                        Material face_mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
                        Texture jacket_tex = assetManager.loadTexture(new TextureKey("models/player/player_jacket.png", false));
                        Texture face_tex = assetManager.loadTexture(new TextureKey("models/player/player_face.png", false));
                        jacket_mat.setTexture("DiffuseMap", jacket_tex);
                        jacket_mat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
                        face_mat.setTexture("DiffuseMap", face_tex);
                        ColorRGBA body_col = new ColorRGBA[]{
                                ColorRGBA.fromRGBA255(255, 41, 41, 255),
                                ColorRGBA.fromRGBA255(41, 87, 255, 255),
                                ColorRGBA.fromRGBA255(41, 255, 80, 255),
                                ColorRGBA.fromRGBA255(255, 219, 41, 255),
                                ColorRGBA.fromRGBA255(241, 41, 255, 255),
                                ColorRGBA.fromRGBA255(41, 205, 255, 255),
                                ColorRGBA.fromRGBA255(144, 41, 255, 255),
                                ColorRGBA.fromRGBA255(200, 60, 0, 255),
                                ColorRGBA.fromRGBA255(200,200,200, 255)
                        }[getTextureIndex(PName)-1];
                        body_mat.setColor("Diffuse", body_col);
                        body_mat.setColor("Ambient", body_col);
                        body_mat.setBoolean("UseMaterialColors", true);
                        hat_mat.setColor("Diffuse", ColorRGBA.fromRGBA255(22, 8, 0, 255));
                        hat_mat.setColor("Ambient", ColorRGBA.fromRGBA255(22, 8, 0,255));
                        hat_mat.setBoolean("UseMaterialColors", true);
                        face_mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
                        if(geom.getName().equals("Cube.003_0")){
                            geom.setMaterial(jacket_mat);
                        }
                        else if (geom.getName().equals("Cube.004_0")){
                            geom.setMaterial(face_mat);
                        }
                        else if (geom.getName().equals("Circle.001_0")){
                            geom.setMaterial(hat_mat);
                        }
                        else {geom.setMaterial(body_mat);}
                    }
                });

                Node newPnode = new Node(PName);
                newPnode.updateModelBound();
                newPnode.setCullHint(Spatial.CullHint.Never);
                newPnode.attachChild(newP);
                CapsuleCollisionShape newC = new CapsuleCollisionShape(1f,2f);
                RigidBodyControl newCTRL = new RigidBodyControl(newC, 0f);
                newCTRL.setKinematic(true);
                newPnode.addControl(newCTRL);
                bulletAppState.getPhysicsSpace().add(newCTRL);
                newP.setLocalTranslation(0,-5f,0);
                newPnode.setLocalScale(0.4f);
                TextUtil.addNameTag(newPnode, PName, assetManager);
                newP.setShadowMode(RenderQueue.ShadowMode.Cast);
                rootNode.attachChild(newPnode);
                playerEntities.put(PName, newPnode);
                ((Node) newP).getChild("Armature").getControl(AnimComposer.class).setCurrentAction("idle", AnimComposer.DEFAULT_LAYER, true);
                ((Node) newP).getChild("Armature").getControl(AnimComposer.class).action("run").setSpeed(1.5f);
            }
        }

        if (!projectiles.isEmpty()) {
            Iterator<Projectile> it = projectiles.iterator();
            while (it.hasNext()) {
                Projectile n = it.next();
                n.updatePos(tpf);
                if (!n.isAlive()) {
                    if (n.getCaster().equals(clientID)){
                        //server.send();
                    }
                    n.removeFromParent();
                    it.remove();
                }
            }
        }


    }

    @Override
    public void destroy() {
        super.destroy();
        // cleanup logic here
        System.out.println("Window closed. Stopping code.");
        System.exit(0); // guarantees JVM exit
    }

}