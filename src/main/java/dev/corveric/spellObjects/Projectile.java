package dev.corveric.spellObjects;

import com.jme3.asset.AssetManager;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Sphere;
import dev.corveric.Main;
import dev.corveric.RotationUtil;

public class Projectile extends Node {
    private Vector3f velocity;
    private float gravity, range, damage, hitrange, rotationSpeed;
    private String type, caster;
    private Vector3f position, origin, forward;
    private boolean hit;

    public Projectile(AssetManager assetManager, String caster, String type, Vector3f origin, Vector3f direction, float gravity, float speed, float range, float damage) {
        this.type = type;
        this.gravity = gravity;
        Quaternion rot = RotationUtil.fromDegrees(direction.x,direction.y,direction.z);
        this.forward = rot.mult(Vector3f.UNIT_Z);
        this.velocity = forward.mult(speed);
        this.position = origin.clone();
        this.origin = origin;
        this.range = range;
        this.caster = caster;
        this.damage = damage;
        this.hit = false;

        if (type.equals("plasmaball")) {
            Spatial geom = assetManager.loadModel("/models/spells/plasmaball/plasmaball.obj");
            geom.depthFirstTraversal(spatial -> {
                if (spatial instanceof Geometry geo){
                    //System.out.println(geo.getName());
                    if (geo.getName().equals("plasmaball-geom-2")){
                        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
                        mat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
                        mat.setColor("Color", ColorRGBA.fromRGBA255(57,0,198,255));
                        geo.setMaterial(mat);
                    }
                }
            });
            geom.setLocalScale(0.45f);
            attachChild(geom);
            this.hitrange = 1.5f;
            this.rotationSpeed = 4f;
            position.subtractLocal(velocity.mult(0.2f));
        }
        else if (type.equals("fireball")){
            Spatial geom = assetManager.loadModel("models/spells/fireball/fireball.obj");
            geom.setLocalScale(0.67f);
            attachChild(geom);
            this.hitrange = 1f;
            this.rotationSpeed = 0f;
            position.subtractLocal(velocity.mult(0.2f));
        }
        setLocalTranslation(position);
    }

    public void updatePos(float tpf) {
        //gravity = gravity * 0.2f * gravity * tpf;
        velocity.y -= gravity * tpf;       // apply gravity
        position.subtractLocal(velocity.mult(tpf)); // move by velocity * delta time
        setLocalTranslation(position);
        rotate(0, rotationSpeed * tpf, 0);

        CollisionResults results = new CollisionResults();

        Ray forwardRay = new Ray(this.position, this.forward.negate());
        Ray downRay = new Ray(this.position, Vector3f.UNIT_Y.negate());
        Main.instance.getRootNode().getChild("world").collideWith(forwardRay, results);
        Main.instance.getRootNode().getChild("world").collideWith(downRay, results);

        if (results.size() > 0) {
            CollisionResult closest = results.getClosestCollision();
            //System.out.println(closest.getDistance());
            if (closest.getDistance() < 0.2f) {
                this.hit = true;

            }}
    }

    public void hit(){
        hit = true;
    }

    public boolean isAlive() {
        if (position.distance(origin) > range || this.hit){
            return false;
        }
        return true;
    }

    public boolean isHit(){return hit;}
    public String getType() {return type;}
    public float getDamage() {return damage;}
    public String getCaster(){return caster;}
    public float getRange(){return range;}
    public Vector3f getPosition(){return getLocalTranslation();}
    public float getHitRange(){return hitrange;}
}
