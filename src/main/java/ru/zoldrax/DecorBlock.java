package ru.zoldrax;
import net.minecraft.block.*;
import net.minecraft.block.entity.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class DecorBlock extends DecorBaseBlock implements BlockEntityProvider {
 public DecorBlock(DecorKind kind){super(kind);}
 @Override public BlockRenderType getRenderType(BlockState state){return BlockRenderType.ENTITYBLOCK_ANIMATED;}
 @Override public BlockEntity createBlockEntity(BlockPos pos,BlockState state){return new DecorBlockEntity(pos,state);}
 @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world,BlockState state,BlockEntityType<T> type){
  if(kind!=DecorKind.CHEST||type!=DecorBlocks.ENTITY)return null;
  return (w,p,s,e)->{if(e instanceof DecorBlockEntity decor){if(w.isClient)decor.clientTick();else decor.serverTick();}};
 }
 @Override public boolean onSyncedBlockEvent(BlockState state,World world,BlockPos pos,int type,int data){return world.getBlockEntity(pos) instanceof DecorBlockEntity entity&&entity.onSyncedBlockEvent(type,data);}
}
