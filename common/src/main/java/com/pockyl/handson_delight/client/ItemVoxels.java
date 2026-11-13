package com.pockyl.handson_delight.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SpriteContents;

import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

import com.pockyl.diegesis.api.client.MeshShape;
import com.pockyl.handson_delight.mixin.SpriteContentsAccessor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * An item lying flat, built from its icon's pixels as one pixel thick voxels, so that a knife can cut it where it came down: the
 * pixels on either side of a cut move apart, along a line at any angle. The icon's top lies away from the viewer, its left on the viewer's left (+x).
 */
final class ItemVoxels {
    private static final Map<Item, Optional<Icon>> ICONS = new HashMap<>();
    /** The model the game falls back on when the icons were read: a new one after every resource reload. */
    private static Object readWith;

    private ItemVoxels() {
    }

    /** The pixels of an item's flat icon: the sprite of the block atlas they come from, and which of them are solid. */
    record Icon(Identifier sprite, int width, int height, boolean[] solid, int[] colors) {
        /** The pixel's colour, ARGB. */
        int color(int column, int row) {
            return colors[row * width + column];
        }

        boolean solid(int column, int row) {
            return column >= 0 && row >= 0 && column < width && row < height && solid[row * width + column];
        }

        /** The first and last columns and rows with a solid pixel: {left, right, top, bottom}. */
        int[] bounds() {
            int[] bounds = {width, -1, height, -1};
            for (int row = 0; row < height; row++) {
                for (int column = 0; column < width; column++) {
                    if (solid(column, row)) {
                        bounds[0] = Math.min(bounds[0], column);
                        bounds[1] = Math.max(bounds[1], column);
                        bounds[2] = Math.min(bounds[2], row);
                        bounds[3] = Math.max(bounds[3], row);
                    }
                }
            }
            return bounds;
        }

        /** How long the icon draws {@code size} pixels across at its longest side: node pixels per icon pixel. */
        float pixel(float size) {
            return size / Math.max(width, height);
        }

        /**
         * The line the item lies along, node pixels for an icon {@code size} across: through the middle of its solid pixels, along
         * their longest spread, pointing away from the viewer (its far end; the tip of a stick drawn as vanilla draws sticks).
         */
        Axis axis(float size) {
            float pixel = pixel(size);
            double sumX = 0;
            double sumZ = 0;
            int count = 0;
            for (int row = 0; row < height; row++) {
                for (int column = 0; column < width; column++) {
                    if (solid(column, row)) {
                        sumX += middleX(column);
                        sumZ += middleZ(row);
                        count++;
                    }
                }
            }
            if (count == 0) {
                return Axis.square(size / 2.0F);
            }
            double cx = sumX / count;
            double cz = sumZ / count;
            double xx = 0;
            double zz = 0;
            double xz = 0;
            for (int row = 0; row < height; row++) {
                for (int column = 0; column < width; column++) {
                    if (solid(column, row)) {
                        double dx = middleX(column) - cx;
                        double dz = middleZ(row) - cz;
                        xx += dx * dx;
                        zz += dz * dz;
                        xz += dx * dz;
                    }
                }
            }
            double angle = 0.5 * Math.atan2(2.0 * xz, xx - zz);
            float dirX = (float) Math.cos(angle);
            float dirZ = (float) Math.sin(angle);
            if (dirZ < -1.0e-3F || Math.abs(dirZ) <= 1.0e-3F && dirX < 0.0F) {
                dirX = -dirX;
                dirZ = -dirZ;
            }
            double spread = Math.sqrt(((xx - zz) / 2.0) * ((xx - zz) / 2.0) + xz * xz);
            double big = (xx + zz) / 2.0 + spread;
            double small = Math.max(1.0e-6, (xx + zz) / 2.0 - spread);
            float from = Float.MAX_VALUE;
            float to = -Float.MAX_VALUE;
            for (int row = 0; row < height; row++) {
                for (int column = 0; column < width; column++) {
                    if (solid(column, row)) {
                        float along = (float) ((middleX(column) - cx) * dirX + (middleZ(row) - cz) * dirZ);
                        from = Math.min(from, along - 0.5F);
                        to = Math.max(to, along + 0.5F);
                    }
                }
            }
            return new Axis((float) cx * pixel, (float) cz * pixel, dirX, dirZ, from * pixel, to * pixel,
                    (float) Math.sqrt(big / small));
        }

