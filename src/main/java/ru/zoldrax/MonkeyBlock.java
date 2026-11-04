package ru.zoldrax;
import net.minecraft.block.*;
import net.minecraft.block.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
public class MonkeyBlock extends BlockWithEntity {
 public static final net.minecraft.state.property.DirectionProperty FACING=net.minecraft.state.property.Properties.HORIZONTAL_FACING;
 public MonkeyBlock(){super(AbstractBlock.Settings.create().strength(4,1200).nonOpaque().luminance(s->7));setDefaultState(getStateManager().getDefaultState().with(FACING,net.minecraft.util.math.Direction.SOUTH));}
 @Override protected void appendProperties(net.minecraft.state.StateManager.Builder<Block,BlockState> b){b.add(FACING);}
 @Override public BlockState rotate(BlockState s,BlockRotation r){return s.with(FACING,r.rotate(s.get(FACING)));}
 @Override public BlockState mirror(BlockState s,BlockMirror m){return s.rotate(m.getRotation(s.get(FACING)));}
 @Override public BlockRenderType getRenderType(BlockState s){return BlockRenderType.INVISIBLE;}
 @Override public BlockEntity createBlockEntity(BlockPos p,BlockState s){return new MonkeyEntity(p,s);}
 @Override public BlockState getPlacementState(ItemPlacementContext c){
  for(int x=-2;x<=3;x++)for(int z=-2;z<=2;z++)for(int y=0;y<5;y++){
   BlockPos q=c.getBlockPos().add(x,y,z);if(!c.getWorld().isInBuildLimit(q)||!c.getWorld().getBlockState(q).isReplaceable())return null;
  }return getDefaultState();
 }
 @Override public ActionResult onUse(BlockState s,World w,BlockPos p,PlayerEntity player,Hand h,BlockHitResult hit){
  if(!w.isClient&&w.getBlockEntity(p) instanceof MonkeyEntity m){
   if(player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer)m.tryFirstEncounter(serverPlayer);
   player.sendMessage(m.status(),false);
  }return ActionResult.SUCCESS;
 }
 @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World w,BlockState s,BlockEntityType<T> type){return w.isClient?null:checkType(type,Zoldrax.MONKEY_ENTITY,MonkeyEntity::tick);}
}
