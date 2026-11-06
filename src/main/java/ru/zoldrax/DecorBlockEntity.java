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
 private final DecorSeatEntity[] benchSeats=new DecorSeatEntity[2];
 private final Map<UUID,Map<Integer,Integer>> mapViewers=new HashMap<>();
 public long lastStrike=-100;public float strikeTicks;private int bookSlot;private int selectedSlot=-1;
 private final Set<UUID> viewers=new HashSet<>();private int clientViewers,page;
 public float previousLid,lid;
 private final PropertyDelegate pages=new PropertyDelegate(){public int size(){return 1;}public int get(int i){return page;}public void set(int i,int value){page=Math.max(0,Math.min(pageCount()-1,value));markDirty();}};
 public DecorBlockEntity(BlockPos pos,BlockState state){super(DecorBlocks.ENTITY,pos,state);kind=((DecorBaseBlock)state.getBlock()).kind;inventory=DefaultedList.ofSize(kind.slots,ItemStack.EMPTY);}
 public ActionResult use(PlayerEntity player,Hand hand){
  if(world==null||world.isClient)return ActionResult.SUCCESS;
  if(player.isSpectator())return ActionResult.CONSUME;
  var held=player.getStackInHand(hand);
  if(kind.monkey())return useMonkey(player,hand);
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

 public ActionResult useAt(PlayerEntity player,Hand hand,net.minecraft.util.hit.BlockHitResult hit){
  if(kind.foodDisplay()||kind==DecorKind.NOTICE_BOARD){double best=Double.MAX_VALUE;var slots=kind==DecorKind.NOTICE_BOARD?java.util.List.of(new FoodDisplaySlots.Slot(.9,1.75,.435,0,0,false),new FoodDisplaySlots.Slot(2.1,1.75,.435,0,0,false),new FoodDisplaySlots.Slot(1.5,.85,.435,0,0,false)):FoodDisplaySlots.forKind(kind);for(int i=0;i<slots.size();i++){var slot=slots.get(i);var point=DecorBlocks.point(pos,getCachedState().get(DecorBaseBlock.FACING),slot.x(),slot.y(),slot.z());double distance=point.squaredDistanceTo(hit.getPos());if(distance<best){best=distance;selectedSlot=i;}}}
  try{return use(player,hand);}finally{selectedSlot=-1;}
 }
 public static boolean isDocument(ItemStack stack){return stack.isOf(Items.PAPER)||stack.isOf(Items.MAP)||stack.isOf(Items.FILLED_MAP)||stack.isIn(DecorBlocks.BOOKS);}
 public boolean insertDisplay(ItemStack source){if(source.isEmpty())return false;for(int i=0;i<size();i++)if(getStack(i).isEmpty()&&isValid(i,source)){setStack(i,source.split(1));return true;}return false;}
 public ItemStack takeDisplay(int slot){return slot>=0&&slot<size()?removeStack(slot):ItemStack.EMPTY;}
 public boolean strike(){if(world==null||world.isClient||world.getTime()-lastStrike<(kind==DecorKind.COURTYARD_GONG?16:3))return false;lastStrike=world.getTime();world.addSyncedBlockEvent(pos,getCachedState().getBlock(),2,0);world.playSound(null,pos,kind==DecorKind.COURTYARD_GONG?SoundEvents.BLOCK_BELL_USE:SoundEvents.BLOCK_NOTE_BLOCK_BASEDRUM.value(),SoundCategory.BLOCKS,kind==DecorKind.COURTYARD_GONG?2:1.2f,kind==DecorKind.COURTYARD_GONG?.65f:.7f);return true;}
 public boolean sitOnBench(net.minecraft.entity.LivingEntity player){
  for(int i=0;i<2;i++){var seat=benchSeats[i];if(seat!=null&&!seat.isRemoved()&&seat.hasPassenger(player)){player.stopRiding();seat.discard();benchSeats[i]=null;return true;}}
  if(player.isSneaking()||player.hasVehicle())return false;
  for(int i=0;i<2;i++)if(benchSeats[i]==null||benchSeats[i].isRemoved()||!benchSeats[i].hasPassengers()){if(benchSeats[i]!=null)benchSeats[i].discard();var seat=DecorBlocks.SEAT.create(world);seat.setPositionForSeat(pos,getCachedState().get(DecorBaseBlock.FACING),i==0?1:2,13/16.0,.6);if(world.spawnEntity(seat)&&player.startRiding(seat)){benchSeats[i]=seat;return true;}seat.discard();}return false;
 }
 private ActionResult useMonkey(PlayerEntity player,Hand hand){var held=player.getStackInHand(hand);
  if(kind==DecorKind.FESTIVAL_BENCH){sitOnBench(player);return ActionResult.CONSUME;}
  if(kind==DecorKind.COURTYARD_GONG||kind==DecorKind.FESTIVAL_DRUM){strike();return ActionResult.CONSUME;}
  if(player.isSneaking()&&held.isEmpty()){if(selectedSlot>=0&&!getStack(selectedSlot).isEmpty()){give(player,removeStack(selectedSlot));return ActionResult.CONSUME;}for(int i=size()-1;i>=0;i--)if(!getStack(i).isEmpty()){give(player,removeStack(i));break;}return ActionResult.CONSUME;}
  if(kind==DecorKind.SUPPLY_BASKET&&held.isEmpty()){player.openHandledScreen(this);return ActionResult.CONSUME;}
  if(!held.isEmpty()&&(kind==DecorKind.NOTICE_BOARD?isDocument(held):FoodDisplaySlots.acceptsFood(held))){var source=player.isCreative()?held.copyWithCount(1):held;insertDisplay(source);return ActionResult.CONSUME;}
  if(kind==DecorKind.NOTICE_BOARD&&held.isEmpty()){if(selectedSlot>=0&&(getStack(selectedSlot).isOf(Items.WRITTEN_BOOK)||getStack(selectedSlot).isOf(Items.WRITABLE_BOOK))){bookSlot=selectedSlot;page=0;player.openHandledScreen(this);return ActionResult.CONSUME;}for(int i=0;i<size();i++)if(getStack(i).isOf(Items.WRITTEN_BOOK)||getStack(i).isOf(Items.WRITABLE_BOOK)||getStack(i).isOf(Items.BOOK)){bookSlot=i;page=0;player.openHandledScreen(this);break;}}
  return ActionResult.CONSUME;
 }
 private Inventory documentInventory(){return new Inventory(){public int size(){return 1;}public boolean isEmpty(){return getStack(0).isEmpty();}public ItemStack getStack(int slot){return DecorBlockEntity.this.getStack(bookSlot);}public ItemStack removeStack(int slot,int amount){return DecorBlockEntity.this.removeStack(bookSlot,amount);}public ItemStack removeStack(int slot){return DecorBlockEntity.this.removeStack(bookSlot);}public void setStack(int slot,ItemStack stack){DecorBlockEntity.this.setStack(bookSlot,stack);}public void markDirty(){changed();}public boolean canPlayerUse(PlayerEntity p){return DecorBlockEntity.this.canPlayerUse(p);}public void clear(){removeStack(0);}};}
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
 @Override public int getMaxCountPerStack(){return kind==DecorKind.CHEST||kind==DecorKind.SUPPLY_BASKET?64:1;}
 @Override public boolean canPlayerUse(PlayerEntity player){return world!=null&&world.getBlockEntity(pos)==this&&player.squaredDistanceTo(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<64;}
 @Override public boolean isValid(int slot,ItemStack stack){return switch(kind){case WEAPON_RACK->stack.isIn(DecorBlocks.WEAPONS);case LECTERN->stack.isIn(DecorBlocks.BOOKS);case OFFERING_PEDESTAL,CHEST->true;case FOOD_SHELF,PREPARATION_TABLE,DRYING_RACK,SUPPLY_BASKET->FoodDisplaySlots.acceptsFood(stack);case NOTICE_BOARD->isDocument(stack);default->false;};}
 @Override public void clear(){inventory.clear();page=0;changed();}
 @Override public int[] getAvailableSlots(Direction side){if(kind!=DecorKind.CHEST)return new int[0];int[] slots=new int[27];for(int i=0;i<27;i++)slots[i]=i;return slots;}
 @Override public boolean canInsert(int slot,ItemStack stack,Direction dir){return kind==DecorKind.CHEST;}
 @Override public boolean canExtract(int slot,ItemStack stack,Direction dir){return kind==DecorKind.CHEST;}
 private int pageCount(){var tag=size()==0?null:getStack(bookSlot).getNbt();return tag==null?1:Math.max(1,tag.getList("pages",8).size());}
 @Override public Text getDisplayName(){return Text.translatable("block.zoldrax."+kind.id);}
 @Override public ScreenHandler createMenu(int syncId,PlayerInventory playerInventory,PlayerEntity player){return kind==DecorKind.CHEST?GenericContainerScreenHandler.createGeneric9x3(syncId,playerInventory,this):kind==DecorKind.SUPPLY_BASKET?new Generic3x3ContainerScreenHandler(syncId,playerInventory,this):kind==DecorKind.NOTICE_BOARD?new LecternScreenHandler(syncId,documentInventory(),pages):kind==DecorKind.LECTERN?new LecternScreenHandler(syncId,this,pages):null;}
 @Override public void onOpen(PlayerEntity player){if(kind==DecorKind.CHEST&&world!=null&&!world.isClient&&!player.isSpectator()){boolean wasEmpty=viewers.isEmpty();if(viewers.add(player.getUuid())){sendViewers();if(wasEmpty)world.playSound(null,pos,DecorBlocks.CHEST_OPEN,SoundCategory.BLOCKS,.6f,.85f);}}}
 @Override public void onClose(PlayerEntity player){if(kind==DecorKind.CHEST&&world!=null&&!world.isClient&&viewers.remove(player.getUuid())){sendViewers();if(viewers.isEmpty())world.playSound(null,pos,DecorBlocks.CHEST_CLOSE,SoundCategory.BLOCKS,.5f,.85f);}}
 private void sendViewers(){world.addSyncedBlockEvent(pos,getCachedState().getBlock(),1,viewers.size());}
 public void syncNoticeMaps(){
  if(kind!=DecorKind.NOTICE_BOARD||!(world instanceof ServerWorld sw))return;
  var nearby=sw.getPlayers(p->p.squaredDistanceTo(Vec3d.ofCenter(pos))<4096);mapViewers.keySet().removeIf(id->nearby.stream().noneMatch(p->p.getUuid().equals(id)));
  for(var stack:inventory)if(stack.isOf(Items.FILLED_MAP)){var id=FilledMapItem.getMapId(stack);var map=id==null?null:FilledMapItem.getMapState(id,world);if(map==null)continue;var icons=new ArrayList<net.minecraft.item.map.MapIcon>();map.getIcons().forEach(icons::add);int hash=31*Arrays.hashCode(map.colors)+icons.hashCode();for(var player:nearby){var sent=mapViewers.computeIfAbsent(player.getUuid(),key->new HashMap<>());if(!Objects.equals(sent.put(id,hash),hash))player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.MapUpdateS2CPacket(id,map.scale,map.locked,icons,new net.minecraft.item.map.MapState.UpdateData(0,0,128,128,map.colors.clone())));}}
 }
 public void serverTick(){if(world.getTime()%20==0)syncNoticeMaps();
 if(world.getTime()%20==0&&!viewers.isEmpty()&&world instanceof ServerWorld sw){boolean changed=viewers.removeIf(id->{var p=sw.getServer().getPlayerManager().getPlayer(id);return p==null||!p.isAlive()||!(p.currentScreenHandler instanceof GenericContainerScreenHandler handler)||handler.getInventory()!=this;});if(changed)sendViewers();}}
 public void clientTick(){if(strikeTicks>0)strikeTicks--;previousLid=lid;float target=clientViewers>0?1:0;lid=net.minecraft.util.math.MathHelper.clamp(lid+(target>lid?.12f:-.12f),0,1);}
 @Override public boolean onSyncedBlockEvent(int type,int data){if(type==2){strikeTicks=12;return true;}if(type==1){clientViewers=data;return true;}return super.onSyncedBlockEvent(type,data);}
 public int viewerCount(){return world!=null&&world.isClient?clientViewers:viewers.size();}
 public void changed(){markDirty();syncNoticeMaps();if(world!=null){world.updateListeners(pos,getCachedState(),getCachedState(),Block.NOTIFY_LISTENERS);for(int i=0;i<kind.cells(getCachedState().get(DecorBaseBlock.LENGTH));i++){var cell=pos.add(DecorBlocks.offset(kind,i,getCachedState().get(DecorBaseBlock.FACING)));world.updateComparators(cell,world.getBlockState(cell).getBlock());}}}
 public void dropContents(){if(world==null||world.isClient)return;for(var seat:benchSeats)if(seat!=null)seat.discard();if(seatAnchor!=null){seatAnchor.discard();seatAnchor=null;}ItemScatterer.spawn(world,pos,this);inventory.clear();viewers.clear();}
 @Override protected void writeNbt(NbtCompound n){super.writeNbt(n);Inventories.writeNbt(n,inventory);n.putInt("Page",page);n.putInt("BookSlot",bookSlot);}
 @Override public void readNbt(NbtCompound n){super.readNbt(n);inventory.clear();Inventories.readNbt(n,inventory);bookSlot=Math.max(0,Math.min(Math.max(0,size()-1),n.getInt("BookSlot")));page=Math.max(0,Math.min(n.getInt("Page"),pageCount()-1));if(n.contains("Viewers"))clientViewers=n.getInt("Viewers");}
 @Override public NbtCompound toInitialChunkDataNbt(){var n=createNbt();if(kind==DecorKind.CHEST)n.remove("Items");n.putInt("Viewers",viewerCount());return n;}
 @Override public BlockEntityUpdateS2CPacket toUpdatePacket(){return BlockEntityUpdateS2CPacket.create(this,entity->toInitialChunkDataNbt());}
}
