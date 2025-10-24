package dev.corveric;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Sphere;

public class Projectile extends Node {
        private Vector3f direction;
        private float gravity;
        private String type;
        private float speed;
        private Vector3f origin, velocity;


    public Projectile(AssetManager assetManager, String type,Vector3f origin, Vector3f direction, float gravity, float speed){
        this.direction = direction.clone();
        this.type = type;
        this.gravity = gravity;
        this.speed = speed;
        this.origin = origin;
        this.velocity = direction.mult(speed);

        Geometry geom = new Geometry("fireball", new Sphere(8, 8, 0.2f));
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setColor("Diffuse", ColorRGBA.Orange);
        mat.setColor("Ambient", ColorRGBA.Orange);
        mat.setBoolean("UseMaterialColors", true);
        geom.setMaterial(mat);attachChild(geom); // add to this node
        setLocalTranslation(origin);
        setLocalRotation(RotationUtil.fromDegrees(this.direction.x, this.direction.y, this.direction.z));

    }

    public void updatePos(){
        Vector3f pos = origin.add(velocity);
        pos.y -= gravity;
        setLocalTranslation(pos);
    }

    public boolean isAlive(){
        //make raycast on ground to know if the fireball has hit ground
        return true;
    }

    public String getType(){
        return type;
    }
}
