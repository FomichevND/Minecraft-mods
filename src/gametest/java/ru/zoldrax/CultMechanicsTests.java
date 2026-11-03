package ru.zoldrax;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.*;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.effect.*;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.*;
import net.minecraft.world.Difficulty;

public class CultMechanicsTests implements FabricGameTest {
 private static final BlockPos ROOT=new BlockPos(3,2,3);
 private record Scene(MonumentEntity altar,CultistEntity npc){}
 private Scene scene(TestContext c,boolean priest,boolean ai){
  c.getWorld().getServer().setDifficulty(Difficulty.NORMAL,true);
  for(int x=-2;x<=5;x++)for(int z=-1;z<=7;z++)c.setBlockState(ROOT.add(x,-1,z),Blocks.STONE);
  c.setBlockState(ROOT,Zoldrax.BLOCK);var altar=(MonumentEntity)c.getWorld().getBlockEntity(c.getAbsolutePos(ROOT));altar.stage=2;
  var npc=(priest?Zoldrax.BLOOD_PRIEST:Zoldrax.CHAIN_PUNISHER).create(c.getWorld());var p=altar.getPos();
  npc.refreshPositionAndAngles(p.getX()+2.5,p.getY(),p.getZ()+3.7,0,0);npc.setAiDisabled(!ai);c.getWorld().spawnEntity(npc);return new Scene(altar,npc);
 }
 private void check(boolean b,String why){if(!b)throw new IllegalStateException(why);}
 private void finish(TestContext c,Scene s){s.npc.discard();c.complete();}
 private ItemEntity gem(TestContext c,Scene s,Item item,double x,double z){var p=s.altar.getPos();var e=new ItemEntity(c.getWorld(),p.getX()+x,p.getY()+.1,p.getZ()+z,new ItemStack(item));e.setNoGravity(true);e.setVelocity(Vec3d.ZERO);e.setPickupDelayInfinite();c.getWorld().spawnEntity(e);return e;}
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=100,batchId="cult_mechanics")
 public void staffIsRealEquipmentAndOldNpcUpgrades(TestContext c){
  var s=scene(c,true,false);var n=new NbtCompound();s.npc.writeCustomDataToNbt(n);n.remove("HandItems");s.npc.readCustomDataFromNbt(n);
  c.waitAndRun(5,()->{check(s.npc.getMainHandStack().isOf(Zoldrax.RITUAL_STAFF),"Priest must equip an actual staff item");var save=new NbtCompound();s.npc.writeCustomDataToNbt(save);check(!save.getList("HandItems",10).isEmpty(),"Held staff must persist as equipment");finish(c,s);});
 }
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=260,batchId="cult_mechanics")
 public void priestReallyCollectsAndThrowsGem(TestContext c){
  var s=scene(c,true,true);var e=gem(c,s,Items.EMERALD,2.5,4.7);
  c.waitAndRun(220,()->{check(e.isRemoved(),"Priest must collect a real dropped emerald");check(s.altar.createNbt().getLong("LastOffering")>0,"Thrown resource must reach and be accepted by altar even without player");check(!s.npc.hasOffering(),"Priest cannot keep a resource after sacrificing it");finish(c,s);});
 }
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=100,batchId="cult_mechanics")
 public void collectedOfferingConservesItemsAndPersists(TestContext c){
  var s=scene(c,true,false);var e=gem(c,s,Items.DIAMOND,3,5);e.setStack(new ItemStack(Items.DIAMOND,3));
  s.npc.collectOffering(e);check(e.getStack().getCount()==2&&s.npc.hasOffering(),"Collect exactly one item, not create a free offering");
  var n=new NbtCompound();s.npc.writeCustomDataToNbt(n);var copy=Zoldrax.BLOOD_PRIEST.create(c.getWorld());copy.readCustomDataFromNbt(n);check(copy.hasOffering()&&n.getCompound("CultOffering").getByte("Count")==1,"Pending item must survive NPC NBT");e.discard();finish(c,s);
 }
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=100,batchId="cult_mechanics")
 public void punisherFireImmunityBuffAndBrazierPush(TestContext c){
  var s=scene(c,false,false);float hp=s.npc.getHealth();s.npc.damage(c.getWorld().getDamageSources().inFire(),10);check(s.npc.isFireImmune()&&s.npc.getHealth()==hp,"Punisher must not take fire damage");
  var e=gem(c,s,Items.GOLD_INGOT,4,5);s.npc.cultPulse();check(s.npc.hasStatusEffect(StatusEffects.STRENGTH)&&s.npc.hasStatusEffect(StatusEffects.RESISTANCE),"Nearby offering must strengthen punisher");
  var cow=EntityType.COW.create(c.getWorld());var p=s.altar.getPos();cow.refreshPositionAndAngles(p.getX()+.5,p.getY(),p.getZ()+5,0,0);cow.setAiDisabled(true);cow.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(100);cow.setHealth(100);c.getWorld().spawnEntity(cow);
  s.npc.setTarget(cow);check(s.npc.tryAttack(cow),"Punisher must actually hit");check(cow.getVelocity().z<0,"Hit must push toward the brazier, not merely away from attacker");cow.discard();e.discard();finish(c,s);
 }
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=100,batchId="cult_mechanics")
 public void priestAmplifiesPlayerOffering(TestContext c){
  var s=scene(c,true,false);var player=c.createMockCreativeServerPlayerInWorld();var p=s.altar.getPos();player.refreshPositionAndAngles(p.getX()+.5,p.getY()+2,p.getZ()+4,0,0);
  var e=gem(c,s,Items.DIAMOND,.5,2.05);e.setThrower(player.getUuid());
  c.waitAndRun(8,()->{check(e.isRemoved(),"Offering must be accepted");check(player.getActiveStatusEffects().values().stream().anyMatch(effect->effect.getDuration()>7000),"Priest must extend blessing to six minutes");check(s.altar.createNbt().getLong("CultRitualUntil")>c.getWorld().getTime(),"Amplified fire ritual must start");s.npc.discard();c.getWorld().getServer().getPlayerManager().remove(player);c.complete();});
 }
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=120,batchId="cult_factions")
 public void futureTernorFactionIsHostileAndCursed(TestContext c){
  // Husk is tagged only by this test mod; no future follower entity is created.
  var s=scene(c,true,true);var enemy=EntityType.HUSK.create(c.getWorld());var p=s.altar.getPos();enemy.refreshPositionAndAngles(p.getX()+7,p.getY(),p.getZ()+3.7,0,0);enemy.setAiDisabled(true);enemy.setNoGravity(true);c.getWorld().spawnEntity(enemy);
  check(CultFactions.isEnemy(enemy),"Follower faction tag must be loaded and recognized");
  c.waitAndRun(12,()->{check(s.npc.getTarget()==enemy,"Priest must identify a faction enemy automatically");s.npc.cultPulse();check(enemy.hasStatusEffect(StatusEffects.WEAKNESS)&&enemy.hasStatusEffect(StatusEffects.SLOWNESS),"Priest must curse faction enemies");enemy.removeStatusEffect(StatusEffects.WEAKNESS);s.npc.cultPulse();check(!enemy.hasStatusEffect(StatusEffects.WEAKNESS),"Curse must have a cooldown");enemy.discard();finish(c,s);});
 }
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=100,batchId="cult_mechanics")
 public void brazierDoesNotSacrificeItsOwnPriest(TestContext c){
  var s=scene(c,true,false);var p=s.altar.getPos();s.npc.refreshPositionAndAngles(p.getX()+.5,p.getY()+.1,p.getZ()+2.05,0,0);s.npc.setNoGravity(true);float hp=s.npc.getHealth();
  c.waitAndRun(45,()->{check(s.npc.isAlive()&&s.npc.getHealth()==hp,"Cult priest must be safe at his own ritual fire");finish(c,s);});
 }
}
