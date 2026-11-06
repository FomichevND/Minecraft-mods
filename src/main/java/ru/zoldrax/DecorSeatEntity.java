package ru.zoldrax;

import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.*;
import net.minecraft.world.World;

/** Only a tracked riding anchor: no visible armor stand, inventory or persistent ghost seat. */
public final class DecorSeatEntity extends Entity {
 private static final net.minecraft.entity.data.TrackedData<BlockPos> ANCHOR=net.minecraft.entity.data.DataTracker.registerData(DecorSeatEntity.class,net.minecraft.entity.data.TrackedDataHandlerRegistry.BLOCK_POS);
 public DecorSeatEntity(EntityType<? extends DecorSeatEntity> type,World world){super(type,world);noClip=true;setNoGravity(true);}
 @Override protected void initDataTracker(){dataTracker.startTracking(ANCHOR,BlockPos.ORIGIN);}
 public BlockPos anchor(){return dataTracker.get(ANCHOR);}
 public void setAnchor(BlockPos pos,Direction facing){dataTracker.set(ANCHOR,pos.toImmutable());setPositionForSeat(pos,facing,1,.75,.6);}
 public void setPositionForSeat(BlockPos pos,Direction facing,double x,double y,double z){dataTracker.set(ANCHOR,pos.toImmutable());var p=DecorBlocks.point(pos,facing,x,y,z);refreshPositionAndAngles(p.x,p.y,p.z,facing.asRotation(),0);}
 @Override protected void readCustomDataFromNbt(NbtCompound n){dataTracker.set(ANCHOR,net.minecraft.nbt.NbtHelper.toBlockPos(n.getCompound("Anchor")));}
 @Override protected void writeCustomDataToNbt(NbtCompound n){n.put("Anchor",net.minecraft.nbt.NbtHelper.fromBlockPos(anchor()));}
 @Override public boolean shouldSave(){return false;}
 @Override public void tick(){super.tick();if(!getWorld().isClient&&(!(getWorld().getBlockState(anchor()).getBlock() instanceof DecorBlock b&&(b.kind==DecorKind.THRONE||b.kind==DecorKind.FESTIVAL_BENCH))||!hasPassengers()))discard();}
 @Override protected boolean canAddPassenger(Entity passenger){return passenger instanceof LivingEntity&&!hasPassengers();}
 @Override protected void updatePassengerPosition(Entity passenger,PositionUpdater updater){
  if(!hasPassenger(passenger))return;updater.accept(passenger,getX(),getY()-.75,getZ());
  passenger.setBodyYaw(getYaw());float delta=MathHelper.wrapDegrees(passenger.getYaw()-getYaw());float clamped=MathHelper.clamp(delta,-100,100);passenger.prevYaw+=clamped-delta;passenger.setYaw(passenger.getYaw()+clamped-delta);passenger.setHeadYaw(passenger.getYaw());
 }
 @Override public Vec3d updatePassengerForDismount(LivingEntity passenger){
  var state=getWorld().getBlockState(anchor());Direction facing=state.getBlock() instanceof DecorBaseBlock?state.get(DecorBaseBlock.FACING):Direction.fromRotation(getYaw());
  for(double[] offset:new double[][]{{1,1.6},{-.6,.5},{2.6,.5},{1,-.6}}){var p=DecorBlocks.point(anchor(),facing,offset[0],0,offset[1]);for(int dy=1;dy>=-2;dy--){var candidate=p.add(0,dy,0);var box=passenger.getDimensions(EntityPose.STANDING).getBoxAt(candidate);if(getWorld().isSpaceEmpty(passenger,box)&&getWorld().getBlockState(BlockPos.ofFloored(candidate).down()).isSolidBlock(getWorld(),BlockPos.ofFloored(candidate).down()))return candidate;}}
  return new Vec3d(getX(),anchor().getY()+1,getZ()+1.5);
 }
}
