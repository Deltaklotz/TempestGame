package dev.corveric;

import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.texture.Texture;

public class TextUtil {
    public static void addNameTag(Node model, String text, com.jme3.asset.AssetManager assetManager) {

        BitmapFont font = assetManager.loadFont("Interface/Fonts/Default.fnt");
        Material material = font.getPage(0);
        Texture tex = (Texture) material.getTextureParam("ColorMap").getTextureValue();
        tex.setMagFilter(Texture.MagFilter.Nearest);
        tex.setMinFilter(Texture.MinFilter.NearestNoMipMaps);


        BitmapText label = new BitmapText(font, false);
        label.setText(text);
        label.setSize(0.15f);

        // Force it into 3D


        label.setQueueBucket(RenderQueue.Bucket.Transparent);
        label.updateGeometricState();


        label.setLocalTranslation(-label.getLineWidth() / 2f, 0.67f, 0.6767f);
        //AAAAAAAAAAAAAAAA 676767676767676767676767676767676767676767676767676767676767676767676767676767676767

        model.attachChild(label);

    }
}
