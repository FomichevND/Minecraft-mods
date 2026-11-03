package ru.zoldrax;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.*;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraft.particle.ParticleTypes;
/** Neutral, persistent followers. Combat is retaliatory until cult mechanics are defined. */
public class CultistEntity extends PathAwareEntity {
 public CultistEntity(EntityType<? extends PathAwareEntity> type,World world){super(type,world);setPersistent();}
 public boolean isPriest(){return getType()==Zoldrax.BLOOD_PRIEST;}
 @Override protected void initGoals(){
  goalSelector.add(0,new SwimGoal(this));goalSelector.add(2,new MeleeAttackGoal(this,1.1,false));
  goalSelector.add(5,new WanderAroundFarGoal(this,.65));goalSelector.add(6,new LookAtEntityGoal(this,PlayerEntity.class,8));goalSelector.add(7,new LookAroundGoal(this));
  targetSelector.add(1,new RevengeGoal(this));
 }
 @Override protected ActionResult interactMob(PlayerEntity player,Hand hand){
  if(!getWorld().isClient)player.sendMessage(Text.literal(isPriest()?"[Жарокровный жрец] Пламя помнит каждое подношение Золдраксу.":"[Цепной каратель] Не оскверняй священный огонь."),false);
  return ActionResult.SUCCESS;
 }
 @Override public void tick(){super.tick();if(isPriest()&&getWorld() instanceof ServerWorld sw&&age%10==0){
  double yaw=Math.toRadians(getYaw());sw.spawnParticles(ParticleTypes.FLAME,getX()-.65*Math.cos(yaw),getY()+2.4,getZ()-.65*Math.sin(yaw),2,.03,.05,.03,.005);
 }}
 @Override protected SoundEvent getAmbientSound(){return isPriest()?SoundEvents.ENTITY_EVOKER_AMBIENT:SoundEvents.ENTITY_VINDICATOR_AMBIENT;}
 @Override protected SoundEvent getHurtSound(net.minecraft.entity.damage.DamageSource s){return SoundEvents.ENTITY_PILLAGER_HURT;}
 @Override protected SoundEvent getDeathSound(){return SoundEvents.ENTITY_PILLAGER_DEATH;}
}