        /**
         * Where along a line {@code x + t * cos(angle), z + t * sin(angle)} (node pixels) the solid pixels of the icon drawn {@code size}
         * across lie: {from, to}; empty ({@code from > to}) when it misses them.
         */
        float[] solidSpan(float x, float z, float angle, float size) {
            float pixel = pixel(size);
            float alongX = Mth.cos(angle);
            float alongZ = Mth.sin(angle);
            float from = Float.MAX_VALUE;
            float to = -Float.MAX_VALUE;
            for (float t = -size; t <= size; t += pixel / 4.0F) {
                float px = (x + alongX * t) / pixel;
                float pz = (z + alongZ * t) / pixel;
                int column = Mth.floor(width / 2.0F - px);
                int row = Mth.floor(height / 2.0F - pz);
                if (solid(column, row)) {
                    from = Math.min(from, t);
                    to = Math.max(to, t);
                }
            }
            return new float[] {from, to};
        }

        /**
         * One half of the icon split along its own length through its middle: the far half or the near one (a loaf opened into a
         * top and a bottom).
         */
        Icon half(boolean far) {
            Axis axis = axis(Math.max(width, height));
            boolean[] kept = new boolean[solid.length];
            for (int row = 0; row < height; row++) {
                for (int column = 0; column < width; column++) {
                    if (!solid(column, row)) {
                        continue;
                    }
                    // Which side of the line along the item the pixel lies on: across it, towards +z (far) or -z.
                    float side = (middleX(column) - axis.x()) * -axis.dirZ() + (middleZ(row) - axis.z()) * axis.dirX();
                    float towardsFar = -axis.dirZ() * 0.0F + axis.dirX();
                    kept[row * width + column] = (side * (towardsFar >= 0.0F ? 1.0F : -1.0F) >= 0.0F) == far;
                }
            }
            return new Icon(sprite, width, height, kept, colors);
        }

        private float middleX(int column) {
            return width / 2.0F - column - 0.5F;
        }

        private float middleZ(int row) {
            return height / 2.0F - row - 0.5F;
        }
    }

    /**
     * The line an item lies along, node pixels: its middle {@code x, z}, the way to its far end {@code dirX, dirZ}, where its solid
     * pixels start and end along it ({@code from} below 0, {@code to} above), and how much longer than wide it is (1 for round).
     */
    record Axis(float x, float z, float dirX, float dirZ, float from, float to, float elongation) {
        /** A block drawn as an item: a square {@code half} pixels from its middle to each side, straight away from the viewer. */
        static Axis square(float half) {
            return new Axis(0.0F, 0.0F, 0.0F, 1.0F, -half, half, 1.0F);
        }

        /** The point a fraction {@code spot} of the length from the far end. */
        float[] at(float spot) {
            float along = to - (to - from) * spot;
            return new float[] {x + dirX * along, z + dirZ * along};
        }

        /** The angle of a line square across the item, radians from straight across (along x), between -90 and 90 degrees. */
        float across() {
            return normalized((float) Math.atan2(dirX, -dirZ));
        }

        /** The angle of the item's own length, radians from straight across. */
        float along() {
            return (float) Math.atan2(dirZ, dirX);
        }

        /** {@code angle} turned by half turns into -90..90 degrees: a line is the same either way. */
        static float normalized(float angle) {
            while (angle > Mth.HALF_PI) {
                angle -= Mth.PI;
            }
            while (angle <= -Mth.HALF_PI) {
                angle += Mth.PI;
            }
            return angle;
        }
    }

    /** The icon of an item drawn flat; empty for one drawn as a block, which is not cut into rows. */
    static Optional<Icon> icon(ItemStack stack) {
        Object missing = KitchenSprites.marker();
        if (missing != readWith) {
            // Resources were reloaded: the icons read so far may show other textures now.
            readWith = missing;
            ICONS.clear();
        }
        return ICONS.computeIfAbsent(stack.getItem(), item -> read(stack));
    }

