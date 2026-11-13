package com.pockyl.handson_delight.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import com.pockyl.diegesis.impl.client.render.SceneItems;
final class KitchenSprites {
    static Object marker() { return Minecraft.getInstance().getModelManager().getBlockStateModelSet().missingModel(); }
    static net.minecraft.client.renderer.item.ItemStackRenderState model(ItemStack stack) {
        return SceneItems.resolve(stack,ItemDisplayContext.FIXED,Minecraft.getInstance().level,0);
    }
    static TextureAtlasSprite sprite(ItemStack stack) {
        var material=model(stack).pickParticleMaterial(RandomSource.create(42));
        if(material==null) return Minecraft.getInstance().getModelManager().getBlockStateModelSet().missingModel().particleMaterial().sprite();
        return material.sprite();
    }
}
