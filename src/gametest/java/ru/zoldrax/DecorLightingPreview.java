package ru.zoldrax;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.item.*;
import net.minecraft.util.math.*;
import net.minecraft.world.GameMode;
public class DecorLightingPreview implements ClientModInitializer {
 int ticks,step=-1;boolean setup;final BlockPos shelf=new BlockPos(150,70,20),basket=new BlockPos(158,70,20);final java.io.File out=new java.io.File("lighting-review");
 public void onInitializeClient(){if(!Boolean.getBoolean("zoldrax.lightingPreview"))return;ClientTickEvents.END_CLIENT_TICK.register(this::tick);}
 void tick(MinecraftClient c){if(c.world==null||c.player==null||c.getServer()==null)return;
  if(!setup){setup=true;out.mkdirs();c.options.hudHidden=true;c.options.getFov().setValue(55);c.getTutorialManager().setStep(net.minecraft.client.tutorial.TutorialStep.NONE);c.getServer().execute(()->{var w=c.getServer().getOverworld();var p=c.getServer().getPlayerManager().getPlayer(c.player.getUuid());p.stopRiding();p.changeGameMode(GameMode.CREATIVE);p.getAbilities().flying=true;p.sendAbilitiesUpdate();p.removeStatusEffect(net.minecraft.entity.effect.StatusEffects.NIGHT_VISION);w.getGameRules().get(net.minecraft.world.GameRules.DO_DAYLIGHT_CYCLE).set(false,c.getServer());w.getGameRules().get(net.minecraft.world.GameRules.DO_MOB_SPAWNING).set(false,c.getServer());for(var r:BlockPos.iterate(145,69,14,164,76,29))w.setBlockState(r,r.getY()==69?Blocks.GRASS_BLOCK.getDefaultState():Blocks.AIR.getDefaultState(),3);
   for(var k:new DecorKind[]{DecorKind.FOOD_SHELF,DecorKind.SUPPLY_BASKET}){var r=k==DecorKind.FOOD_SHELF?shelf:basket;var b=DecorBlocks.BLOCKS.get(k);w.setBlockState(r,b.getDefaultState(),3);b.onPlaced(w,r,b.getDefaultState(),p,new ItemStack(b));}
   w.setBlockState(shelf.west(),Blocks.JUNGLE_PLANKS.getDefaultState(),3);w.setBlockState(shelf.east(3),Blocks.RED_WOOL.getDefaultState(),3);w.setBlockState(shelf.east(4),Blocks.GOLD_BLOCK.getDefaultState(),3);
  });return;}
  if(++ticks==10&&step==-1)c.getServer().execute(()->{var w=c.getServer().getOverworld();for(var drop:w.getEntitiesByClass(net.minecraft.entity.ItemEntity.class,new Box(144,60,13,166,85,30),e->true))drop.discard();});if(ticks<65)return;ticks=0;
  if(step>=0){System.out.println("LIGHTING_DEBUG step="+step+" skyBrightness="+c.world.getSkyBrightness(0)+" time="+c.world.getTimeOfDay()+" sky="+c.world.getLightLevel(net.minecraft.world.LightType.SKY,shelf));try{var manager=c.gameRenderer.getLightmapTextureManager();var field=manager.getClass().getDeclaredField("image");field.setAccessible(true);((net.minecraft.client.texture.NativeImage)field.get(manager)).writeTo(java.nio.file.Path.of("lighting-review/lightmap-"+step+".png"));}catch(Exception e){System.out.println(e);}}
  if(step>=0)ScreenshotRecorder.saveScreenshot(out,String.format("%02d",step)+new String[]{"-shelf-day","-shelf-night","-shelf-torch","-basket-full-day","-basket-full-night","-basket-empty","-basket-one","-basket-quarter","-basket-half","-basket-full-mixed","-basket-full-bread","-basket-full-fish","-shelf-day-stable","-shelf-night-stable"}[step]+".png",c.getFramebuffer(),t->{});
  if(++step>=14){System.out.println("LIGHTING_PREVIEW_COMPLETE");c.scheduleStop();return;}final int s=step;c.getServer().execute(()->{var w=c.getServer().getOverworld();var p=c.getServer().getPlayerManager().getPlayer(c.player.getUuid());w.setTimeOfDay(s==1||s==2||s==4||s==13?18000:6000);var r=s<3||s>=12?shelf:basket;p.networkHandler.requestTeleport(r.getX()+(s<3||s>=12?1.5:.5),s<3||s>=12?70.2:70.5,r.getZ()+4.5,180,s<3||s>=12?7:22);w.setBlockState(shelf.add(1,0,2),s==2?Blocks.TORCH.getDefaultState():Blocks.AIR.getDefaultState(),3);var e=(DecorBlockEntity)w.getBlockEntity(basket);e.clear();if(s==3||s==4||s>=7&&s<=11){for(int i=0;i<9;i++)e.setStack(i,new ItemStack(s==10?Items.BREAD:s==11?Items.COD:s==9&&i%3==0?Items.CARROT:Items.APPLE,s==7?16:s==8?32:64));}if(s==6)e.setStack(0,new ItemStack(Items.APPLE));});
 }
}