    private static Optional<Icon> read(ItemStack stack) {
        var model = KitchenSprites.model(stack);
        if (model.usesBlockLight() || com.pockyl.diegesis.impl.client.render.SceneItems.custom(model)) {
            return Optional.empty();
        }
        SpriteContents contents = KitchenSprites.sprite(stack).contents();
        NativeImage image = ((SpriteContentsAccessor) contents).handson$originalImage();
        int width = contents.width();
        int height = contents.height();
        boolean[] solid = new boolean[width * height];
        int[] colors = new int[width * height];
        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                int abgr = net.minecraft.util.ARGB.toABGR(image.getPixel(column, row));
                solid[row * width + column] = (abgr >>> 24) >= 128;
                colors[row * width + column] = (abgr & 0xFF00FF00) | (abgr & 0xFF) << 16 | (abgr >> 16) & 0xFF;
            }
        }
        return Optional.of(new Icon(contents.name(), width, height, solid, colors));
    }

    /**
     * A cut across an item lying flat: the line through {@code x, z} (node pixels from the item's middle) at {@code angle} radians
     * from straight across (along x). Pixels are on one side or the other by their middles: the cut goes in steps, as pixels do.
     */
    record Cut(float x, float z, float angle) {
        /** The line's normal, towards +z when the cut runs straight across. */
        float normalX() {
            return -Mth.sin(angle);
        }

        float normalZ() {
            return Mth.cos(angle);
        }

        /** How far {@code px, pz} lies from the line, on the normal's side positive. */
        float distance(float px, float pz) {
            return normalX() * (px - x) + normalZ() * (pz - z);
        }
    }

    /**
     * The icon {@code size} pixels across at its longest side, cut along {@code cuts}, the pieces {@code gap} pixels apart: each one
     * moves away from every cut it lies beside.
     */
    static MeshShape build(Icon icon, float size, List<Cut> cuts, float gap) {
        int width = icon.width();
        int height = icon.height();
        float pixel = size / Math.max(width, height);
        int[] piece = new int[width * height];
        float[] shiftX = new float[width * height];
        float[] shiftZ = new float[width * height];
        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                int index = row * width + column;
                float middleX = (width / 2.0F - column - 0.5F) * pixel;
                float middleZ = (height / 2.0F - row - 0.5F) * pixel;
                for (int i = 0; i < cuts.size(); i++) {
                    Cut cut = cuts.get(i);
                    boolean beyond = cut.distance(middleX, middleZ) > 0.0F;
                    float half = (beyond ? gap : -gap) / 2.0F;
                    piece[index] |= beyond ? 1 << i : 0;
                    shiftX[index] += cut.normalX() * half;
                    shiftZ[index] += cut.normalZ() * half;
                }
            }
        }
        MeshShape.Builder shape = MeshShape.builder();
        for (int row = 0; row < height; row++) {
            int start = -1;
            for (int column = 0; column <= width; column++) {
                boolean solid = icon.solid(column, row);
                int index = row * width + column;
                if (start >= 0 && !(solid && piece[index] == piece[row * width + start])) {
                    int first = row * width + start;
                    top(shape, icon, pixel, start, column, row, shiftX[first], shiftZ[first]);
                    start = -1;
                }
                if (!solid) {
                    continue;
                }
                if (start < 0) {
                    start = column;
                }
                int own = piece[index];
                float left = (width / 2.0F - column) * pixel + shiftX[index];
                float right = left - pixel;
                float far = (height / 2.0F - row) * pixel + shiftZ[index];
                float near = far - pixel;
                float u = (column + 0.5F) / width;
                float v = (row + 0.5F) / height;
                if (!joined(icon, piece, column - 1, row, own)) {
                    side(shape, new Vector3f(left, 0, near), new Vector3f(left, pixel, near), new Vector3f(left, pixel, far),
                            new Vector3f(left, 0, far), 1, 0, 0, u, v);
                }
                if (!joined(icon, piece, column + 1, row, own)) {
                    side(shape, new Vector3f(right, 0, near), new Vector3f(right, pixel, near), new Vector3f(right, pixel, far),
                            new Vector3f(right, 0, far), -1, 0, 0, u, v);
                }
                if (!joined(icon, piece, column, row - 1, own)) {
                    side(shape, new Vector3f(left, 0, far), new Vector3f(left, pixel, far), new Vector3f(right, pixel, far),
                            new Vector3f(right, 0, far), 0, 0, 1, u, v);
                }
                if (!joined(icon, piece, column, row + 1, own)) {
                    side(shape, new Vector3f(left, 0, near), new Vector3f(left, pixel, near), new Vector3f(right, pixel, near),
                            new Vector3f(right, 0, near), 0, 0, -1, u, v);
                }
            }
        }
        return shape.build();
    }

    /**
     * A thin film the shape of {@code shape} drawn {@code size} across, {@code thick} pixels high, all in the colour of one pixel of
     * {@code colour}'s sprite: a liquid poured over food. Only its top and edges are drawn.
     */
    static MeshShape film(Icon shape, float size, float thick, Icon colour, int column, int row) {
        float pixel = shape.pixel(size);
        float u = (column + 0.5F) / colour.width();
        float v = (row + 0.5F) / colour.height();
        MeshShape.Builder mesh = MeshShape.builder();
        for (int r = 0; r < shape.height(); r++) {
            for (int c = 0; c < shape.width(); c++) {
                if (!shape.solid(c, r)) {
                    continue;
                }
                float left = (shape.width() / 2.0F - c) * pixel;
                float right = left - pixel;
                float far = (shape.height() / 2.0F - r) * pixel;
                float near = far - pixel;
                mesh.corner(right, thick, near, u, v, 0, 1, 0).corner(right, thick, far, u, v, 0, 1, 0)
                        .corner(left, thick, far, u, v, 0, 1, 0).corner(left, thick, near, u, v, 0, 1, 0);
                if (!shape.solid(c, r + 1)) {
                    side(mesh, new Vector3f(left, 0, near), new Vector3f(left, thick, near), new Vector3f(right, thick, near),
                            new Vector3f(right, 0, near), 0, 0, -1, u, v);
                }
            }
        }
        return mesh.build();
    }

    /**
     * The pixel of {@code full} that shows what is in it: the commonest colour among the pixels where {@code empty} (its container)
     * differs, {column, row}; the middle solid pixel without a container.
     */
    static int[] contents(Icon full, Optional<Icon> empty) {
        Map<Integer, Integer> counts = new HashMap<>();
        Map<Integer, int[]> where = new HashMap<>();
        for (int row = 0; row < full.height(); row++) {
            for (int column = 0; column < full.width(); column++) {
                if (!full.solid(column, row)) {
                    continue;
                }
                int color = full.color(column, row);
                boolean same = empty.isPresent() && empty.get().width() == full.width() && empty.get().height() == full.height()
                        && empty.get().solid(column, row) && empty.get().color(column, row) == color;
                if (!same) {
                    counts.merge(color, 1, Integer::sum);
                    where.putIfAbsent(color, new int[] {column, row});
                }
            }
        }
        return counts.entrySet().stream().max(Map.Entry.comparingByValue()).map(entry -> where.get(entry.getKey()))
                .orElse(new int[] {full.width() / 2, full.height() / 2});
    }

    /** Whether the pixel is solid and in the same piece: no face between the two then. */
    private static boolean joined(Icon icon, int[] piece, int column, int row, int with) {
        return icon.solid(column, row) && piece[row * icon.width() + column] == with;
    }

    /** The top of a run of pixels of one piece in a row, the icon mapped straight onto it, moved with its piece. */
    private static void top(MeshShape.Builder shape, Icon icon, float pixel, int from, int to, int row, float shiftX, float shiftZ) {
        float west = (icon.width() / 2.0F - to) * pixel + shiftX;
        float east = (icon.width() / 2.0F - from) * pixel + shiftX;
        float far = (icon.height() / 2.0F - row) * pixel + shiftZ;
        float near = far - pixel;
        float u0 = (float) from / icon.width();
        float u1 = (float) to / icon.width();
        float vFar = (float) row / icon.height();
        float vNear = (float) (row + 1) / icon.height();
        shape.corner(west, pixel, near, u1, vNear, 0, 1, 0)
                .corner(west, pixel, far, u1, vFar, 0, 1, 0)
                .corner(east, pixel, far, u0, vFar, 0, 1, 0)
                .corner(east, pixel, near, u0, vNear, 0, 1, 0);
    }

    /** A side face in one pixel's colour, turned to face out along its normal whichever way the corners were given. */
    private static void side(MeshShape.Builder shape, Vector3f a, Vector3f b, Vector3f c, Vector3f d, float nx, float ny, float nz,
            float u, float v) {
        Vector3f normal = new Vector3f(b).sub(a).cross(new Vector3f(c).sub(b));
        Vector3f[] corners = normal.dot(nx, ny, nz) >= 0 ? new Vector3f[] {a, b, c, d} : new Vector3f[] {d, c, b, a};
        for (Vector3f corner : corners) {
            shape.corner(corner.x, corner.y, corner.z, u, v, nx, ny, nz);
        }
    }
}
