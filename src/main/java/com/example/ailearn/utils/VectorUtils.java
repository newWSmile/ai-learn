package com.example.ailearn.utils;

public class VectorUtils {

    private VectorUtils() {
    }

    /**
     * 这个分数一般在 -1 ~ 1 之间
     * 越接近 1，越相似
     * @param a
     * @param b
     * @return
     */
    public static double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || b.length == 0 || a.length != b.length) {
            return 0.0D;
        }

        double dot = 0.0D;
        double normA = 0.0D;
        double normB = 0.0D;

        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0.0D || normB == 0.0D) {
            return 0.0D;
        }

        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

}
