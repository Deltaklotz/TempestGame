package dev.corveric.spellObjects;

import com.jme3.asset.AssetManager;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

public class Stationary extends Node {
    private String type;
    private Vector3f position;
    private float damageRange, damagePerSecond, lifetime;

    public Stationary(AssetManager assetManager, String type, Vector3f position, float lifetime, float damageRange, float damagePerSecond){
        this.type = type;
        this.position = position;
        this.damageRange = damageRange;
        this. damagePerSecond = damagePerSecond;
        this.lifetime = lifetime;

        if(type.equals("firemolly")){
            //Spatial geom = assetManager.loadModel("models/spells/firemolly/firemolly.obj");
            Spatial geom = assetManager.loadModel("models/spells/firemolly/firemolly.obj");
            attachChild(geom);
        }
        else {
            Spatial geom = assetManager.loadModel("models/spells/firemolly/firemolly.obj");
        }
        setLocalTranslation(position);
    }

    public void update(float tpf){
        lifetime -= tpf;
        System.out.println(lifetime);
    }

    public boolean isAlive(){
        return !(lifetime <= 0f);
    }

    public String getType(){return type;}
    public float getDamage(){return damagePerSecond;}
    public float getDamageRange(){return damageRange;}
    public float getLifetime(){return lifetime;}
}
