package ru.zoldrax;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.*;
import net.minecraft.client.*;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.world.GameMode;

/** Optional development-only visual review, excluded from the released mod. */
public class DecorClientPreview implements ClientModInitializer {
 private int ticks,step=-1;private boolean prepared;
 private static final java.io.File OUTPUT=new java.io.File("review");
 public void onInitializeClient(){if(!Boolean.getBoolean("zoldrax.decorPreview"))return;ClientTickEvents.END_CLIENT_TICK.register(this::tick);}
 private static BlockPos root(int i){return new BlockPos(10+i*8,i==3?73:70,10);}
 private void tick(MinecraftClient client){
  if(client.world==null||client.player==null||client.getServer()==null)return;
  if(!prepared){OUTPUT.mkdirs();prepared=true;client.getTutorialManager().setStep(net.minecraft.client.tutorial.TutorialStep.NONE);client.options.hudHidden=true;client.options.getViewDistance().setValue(5);client.options.getFov().setValue(55);client.getServer().execute(()->{
   var server=client.getServer();var w=server.getOverworld();w.setTimeOfDay(6000);w.getGameRules().get(net.minecraft.world.GameRules.DO_DAYLIGHT_CYCLE).set(false,server);w.getGameRules().get(net.minecraft.world.GameRules.DO_MOB_SPAWNING).set(false,server);
   var player=server.getPlayerManager().getPlayer(client.player.getUuid());player.changeGameMode(GameMode.SPECTATOR);player.removeStatusEffect(net.minecraft.entity.effect.StatusEffects.NIGHT_VISION);
   for(int i=0;i<8;i++){var r=root(i);for(var pos:BlockPos.iterate(r.getX()-4,69,4,r.getX()+5,77,16))w.setBlockState(pos,pos.getY()==69?Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState():Blocks.AIR.getDefaultState(),3);
    var kind=DecorKind.values()[i];if(!kind.hanging()){for(int dx=0;dx<kind.width;dx++)w.setBlockState(r.add(dx,-1,0),Blocks.SEA_LANTERN.getDefaultState(),3);}if(kind.hanging())w.setBlockState(r.up(),Blocks.SEA_LANTERN.getDefaultState(),3);
    System.out.println("DECOR_SERVER_LIGHT "+kind+" sky="+w.getLightLevel(net.minecraft.world.LightType.SKY,r)+" hasSky="+w.getDimension().hasSkyLight());var block=DecorBlocks.BLOCKS.get(kind);var state=block.getDefaultState().with(DecorBaseBlock.FACING,Direction.SOUTH);w.setBlockState(r,state,3);block.onPlaced(w,r,state,player,new ItemStack(block));var e=(DecorBlockEntity)w.getBlockEntity(r);
    if(kind==DecorKind.WEAPON_RACK){e.setStack(0,new ItemStack(Items.DIAMOND_SWORD));e.setStack(1,new ItemStack(Items.IRON_AXE));e.setStack(2,new ItemStack(Items.NETHERITE_SWORD));}
    if(kind==DecorKind.CHEST)e.setStack(0,new ItemStack(Items.EMERALD,32));
    if(kind==DecorKind.LECTERN){var book=new ItemStack(Items.WRITTEN_BOOK);var n=book.getOrCreateNbt();n.putString("title","Zoldrax rituals");n.putString("author","Cult");var pages=new NbtList();pages.add(NbtString.of("{\"text\":\"Ritual of retribution\\n\\nThe cult remembers.\"}"));n.put("pages",pages);e.setStack(0,book);}
    if(kind==DecorKind.OFFERING_PEDESTAL)e.setStack(0,new ItemStack(Items.DIAMOND));
   }
  });ticks=0;return;}
  if(++ticks==10&&step==-1)client.getServer().execute(()->{var w=client.getServer().getOverworld();for(var item:w.getEntitiesByClass(net.minecraft.entity.ItemEntity.class,new Box(0,60,0,100,90,30),e->true))item.discard();});if(ticks==25&&step==41)client.setScreen(null);if(ticks<50)return;ticks=0;
  if(step>=0){String name=step<32?DecorKind.values()[step/4].id+new String[]{"-front","-side","-back","-top"}[step%4]:new String[]{"chest-open","lectern-read","throne-seated","creative-tab","brazier-off","skull-lamp-off","lantern-off","lantern-long","pedestal-crystal","chest-lid-open-world"}[step-32];ScreenshotRecorder.saveScreenshot(OUTPUT,name+".png",client.getFramebuffer(),text->System.out.println("DECOR_SCREENSHOT "+name));}
  step++;if(Boolean.getBoolean("zoldrax.decorIconOnly")&&step==0)step=35;if(step>=42||Boolean.getBoolean("zoldrax.decorIconOnly")&&step>=36){System.out.println("DECOR_PREVIEW_COMPLETE");client.scheduleStop();return;}
  final int target=step;client.setScreen(null);
  if(step>=36){int i=new int[]{1,2,3,3,7,5}[step-36];var r=root(i);client.options.hudHidden=true;client.options.setPerspective(net.minecraft.client.option.Perspective.FIRST_PERSON);client.getServer().execute(()->{var w=client.getServer().getOverworld();var p=client.getServer().getPlayerManager().getPlayer(client.player.getUuid());p.stopRiding();p.changeGameMode(GameMode.CREATIVE);p.networkHandler.requestTeleport(r.getX()+.5,r.getY()+(i==3?-1:1.1)+.5-1.62,r.getZ()+4.5,180,7.125f);var e=(DecorBlockEntity)w.getBlockEntity(r);if(target<=38)e.setLit(false);if(target==39){e.setLit(true);if(e.getCachedState().get(DecorBaseBlock.LENGTH)==1)e.changeLength(p);}if(target==40)e.removeStack(0);if(target==41)e.use(p,Hand.MAIN_HAND);});return;}
  if(step<32)System.out.println("DECOR_CLIENT_LIGHT "+step+" sky="+client.world.getLightLevel(net.minecraft.world.LightType.SKY,root(step/4))+" block="+client.world.getLightLevel(net.minecraft.world.LightType.BLOCK,root(step/4)));
  if(step<32){int i=step/4;var r=root(i);double cx=r.getX()+(i==0||i==4?1:.5),cy=r.getY()+(i==3?-1:1.1),cz=r.getZ()+.5;
   double[][] views={{0,.5,4},{4,.5,0},{0,.5,-4},{0,4.5,.001}};var v=views[step%4];double x=cx+v[0],y=cy+v[1]-1.62,z=cz+v[2];float yaw=(float)(Math.toDegrees(Math.atan2(-v[0],v[2]))+180);float pitch=(float)Math.toDegrees(Math.atan2(v[1],Math.sqrt(v[0]*v[0]+v[2]*v[2])));
   client.getServer().execute(()->{var p=client.getServer().getPlayerManager().getPlayer(client.player.getUuid());p.networkHandler.requestTeleport(x,y,z,yaw,pitch);});
  }else client.getServer().execute(()->{var w=client.getServer().getOverworld();var p=client.getServer().getPlayerManager().getPlayer(client.player.getUuid());int i=target==32?5:target==33?6:0;var r=root(i);p.changeGameMode(GameMode.CREATIVE);p.networkHandler.requestTeleport(r.getX()+.5,r.getY(),r.getZ()+3,target==34?0:180,10);if(target==32||target==33)((DecorBlockEntity)w.getBlockEntity(r)).use(p,Hand.MAIN_HAND);if(target==34){((DecorBlockEntity)w.getBlockEntity(r)).use(p,Hand.MAIN_HAND);client.execute(()->client.options.setPerspective(net.minecraft.client.option.Perspective.THIRD_PERSON_FRONT));}if(target==35)client.execute(()->{client.options.setPerspective(net.minecraft.client.option.Perspective.FIRST_PERSON);client.options.hudHidden=false;var screen=new net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen(client.player,client.player.networkHandler.getEnabledFeatures(),true);client.setScreen(screen);try{var group=net.minecraft.registry.Registries.ITEM_GROUP.get(DecorBlocks.GROUP);int targetPage=((net.fabricmc.fabric.impl.itemgroup.FabricItemGroup)group).getPage();var pages=(net.fabricmc.fabric.impl.client.itemgroup.CreativeGuiExtensions)screen;while(pages.fabric_currentPage()<targetPage)pages.fabric_nextPage();if(group.getDisplayStacks().size()!=8)throw new IllegalStateException("Decor creative tab must contain eight items");var method=screen.getClass().getDeclaredMethod("setSelectedTab",net.minecraft.item.ItemGroup.class);method.setAccessible(true);method.invoke(screen,net.minecraft.registry.Registries.ITEM_GROUP.get(DecorBlocks.GROUP));}catch(Exception ex){throw new RuntimeException(ex);}});});
 }
}
