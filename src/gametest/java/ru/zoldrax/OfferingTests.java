package ru.zoldrax;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.*;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.effect.*;
import net.minecraft.item.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.*;

public class OfferingTests implements FabricGameTest {
 private static final BlockPos ROOT=new BlockPos(3,2,3);
 private record Scene(MonumentEntity altar,ServerPlayerEntity player,Vec3d center) {}
 private Scene scene(TestContext c,int stage) {
  c.setBlockState(ROOT,Zoldrax.BLOCK);
  MonumentEntity m=(MonumentEntity)c.getWorld().getBlockEntity(c.getAbsolutePos(ROOT));m.stage=stage;
  Vec3d center=Vec3d.ofBottomCenter(c.getAbsolutePos(ROOT));
  ServerPlayerEntity player=c.createMockCreativeServerPlayerInWorld();
  player.refreshPositionAndAngles(center.x,center.y+2,center.z+2.5,0,0);
  return new Scene(m,player,center);
 }
 private ItemEntity item(TestContext c,Scene s,Item kind,int count,double y,double z) {
  ItemEntity item=new ItemEntity(c.getWorld(),s.center.x,s.center.y+y,s.center.z+z,new ItemStack(kind,count));
  item.setNoGravity(true);item.setVelocity(Vec3d.ZERO);item.setPickupDelayInfinite();item.setThrower(s.player.getUuid());c.getWorld().spawnEntity(item);return item;
 }
 private <T extends MobEntity> T mob(TestContext c,Scene s,EntityType<T> type) {
  T mob=type.create(c.getWorld());mob.setAiDisabled(true);mob.setNoGravity(true);
  mob.refreshPositionAndAngles(s.center.x,s.center.y+.1,s.center.z+1,0,0);
  c.getWorld().spawnEntity(mob);return mob;
 }
 private long last(Scene s) {return s.altar.createNbt().getLong("LastOffering");}
 private void check(boolean b,String message) {if(!b)throw new IllegalStateException(message);}
 private void complete(TestContext c,Scene s) {c.getWorld().getServer().getPlayerManager().remove(s.player);c.complete();}
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=80)
 public void groundedDiamondAndBlessing(TestContext c) {
  Scene s=scene(c,2);s.player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS,500));
  ItemEntity item=item(c,s,Items.DIAMOND,1,0,1);
  c.waitAndRun(8,()->{
   check(item.isRemoved(),"Ground-level diamond under decorative brazier must be consumed");
   check(last(s)>=0,"Offering must be recorded");
   check(!s.player.hasStatusEffect(StatusEffects.WEAKNESS),"Offering must cleanse curse");
   check(!s.player.getActiveStatusEffects().isEmpty(),"Throwing player must receive a blessing");
   complete(c,s);
  });
 }
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=80)
 public void fastThrownGemIsDetected(TestContext c) {
  Scene s=scene(c,2);ItemEntity gem=item(c,s,Items.EMERALD,1,.9,1.9);gem.setVelocity(0,0,-.5);
  c.waitAndRun(8,()->{check(gem.isRemoved()&&last(s)>=0,"Fast gem must be caught between old one-second scans");complete(c,s);});
 }
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=80)
 public void ordinaryAndOutsideItemsArePreserved(TestContext c) {
  Scene s=scene(c,2);
  ItemEntity ordinary=item(c,s,Items.COBBLESTONE,1,.1,1);
  ItemEntity outside=item(c,s,Items.DIAMOND,1,.1,-2);
  c.waitAndRun(30,()->{check(!ordinary.isRemoved()&&!outside.isRemoved(),"Only accepted items inside brazier may be consumed");check(last(s)==-1,"No offering from invalid items");complete(c,s);});
 }
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=80)
 public void unfinishedAltarDoesNotAcceptOfferings(TestContext c) {
  Scene s=scene(c,1);ItemEntity gem=item(c,s,Items.DIAMOND,1,.1,1);
  var chicken=mob(c,s,EntityType.CHICKEN);
  c.waitAndRun(25,()->{check(!gem.isRemoved()&&chicken.isAlive()&&!chicken.isOnFire(),"Unfinished altar must not consume or burn");check(last(s)==-1,"Unfinished altar must not bless");complete(c,s);});
 }
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=80)
 public void stackConsumesAtMostOnePerSecond(TestContext c) {
  Scene s=scene(c,2);ItemEntity gold=item(c,s,Items.GOLD_INGOT,3,.1,1);
  c.waitAndRun(8,()->{check(gold.getStack().getCount()==2,"First offering consumes only one item");});
  c.waitAndRun(30,()->{check(gold.getStack().getCount()==1,"Second offering consumes one after 20 ticks");complete(c,s);});
 }
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=80)
 public void mobSacrificeRewardsExactlyOnce(TestContext c) {
  Scene s=scene(c,2);var chicken=mob(c,s,EntityType.CHICKEN);
  long[] accepted={-1};
  c.waitAndRun(8,()->{check(!chicken.isAlive()&&last(s)>=0,"Mob killed by brazier must bless");accepted[0]=last(s);});
  c.waitAndRun(30,()->{check(last(s)==accepted[0],"Death must not grant repeated offerings");complete(c,s);});
 }
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=80)
 public void burningDeathAfterLeavingBrazierCounts(TestContext c) {
  Scene s=scene(c,2);var cow=mob(c,s,EntityType.COW);
  c.waitAndRun(4,()->{
   check(cow.isOnFire(),"Brazier must ignite mob");
   cow.refreshPositionAndAngles(s.center.x+2,s.center.y+.1,s.center.z,0,0);
   cow.damage(c.getWorld().getDamageSources().onFire(),100);
  });
  c.waitAndRun(8,()->{check(last(s)>=0,"Fire death between damage ticks and outside bowl must count");complete(c,s);});
 }
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=80)
 public void nonFireDeathDoesNotCount(TestContext c) {
  Scene s=scene(c,2);var cow=mob(c,s,EntityType.COW);
  c.waitAndRun(4,()->{cow.damage(c.getWorld().getDamageSources().genericKill(),1000);});
  c.waitAndRun(8,()->{check(last(s)==-1,"Non-fire death must not be attributed to brazier");complete(c,s);});
 }
}
