package ru.zoldrax;

import java.util.Optional;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Stable entry points for future smith, farmer and keeper NPC goals. */
public final class TernorWorkstationApi {
 public static Optional<DecorBlockEntity> find(World world,BlockPos pos,DecorKind expected){
  var state=world.getBlockState(pos);if(!(state.getBlock() instanceof DecorBaseBlock block)||block.kind!=expected)return Optional.empty();var entity=DecorBlocks.entity(world,pos,state);return entity!=null&&entity.kind==expected?Optional.of(entity):Optional.empty();
 }
 public static boolean activateBellows(World world,BlockPos pos){return find(world,pos,DecorKind.BELLOWS).map(e->e.ternor.activateBellows()).orElse(false);}
 public static int fillWater(World world,BlockPos pos,int levels){var state=world.getBlockState(pos);if(!(state.getBlock() instanceof DecorBaseBlock block)||(block.kind!=DecorKind.ANIMAL_TROUGH&&block.kind!=DecorKind.QUENCHING_TUB))return 0;var entity=DecorBlocks.entity(world,pos,state);return entity==null?0:entity.ternor.addWater(levels);}
 public static int insert(World world,BlockPos pos,ItemStack source,int limit){
  var state=world.getBlockState(pos);if(!(state.getBlock() instanceof DecorBaseBlock))return 0;var entity=DecorBlocks.entity(world,pos,state);if(entity==null||source.isEmpty()||limit<=0)return 0;int moved=0;
  for(int pass=0;pass<2&&moved<limit&&!source.isEmpty();pass++)for(int slot=0;slot<entity.size()&&moved<limit&&!source.isEmpty();slot++){var current=entity.getStack(slot);if(!entity.isValid(slot,source)||pass==0&&(current.isEmpty()||!ItemStack.canCombine(current,source))||pass==1&&!current.isEmpty())continue;int room=current.isEmpty()?Math.min(entity.getMaxCountPerStack(),source.getMaxCount()):Math.min(entity.getMaxCountPerStack(),current.getMaxCount())-current.getCount();int count=Math.min(Math.min(room,limit-moved),source.getCount());if(count<=0)continue;if(current.isEmpty())entity.setStack(slot,source.copyWithCount(count));else current.increment(count);source.decrement(count);moved+=count;}
  if(moved>0)entity.changed();return moved;
 }
 private TernorWorkstationApi(){}
}
