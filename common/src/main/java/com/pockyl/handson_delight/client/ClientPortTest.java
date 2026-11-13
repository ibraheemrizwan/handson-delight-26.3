package com.pockyl.handson_delight.client;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import com.pockyl.diegesis.impl.client.DiegesisClientRuntime;
import com.pockyl.diegesis.impl.client.focus.FocusController;
import com.pockyl.diegesis.impl.client.render.*;
import com.pockyl.handson_delight.*;
import com.pockyl.handson_delight.platform.Platform;
import vectorwing.farmersdelight.common.block.entity.*;
/** Native cooking scenes, synchronized recipes, book drawing, reload and saved-world reopening. */
public final class ClientPortTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave save;
        try(var world=context.worldBuilder().create()) {
            save=world.getWorldSave();
            var server=world.getServer();
            if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("petrichor")) {
                server.runCommand("petrichor weather thunderstorm 600");
                server.runCommand("petrichor wetness 1");
            }
            String[] blocks={"cutting_board","cooking_pot","skillet","stove"};
            for(int i=0;i<blocks.length;i++) {
                int x=i*4;
                server.runCommand("setblock "+x+" -60 0 farmersdelight:"+blocks[i]);
                if(i==1 || i==2) server.runCommand("setblock "+x+" -61 0 minecraft:campfire");
            }
            server.runOnServer(game -> {
                var level=game.overworld();
                Platform.INSTANCE.setItem((CuttingBoardBlockEntity)level.getBlockEntity(new BlockPos(0,-60,0)),new ItemStack(Items.BEEF,4));
                for(var player:game.getPlayerList().getPlayers()) player.getInventory().setItem(0,new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(FarmersDelightContent.id("iron_knife"))));
                var pot=(CookingPotBlockEntity)level.getBlockEntity(new BlockPos(4,-60,0));
                Platform.INSTANCE.setItem(pot,0,new ItemStack(Items.BEEF,4));
                Platform.INSTANCE.setItem(pot,1,new ItemStack(Items.CARROT,4));
                Platform.INSTANCE.setItem(pot,2,new ItemStack(Items.POTATO,4));
                Platform.INSTANCE.setItem((SkilletBlockEntity)level.getBlockEntity(new BlockPos(8,-60,0)),new ItemStack(Items.BEEF,3));
                var stove=(AbstractStoveBlockEntity)level.getBlockEntity(new BlockPos(12,-60,0));
                Platform.INSTANCE.setItem(stove,0,new ItemStack(Items.PORKCHOP));
                level.sendBlockUpdated(new BlockPos(0,-60,0),level.getBlockState(new BlockPos(0,-60,0)),level.getBlockState(new BlockPos(0,-60,0)),3);
            });
            world.getConnection().waitForChunksRender();context.waitTicks(20);
            context.computeOnClient(client -> {
                if(KitchenRecipes.all(client.level,net.minecraft.world.item.crafting.RecipeType.CAMPFIRE_COOKING).isEmpty()) throw new AssertionError("Campfire recipes did not synchronize");
                if(Platform.INSTANCE.cookingRecipes(client.level).isEmpty()) throw new AssertionError("Cooking recipes did not synchronize");
                var sprite=SceneSprites.get(net.minecraft.resources.Identifier.withDefaultNamespace("item/carrot"));
                if(!sprite.contents().name().getPath().equals("item/carrot")) throw new AssertionError("Food sprite missing from native atlases");
                return null;
            });
            for(int i=0;i<blocks.length;i++) {
                int x=i*4;
                server.runCommand("tp @a "+(x+0.5)+" -60 -2.5 0 0");
                context.waitTicks(10);
                context.computeOnClient(client -> {
                    var pos=new BlockPos(x,-60,0);
                    var result=DiegesisClientRuntime.onUseBlock(client.player,net.minecraft.world.InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.NORTH,pos,false));
                    if(result==null) throw new AssertionError("Station did not intercept interaction at "+pos);
                    return null;
                });
                context.waitFor(client -> FocusController.get().isInteractive(),1200);
                context.computeOnClient(client -> {
                    if(!FocusController.get().station().pos().equals(new BlockPos(x,-60,0))) throw new AssertionError("Focused wrong station");
                    var input=com.pockyl.diegesis.impl.client.focus.FocusInput.get();
                    if(!client.gui.screen().keyPressed(new net.minecraft.client.input.KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_TAB,0,0))) throw new AssertionError("Native inventory key was not handled");
                    return null;
                });
                context.waitFor(client -> SceneRenderer.geometrySubmissions()>0 && SmoothPass.successfulFrames()>0,1200);
                context.waitTicks(35);
                context.takeScreenshot("handson-"+blocks[i]);
                if(i==0) {
                    context.computeOnClient(client -> {
                        FocusController.get().station().sendAction(com.pockyl.handson_delight.station.CuttingBoardStation.ACTION_CHOP,new net.minecraft.nbt.CompoundTag());
                        return null;
                    });
                    server.waitFor(game -> game.getPlayerList().getPlayers().stream().anyMatch(player -> player.getInventory().countItem(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(FarmersDelightContent.id("minced_beef")))==8),1200);
                    context.waitTicks(15);context.takeScreenshot("handson-after-network-chop");
                }
                if(i==1) {
                    context.computeOnClient(client -> {
                        var station=FocusController.get().station();var source=new PotBook();var current=source.current(station);
                        if(current==null || source.pages(station).stream().noneMatch(page -> page.key().equals(current))) throw new AssertionError("Cooking book did not select the current recipe");
                        return null;
                    });
                    server.runCommand("reload");
                    context.waitTicks(30);
                    var reload=context.computeOnClient(client -> client.reloadResourcePacks());
                    context.waitFor(client -> reload.isDone(),1200);reload.join();context.waitTicks(30);
                    context.takeScreenshot("handson-pot-after-resource-reload");
                }
                context.computeOnClient(client -> {FocusController.get().requestExit();return null;});
                context.waitFor(client -> !FocusController.get().isActive(),1200);
            }
            server.runOnServer(game -> {
                var board=(CuttingBoardBlockEntity)game.overworld().getBlockEntity(new BlockPos(0,-60,0));
                ((com.pockyl.handson_delight.station.BoardLayers)board).handson$layers().set(0,new ItemStack(Items.BREAD,2));
                ((com.pockyl.handson_delight.station.BoardLayers)board).handson$layersChanged();
            });
        }
        try(var world=save.open()) {
            boolean persisted=world.getServer().computeOnServer(game -> {
                var board=(CuttingBoardBlockEntity)game.overworld().getBlockEntity(new BlockPos(0,-60,0));
                var bread=((com.pockyl.handson_delight.station.BoardLayers)board).handson$layers().get(0);
                return bread.is(Items.BREAD) && bread.getCount()==2;
            });
            if(!persisted) throw new AssertionError("Board assembly did not persist across saved-world reload");
            context.waitFor(client -> !Platform.INSTANCE.cookingRecipes(client.level).isEmpty(),1200);
            context.waitTicks(15);
            context.takeScreenshot("handson-after-world-reload");
        }
        HandsOnDelight.LOGGER.info("HANDSON_CLIENT: four native cooking scenes, inventory key, network cutting, synchronized recipes, supersampled books, data/resource reload and saved-world reload passed");
    }
}
