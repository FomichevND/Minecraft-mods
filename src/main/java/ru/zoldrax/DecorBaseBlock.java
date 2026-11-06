package ru.zoldrax;

import net.minecraft.block.*;
import net.minecraft.block.enums.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.*;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.*;
import net.minecraft.particle.*;
import net.minecraft.sound.*;

/** Shared state and structural integrity for master and non-inventory companion cells. */
public class DecorBaseBlock extends Block implements InventoryProvider {
 public static final DirectionProperty FACING=Properties.HORIZONTAL_FACING;
 public static final BooleanProperty LIT=Properties.LIT;
 public static final IntProperty PART=IntProperty.of("part",0,15),LENGTH=IntProperty.of("chain_length",1,2);
 public final DecorKind kind;
 public DecorBaseBlock(DecorKind kind){
  super(AbstractBlock.Settings.create().strength(3.5f,9).pistonBehavior(net.minecraft.block.piston.PistonBehavior.BLOCK).nonOpaque().dynamicBounds().sounds(kind.monkey()?BlockSoundGroup.WOOD:BlockSoundGroup.DEEPSLATE_TILES).luminance(s->s.get(LIT)&&s.get(PART)==kind.emitter(s.get(LENGTH))?kind.light:0));
  this.kind=kind;setDefaultState(getStateManager().getDefaultState().with(FACING,Direction.SOUTH).with(PART,0).with(LENGTH,1).with(LIT,true));
 }
 @Override protected void appendProperties(StateManager.Builder<Block,BlockState> builder){builder.add(FACING,LIT,PART,LENGTH);}
 @Override public BlockRenderType getRenderType(BlockState state){return BlockRenderType.INVISIBLE;}
 @Override public int getOpacity(BlockState state,BlockView world,BlockPos pos){return 0;}
 @Override public VoxelShape getOutlineShape(BlockState state,BlockView world,BlockPos pos,ShapeContext context){return DecorBlocks.shape(kind,state,false);}
 @Override public VoxelShape getCollisionShape(BlockState state,BlockView world,BlockPos pos,ShapeContext context){return DecorBlocks.shape(kind,state,true);}
 @Override public BlockState rotate(BlockState state,BlockRotation rotation){return state.with(FACING,rotation.rotate(state.get(FACING)));}
 @Override public BlockState getPlacementState(ItemPlacementContext context){
  if(!(this instanceof DecorBlock))return null;
  var state=getDefaultState().with(FACING,context.getHorizontalPlayerFacing().getOpposite());
  if(!canPlaceAt(state,context.getWorld(),context.getBlockPos()))return null;
  for(int i=0;i<kind.cells(1);i++){
   var p=context.getBlockPos().add(DecorBlocks.offset(kind,i,state.get(FACING)));
   var cell=i==0?state:DecorBlocks.partState(kind,state,i);
   if(!context.getWorld().isInBuildLimit(p)||!context.getWorld().getWorldBorder().contains(p)||!context.getWorld().getBlockState(p).canReplace(context)||!context.getWorld().canPlace(cell,p,ShapeContext.absent()))return null;
  }
  return state;
 }
 @Override public boolean canPlaceAt(BlockState state,WorldView world,BlockPos pos){
  if(state.get(PART)>0)return true;
  if(kind.hanging())return world.getBlockState(pos.up()).isSideSolidFullSquare(world,pos.up(),Direction.DOWN);
  for(int i=0;i<kind.width*kind.depth();i++){var p=pos.add(DecorBlocks.offset(kind,i,state.get(FACING))).down();if(!world.getBlockState(p).isSideSolidFullSquare(world,p,Direction.UP)&&!(kind==DecorKind.CHEST&&world.getBlockState(p).isOf(Blocks.HOPPER)))return false;}
  return true;
 }
 @Override public void onPlaced(World world,BlockPos pos,BlockState state,net.minecraft.entity.LivingEntity placer,ItemStack stack){
  super.onPlaced(world,pos,state,placer,stack);if(world.isClient||state.get(PART)!=0)return;
  DecorBlocks.beginChange(world,pos);try{for(int i=1;i<kind.cells(state.get(LENGTH));i++)world.setBlockState(pos.add(DecorBlocks.offset(kind,i,state.get(FACING))),DecorBlocks.partState(kind,state,i),Block.NOTIFY_ALL);}finally{DecorBlocks.endChange(world,pos);}
 }
 @Override public BlockState getStateForNeighborUpdate(BlockState state,Direction direction,BlockState neighbor,WorldAccess world,BlockPos pos,BlockPos neighborPos){
  var root=DecorBlocks.root(pos,state);
  if(state.get(PART)>0){
   if(world.isChunkLoaded(root)){var master=world.getBlockState(root);if(master.getBlock()!=DecorBlocks.BLOCKS.get(kind))return Blocks.AIR.getDefaultState();}
  }else if(!canPlaceAt(state,world,pos))world.scheduleBlockTick(pos,this,1);
  return state;
 }
 @Override public void scheduledTick(BlockState state,ServerWorld world,BlockPos pos,Random random){if(state.get(PART)==0&&!canPlaceAt(state,world,pos))world.breakBlock(pos,true);}
 @Override public void onBreak(World world,BlockPos pos,BlockState state,PlayerEntity player){
  if(!world.isClient&&state.get(PART)>0){var root=DecorBlocks.root(pos,state);if(world.getBlockState(root).getBlock()==DecorBlocks.BLOCKS.get(kind))world.breakBlock(root,!player.isCreative(),player);}
  super.onBreak(world,pos,state,player);
 }
 @Override public void onStateReplaced(BlockState state,World world,BlockPos pos,BlockState next,boolean moved){
  if(state.getBlock()!=next.getBlock()&&!world.isClient){
   var root=DecorBlocks.root(pos,state);
   if(state.get(PART)==0&&!DecorBlocks.changing(world,root)){
    DecorBlocks.beginChange(world,root);try{
     if(world.getBlockEntity(root) instanceof DecorBlockEntity entity)entity.dropContents();
     for(var seat:world.getEntitiesByClass(DecorSeatEntity.class,new Box(root).expand(4),e->e.anchor().equals(root)))seat.discard();
     for(int i=1;i<kind.cells(state.get(LENGTH));i++){var p=root.add(DecorBlocks.offset(kind,i,state.get(FACING)));var cell=world.getBlockState(p);if(cell.getBlock()==DecorBlocks.PARTS.get(kind))world.removeBlock(p,false);}
    }finally{DecorBlocks.endChange(world,root);}
   }else if(state.get(PART)>0&&!DecorBlocks.changing(world,root)&&world.getBlockState(root).getBlock()==DecorBlocks.BLOCKS.get(kind))world.breakBlock(root,true);
  }
  if(state.get(PART)==0&&state.getBlock()!=next.getBlock())world.removeBlockEntity(pos);
  super.onStateReplaced(state,world,pos,next,moved);
 }
 @Override public ActionResult onUse(BlockState state,World world,BlockPos pos,PlayerEntity player,Hand hand,BlockHitResult hit){
  if(world.isClient)return ActionResult.SUCCESS;
  var entity=DecorBlocks.entity(world,pos,state);return entity==null?ActionResult.PASS:entity.useAt(player,hand,hit);
 }
 @Override public net.minecraft.inventory.SidedInventory getInventory(BlockState state,WorldAccess world,BlockPos pos){
  var root=DecorBlocks.root(pos,state);
  return kind==DecorKind.CHEST&&world.getBlockEntity(root) instanceof DecorBlockEntity entity?entity:EMPTY_INVENTORY;
 }
 private static final net.minecraft.inventory.SidedInventory EMPTY_INVENTORY=new net.minecraft.inventory.SidedInventory(){
  public int size(){return 0;}public boolean isEmpty(){return true;}public ItemStack getStack(int slot){return ItemStack.EMPTY;}public ItemStack removeStack(int slot,int amount){return ItemStack.EMPTY;}public ItemStack removeStack(int slot){return ItemStack.EMPTY;}public void setStack(int slot,ItemStack stack){}public void markDirty(){}public boolean canPlayerUse(PlayerEntity p){return false;}public void clear(){}public int[] getAvailableSlots(Direction side){return new int[0];}public boolean canInsert(int slot,ItemStack stack,Direction side){return false;}public boolean canExtract(int slot,ItemStack stack,Direction side){return false;}
 };
 @Override public boolean hasComparatorOutput(BlockState state){return kind.slots>0;}
 @Override public int getComparatorOutput(BlockState state,World world,BlockPos pos){var entity=DecorBlocks.entity(world,pos,state);return entity==null?0:net.minecraft.screen.ScreenHandler.calculateComparatorOutput((net.minecraft.inventory.Inventory)entity);}
 @Override public void randomDisplayTick(BlockState state,World world,BlockPos pos,Random random){
  if(state.get(PART)!=0)return;boolean lit=state.get(LIT);double y=kind==DecorKind.CHAIN_LANTERN?-.5-(state.get(LENGTH)-1):kind==DecorKind.SKULL_LAMP?1.7:1.25;
  var dust=new DustParticleEffect(new org.joml.Vector3f(1,.06f,.015f),.7f);
  if(lit&&kind.light>0){
   if(kind==DecorKind.BRAZIER){world.addParticle(dust,pos.getX()+.5+(random.nextDouble()-.5)*.4,pos.getY()+y,pos.getZ()+.5+(random.nextDouble()-.5)*.4,0,.025,0);if(random.nextInt(3)==0)world.addParticle(ParticleTypes.SMALL_FLAME,pos.getX()+.5+(random.nextDouble()-.5)*.3,pos.getY()+y,pos.getZ()+.5+(random.nextDouble()-.5)*.3,0,.015,0);if(random.nextInt(4)==0)world.addParticle(ParticleTypes.SMOKE,pos.getX()+.5,pos.getY()+y+.2,pos.getZ()+.5,0,.025,0);if(random.nextInt(45)==0)world.playSound(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,SoundEvents.BLOCK_CAMPFIRE_CRACKLE,SoundCategory.BLOCKS,.12f,.8f+random.nextFloat()*.2f,false);}
   else if(random.nextInt(3)==0){var p=DecorBlocks.point(pos,state.get(FACING),.5+(random.nextBoolean()?-.16:.16),y,kind==DecorKind.SKULL_LAMP?.91:.5);world.addParticle(dust,p.x,p.y,p.z,0,.01,0);}
  }
  if(kind==DecorKind.OFFERING_PEDESTAL&&world.getBlockEntity(pos) instanceof DecorBlockEntity e&&e.getStack(0).isIn(DecorBlocks.GEMS)&&random.nextInt(3)==0)world.addParticle(dust,pos.getX()+.5+(random.nextDouble()-.5)*.35,pos.getY()+1.45,pos.getZ()+.5+(random.nextDouble()-.5)*.35,0,.01,0);
 }
}
