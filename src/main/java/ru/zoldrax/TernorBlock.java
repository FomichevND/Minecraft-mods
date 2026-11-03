package ru.zoldrax;
import net.minecraft.block.*;
import net.minecraft.block.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
public class TernorBlock extends BlockWithEntity {
 public TernorBlock(){super(AbstractBlock.Settings.create().strength(4,1200).nonOpaque().luminance(s->7));}
 @Override public BlockRenderType getRenderType(BlockState s){return BlockRenderType.INVISIBLE;}
 @Override public BlockEntity createBlockEntity(BlockPos p,BlockState s){return new TernorEntity(p,s);}
 @Override public BlockState getPlacementState(ItemPlacementContext c){
  for(int x=-1;x<=2;x++)for(int z=-1;z<=2;z++)for(int y=0;y<8;y++){
   BlockPos q=c.getBlockPos().add(x,y,z);if(!c.getWorld().isInBuildLimit(q)||!c.getWorld().getBlockState(q).isReplaceable())return null;
  }return getDefaultState();
 }
 @Override public ActionResult onUse(BlockState s,World w,BlockPos p,PlayerEntity player,Hand h,BlockHitResult hit){
  if(!w.isClient&&w.getBlockEntity(p) instanceof TernorEntity m)m.interact(player);return ActionResult.SUCCESS;
 }
 @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World w,BlockState s,BlockEntityType<T> type){return w.isClient?null:checkType(type,Zoldrax.TERNOR_ENTITY,TernorEntity::tick);}
}
