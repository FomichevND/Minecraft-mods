package ru.zoldrax;

import net.minecraft.block.*;
import net.minecraft.block.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.*;

public class MonumentBlock extends BlockWithEntity {
 public MonumentBlock() { super(AbstractBlock.Settings.create().strength(4, 1200).nonOpaque().luminance(s -> 10)); }
 @Override public BlockRenderType getRenderType(BlockState s) { return BlockRenderType.INVISIBLE; }
 @Override public BlockEntity createBlockEntity(BlockPos p, BlockState s) { return new MonumentEntity(p,s); }
 @Override public BlockState getPlacementState(ItemPlacementContext c) {
  BlockPos p=c.getBlockPos();
  for(int x=-1;x<=1;x++) for(int z=-1;z<=1;z++) for(int y=0;y<6;y++) {
   BlockPos q=p.add(x,y,z);
   if(!c.getWorld().isInBuildLimit(q) || !c.getWorld().getBlockState(q).isReplaceable()) return null;
  }
  return getDefaultState();
 }
 @Override public ActionResult onUse(BlockState s, World w, BlockPos p, PlayerEntity player, Hand hand, BlockHitResult hit) {
  if(!w.isClient && w.getBlockEntity(p) instanceof MonumentEntity m) player.sendMessage(Text.literal(m.description()),false);
  return ActionResult.SUCCESS;
 }
 @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World w, BlockState s, BlockEntityType<T> t) {
  return w.isClient ? null : checkType(t,Zoldrax.ENTITY,MonumentEntity::tick);
 }
}
