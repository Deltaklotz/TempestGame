package dev.corveric.spellObjects;

import com.jme3.asset.AssetManager;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

public class Stationary extends Node {
    private String type;
    private Vector3f position;
    private float damageRange, damagePerSecond, lifetime;

    public Stationary(AssetManager assetManager, String type, Vector3f position, float lifetime, float damageRange, float damagePerSecond){
        this.type = type;
        this.position = position;
        this.damageRange = damageRange;
        this. damagePerSecond = damagePerSecond;
    }

    public String getType(){return type;}
    public float getDamage(){return damagePerSecond;}
    public float getDamageRange(){return damageRange;}
    public float getLifetime(){return lifetime;}
}
