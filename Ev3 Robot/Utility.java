/**
 * Utility functions class.
 * @author group 16
 */
public class Utility {
    public static float shortestRotationAngle(float ang) {
        if (ang > 180)
            ang -= 360;
        else if (ang < -180)
            ang += 360;

        return ang;
    }

    public static float shortestDifferenceAngle(float ang1, float ang2) {
        float ang = ang1 - ang2;
        ang = (ang + 180) % 360 - 180;
        return ang;
    }
}
