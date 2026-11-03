package ru.zoldrax;
import net.minecraft.entity.*;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.nbt.*;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
public class TernorGuardian extends IronGolemEntity {
 private BlockPos home;
 public TernorGuardian(EntityType<? extends IronGolemEntity> type,World world){super(type,world);setPlayerCreated(true);setPersistent();}
 public void setHome(BlockPos pos){home=pos.toImmutable();setPositionTarget(home,48);}
 public BlockPos getHome(){return home==null?getBlockPos():home;}
 public boolean protects(Vec3d point){return point.squaredDistanceTo(Vec3d.ofCenter(getHome()))<=2500;}
 @Override public void setTarget(net.minecraft.entity.LivingEntity target){
  super.setTarget(target instanceof PlayerEntity||(target!=null&&!protects(target.getPos()))?null:target);
 }
 @Override public void tick(){
  super.tick();if(getWorld().isClient||age%20!=0)return;
  if(home==null)setHome(getBlockPos());
  if(!protects(getPos())){setTarget(null);getNavigation().startMovingTo(home.getX()+.5,home.getY(),home.getZ()+.5,1.15);return;}
  if(getTarget()!=null&&(!getTarget().isAlive()||!protects(getTarget().getPos())))setTarget(null);
  if(getTarget()==null){
   var enemies=getWorld().getEntitiesByClass(HostileEntity.class,new Box(home).expand(50),e->e.isAlive()&&protects(e.getPos())&&canSee(e));
   enemies.stream().min(java.util.Comparator.comparingDouble(this::squaredDistanceTo)).ifPresent(this::setTarget);
  }
 }
 @Override protected ActionResult interactMob(PlayerEntity player,Hand hand){
  if(player.getStackInHand(hand).isOf(Items.IRON_INGOT)){
   if(!getWorld().isClient)player.sendMessage(Text.literal("Страж Тернора не восстанавливается железом."),true);
   return ActionResult.FAIL;
  }return super.interactMob(player,hand);
 }
 @Override public void writeCustomDataToNbt(NbtCompound n){super.writeCustomDataToNbt(n);if(home!=null)n.put("TernorHome",NbtHelper.fromBlockPos(home));}
 @Override public void readCustomDataFromNbt(NbtCompound n){super.readCustomDataFromNbt(n);if(n.contains("TernorHome"))setHome(NbtHelper.toBlockPos(n.getCompound("TernorHome")));setPlayerCreated(true);setPersistent();}
}
