package dev.corveric;

import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.texture.Texture;

public class PlayerUtil {
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

    public static ColorRGBA getPlayerColor(String username){
        int texIndex;
        int sum = 0;
        for (char c : username.toCharArray()){
            sum += (int) c;
        }
        if (sum == 0){return ColorRGBA.Red;}

        texIndex = Integer.toString(sum).charAt(0) - '0';
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
        }[texIndex-1];
        return body_col;
    }

}
