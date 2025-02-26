package grafo.cdp.util;

public class FloatUtils {

    private static final int EPS = 100;

    public static int compareFloat(float f1, float f2) {
        return ((int)(f1*EPS)) - ((int)(f2*EPS));
    }

    public static float round(float f1) {
        return ((int)(f1 * EPS)) / ((float) EPS);
    }
}
