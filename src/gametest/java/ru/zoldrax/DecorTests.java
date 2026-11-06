package ru.zoldrax;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.*;
import net.minecraft.block.entity.*;
import net.minecraft.entity.*;
import net.minecraft.entity.player.*;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.screen.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.*;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;

public class DecorTests implements FabricGameTest {
 private void check(boolean value,String message){if(!value)throw new IllegalStateException(message);}
 private BlockPos root(TestContext c){return c.getAbsolutePos(new BlockPos(6,3,6));}
 private PlayerEntity player(TestContext c,BlockPos root){var p=c.createMockSurvivalPlayer();p.setPosition(root.getX()+.5,root.getY(),root.getZ()+4);p.setYaw(180);return p;}
 private DecorBlockEntity place(TestContext c,DecorKind kind,BlockPos root,Direction facing){
  var w=c.getWorld();var p=player(c,root);p.setYaw(facing.getOpposite().asRotation());
  if(kind.hanging())w.setBlockState(root.up(),Blocks.STONE.getDefaultState());
  var stack=new ItemStack(DecorBlocks.BLOCKS.get(kind),2);p.setStackInHand(Hand.MAIN_HAND,stack);
  var support=kind.hanging()?root.up():root.down();var side=kind.hanging()?Direction.DOWN:Direction.UP;
  var hit=new BlockHitResult(Vec3d.ofCenter(support).add(0,kind.hanging()?-.5:.5,0),side,support,false);
  check(((BlockItem)stack.getItem()).place(new ItemPlacementContext(p,Hand.MAIN_HAND,stack,hit)).isAccepted(),"Cannot place "+kind+" "+facing+" root="+w.getBlockState(root)+" floor="+w.getBlockState(root.down())+" hit="+new ItemPlacementContext(p,Hand.MAIN_HAND,stack,hit).getBlockPos()+" expected="+root);
  check(stack.getCount()==1,"Placement must consume exactly one item");
  check(w.getBlockState(root).get(DecorBaseBlock.FACING)==facing,"Wrong rotation");
  var entity=(DecorBlockEntity)w.getBlockEntity(root);check(entity!=null,"Missing root entity");return entity;
 }
 private void clear(TestContext c,BlockPos root){c.getWorld().removeBlock(root,false);}
 @GameTest(templateName="zoldrax_test:decor_arena",tickLimit=100)
 public void allEightPlaceRotateAndRemove(TestContext c){
  var w=c.getWorld();
  for(var kind:DecorKind.values())for(var facing:Direction.Type.HORIZONTAL){
   var r=kind.hanging()?root(c).up(5):root(c);
   place(c,kind,r,facing);var state=w.getBlockState(r);
   for(int i=1;i<kind.cells(1);i++){var p=r.add(DecorBlocks.offset(kind,i,facing));check(w.getBlockState(p).getBlock()==DecorBlocks.PARTS.get(kind),"Missing companion");check(w.getBlockEntity(p)==null,"No duplicate inventory");}
   clear(c,r);check(w.getBlockEntity(r)==null,"Removed entity retained");
   for(int i=1;i<kind.cells(1);i++)check(w.isAir(r.add(DecorBlocks.offset(kind,i,facing))),"Orphan after removal");
   w.removeBlock(r.up(),false);
  }c.complete();
 }
 @GameTest(templateName="zoldrax_test:decor_arena")
 public void obstructionDoesNotConsumeItem(TestContext c){
  var r=root(c);var w=c.getWorld();w.setBlockState(r.up(),Blocks.STONE.getDefaultState());var p=player(c,r);var stack=new ItemStack(DecorBlocks.BLOCKS.get(DecorKind.THRONE),2);p.setStackInHand(Hand.MAIN_HAND,stack);
  var hit=new BlockHitResult(Vec3d.ofCenter(r.down()).add(0,.5,0),Direction.UP,r.down(),false);
  check(!((BlockItem)stack.getItem()).place(new ItemPlacementContext(p,Hand.MAIN_HAND,stack,hit)).isAccepted(),"Occupied height accepted");check(stack.getCount()==2&&w.isAir(r),"Failed placement changed world/inventory count="+stack.getCount()+" state="+w.getBlockState(r));c.complete();
 }
 @GameTest(templateName="zoldrax_test:decor_arena")
 public void lightsToggleEveryCellAndPersist(TestContext c){
  var w=c.getWorld();for(var kind:new DecorKind[]{DecorKind.BRAZIER,DecorKind.SKULL_LAMP,DecorKind.CHAIN_LANTERN}){
   var r=kind.hanging()?root(c).up(5):root(c);var e=place(c,kind,r,Direction.SOUTH);var p=player(c,r);e.use(p,Hand.MAIN_HAND);
   for(int i=0;i<kind.cells(1);i++){var s=w.getBlockState(r.add(DecorBlocks.offset(kind,i,Direction.SOUTH)));check(!s.get(DecorBaseBlock.LIT)&&s.getLuminance()==0,"Toggle off incomplete");}
   e.use(p,Hand.MAIN_HAND);var emitter=w.getBlockState(r.add(DecorBlocks.offset(kind,kind.emitter(1),Direction.SOUTH)));check(emitter.getLuminance()==kind.light,"Wrong light level");
   var saved=NbtHelper.fromBlockState(w.getBlockState(r));check(NbtHelper.toBlockState(w.getRegistryManager().get(net.minecraft.registry.RegistryKeys.BLOCK).getReadOnlyWrapper(),saved).get(DecorBaseBlock.LIT),"Light state does not serialize");clear(c,r);w.removeBlock(r.up(),false);
  }c.complete();
 }
 @GameTest(templateName="zoldrax_test:decor_arena")
 public void chainLengthRespectsObstructions(TestContext c){var r=root(c).up(5);var w=c.getWorld();var e=place(c,DecorKind.CHAIN_LANTERN,r,Direction.WEST);var p=player(c,r);w.setBlockState(r.down(3),Blocks.STONE.getDefaultState());check(!e.changeLength(p),"Length ignored obstruction");check(w.getBlockState(r).get(DecorBaseBlock.LENGTH)==1,"Failed extension changed state");w.removeBlock(r.down(3),false);check(e.changeLength(p),"Clear extension rejected");check(w.getBlockState(r).get(DecorBaseBlock.LENGTH)==2&&!w.isAir(r.down(3)),"Missing extended cell");check(e.changeLength(p)&&w.isAir(r.down(3)),"Contraction leaves orphan");c.complete();}
 @GameTest(templateName="zoldrax_test:decor_arena")
 public void rackTransactionsAndNbt(TestContext c){var r=root(c);var e=place(c,DecorKind.WEAPON_RACK,r,Direction.SOUTH);var p=player(c,r);p.setStackInHand(Hand.MAIN_HAND,new ItemStack(Items.DIRT,8));e.use(p,Hand.MAIN_HAND);check(e.isEmpty(),"Rack accepts dirt");for(var item:new Item[]{Items.IRON_SWORD,Items.IRON_AXE,Items.TRIDENT}){var stack=new ItemStack(item);stack.setDamage(7);stack.setCustomName(Text.literal("Saved weapon"));p.setStackInHand(Hand.MAIN_HAND,stack);e.use(p,Hand.MAIN_HAND);check(stack.isEmpty(),"Survival weapon not consumed");}p.setStackInHand(Hand.MAIN_HAND,new ItemStack(Items.DIAMOND_SWORD));e.use(p,Hand.MAIN_HAND);check(p.getMainHandStack().isOf(Items.DIAMOND_SWORD),"Full rack consumed item");var copy=new DecorBlockEntity(r,e.getCachedState());copy.readNbt(e.createNbt());for(int i=0;i<3;i++)check(copy.getStack(i).getDamage()==7&&copy.getStack(i).getName().getString().equals("Saved weapon"),"Weapon NBT lost");p.setStackInHand(Hand.MAIN_HAND,ItemStack.EMPTY);e.use(p,Hand.MAIN_HAND);check(e.getStack(2).isEmpty()&&p.getInventory().count(Items.TRIDENT)==1,"Retrieval duplicated/lost weapon");c.complete();}
 @GameTest(templateName="zoldrax_test:decor_arena")
 public void chestHas27SlotsAndTwoViewers(TestContext c){var r=root(c);var e=place(c,DecorKind.CHEST,r,Direction.SOUTH);var a=c.createMockCreativeServerPlayerInWorld();var b=c.createMockCreativeServerPlayerInWorld();a.setPosition(Vec3d.ofCenter(r).add(0,0,3));b.setPosition(a.getPos());a.openHandledScreen(e);b.openHandledScreen(e);check(a.currentScreenHandler instanceof GenericContainerScreenHandler&&e.viewerCount()==2,"Two viewers not tracked");for(int i=0;i<27;i++)e.setStack(i,new ItemStack(Items.EMERALD,i+1));var copy=new DecorBlockEntity(r,e.getCachedState());copy.readNbt(e.createNbt());check(copy.size()==27&&copy.getStack(26).getCount()==27,"Chest inventory failed reload");a.closeHandledScreen();check(e.viewerCount()==1,"First close closes other viewer");b.closeHandledScreen();check(e.viewerCount()==0,"Last viewer retained");a.discard();b.discard();c.complete();}
 @GameTest(templateName="zoldrax_test:decor_arena",tickLimit=100)
 public void realHoppersInsertAndExtract(TestContext c){var r=root(c);var w=c.getWorld();w.setBlockState(r.down(),Blocks.HOPPER.getDefaultState());var chest=place(c,DecorKind.CHEST,r,Direction.SOUTH);w.setBlockState(r.up(2),Blocks.HOPPER.getDefaultState());var upper=(HopperBlockEntity)w.getBlockEntity(r.up(2));var lower=(HopperBlockEntity)w.getBlockEntity(r.down());upper.setStack(0,new ItemStack(Items.DIAMOND,4));
  c.waitAndRun(60,()->{check(lower.count(Items.DIAMOND)>0,"Hopper cannot extract");check(upper.count(Items.DIAMOND)<4,"Hopper cannot insert via upper companion");check(upper.count(Items.DIAMOND)+lower.count(Items.DIAMOND)+chest.count(Items.DIAMOND)==4,"Hoppers duplicated/lost items");c.complete();});}
 @GameTest(templateName="zoldrax_test:decor_arena")
 public void lecternReadsPagesAndReturnsBook(TestContext c){var r=root(c);var e=place(c,DecorKind.LECTERN,r,Direction.NORTH);var p=c.createMockCreativeServerPlayerInWorld();p.changeGameMode(net.minecraft.world.GameMode.SURVIVAL);p.setPosition(Vec3d.ofCenter(r).add(0,0,3));var book=new ItemStack(Items.WRITTEN_BOOK);var n=book.getOrCreateNbt();n.putString("title","Ritual");n.putString("author","Test");var pages=new NbtList();pages.add(NbtString.of("{\"text\":\"First\"}"));pages.add(NbtString.of("{\"text\":\"Second\"}"));n.put("pages",pages);p.setStackInHand(Hand.MAIN_HAND,book);e.use(p,Hand.MAIN_HAND);e.use(p,Hand.MAIN_HAND);check(p.currentScreenHandler instanceof LecternScreenHandler,"No readable book GUI");p.currentScreenHandler.onButtonClick(p,2);check(e.createNbt().getInt("Page")==1,"Page not saved");p.closeHandledScreen();p.setSneaking(true);p.setStackInHand(Hand.MAIN_HAND,new ItemStack(Items.STICK,3));e.use(p,Hand.MAIN_HAND);check(e.isEmpty()&&p.getInventory().count(Items.WRITTEN_BOOK)==1&&p.getMainHandStack().getCount()==3,"Taking book lost item/overwrote hand");p.discard();c.complete();}
 @GameTest(templateName="zoldrax_test:decor_arena")
 public void pedestalConsumesOneAndNeverReplaces(TestContext c){var r=root(c);var e=place(c,DecorKind.OFFERING_PEDESTAL,r,Direction.EAST);var p=player(c,r);p.setStackInHand(Hand.MAIN_HAND,new ItemStack(Items.DIAMOND,7));e.use(p,Hand.MAIN_HAND);check(e.getStack(0).getCount()==1&&p.getMainHandStack().getCount()==6,"Pedestal count wrong");p.setStackInHand(Hand.MAIN_HAND,new ItemStack(Items.APPLE,4));e.use(p,Hand.MAIN_HAND);check(e.getStack(0).isOf(Items.DIAMOND)&&p.getMainHandStack().getCount()==4,"Replaced occupied offering");var copy=new DecorBlockEntity(r,e.getCachedState());copy.readNbt(e.createNbt());check(copy.getStack(0).isOf(Items.DIAMOND),"Offering lost on reload");p.setSneaking(true);e.use(p,Hand.MAIN_HAND);check(e.isEmpty()&&p.getInventory().count(Items.DIAMOND)==1,"Offering return wrong");c.complete();}
 @GameTest(templateName="zoldrax_test:decor_arena",tickLimit=100)
 public void throneExclusiveAndBreakDismounts(TestContext c){var r=root(c);var w=c.getWorld();var e=place(c,DecorKind.THRONE,r,Direction.SOUTH);var a=c.createMockCreativeServerPlayerInWorld();var b=c.createMockCreativeServerPlayerInWorld();a.setPosition(Vec3d.ofCenter(r).add(0,0,3));b.setPosition(a.getPos());e.use(a,Hand.MAIN_HAND);check(a.getVehicle() instanceof DecorSeatEntity,"Cannot sit");e.use(b,Hand.MAIN_HAND);check(!b.hasVehicle(),"Two riders on same throne");e.use(a,Hand.MAIN_HAND);check(!a.hasVehicle(),"Repeat click did not dismount");e.use(b,Hand.MAIN_HAND);check(b.hasVehicle(),"Seat not reusable");w.breakBlock(r.east().up(),true);check(!b.hasVehicle()&&w.isAir(r),"Breaking companion retains seat/root");a.discard();b.discard();c.complete();}
 @GameTest(templateName="zoldrax_test:decor_arena",tickLimit=100)
 public void breakAnyCellDropsContentsExactlyOnce(TestContext c){var r=root(c);var w=c.getWorld();var e=place(c,DecorKind.WEAPON_RACK,r,Direction.SOUTH);e.setStack(0,new ItemStack(Items.DIAMOND_SWORD));e.setStack(1,new ItemStack(Items.IRON_AXE));w.breakBlock(r.east().up(),true);c.waitAndRun(2,()->{var drops=w.getEntitiesByClass(ItemEntity.class,new Box(r).expand(4),item->true);check(drops.stream().mapToInt(x->x.getStack().isOf(DecorBlocks.BLOCKS.get(DecorKind.WEAPON_RACK).asItem())?x.getStack().getCount():0).sum()==1,"Core drop duplicated/lost");check(drops.stream().mapToInt(x->x.getStack().isOf(Items.DIAMOND_SWORD)||x.getStack().isOf(Items.IRON_AXE)?x.getStack().getCount():0).sum()==2,"Contents duplicated/lost");c.complete();});}
 @GameTest(templateName="zoldrax_test:decor_arena",tickLimit=100)
 public void ceilingRemovalRemovesEntireLantern(TestContext c){var r=root(c).up(5);var w=c.getWorld();place(c,DecorKind.CHAIN_LANTERN,r,Direction.NORTH);w.removeBlock(r.up(),false);c.waitAndRun(4,()->{check(w.isAir(r)&&w.isAir(r.down())&&w.isAir(r.down(2)),"Unsupported lantern remains");c.complete();});}
 @GameTest(templateName="zoldrax_test:decor_arena",tickLimit=100)
 public void fullInventoryRetrievalDropsOneOffering(TestContext c){var r=root(c);var w=c.getWorld();var e=place(c,DecorKind.OFFERING_PEDESTAL,r,Direction.SOUTH);var p=c.createMockCreativeServerPlayerInWorld();p.changeGameMode(net.minecraft.world.GameMode.SURVIVAL);p.setPosition(Vec3d.ofCenter(r).add(0,0,3));for(int i=0;i<p.getInventory().size();i++)p.getInventory().setStack(i,new ItemStack(Items.COBBLESTONE,64));e.setStack(0,new ItemStack(Items.EMERALD));p.setSneaking(true);e.use(p,Hand.MAIN_HAND);check(e.isEmpty(),"Offering remains after return");c.waitAndRun(2,()->{check(w.getEntitiesByClass(ItemEntity.class,new Box(r).expand(8),x->x.getStack().isOf(Items.EMERALD)).stream().mapToInt(x->x.getStack().getCount()).sum()==1,"Full inventory must drop exactly one offering");p.discard();c.complete();});}
 @GameTest(templateName="zoldrax_test:decor_arena")
 public void dataPackTagsAcceptCustomWeaponsAndScrolls(TestContext c){var r=root(c);var rack=place(c,DecorKind.WEAPON_RACK,r,Direction.SOUTH);var p=player(c,r);p.setStackInHand(Hand.MAIN_HAND,new ItemStack(Items.IRON_PICKAXE));rack.use(p,Hand.MAIN_HAND);check(rack.getStack(0).isOf(Items.IRON_PICKAXE),"Custom weapon tag ignored");clear(c,r);var desk=place(c,DecorKind.LECTERN,r,Direction.SOUTH);p.setStackInHand(Hand.MAIN_HAND,new ItemStack(Items.PAPER,3));desk.use(p,Hand.MAIN_HAND);check(desk.getStack(0).isOf(Items.PAPER)&&p.getMainHandStack().getCount()==2,"Tagged scroll rejected or count lost");c.complete();}
 @GameTest(templateName="zoldrax_test:decor_arena")
 public void allEightRecipesRegistered(TestContext c){for(var kind:DecorKind.values()){var recipe=c.getWorld().getRecipeManager().get(new Identifier("zoldrax",kind.id));check(recipe.isPresent(),"Missing recipe "+kind);check(recipe.get().getOutput(c.getWorld().getRegistryManager()).isOf(DecorBlocks.BLOCKS.get(kind).asItem()),"Wrong recipe output");}c.complete();}

}
