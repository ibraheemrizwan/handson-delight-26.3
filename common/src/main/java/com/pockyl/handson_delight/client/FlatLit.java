package com.pockyl.handson_delight.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pockyl.diegesis.api.client.SceneBuffers;

/**
 * Items drawn on a page are icons, lit as in the inventory: every normal points up, into both of the level's lights, so a flat
 * icon is never shaded by the way the page happens to face. Since 1.20.5 the entity shader lights normals in the pose's own
 * space, and the book's items came out darker than on 1.20.1.
 */
final class FlatLit implements VertexConsumer {
    private final VertexConsumer delegate;

    private FlatLit(VertexConsumer delegate) {
        this.delegate = delegate;
    }

    static SceneBuffers of(SceneBuffers buffers) {
        return type -> new FlatLit(buffers.getBuffer(type));
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        delegate.addVertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setColor(int red, int green, int blue, int alpha) {
        delegate.setColor(red, green, blue, alpha);
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        delegate.setUv(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        delegate.setUv1(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        delegate.setUv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        delegate.setNormal(0.0F, 1.0F, 0.0F);
        return this;
    }

    @Override
    public void addVertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float normalX, float normalY,
            float normalZ) {
        delegate.addVertex(x, y, z, color, u, v, overlay, light, 0.0F, 1.0F, 0.0F);
    }
    @Override public VertexConsumer setColor(int color) {return setColor(color>>16&255,color>>8&255,color&255,color>>>24);}
    @Override public VertexConsumer setUv3(float u,float v) {delegate.setUv3(u,v);return this;}
    @Override public VertexConsumer setLineWidth(float width) {delegate.setLineWidth(width);return this;}
}
