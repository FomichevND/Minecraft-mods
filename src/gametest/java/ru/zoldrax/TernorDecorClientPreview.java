package ru.zoldrax;

import java.io.File;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.item.*;
import net.minecraft.util.math.*;
import net.minecraft.world.GameMode;

/** Reproducible real-client review. This gametest source is never packaged in the mod JAR. */
public final class TernorDecorClientPreview implements ClientModInitializer {
 private static final DecorKind[] KINDS={DecorKind.GARDEN_CANOPY,DecorKind.STORAGE_SHELF,DecorKind.ANIMAL_FEEDER,DecorKind.ANIMAL_TROUGH,DecorKind.STONE_FORGE,DecorKind.BELLOWS,DecorKind.QUENCHING_TUB};
 private static final String[] VIEWS={"empty-front","filled-front","filled-side","filled-top"};
 private final File out=new File("ternor-review");private boolean setup;private int ticks,step=-1;
 private static BlockPos root(int i){return new BlockPos(210+(i%4)*9,70,20+(i/4)*10);}
 @Override public void onInitializeClient(){if(Boolean.getBoolean("zoldrax.ternorPreview"))ClientTickEvents.END_CLIENT_TICK.register(this::tick);}
 private void tick(MinecraftClient client){if(client.world==null||client.player==null||client.getServer()==null)return;
  if(!setup){setup=true;out.mkdirs();client.options.hudHidden=true;client.options.getFov().setValue(58);client.options.getViewDistance().setValue(6);client.getTutorialManager().setStep(net.minecraft.client.tutorial.TutorialStep.NONE);client.getServer().execute(()->setupWorld(client));return;}
  if(step>=0&&step<28&&step/4==5&&step%4>0&&ticks%8==0)client.getServer().execute(()->{var e=entity(client,5);if(e!=null)e.ternor.activateBellows();});
  if(step>=0&&step<28&&step/4==6&&step%4>0&&ticks%12==0)client.getServer().execute(()->{var e=entity(client,6);if(e!=null)e.getWorld().addSyncedBlockEvent(e.getPos(),e.getCachedState().getBlock(),4,0);});
  if(step==-1){ticks++;if(ticks==70)client.getServer().execute(()->client.getServer().getOverworld().setTimeOfDay(6000));if(ticks<170)return;ticks=0;step=Boolean.getBoolean("zoldrax.ternorFeederOnly")?8:0;prepare(client,step);return;}
  if(++ticks<45)return;ticks=0;if(step>=0)ScreenshotRecorder.saveScreenshot(out,fileName(step),client.getFramebuffer(),t->System.out.println("TERNOR_SCREENSHOT "+step));
  if(++step>=34||Boolean.getBoolean("zoldrax.ternorFeederOnly")&&step>=12){System.out.println("TERNOR_PREVIEW_COMPLETE");client.scheduleStop();return;}prepare(client,step);
 }
 private DecorBlockEntity entity(MinecraftClient client,int i){return client.getServer().getOverworld().getBlockEntity(root(i)) instanceof DecorBlockEntity e?e:null;}
 private void setupWorld(MinecraftClient client){var w=client.getServer().getOverworld();var p=client.getServer().getPlayerManager().getPlayer(client.player.getUuid());p.changeGameMode(GameMode.SPECTATOR);p.stopRiding();w.setTimeOfDay(6000);w.getGameRules().get(net.minecraft.world.GameRules.DO_DAYLIGHT_CYCLE).set(false,client.getServer());w.getGameRules().get(net.minecraft.world.GameRules.DO_MOB_SPAWNING).set(false,client.getServer());
  for(var pos:BlockPos.iterate(205,69,14,245,78,38))w.setBlockState(pos,pos.getY()==69?Blocks.GRASS_BLOCK.getDefaultState():Blocks.AIR.getDefaultState(),3);for(var drop:w.getEntitiesByClass(net.minecraft.entity.ItemEntity.class,new Box(202,60,11,248,85,41),e->true))drop.discard();w.setTimeOfDay(18000);
  for(int i=0;i<KINDS.length;i++){var r=root(i);var block=DecorBlocks.BLOCKS.get(KINDS[i]);w.setBlockState(r,block.getDefaultState(),3);block.onPlaced(w,r,block.getDefaultState(),p,new ItemStack(block));w.setBlockState(r.west().down(),Blocks.STONE_BRICKS.getDefaultState(),3);w.setBlockState(r.east(KINDS[i].width).down(),Blocks.OAK_PLANKS.getDefaultState(),3);}
 }
 private void fill(DecorBlockEntity e){e.clear();switch(e.kind){
   case GARDEN_CANOPY->{Item[] plants={Items.POPPY,Items.BLUE_ORCHID,Items.OAK_SAPLING,Items.FERN,Items.VINE,Items.SWEET_BERRIES};for(int i=0;i<plants.length;i++)e.setStack(i,new ItemStack(plants[i]));}
   case STORAGE_SHELF->{Item[] items={Items.IRON_INGOT,Items.OAK_PLANKS,Items.COBBLESTONE,Items.WHEAT,Items.WHEAT_SEEDS,Items.IRON_PICKAXE,Items.BREAD,Items.APPLE,Items.CARROT};for(int i=0;i<items.length;i++)e.setStack(i,new ItemStack(items[i],items[i].getMaxCount()==1?1:32));}
   case ANIMAL_FEEDER->{Item[] feed={Items.WHEAT,Items.CARROT,Items.POTATO,Items.BEETROOT,Items.WHEAT_SEEDS,Items.PUMPKIN_SEEDS,Items.MELON_SEEDS,Items.BEETROOT_SEEDS,Items.TORCHFLOWER_SEEDS};for(int i=0;i<9;i++)e.setStack(i,new ItemStack(feed[i],64));}
   case ANIMAL_TROUGH,QUENCHING_TUB->{e.ternor.water=4;e.changed();}
   case STONE_FORGE->{e.setStack(0,new ItemStack(Items.IRON_INGOT,8));e.setStack(1,new ItemStack(Items.COAL,8));e.ternor.burn=e.ternor.fuelTotal=1600;e.setLit(true);}
   case BELLOWS->e.ternor.activateBellows();default->{}
  }}
 private void empty(DecorBlockEntity e){e.clear();e.ternor.water=e.ternor.burn=e.ternor.fuelTotal=e.ternor.cook=e.ternor.workTicks=0;if(e.kind==DecorKind.STONE_FORGE)e.setLit(false);e.changed();}
 private void prepare(MinecraftClient client,int target){client.setScreen(null);client.getServer().execute(()->{var w=client.getServer().getOverworld();var p=client.getServer().getPlayerManager().getPlayer(client.player.getUuid());p.changeGameMode(GameMode.SPECTATOR);if(target<28){int i=target/4,view=target%4;var r=root(i);var e=entity(client,i);if(view==0)empty(e);else if(view==1)fill(e);double cx=r.getX()+KINDS[i].width/2.0,cy=r.getY()+(KINDS[i].height==1?1.5:KINDS[i].height/2.0);double x=cx,z=r.getZ()+5.5;float yaw=180,pitch=KINDS[i].height==1?18:10;if(view==2){x=r.getX()+KINDS[i].width+5;z=r.getZ()+.5;yaw=90;}if(view==3){x=cx;z=r.getZ()+.5;cy=r.getY()+KINDS[i].height+5;pitch=90;}p.networkHandler.requestTeleport(x,cy-1.62,z,yaw,pitch);return;}
   for(int i=0;i<KINDS.length;i++)fill(entity(client,i));if(target==28||target==29){w.setTimeOfDay(target==29?18000:6000);p.networkHandler.requestTeleport(223,82,45,180,25);return;}w.setTimeOfDay(6000);int i=target==30?1:target==31?2:4;var r=root(i);p.changeGameMode(GameMode.CREATIVE);p.networkHandler.requestTeleport(r.getX()+KINDS[i].width/2.0,70,r.getZ()+5,180,10);var e=entity(client,i);if(target==32){p.setSneaking(true);e.use(p,net.minecraft.util.Hand.MAIN_HAND);p.setSneaking(false);}else e.use(p,net.minecraft.util.Hand.MAIN_HAND);
  });}
 private static String fileName(int step){if(step<28)return String.format("%02d-%s-%s.png",step,KINDS[step/4].id,VIEWS[step%4]);return switch(step){case 28->"28-all-day.png";case 29->"29-all-night.png";case 30->"30-storage-menu.png";case 31->"31-feeder-menu.png";case 32->"32-forge-smelting-menu.png";default->"33-forge-repair-menu.png";};}
}
