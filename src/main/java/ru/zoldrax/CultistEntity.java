package ru.zoldrax;
import java.util.*;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.effect.*;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.*;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.particle.ParticleTypes;
/** Neutral to ordinary players, hostile to the future Ternor follower faction. */
public class CultistEntity extends PathAwareEntity {
 private BlockPos shrine;
 private ItemStack offering=ItemStack.EMPTY;
 private UUID donor;
 private long nextShrineSearch,nextCurse;
 public CultistEntity(EntityType<? extends PathAwareEntity> type,World world){super(type,world);setPersistent();}
 public boolean isPriest(){return getType()==Zoldrax.BLOOD_PRIEST;}
 @Override public boolean isFireImmune(){return !isPriest()||super.isFireImmune();}
 @Override protected void initGoals(){
  goalSelector.add(0,new SwimGoal(this));goalSelector.add(2,new MeleeAttackGoal(this,1.1,false));
  goalSelector.add(3,new CultOfferingGoal(this));
  goalSelector.add(4,new Goal(){
   {setControls(java.util.EnumSet.of(Control.MOVE));}
   @Override public boolean canStart(){return getTarget()==null&&shrine!=null&&squaredDistanceTo(Vec3d.ofCenter(shrine))>256;}
   @Override public void tick(){if(shrine!=null)getNavigation().startMovingTo(shrine.getX()+.5,shrine.getY(),shrine.getZ()+3.7,.9);}
  });
  goalSelector.add(5,new WanderAroundFarGoal(this,.65));goalSelector.add(6,new LookAtEntityGoal(this,PlayerEntity.class,8));goalSelector.add(7,new LookAroundGoal(this));
  targetSelector.add(1,new RevengeGoal(this));targetSelector.add(2,new ActiveTargetGoal<>(this,LivingEntity.class,0,true,false,CultFactions::isEnemy));
 }
 @Override public void setTarget(LivingEntity target){super.setTarget(target instanceof CultistEntity?null:target);}
 /** Resolves only loaded chunks; does not generate new terrain. */
 public MonumentEntity altar(){
  if(!(getWorld() instanceof ServerWorld sw))return null;
  if(shrine!=null){
   if(!sw.isChunkLoaded(shrine))return null;
   if(sw.getBlockEntity(shrine) instanceof MonumentEntity altar&&altar.stage==2)return altar;
   shrine=null;offeringCheckHome();
  }
  if(sw.getTime()<nextShrineSearch)return null;nextShrineSearch=sw.getTime()+100;
  MonumentEntity best=null;double distance=1024;int cx=getBlockX()>>4,cz=getBlockZ()>>4;
  for(int x=cx-2;x<=cx+2;x++)for(int z=cz-2;z<=cz+2;z++){
   var chunk=sw.getChunkManager().getWorldChunk(x,z);if(chunk==null)continue;
   for(var entity:chunk.getBlockEntities().values())if(entity instanceof MonumentEntity altar&&altar.stage==2){double d=squaredDistanceTo(Vec3d.ofCenter(altar.getPos()));if(d<distance){best=altar;distance=d;}}
  }
  if(best!=null){shrine=best.getPos().toImmutable();setPositionTarget(shrine,12);}return best;
 }
 private void offeringCheckHome(){clearPositionTarget();}
 public boolean hasOffering(){return !offering.isEmpty();}
 ItemEntity findOffering(){
  var altar=altar();if(altar==null)return null;
  return getWorld().getEntitiesByClass(ItemEntity.class,getBoundingBox().expand(12),item->item.isAlive()&&CultFactions.isOffering(item.getStack())&&!altar.brazierBounds().intersects(item.getBoundingBox()))
   .stream().min(Comparator.comparingDouble(this::squaredDistanceTo)).orElse(null);
 }
 void collectOffering(ItemEntity item){
  if(hasOffering()||!item.isAlive()||!CultFactions.isOffering(item.getStack()))return;
  offering=item.getStack().copyWithCount(1);donor=item.getOwner() instanceof PlayerEntity p?p.getUuid():null;
  item.getStack().decrement(1);if(item.getStack().isEmpty())item.discard();
 }
 void throwOffering(){
  var altar=altar();if(altar==null||offering.isEmpty())return;
  var stack=offering.copy();stack.getOrCreateNbt().putBoolean("ZoldraxCultOffering",true);stack.getOrCreateNbt().putLong("ZoldraxAltar",altar.getPos().asLong());
  var item=new ItemEntity(getWorld(),getX(),getY()+1.2,getZ(),stack);item.setPickupDelay(40);item.setThrower(donor==null?getUuid():donor);
  Vec3d aim=new Vec3d(altar.getPos().getX()+.5,altar.getPos().getY()+.6,altar.getPos().getZ()+2.05).subtract(item.getPos()).normalize().multiply(.28);
  item.setVelocity(aim.x,.05,aim.z);if(getWorld().spawnEntity(item)){offering=ItemStack.EMPTY;donor=null;swingHand(Hand.MAIN_HAND);}
 }
 @Override protected ActionResult interactMob(PlayerEntity player,Hand hand){
  if(!getWorld().isClient)player.sendMessage(Text.literal(isPriest()?"[Жарокровный жрец] Я собираю драгоценности у алтаря и усиливаю жертвы. Последователи Тернора здесь нежеланны.":"[Цепной каратель] Священный огонь под моей защитой. Последователям Тернора сюда пути нет."),false);
  return ActionResult.SUCCESS;
 }
 void cultPulse(){
  var altar=altar();
  if(!isPriest()){
   boolean treasure=!getWorld().getEntitiesByClass(ItemEntity.class,getBoundingBox().expand(6),i->i.isAlive()&&CultFactions.isOffering(i.getStack())).isEmpty();
   if(treasure||(altar!=null&&altar.wasRecentlyFed()&&squaredDistanceTo(Vec3d.ofCenter(altar.getPos()))<=144)){
    addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH,80,0));addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE,80,0));
   }
  }else{
   LivingEntity enemy=getTarget();
   if(enemy!=null&&enemy.isAlive()&&squaredDistanceTo(enemy)<=144&&canSee(enemy)&&getWorld().getTime()>=nextCurse){
    enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS,120,0));enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,120,0));nextCurse=getWorld().getTime()+200;
    if(getWorld() instanceof ServerWorld sw)sw.spawnParticles(ParticleTypes.SMOKE,enemy.getX(),enemy.getY()+1,enemy.getZ(),10,.3,.4,.3,.01);swingHand(Hand.MAIN_HAND);
   }
  }
 }
 @Override public boolean tryAttack(Entity target){
  boolean hit=super.tryAttack(target);if(hit&&!isPriest()&&target instanceof LivingEntity living&&living.isAlive()){
   var altar=altar();if(altar!=null){var box=altar.brazierBounds();Vec3d toward=box.getCenter().subtract(target.getPos()).multiply(1,0,1).normalize();target.addVelocity(toward.x*.9,.18,toward.z*.9);target.velocityModified=true;}
  }return hit;
 }
 @Override public void tick(){super.tick();if(getWorld().isClient)return;
  if(isPriest()&&getMainHandStack().isEmpty()){equipStack(EquipmentSlot.MAINHAND,new ItemStack(Zoldrax.RITUAL_STAFF));setEquipmentDropChance(EquipmentSlot.MAINHAND,0);}
  if(age%20==0)cultPulse();
  if(isPriest()&&getWorld() instanceof ServerWorld sw&&age%10==0&&getMainHandStack().isOf(Zoldrax.RITUAL_STAFF)){
   double yaw=Math.toRadians(getYaw());sw.spawnParticles(ParticleTypes.FLAME,getX()-.625*Math.cos(yaw),getY()+2.3,getZ()-.625*Math.sin(yaw),2,.03,.05,.03,.005);
  }
 }
 @Override public void writeCustomDataToNbt(NbtCompound n){super.writeCustomDataToNbt(n);if(shrine!=null)n.put("CultShrine",NbtHelper.fromBlockPos(shrine));if(!offering.isEmpty())n.put("CultOffering",offering.writeNbt(new NbtCompound()));if(donor!=null)n.putUuid("CultDonor",donor);n.putLong("CultNextCurse",nextCurse);}
 @Override public void readCustomDataFromNbt(NbtCompound n){super.readCustomDataFromNbt(n);shrine=n.contains("CultShrine")?NbtHelper.toBlockPos(n.getCompound("CultShrine")):null;if(shrine!=null)setPositionTarget(shrine,12);offering=n.contains("CultOffering")?ItemStack.fromNbt(n.getCompound("CultOffering")):ItemStack.EMPTY;donor=n.containsUuid("CultDonor")?n.getUuid("CultDonor"):null;nextCurse=n.getLong("CultNextCurse");}
 @Override protected void dropEquipment(net.minecraft.entity.damage.DamageSource source,int looting,boolean drops){super.dropEquipment(source,looting,drops);if(!offering.isEmpty()){dropStack(offering);offering=ItemStack.EMPTY;}}
 @Override protected SoundEvent getAmbientSound(){return isPriest()?SoundEvents.ENTITY_EVOKER_AMBIENT:SoundEvents.ENTITY_VINDICATOR_AMBIENT;}
 @Override protected SoundEvent getHurtSound(net.minecraft.entity.damage.DamageSource s){return SoundEvents.ENTITY_PILLAGER_HURT;}
 @Override protected SoundEvent getDeathSound(){return SoundEvents.ENTITY_PILLAGER_DEATH;}
}
