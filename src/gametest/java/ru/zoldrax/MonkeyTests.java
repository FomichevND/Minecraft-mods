package ru.zoldrax;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.*;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.*;
import net.minecraft.world.Difficulty;

public class MonkeyTests implements FabricGameTest {
 private static final BlockPos ROOT=new BlockPos(3,2,3);
 private record Scene(MonkeyEntity altar,ChestBlockEntity chest,ServerPlayerEntity player){}
 private Scene scene(TestContext c){
  c.setBlockState(ROOT,Zoldrax.MONKEY_BLOCK);c.setBlockState(ROOT.add(2,0,2),Blocks.CHEST);
  var m=(MonkeyEntity)c.getWorld().getBlockEntity(c.getAbsolutePos(ROOT));m.stage=2;
  var chest=(ChestBlockEntity)c.getWorld().getBlockEntity(c.getAbsolutePos(ROOT.add(2,0,2)));
  var player=c.createMockCreativeServerPlayerInWorld();var p=c.getAbsolutePos(ROOT);
  player.refreshPositionAndAngles(p.getX()+.5,p.getY()+1,p.getZ()+1,0,0);Zoldrax.MONKEY_BLOCK.onPlaced(c.getWorld(),p,m.getCachedState(),player,new ItemStack(Zoldrax.MONKEY_BLOCK));return new Scene(m,chest,player);
 }
 private void check(boolean b,String reason){if(!b)throw new IllegalStateException(reason);}
 private int bananas(Scene s){int n=0;for(int i=0;i<s.chest.size();i++)if(s.chest.getStack(i).isOf(Zoldrax.GOLDEN_BANANA))n+=s.chest.getStack(i).getCount();return n;}
 private void finish(TestContext c,Scene s){c.getWorld().getServer().getPlayerManager().remove(s.player);c.complete();}
 private void request(Scene s,Item food,int amount){s.altar.beginRequest(food,amount,s.player.getUuid());}
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=130)
 public void foodFulfilmentAndSingleReward(TestContext c){
  var s=scene(c);request(s,Items.BREAD,32);s.chest.setStack(0,new ItemStack(Items.BREAD,40));
  c.waitAndRun(45,()->{check(s.chest.getStack(0).getCount()==8,"Only requested amount is consumed");check(bananas(s)==1,"One golden banana must be placed into chest");check(s.altar.createNbt().getString("Food").equals("minecraft:air"),"Request must end");});
  c.waitAndRun(85,()->{check(bananas(s)==1,"Reward must not duplicate on subsequent ticks");finish(c,s);});
 }
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=130)
 public void incompleteAndWrongFoodRemainUntouched(TestContext c){
  var s=scene(c);request(s,Items.CARROT,16);s.chest.setStack(0,new ItemStack(Items.CARROT,15));s.chest.setStack(1,new ItemStack(Items.BREAD,64));
  c.waitAndRun(40,()->{check(s.chest.getStack(0).getCount()==15&&s.chest.getStack(1).getCount()==64,"Incomplete/wrong food must not be consumed");check(bananas(s)==0,"No early reward");s.chest.setStack(0,new ItemStack(Items.CARROT,16));});
  c.waitAndRun(85,()->{check(bananas(s)==1,"Completing amount must feed the king");check(s.chest.getStack(1).getCount()==64,"Other food remains");finish(c,s);});
 }
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=80)
 public void timerAndRequestPersist(TestContext c){
  var s=scene(c);request(s,Items.APPLE,64);var n=s.altar.createNbt();
  check(n.getLong("Deadline")-c.getWorld().getTime()==2400,"Timer must be exactly 2 minutes");
  var loaded=new MonkeyEntity(s.altar.getPos(),Zoldrax.MONKEY_BLOCK.getDefaultState());loaded.readNbt(n);var copy=loaded.createNbt();
  check(copy.getInt("Amount")==64&&copy.getString("Food").equals("minecraft:apple"),"Food and count survive save");
  check(copy.getLong("Deadline")==n.getLong("Deadline")&&copy.getUuid("Customer").equals(s.player.getUuid()),"Deadline and target survive save");finish(c,s);
 }
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=80)
 public void goldenBananaEffects(TestContext c){
  var s=scene(c);ItemStack banana=new ItemStack(Zoldrax.GOLDEN_BANANA);
  banana.finishUsing(c.getWorld(),s.player);
  var jump=s.player.getStatusEffect(StatusEffects.JUMP_BOOST);var food=s.player.getStatusEffect(StatusEffects.SATURATION);
  check(jump!=null&&jump.getAmplifier()==255&&jump.getDuration()==12000,"Maximum jump boost must last ten minutes");
  check(food!=null&&food.getDuration()==12000,"Saturation must last ten minutes");finish(c,s);
 }
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=130)
 public void constructionBothStages(TestContext c){
  var s=scene(c);s.altar.stage=0;
  Item[] items={Items.MOSSY_COBBLESTONE,Items.CHISELED_STONE_BRICKS,Items.JUNGLE_LOG,Items.GOLD_NUGGET,Items.VINE,Items.GOLD_INGOT};
  int[] counts={48,56,32,16,16,8};for(int i=0;i<items.length;i++)s.chest.setStack(i,new ItemStack(items[i],counts[i]));
  c.waitAndRun(85,()->{check(s.altar.stage==2,"Both Monkey King building stages must complete");check(s.chest.isEmpty(),"Costs consumed exactly");finish(c,s);});
 }
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=500)
 public void deadlineTriggersThreeWaveRaid(TestContext c){
  c.getWorld().getServer().setDifficulty(Difficulty.NORMAL,true);
  var s=scene(c);request(s,Items.BREAD,16);var n=s.altar.createNbt();n.putLong("Deadline",c.getWorld().getTime()+20);s.altar.readNbt(n);
  s.chest.setStack(0,new ItemStack(Items.BREAD,15));
  c.waitAndRun(45,()->{
   var state=s.altar.createNbt();check(state.getInt("RaidWave")==1,"Expired order must trigger raid");
   check(state.getList("Raiders",10).size()==2,"First wave must have 2 raiders");
   check(s.chest.getStack(0).getCount()==15&&bananas(s)==0,"Expired food cannot be rewarded");
   var first=(net.minecraft.nbt.NbtCompound)state.getList("Raiders",10).get(0);
   c.getWorld().getEntity(first.getUuid("Id")).kill();
  });
  c.waitAndRun(80,()->{
   var state=s.altar.createNbt();check(state.getList("Raiders",10).size()==1,"Partial wave losses must persist");
   var copy=new MonkeyEntity(s.altar.getPos(),Zoldrax.MONKEY_BLOCK.getDefaultState());copy.readNbt(state);
   check(copy.createNbt().getList("Raiders",10).size()==1&&copy.createNbt().getBoolean("WaveStarted"),"Raid wave and survivor UUID must survive save");
   killWave(c,s);
  });
  c.waitAndRun(230,()->{check(s.altar.createNbt().getInt("RaidWave")==2,"Second wave must follow victory");check(s.altar.createNbt().getList("Raiders",10).size()==3,"Second wave has 3 raiders");killWave(c,s);});
  c.waitAndRun(375,()->{check(s.altar.createNbt().getInt("RaidWave")==3,"Third wave must follow victory");check(s.altar.createNbt().getList("Raiders",10).size()==4,"Third wave has 4 raiders");killWave(c,s);});
  c.waitAndRun(425,()->{check(s.altar.createNbt().getInt("RaidWave")==0,"Raid must end after last wave");check(s.altar.createNbt().getLong("NextRequest")>c.getWorld().getTime(),"New hunger must be delayed after raid");finish(c,s);});
 }
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=80)
 public void randomHungerStartsForNearbyPlayer(TestContext c){
  var s=scene(c);var n=s.altar.createNbt();n.putLong("NextRequest",c.getWorld().getTime());s.altar.readNbt(n);
  c.waitAndRun(25,()->{
   var state=s.altar.createNbt();var id=new net.minecraft.util.Identifier(state.getString("Food"));
   check(Arrays.asList(MonkeyEntity.FOODS).contains(net.minecraft.registry.Registries.ITEM.get(id)),"Random request must use ordinary food pool");
   check(state.getInt("Amount")>=16&&state.getInt("Amount")<=64,"Random amount must be 16 through 64");
   check(state.getLong("Deadline")>c.getWorld().getTime()+2300,"Random request must receive full timer");finish(c,s);
  });
 }
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=240,batchId="monkey_first_encounter")
 public void naturallyGeneratedShrineStaysDecorative(TestContext c){
  var w=c.getWorld();var root=new BlockPos(3600,70,3800);
  var template=w.getStructureTemplateManager().getTemplate(new net.minecraft.util.Identifier("zoldrax","monkey/court_temple")).orElseThrow();
  var data=new net.minecraft.structure.StructurePlacementData().setIgnoreEntities(true);
  var bounds=template.calculateBoundingBox(data,root);
  for(int x=bounds.getMinX()>>4;x<=bounds.getMaxX()>>4;x++)for(int z=bounds.getMinZ()>>4;z<=bounds.getMaxZ()>>4;z++){w.setChunkForced(x,z,true);w.getChunk(x,z);}
  var element=net.minecraft.structure.pool.StructurePoolElement.ofSingle("zoldrax:monkey/court_temple").apply(net.minecraft.structure.pool.StructurePool.Projection.RIGID);
  var piece=new net.minecraft.structure.PoolStructurePiece(w.getStructureTemplateManager(),element,root,10,net.minecraft.util.BlockRotation.NONE,bounds);
  piece.generate(w,w.getStructureAccessor(),w.getChunkManager().getChunkGenerator(),net.minecraft.util.math.random.Random.create(7),bounds,new ChunkPos(root),root);
  var anchor=root.add(8,12,6);var altar=(MonkeyEntity)w.getBlockEntity(anchor);check(altar!=null&&altar.stage==2,"Natural shrine must already be completed");
  var structure=w.getRegistryManager().get(net.minecraft.registry.RegistryKeys.STRUCTURE).get(new net.minecraft.util.Identifier("zoldrax","banana_court"));
  var start=new net.minecraft.structure.StructureStart(structure,new ChunkPos(root),0,new net.minecraft.structure.StructurePiecesList(java.util.List.of(piece)));
  w.getStructureAccessor().setStructureStart(ChunkSectionPos.from(root),structure,start,w.getChunk(root));
  w.getStructureAccessor().addStructureReference(ChunkSectionPos.from(anchor),structure,new ChunkPos(root).toLong(),w.getChunk(anchor));
  var legacy=altar.createNbt();legacy.remove("PlayerBuilt");altar.readNbt(legacy);
  check(!altar.isPlayerBuilt(),"Legacy generated altar must be identified from its saved natural structure reference");
  var residentArea=new Box(bounds.getMinX(),bounds.getMinY(),bounds.getMinZ(),bounds.getMaxX()+1,bounds.getMaxY()+1,bounds.getMaxZ()+1);
  var residents=w.getEntitiesByClass(MonkeyFollowerEntity.class,residentArea,e->true);for(var npc:residents)npc.setAiDisabled(true);
  var chest=anchor.add(0,0,3);w.setBlockState(chest,Blocks.AIR.getDefaultState());w.setBlockState(chest,Blocks.CHEST.getDefaultState());
  var player=c.createMockCreativeServerPlayerInWorld();player.refreshPositionAndAngles(anchor.getX()+200,anchor.getY(),anchor.getZ(),0,0);
  c.waitAndRun(30,()->{check(!altar.hasRequest(),"Unvisited shrine must not start an unseen challenge");player.refreshPositionAndAngles(anchor.getX()+2,anchor.getY(),anchor.getZ()+3,0,0);});
  c.waitAndRun(80,()->{
   check(!altar.hasRequest(),"A found shrine must not give challenges when approached");
   var hit=new net.minecraft.util.hit.BlockHitResult(Vec3d.ofCenter(anchor),Direction.UP,anchor,false);
   Zoldrax.MONKEY_BLOCK.onUse(altar.getCachedState(),w,anchor,player,net.minecraft.util.Hand.MAIN_HAND,hit);
   check(!altar.hasRequest()&&!altar.isPlayerBuilt(),"Clicking a generated shrine must not activate or claim it");
   check(!altar.createNbt().getBoolean("PlayerBuilt"),"Generated provenance must survive saving");
   w.getServer().getPlayerManager().remove(player);for(var npc:residents)npc.discard();
   for(int x=bounds.getMinX()>>4;x<=bounds.getMaxX()>>4;x++)for(int z=bounds.getMinZ()>>4;z<=bounds.getMaxZ()>>4;z++)w.setChunkForced(x,z,false);
   c.complete();
  });
 }
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=80,batchId="monkey_first_encounter")
 public void playerPlacedAltarStartsAndCannotReset(TestContext c){
  var s=scene(c);check(s.altar.isPlayerBuilt(),"Player placement callback must mark provenance");
  var p=s.altar.getPos();var hit=new net.minecraft.util.hit.BlockHitResult(Vec3d.ofCenter(p),Direction.UP,p,false);
  Zoldrax.MONKEY_BLOCK.onUse(s.altar.getCachedState(),c.getWorld(),p,s.player,net.minecraft.util.Hand.MAIN_HAND,hit);
  check(s.altar.hasRequest(),"A completed player-built altar must give its first challenge");
  var started=s.altar.createNbt();Zoldrax.MONKEY_BLOCK.onUse(s.altar.getCachedState(),c.getWorld(),p,s.player,net.minecraft.util.Hand.MAIN_HAND,hit);
  check(s.altar.requestDeadline()==started.getLong("Deadline")&&s.altar.createNbt().getString("Food").equals(started.getString("Food")),"Repeated clicks must not reset the trial");
  var copy=new MonkeyEntity(p,s.altar.getCachedState());copy.readNbt(started);check(copy.isPlayerBuilt()&&copy.requestDeadline()==s.altar.requestDeadline(),"Origin and timer must survive reload");finish(c,s);
 }
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=80,batchId="monkey_first_encounter")
 public void oldCompletedChallengeKeepsCooldown(TestContext c){
  var s=scene(c);request(s,Items.APPLE,32);var n=s.altar.createNbt();n.remove("Awakened");n.remove("PlayerBuilt");n.putString("Food","minecraft:air");n.putLong("NextRequest",c.getWorld().getTime()+6000);s.altar.readNbt(n);
  check(!s.altar.tryFirstEncounter(s.player),"Old player history must prevent resetting a completed challenge cooldown");check(!s.altar.hasRequest()&&s.altar.createNbt().getLong("NextRequest")==n.getLong("NextRequest"),"Existing cooldown must survive migration and interaction");
  var fresh=s.altar.createNbt();fresh.remove("Awakened");fresh.remove("Customer");fresh.putInt("Stage",1);s.altar.readNbt(fresh);check(!s.altar.tryFirstEncounter(s.player),"Unfinished monuments must still require construction");finish(c,s);
 }
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=100,batchId="monkey_first_encounter")
 public void naturalAltarCancelsTrialFromOlderVersionWithoutReward(TestContext c){
  var s=scene(c);var n=s.altar.createNbt();n.putBoolean("PlayerBuilt",false);n.putBoolean("Awakened",true);n.putString("Food","minecraft:bread");n.putInt("Amount",16);n.putLong("Deadline",c.getWorld().getTime()+2400);n.putUuid("Customer",s.player.getUuid());s.altar.readNbt(n);s.chest.setStack(0,new ItemStack(Items.BREAD,32));
  check(!s.altar.tryFirstEncounter(s.player),"Natural altar must not be claimed by proximity or interaction");
  c.waitAndRun(40,()->{check(!s.altar.hasRequest()&&s.altar.createNbt().getInt("RaidWave")==0,"Old accidental natural trial must stop");check(s.chest.count(Items.BREAD)==32&&bananas(s)==0,"Natural altar must not consume offerings or create rewards");finish(c,s);});
 }
 private void killWave(TestContext c,Scene s){
  for(var el:s.altar.createNbt().getList("Raiders",10)){
   var id=((net.minecraft.nbt.NbtCompound)el).getUuid("Id");Entity e=c.getWorld().getEntity(id);check(e!=null,"Raider must really exist");
   e.kill();check(MonkeyRaidState.get(c.getWorld()).isDead(id),"Raider death must be recorded independently of altar loading");
  }
 }
}
