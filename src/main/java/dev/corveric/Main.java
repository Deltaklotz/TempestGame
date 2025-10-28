package dev.corveric;

import com.jme3.anim.AnimComposer;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.TextureKey;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
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
import dev.corveric.spellObjects.SpellUtil;
import dev.corveric.spellObjects.Stationary;

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
    public static ArrayList<Stationary> stationaries = new ArrayList<>();
    public static ArrayList<String> selectableSpells;
    public static ArrayList<Integer> spellInventory = new ArrayList<>(); //consists of indexes for spellList
    public static ArrayList<Spatial> spellInvSpatials = new ArrayList<>();
    public static int selectedInvSlot = 0;
    public static String animState = "1"; //1 = idle; 2 = walk
    public float Health = 100f;
    public BitmapText HPtext;
    Spatial InvIndicator;

    public static Spatial hand;
    private static Vector3f handOffset;
    private static Node GUINode = new Node("GUI");
    private static Node playerView;

    public static String serverAdress;
    public static String clientID;
    public static boolean useInstancing;

    public static void main(String[] args) throws Exception{
        selectableSpells = new SpellUtil().getCastable();
        spellInventory.add(1);
        spellInventory.add(0);
        spellInventory.add(0);
        spellInventory.add(0);
        spellInventory.add(0);

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

    public void simpleInitApp(){
        instance = this;

        // Physics
        bulletAppState = new BulletAppState();
        //bulletAppState.setDebugEnabled(true);
        stateManager.attach(bulletAppState);

        //test data for testing entity creation
        playerData.put("batman007", "130;5;2;5;3");
        /*
        Projectile p = new Projectile(assetManager, clientID,"plasmaball", new Vector3f(0,5,0), new Vector3f(90,0,0), 0f, 15f, 100f, 10f);
        projectiles.add(p);
        rootNode.attachChild(p);
         */

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

        //GUI Layout with playerhand
        playerView = new Node("PlayerView");
        playerView.setLocalTranslation(new Vector3f(0, 1.5f, 0)); // camera height relative to player
        rootNode.attachChild(playerView);
        Spatial hand = assetManager.loadModel("models/player_hand/player_hand2.obj");
        handOffset = new Vector3f(0.6f, -0.4f, -.75f);
        cam.setFrustumPerspective(45, (float) cam.getWidth() / cam.getHeight(), 0.01f, 1000f);
        hand.setLocalScale(.25f); // scale to match scene
        Material handMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        handMat.setColor("Diffuse", PlayerUtil.getPlayerColor(clientID));
        handMat.setColor("Ambient", PlayerUtil.getPlayerColor(clientID));
        handMat.setBoolean("UseMaterialColors", true);
        rootNode.attachChild(hand);
        GUINode.attachChild(hand);
        hand.setLocalTranslation(handOffset);
        playerView.attachChild(GUINode);
        hand.setMaterial(handMat);

        for (int i = 0; i < 5; i++){
            Spatial nextSlot = assetManager.loadModel("models/InvCard/card.obj");
            nextSlot.setQueueBucket(RenderQueue.Bucket.Transparent);
            nextSlot.setLocalTranslation(-0.5f + i*0.15f, -0.3f, -1);
            nextSlot.setLocalScale(0.067f);
            rootNode.attachChild(nextSlot);
            GUINode.attachChild(nextSlot);
            spellInvSpatials.add(nextSlot);
        }

        InvIndicator = assetManager.loadModel("models/InvCard/card.obj");
        Material invCardMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Texture invCardTex = assetManager.loadTexture("textures/spellcards/border.png");
        invCardMat.setTexture("ColorMap", invCardTex);
        invCardMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        InvIndicator.setMaterial(invCardMat);
        InvIndicator.setLocalScale(0.071f);
        InvIndicator.setQueueBucket(RenderQueue.Bucket.Transparent);
        rootNode.attachChild(InvIndicator);
        GUINode.attachChild(InvIndicator);

        BitmapFont font = assetManager.loadFont("Interface/Fonts/Default.fnt");
        HPtext = new BitmapText(font);
        HPtext.setText(String.valueOf(Health));
        HPtext.setSize(0.05f); // scale text size
        HPtext.setQueueBucket(RenderQueue.Bucket.Transparent); // render correctly
        HPtext.setCullHint(Spatial.CullHint.Never);
        HPtext.setLocalTranslation(-0.6f, 0.35f, -1f);
        rootNode.attachChild(HPtext);
        GUINode.attachChild(HPtext);



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
        inputManager.addMapping("Num1", new KeyTrigger(KeyInput.KEY_1));
        inputManager.addMapping("Num2", new KeyTrigger(KeyInput.KEY_2));
        inputManager.addMapping("Num3", new KeyTrigger(KeyInput.KEY_3));
        inputManager.addMapping("Num4", new KeyTrigger(KeyInput.KEY_4));
        inputManager.addMapping("Num5", new KeyTrigger(KeyInput.KEY_5));

        inputManager.addListener(actionListener,
                "Left", "Right", "Forward", "Backward", "Jump","Num1", "Num2", "Num3","Num4","Num5");
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
                case "Num1": selectedInvSlot = 0; break;
                case "Num2": selectedInvSlot = 1; break;
                case "Num3": selectedInvSlot = 2; break;
                case "Num4": selectedInvSlot = 3; break;
                case "Num5": selectedInvSlot = 4; break;
            }
        }
    };

    @Override
    public void simpleUpdate(float tpf){
        playerView.setLocalTranslation(player.getPhysicsLocation().add(0, 1.5f, 0)); // camera height
        GUINode.setLocalRotation(RotationUtil.fromDegrees(-getPlayerRotation().x, getPlayerRotation().y+180, 0));
        HPtext.setText((int) Health + "HP");
        InvIndicator.setLocalTranslation(-0.505f + selectedInvSlot*0.1515f, -0.303f, -1.01f);

        for (int i = 0; i < 5; i++){
            Material slotMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            Texture slotTex = assetManager.loadTexture("textures/spellcards/"+ selectableSpells.get(spellInventory.get(i))+".png");
            slotMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            slotMat.setTexture("ColorMap", slotTex);
            spellInvSpatials.get(i).setMaterial(slotMat);
        }

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
                        ColorRGBA body_col = PlayerUtil.getPlayerColor(PName);
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
                PlayerUtil.addNameTag(newPnode, PName, assetManager);
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
                    if (n.getCaster().equals(clientID) && n.getType().equals("fireball")){
                        server.send("2" + clientID + "§" + "firemolly§" + getPlayerPosition().x + ":" + getPlayerPosition().y + ":" + getPlayerPosition().z + ";");
                    }
                    n.removeFromParent();
                    it.remove();
                }
            }
        }
        if (!stationaries.isEmpty()) {
            Iterator<Stationary> it = stationaries.iterator();
            while (it.hasNext()) {
                Stationary n = it.next();
                    //do something
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