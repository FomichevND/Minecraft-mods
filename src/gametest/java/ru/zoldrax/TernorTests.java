package ru.zoldrax;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.*;
import net.minecraft.block.*;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;

public class TernorTests implements FabricGameTest {
 private static final BlockPos ROOT=new BlockPos(3,2,3);
 private record Scene(TernorEntity altar,ServerPlayerEntity player){}
 private Scene scene(TestContext c){
  c.setBlockState(ROOT,Zoldrax.TERNOR_BLOCK);var m=(TernorEntity)c.getWorld().getBlockEntity(c.getAbsolutePos(ROOT));m.stage=2;
  var p=c.createMockCreativeServerPlayerInWorld();var q=c.getAbsolutePos(ROOT);p.refreshPositionAndAngles(q.getX()+.5,q.getY()+1,q.getZ()+1.5,0,0);return new Scene(m,p);
 }
 private void check(boolean b,String why){if(!b)throw new IllegalStateException(why);}
 private void finish(TestContext c,Scene s){c.getWorld().getServer().getPlayerManager().remove(s.player);c.complete();}
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=130,batchId="ternor_build")
 public void constructionConsumesBothStageCosts(TestContext c){
  var s=scene(c);s.altar.stage=0;c.setBlockState(ROOT.add(2,0,2),Blocks.CHEST);
  var chest=(ChestBlockEntity)c.getWorld().getBlockEntity(c.getAbsolutePos(ROOT.add(2,0,2)));
  Item[] items={Items.IRON_INGOT,Items.STONE,Items.OBSIDIAN,Items.LANTERN,Items.MOSS_BLOCK};int[] counts={24,64,8,8,16};
  for(int i=0;i<items.length;i++)chest.setStack(i,new ItemStack(items[i],counts[i]));
  c.waitAndRun(85,()->{check(s.altar.stage==2&&chest.isEmpty(),"Both stages must consume the documented total costs");finish(c,s);});
 }
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=100,batchId="ternor_day")
 public void dayRefusesTrialAndBlessesField(TestContext c){
  c.getWorld().setTimeOfDay(1000);c.getWorld().getGameRules().get(GameRules.RANDOM_TICK_SPEED).set(0,c.getWorld().getServer());var s=scene(c);
  BlockPos near=c.getAbsolutePos(ROOT.add(2,0,0)),far=c.getAbsolutePos(ROOT.add(31,0,0));
  for(BlockPos p:List.of(near,far)){c.getWorld().setBlockState(p,Blocks.FARMLAND.getDefaultState().with(FarmlandBlock.MOISTURE,0));c.getWorld().setBlockState(p.up(),Blocks.WHEAT.getDefaultState());}
  BlockPos cocoa=c.getAbsolutePos(ROOT.add(4,1,2));c.getWorld().setBlockState(cocoa.east(),Blocks.JUNGLE_LOG.getDefaultState());c.getWorld().setBlockState(cocoa,Blocks.COCOA.getDefaultState().with(CocoaBlock.FACING,Direction.EAST));
  c.waitAndRun(5,()->{
   s.altar.interact(s.player);check(s.altar.createNbt().getInt("Wave")==0,"Daytime cannot launch trial");
   for(int i=0;i<1000;i++)s.altar.farmPulse();
   check(c.getWorld().getBlockState(near).get(FarmlandBlock.MOISTURE)==7,"Nearby farmland must become fully moist");
   check(c.getWorld().getBlockState(cocoa).get(CocoaBlock.AGE)>0,"Planted cocoa must also grow faster");
   check(c.getWorld().getBlockState(near.up()).get(CropBlock.AGE)>0,"Planted wheat must grow with natural random ticks disabled");
   check(c.getWorld().getBlockState(far).get(FarmlandBlock.MOISTURE)==0&&c.getWorld().getBlockState(far.up()).get(CropBlock.AGE)==0,"Outside 30 blocks farmland/crops must remain unchanged");finish(c,s);
  });
 }
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=80,batchId="ternor_guardian")
 public void guardianStatsRepairAndAnchor(TestContext c){
  var s=scene(c);var golem=Zoldrax.TERNOR_GUARDIAN.create(c.getWorld());golem.setHome(c.getAbsolutePos(ROOT));
  check(golem.getMaxHealth()==150&&golem.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE)==22,"Guardian must have increased health and attack");
  golem.setHealth(60);s.player.setStackInHand(Hand.MAIN_HAND,new ItemStack(Items.IRON_INGOT,4));
  check(golem.interactMob(s.player,Hand.MAIN_HAND)==ActionResult.FAIL&&golem.getHealth()==60&&s.player.getMainHandStack().getCount()==4,"Iron must neither heal guardian nor be consumed");
  check(golem.protects(Vec3d.ofCenter(golem.getHome()).add(49,0,0))&&!golem.protects(Vec3d.ofCenter(golem.getHome()).add(51,0,0)),"Protection radius must be 50");
  golem.setTarget(s.player);check(golem.getTarget()==null,"Guardian cannot target players");
  var hostile=net.minecraft.entity.EntityType.CREEPER.create(c.getWorld());hostile.setPosition(Vec3d.ofCenter(golem.getHome()).add(10,0,0));golem.setTarget(hostile);check(golem.getTarget()==hostile,"Guardian must accept nearby threats");
  hostile.setPosition(Vec3d.ofCenter(golem.getHome()).add(51,0,0));golem.setTarget(hostile);check(golem.getTarget()==null,"Guardian must reject threats outside its area");
  NbtCompound n=new NbtCompound();golem.writeCustomDataToNbt(n);var copy=Zoldrax.TERNOR_GUARDIAN.create(c.getWorld());copy.readCustomDataFromNbt(n);check(copy.getHome().equals(golem.getHome()),"Home must survive entity NBT save");finish(c,s);
 }
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=100,batchId="ternor_night")
 public void nightHasNoFarmBlessing(TestContext c){
  c.getWorld().setTimeOfDay(18000);var s=scene(c);var p=c.getAbsolutePos(ROOT.add(2,0,0));c.getWorld().setBlockState(p,Blocks.FARMLAND.getDefaultState().with(FarmlandBlock.MOISTURE,0));c.getWorld().setBlockState(p.up(),Blocks.WHEAT.getDefaultState());
  c.waitAndRun(5,()->{for(int i=0;i<1000;i++)s.altar.farmPulse();check(c.getWorld().getBlockState(p).get(FarmlandBlock.MOISTURE)==0,"No automatic hydration at night");finish(c,s);});
 }
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=550,batchId="ternor_night")
 public void threeWavesSummonOneGuardian(TestContext c){
  c.getWorld().setTimeOfDay(18000);c.getWorld().getServer().setDifficulty(Difficulty.NORMAL,true);var s=scene(c);
  c.waitAndRun(5,()->{s.altar.interact(s.player);check(s.altar.createNbt().getInt("Wave")==1,"Night interaction must start trial");s.altar.interact(s.player);check(s.altar.createNbt().getInt("Wave")==1,"Active trial cannot restart");});
  c.waitAndRun(45,()->{check(s.altar.createNbt().getList("Enemies",10).size()==3,"First wave must have 3 real enemies");kill(c,s);});
  c.waitAndRun(190,()->{check(s.altar.createNbt().getInt("Wave")==2&&s.altar.createNbt().getList("Enemies",10).size()==5,"Second wave must have 5 enemies");kill(c,s);});
  c.waitAndRun(335,()->{check(s.altar.createNbt().getInt("Wave")==3&&s.altar.createNbt().getList("Enemies",10).size()==7,"Third wave must have 7 enemies");kill(c,s);});
  c.waitAndRun(390,()->{
   var state=s.altar.createNbt();check(state.getInt("Wave")==0&&state.containsUuid("Guardian"),"Victory must end trial and register guardian");
   var entity=c.getWorld().getEntity(state.getUuid("Guardian"));check(entity instanceof TernorGuardian&&entity.isAlive(),"Reward must be an actual living guardian");
   var golem=(TernorGuardian)entity;check(golem.getHome().equals(s.altar.getPos())&&golem.getMaxHealth()==150,"Spawned guardian must be anchored and buffed");
   var copy=new TernorEntity(s.altar.getPos(),Zoldrax.TERNOR_BLOCK.getDefaultState());copy.readNbt(state);check(copy.createNbt().getUuid("Guardian").equals(golem.getUuid()),"Guardian UUID must survive altar reload");
   s.altar.interact(s.player);check(s.altar.createNbt().getInt("Wave")==0,"Living guardian must prevent repeat reward farming");finish(c,s);
  });
 }
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=100,batchId="ternor_failure")
 public void challengerDeathLosesTrial(TestContext c){
  c.getWorld().setTimeOfDay(18000);c.getWorld().getServer().setDifficulty(Difficulty.NORMAL,true);var s=scene(c);
  c.waitAndRun(5,()->s.altar.interact(s.player));
  c.waitAndRun(30,()->{check(s.altar.createNbt().getInt("Wave")>0,"Trial should run before failure");MonkeyRaidState.get(c.getWorld()).playerDied(s.player.getUuid(),c.getWorld().getTime());});
  c.waitAndRun(60,()->{check(s.altar.createNbt().getInt("Wave")==0&&!s.altar.createNbt().containsUuid("Guardian"),"Challenger death must fail trial without reward");check(s.altar.createNbt().getList("Enemies",10).isEmpty(),"Failure must clear enemy tracking");finish(c,s);});
 }
 private void kill(TestContext c,Scene s){
  var state=s.altar.createNbt();var copy=new TernorEntity(s.altar.getPos(),Zoldrax.TERNOR_BLOCK.getDefaultState());copy.readNbt(state);check(copy.createNbt().getList("Enemies",10).size()==state.getList("Enemies",10).size(),"Wave enemy UUIDs must survive save");
  Set<String> types=new HashSet<>();for(var el:state.getList("Enemies",10)){var id=((NbtCompound)el).getUuid("Id");Entity mob=c.getWorld().getEntity(id);check(mob!=null,"Trial enemy must exist");types.add(net.minecraft.registry.Registries.ENTITY_TYPE.getId(mob.getType()).toString());mob.kill();}
  check(types.containsAll(List.of("minecraft:zombie","minecraft:skeleton","minecraft:spider")),"Each wave must include all requested monster types");
 }
}
