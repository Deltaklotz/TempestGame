package dev.corveric;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;

public class RotationUtil {
    public static Quaternion fromDegrees(float xDeg, float yDeg, float zDeg) {
        float xRad = xDeg * FastMath.DEG_TO_RAD;
        float yRad = yDeg * FastMath.DEG_TO_RAD;
        float zRad = zDeg * FastMath.DEG_TO_RAD;
        return new Quaternion().fromAngles(xRad, yRad, zRad);
    }
    public static float mapRange(float value, float inMin, float inMax, float outMin, float outMax) {
        return (value - inMin) * (outMax - outMin) / (inMax - inMin) + outMin;
    }

}
