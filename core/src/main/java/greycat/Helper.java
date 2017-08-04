package greycat;

public class Helper {

    /**
     * {@native ts
     * var hash = 0, i, chr, len;
     * if (data.length === 0) return hash;
     * for (i = 0, len = data.length; i < len; i++) {
     * chr   = data.charCodeAt(i);
     * hash  = ((hash << 5) - hash) + chr;
     * hash |= 0; // Convert to 32bit integer
     * }
     * return hash;
     * }
     */
    public static int hash(String data) {
        return data.hashCode();
    }

    public static long longHash(long number, long max) {
        long hash = number % max;
        return hash < 0 ? hash * -1 : hash;
    }

}
