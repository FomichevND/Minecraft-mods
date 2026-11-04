package ru.zoldrax;
import java.util.*;
import net.minecraft.block.*;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.item.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.world.GameRules;
/** Navigate before touching resources. A full destination or blocked path preserves cargo. */
public class MonkeyFoodGoal extends Goal {
 private final MonkeyFollowerEntity npc;private BlockPos target;private ItemEntity dropped;private ChestBlockEntity chest;private int ticks;private boolean delivering;
 public MonkeyFoodGoal(MonkeyFollowerEntity npc){this.npc=npc;setControls(EnumSet.of(Control.MOVE,Control.LOOK));}
 @Override public boolean canStart(){
  if(npc.getTarget()!=null||npc.age%20!=0||npc.altar()==null)return false;
  target=null;dropped=null;chest=null;delivering=npc.carrying();var altar=npc.altar();
  if(delivering){for(var candidate:npc.chests())if(altar.acceptsChest(candidate.getPos())&&npc.canDeliver(candidate)){chest=candidate;target=chest.getPos();break;}}
  else{
   dropped=npc.getWorld().getEntitiesByClass(ItemEntity.class,npc.getBoundingBox().expand(10),e->e.isAlive()&&MonkeyFollowerEntity.food(e.getStack())).stream().min(Comparator.comparingDouble(npc::squaredDistanceTo)).orElse(null);
   if(dropped!=null)target=dropped.getBlockPos();
   if(target==null&&!npc.isHerald()){
    if(npc.getWorld().getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING))for(var pos:BlockPos.iterateOutwards(npc.getBlockPos(),10,3,10))if(npc.getWorld().isChunkLoaded(pos)&&npc.getWorld().getBlockState(pos).getBlock() instanceof CropBlock crop&&crop.isMature(npc.getWorld().getBlockState(pos))){target=pos.toImmutable();break;}
    if(target==null)for(var candidate:npc.chests())if(!altar.acceptsChest(candidate.getPos())){for(int i=0;i<candidate.size();i++)if(MonkeyFollowerEntity.food(candidate.getStack(i))){chest=candidate;target=chest.getPos();break;}if(target!=null)break;}
   }
  }
  return target!=null;
 }
 @Override public void start(){ticks=0;}
 @Override public boolean shouldContinue(){return target!=null&&ticks<240&&npc.getTarget()==null;}
 @Override public void stop(){npc.getNavigation().stop();target=null;}
 @Override public void tick(){
  ticks++;if(dropped!=null){if(!dropped.isAlive()){target=null;return;}target=dropped.getBlockPos();}
  npc.getLookControl().lookAt(target.getX()+.5,target.getY()+.5,target.getZ()+.5);npc.getNavigation().startMovingTo(target.getX()+.5,target.getY(),target.getZ()+.5,1);
  if(npc.squaredDistanceTo(Vec3d.ofCenter(target))>6.25)return;
  if(dropped!=null){dropped.setStack(npc.cargo.addStack(dropped.getStack().copy()));if(dropped.getStack().isEmpty())dropped.discard();}
  else if(chest!=null){
   if(npc.getWorld().getBlockEntity(target)!=chest){target=null;return;}
   if(delivering)npc.deliver(chest);
   else for(int i=0;i<chest.size();i++)if(MonkeyFollowerEntity.food(chest.getStack(i))){var stack=chest.getStack(i);int count=Math.min(16,stack.getCount());var rest=npc.cargo.addStack(stack.copyWithCount(count));stack.decrement(count-rest.getCount());chest.markDirty();break;}
  }else harvest(target);
  npc.swingHand(Hand.MAIN_HAND);target=null;
 }
 void harvest(BlockPos pos){
  if(!(npc.getWorld() instanceof ServerWorld sw)||!sw.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING))return;
  var state=sw.getBlockState(pos);if(!(state.getBlock() instanceof CropBlock crop)||!crop.isMature(state))return;
  var drops=Block.getDroppedStacks(state,sw,pos,null,npc,ItemStack.EMPTY);var seed=crop.getPickStack(sw,pos,state).getItem();
  var replant=drops.stream().filter(s->s.isOf(seed)&&!s.isEmpty()).findFirst().orElse(null);if(replant==null)return;replant.decrement(1);
  sw.setBlockState(pos,crop.withAge(0),3);
  for(var stack:drops)if(!stack.isEmpty()){var left=npc.cargo.addStack(stack);if(!left.isEmpty())Block.dropStack(sw,pos,left);}
 }
}
