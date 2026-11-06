package ru.zoldrax;

import java.util.*;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.*;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.screen.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.*;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.*;

/** Server owns every item transaction; standard handlers synchronize the chest and book. */
public final class DecorBlockEntity extends BlockEntity implements SidedInventory,NamedScreenHandlerFactory {
 public final DecorKind kind;
 private final DefaultedList<ItemStack> inventory;
 private DecorSeatEntity seatAnchor;
 private final Set<UUID> viewers=new HashSet<>();private int clientViewers,page;
 public float previousLid,lid;
 private final PropertyDelegate pages=new PropertyDelegate(){public int size(){return 1;}public int get(int i){return page;}public void set(int i,int value){page=Math.max(0,Math.min(pageCount()-1,value));markDirty();}};
 public DecorBlockEntity(BlockPos pos,BlockState state){super(DecorBlocks.ENTITY,pos,state);kind=((DecorBaseBlock)state.getBlock()).kind;inventory=DefaultedList.ofSize(kind.slots,ItemStack.EMPTY);}
 public ActionResult use(PlayerEntity player,Hand hand){
  if(world==null||world.isClient)return ActionResult.SUCCESS;
  if(player.isSpectator())return ActionResult.CONSUME;
  var held=player.getStackInHand(hand);
  switch(kind){
   case THRONE->{
    if(seatAnchor!=null&&seatAnchor.isRemoved())seatAnchor=null;
    if(seatAnchor!=null&&seatAnchor.hasPassenger(player)){player.stopRiding();seatAnchor.discard();seatAnchor=null;return ActionResult.CONSUME;}
    if(player.isSneaking())return ActionResult.CONSUME;
    if(seatAnchor!=null&&seatAnchor.hasPassengers()){player.sendMessage(Text.translatable("message.zoldrax.seat_occupied"),true);return ActionResult.CONSUME;}
    if(player.hasVehicle())return ActionResult.CONSUME;
    if(seatAnchor!=null)seatAnchor.discard();
    var seat=DecorBlocks.SEAT.create(world);if(seat==null)return ActionResult.FAIL;seat.setAnchor(pos,getCachedState().get(DecorBaseBlock.FACING));
    if(world.spawnEntity(seat)){seatAnchor=seat;if(!player.startRiding(seat)){seat.discard();seatAnchor=null;}}else seat.discard();
   }
   case BRAZIER,SKULL_LAMP,CHAIN_LANTERN->{
    if(!held.isEmpty())return ActionResult.CONSUME;
    if(kind==DecorKind.CHAIN_LANTERN&&player.isSneaking())changeLength(player);
    else setLit(!getCachedState().get(DecorBaseBlock.LIT));
   }
   case CHEST->player.openHandledScreen(this);
   case WEAPON_RACK->{
    if(held.isEmpty()){for(int i=size()-1;i>=0;i--)if(!getStack(i).isEmpty()){give(player,removeStack(i));break;}}
    else if(held.isIn(DecorBlocks.WEAPONS)){for(int i=0;i<size();i++)if(getStack(i).isEmpty()){setStack(i,takeOne(player,held));return ActionResult.CONSUME;}player.sendMessage(Text.translatable("message.zoldrax.rack_full"),true);}
    else player.sendMessage(Text.translatable("message.zoldrax.rack_weapon"),true);
   }
   case LECTERN->{
    if(player.isSneaking()){if(!getStack(0).isEmpty())give(player,removeStack(0));}
    else if(getStack(0).isEmpty()){if(held.isIn(DecorBlocks.BOOKS)){page=0;setStack(0,takeOne(player,held));}else player.sendMessage(Text.translatable("message.zoldrax.lectern_book"),true);}
    else player.openHandledScreen(this);
   }
   case OFFERING_PEDESTAL->{
    if(player.isSneaking()){if(!getStack(0).isEmpty())give(player,removeStack(0));}
    else if(getStack(0).isEmpty()&&!held.isEmpty())setStack(0,takeOne(player,held));
    else if(!getStack(0).isEmpty())player.sendMessage(Text.translatable("message.zoldrax.take_offering"),true);
   }
  }
  return ActionResult.CONSUME;
 }
 private static ItemStack takeOne(PlayerEntity player,ItemStack held){return player.isCreative()?held.copyWithCount(1):held.split(1);}
 private static void give(PlayerEntity player,ItemStack stack){if(!stack.isEmpty()&&!player.getInventory().insertStack(stack))player.dropItem(stack,false);}
 public void setLit(boolean lit){
  if(world==null||world.isClient||kind.light==0)return;var state=getCachedState();world.setBlockState(pos,state.with(DecorBaseBlock.LIT,lit),Block.NOTIFY_ALL);state=world.getBlockState(pos);
  DecorBlocks.beginChange(world,pos);try{for(int i=1;i<kind.cells(state.get(DecorBaseBlock.LENGTH));i++){var p=pos.add(DecorBlocks.offset(kind,i,state.get(DecorBaseBlock.FACING)));if(world.getBlockState(p).getBlock()==DecorBlocks.PARTS.get(kind))world.setBlockState(p,DecorBlocks.partState(kind,state,i),Block.NOTIFY_ALL);}}finally{DecorBlocks.endChange(world,pos);}
  world.playSound(null,pos,SoundEvents.BLOCK_LEVER_CLICK,SoundCategory.BLOCKS,.22f,lit?.7f:.5f);markDirty();
 }
 public boolean changeLength(PlayerEntity player){
  if(world==null||world.isClient||kind!=DecorKind.CHAIN_LANTERN)return false;
  var old=getCachedState();int next=old.get(DecorBaseBlock.LENGTH)==1?2:1;var state=old.with(DecorBaseBlock.LENGTH,next);
  for(int i=kind.cells(old.get(DecorBaseBlock.LENGTH));i<kind.cells(next);i++){
   var p=pos.add(DecorBlocks.offset(kind,i,state.get(DecorBaseBlock.FACING)));var cell=DecorBlocks.partState(kind,state,i);
   if(!world.isInBuildLimit(p)||!world.getWorldBorder().contains(p)||!world.getBlockState(p).isReplaceable()||!world.canPlace(cell,p,ShapeContext.absent())){player.sendMessage(Text.translatable("message.zoldrax.chain_blocked"),true);return false;}
  }
  DecorBlocks.beginChange(world,pos);try{
   world.setBlockState(pos,state,Block.NOTIFY_ALL);
   for(int i=1;i<kind.cells(next);i++)world.setBlockState(pos.add(DecorBlocks.offset(kind,i,state.get(DecorBaseBlock.FACING))),DecorBlocks.partState(kind,state,i),Block.NOTIFY_ALL);
   for(int i=kind.cells(next);i<kind.cells(old.get(DecorBaseBlock.LENGTH));i++){var p=pos.add(DecorBlocks.offset(kind,i,old.get(DecorBaseBlock.FACING)));if(world.getBlockState(p).getBlock()==DecorBlocks.PARTS.get(kind))world.removeBlock(p,false);}
  }finally{DecorBlocks.endChange(world,pos);}
  world.playSound(null,pos,SoundEvents.BLOCK_CHAIN_PLACE,SoundCategory.BLOCKS,.35f,.8f);markDirty();return true;
 }
 @Override public int size(){return inventory.size();}
 @Override public boolean isEmpty(){for(var s:inventory)if(!s.isEmpty())return false;return true;}
 @Override public ItemStack getStack(int slot){return inventory.get(slot);}
 @Override public ItemStack removeStack(int slot,int amount){var stack=Inventories.splitStack(inventory,slot,amount);if(!stack.isEmpty())changed();return stack;}
 @Override public ItemStack removeStack(int slot){var stack=Inventories.removeStack(inventory,slot);if(!stack.isEmpty())changed();return stack;}
 @Override public void setStack(int slot,ItemStack stack){inventory.set(slot,stack);if(stack.getCount()>getMaxCountPerStack())stack.setCount(getMaxCountPerStack());changed();}
 @Override public int getMaxCountPerStack(){return kind==DecorKind.CHEST?64:1;}
 @Override public boolean canPlayerUse(PlayerEntity player){return world!=null&&world.getBlockEntity(pos)==this&&player.squaredDistanceTo(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<64;}
 @Override public boolean isValid(int slot,ItemStack stack){return switch(kind){case WEAPON_RACK->stack.isIn(DecorBlocks.WEAPONS);case LECTERN->stack.isIn(DecorBlocks.BOOKS);case OFFERING_PEDESTAL,CHEST->true;default->false;};}
 @Override public void clear(){inventory.clear();page=0;changed();}
 @Override public int[] getAvailableSlots(Direction side){if(kind!=DecorKind.CHEST)return new int[0];int[] slots=new int[27];for(int i=0;i<27;i++)slots[i]=i;return slots;}
 @Override public boolean canInsert(int slot,ItemStack stack,Direction dir){return kind==DecorKind.CHEST;}
 @Override public boolean canExtract(int slot,ItemStack stack,Direction dir){return kind==DecorKind.CHEST;}
 private int pageCount(){var tag=size()==0?null:getStack(0).getNbt();return tag==null?1:Math.max(1,tag.getList("pages",8).size());}
 @Override public Text getDisplayName(){return Text.translatable("block.zoldrax."+kind.id);}
 @Override public ScreenHandler createMenu(int syncId,PlayerInventory playerInventory,PlayerEntity player){return kind==DecorKind.CHEST?GenericContainerScreenHandler.createGeneric9x3(syncId,playerInventory,this):kind==DecorKind.LECTERN?new LecternScreenHandler(syncId,this,pages):null;}
 @Override public void onOpen(PlayerEntity player){if(kind==DecorKind.CHEST&&world!=null&&!world.isClient&&!player.isSpectator()){boolean wasEmpty=viewers.isEmpty();if(viewers.add(player.getUuid())){sendViewers();if(wasEmpty)world.playSound(null,pos,DecorBlocks.CHEST_OPEN,SoundCategory.BLOCKS,.6f,.85f);}}}
 @Override public void onClose(PlayerEntity player){if(kind==DecorKind.CHEST&&world!=null&&!world.isClient&&viewers.remove(player.getUuid())){sendViewers();if(viewers.isEmpty())world.playSound(null,pos,DecorBlocks.CHEST_CLOSE,SoundCategory.BLOCKS,.5f,.85f);}}
 private void sendViewers(){world.addSyncedBlockEvent(pos,getCachedState().getBlock(),1,viewers.size());}
 public void serverTick(){if(world.getTime()%20==0&&!viewers.isEmpty()&&world instanceof ServerWorld sw){boolean changed=viewers.removeIf(id->{var p=sw.getServer().getPlayerManager().getPlayer(id);return p==null||!p.isAlive()||!(p.currentScreenHandler instanceof GenericContainerScreenHandler handler)||handler.getInventory()!=this;});if(changed)sendViewers();}}
 public void clientTick(){previousLid=lid;float target=clientViewers>0?1:0;lid=net.minecraft.util.math.MathHelper.clamp(lid+(target>lid?.12f:-.12f),0,1);}
 @Override public boolean onSyncedBlockEvent(int type,int data){if(type==1){clientViewers=data;return true;}return super.onSyncedBlockEvent(type,data);}
 public int viewerCount(){return world!=null&&world.isClient?clientViewers:viewers.size();}
 public void changed(){markDirty();if(world!=null){world.updateListeners(pos,getCachedState(),getCachedState(),Block.NOTIFY_LISTENERS);for(int i=0;i<kind.cells(getCachedState().get(DecorBaseBlock.LENGTH));i++){var cell=pos.add(DecorBlocks.offset(kind,i,getCachedState().get(DecorBaseBlock.FACING)));world.updateComparators(cell,world.getBlockState(cell).getBlock());}}}
 public void dropContents(){if(world==null||world.isClient)return;if(seatAnchor!=null){seatAnchor.discard();seatAnchor=null;}ItemScatterer.spawn(world,pos,this);inventory.clear();viewers.clear();}
 @Override protected void writeNbt(NbtCompound n){super.writeNbt(n);Inventories.writeNbt(n,inventory);n.putInt("Page",page);}
 @Override public void readNbt(NbtCompound n){super.readNbt(n);inventory.clear();Inventories.readNbt(n,inventory);page=Math.max(0,Math.min(n.getInt("Page"),pageCount()-1));if(n.contains("Viewers"))clientViewers=n.getInt("Viewers");}
 @Override public NbtCompound toInitialChunkDataNbt(){var n=createNbt();if(kind==DecorKind.CHEST)n.remove("Items");n.putInt("Viewers",viewerCount());return n;}
 @Override public BlockEntityUpdateS2CPacket toUpdatePacket(){return BlockEntityUpdateS2CPacket.create(this,entity->toInitialChunkDataNbt());}
}
