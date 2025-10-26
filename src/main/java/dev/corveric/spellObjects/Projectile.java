package dev.corveric.spellObjects;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Sphere;

public class Projectile extends Node {
    private Vector3f velocity;
    private float gravity, range;
    private String type, caster;
    private Vector3f position, origin;

    public Projectile(AssetManager assetManager, String caster, String type, Vector3f origin, Vector3f direction, float gravity, float speed, float range) {
        this.type = type;
        this.gravity = gravity;
        this.velocity = direction.normalize().mult(speed);
        this.position = origin.clone();
        this.origin = origin;
        this.range = range;
        this.caster = caster;

        if (type.equals("plasma")) {
            Geometry geom = new Geometry("plasma", new Sphere(8, 8, 0.2f));
            Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
            mat.setColor("Diffuse", ColorRGBA.Magenta);
            mat.setColor("Ambient", ColorRGBA.Magenta);
            mat.setBoolean("UseMaterialColors", true);
            geom.setMaterial(mat);
            attachChild(geom);
        }
        else{
            Geometry geom = new Geometry("fireball", new Sphere(8, 8, 0.2f));
            Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
            mat.setColor("Diffuse", ColorRGBA.Orange);
            mat.setColor("Ambient", ColorRGBA.Orange);
            mat.setBoolean("UseMaterialColors", true);
            geom.setMaterial(mat);
            attachChild(geom);
        }
        setLocalTranslation(position);
    }

    public void updatePos(float tpf) {
        velocity.y -= gravity * tpf;       // apply gravity
        position.addLocal(velocity.mult(tpf)); // move by velocity * delta time
        setLocalTranslation(position);
    }

    public boolean isAlive() {
        if (position.distance(origin) > range){
            return false;
        }
        return true;
    }

    public String getType() {
        return type;
    }

    public String getCaster(){
        return caster;
    }
}
