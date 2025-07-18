package com.example.utils;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.util.Vector;

public class GeometryUtils {

    public static List<int[]> getTouchedCells(
            double cx, double cy, double cz,         // Центр параллелепипеда
            double width, double height, double depth, // Размеры
            double yawDeg                              // Угол поворота по горизонтали (в градусах)
    ) {
        double yawRad = Math.toRadians(-yawDeg); // Поворачиваем точку в обратную сторону

        // С запасом берём границы перебора
        int minX = (int)Math.floor(cx - Math.max(width, depth));
        int maxX = (int)Math.ceil(cx + Math.max(width, depth));
        int minY = (int)Math.floor(cy - height);
        int maxY = (int)Math.ceil(cy + height);
        int minZ = (int)Math.floor(cz - Math.max(width, depth));
        int maxZ = (int)Math.ceil(cz + Math.max(width, depth));

        List<int[]> result = new ArrayList<>();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    // Центр клетки
                    double px = x + 0.5;
                    double py = y + 0.5;
                    double pz = z + 0.5;

                    // Перенос точки в систему координат прямоугольника
                    double dx = px - cx;
                    double dy = py - cy;
                    double dz = pz - cz;

                    // Только поворот вокруг Y (горизонтальный)
                    double localX = dx * Math.cos(yawRad) + dz * Math.sin(yawRad);
                    double localZ = -dx * Math.sin(yawRad) + dz * Math.cos(yawRad);
                    double localY = dy; // не трогаем по высоте

                    // Проверка попадания в осевой параллелепипед
                    if (Math.abs(localX) <= width / 2 &&
                        Math.abs(localY) <= height / 2 &&
                        Math.abs(localZ) <= depth / 2) {
                        result.add(new int[]{x, y, z});
                    }
                }
            }
        }

        return result;
    }

    public static Vector rotateAroundY(Vector vec, float yawDegrees) {
        double yawRad = Math.toRadians(-yawDegrees);
        double cos = Math.cos(yawRad);
        double sin = Math.sin(yawRad);

        double x = vec.getX() * cos - vec.getZ() * sin;
        double z = vec.getX() * sin + vec.getZ() * cos;

        return new Vector(x, vec.getY(), z);
    }
}