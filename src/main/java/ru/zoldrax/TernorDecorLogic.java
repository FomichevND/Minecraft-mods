package ru.zoldrax;

import java.util.*;
import net.minecraft.block.*;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.recipe.RecipeType;
import net.minecraft.screen.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.*;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.World;

/** Server-owned state; natural animal care never calls lovePlayer or breed. */
public final class TernorDecorLogic {
 private final DecorBlockEntity e;public int water,burn,fuelTotal,cook,workTicks;private long nextAnimal;private final Map<UUID,Long> animalCooldown=new HashMap<>();
 private boolean boosted;private final PropertyDelegate furnaceProperties=new PropertyDelegate(){public int size(){return 4;}public int get(int i){return switch(i){case 0->burn;case 1->fuelTotal;case 2->cook;default->200;};}public void set(int i,int value){switch(i){case 0->burn=value;case 1->fuelTotal=value;case 2->cook=value;default->{}}}};
 public TernorDecorLogic(DecorBlockEntity entity){e=entity;}
 private static final Set<Item> DISPLAYED_PLANTS=Set.of(Items.DANDELION,Items.POPPY,Items.BLUE_ORCHID,Items.ALLIUM,Items.AZURE_BLUET,Items.RED_TULIP,Items.ORANGE_TULIP,Items.WHITE_TULIP,Items.PINK_TULIP,Items.OXEYE_DAISY,Items.CORNFLOWER,Items.LILY_OF_THE_VALLEY,Items.WITHER_ROSE,Items.TORCHFLOWER,Items.OAK_SAPLING,Items.SPRUCE_SAPLING,Items.BIRCH_SAPLING,Items.JUNGLE_SAPLING,Items.ACACIA_SAPLING,Items.DARK_OAK_SAPLING,Items.CHERRY_SAPLING,Items.MANGROVE_PROPAGULE,Items.FERN,Items.GRASS,Items.DEAD_BUSH,Items.AZALEA,Items.FLOWERING_AZALEA,Items.BROWN_MUSHROOM,Items.RED_MUSHROOM,Items.VINE,Items.GLOW_BERRIES,Items.SWEET_BERRIES);
 public static boolean isPlant(ItemStack stack){return DISPLAYED_PLANTS.contains(stack.getItem());}
 public static boolean isFeed(ItemStack stack){return stack.isOf(Items.WHEAT)||stack.isOf(Items.CARROT)||stack.isOf(Items.POTATO)||stack.isOf(Items.BEETROOT)||stack.isOf(Items.WHEAT_SEEDS)||stack.isOf(Items.BEETROOT_SEEDS)||stack.isOf(Items.PUMPKIN_SEEDS)||stack.isOf(Items.MELON_SEEDS)||stack.isOf(Items.TORCHFLOWER_SEEDS);}
 public boolean accepts(int slot,ItemStack stack){return switch(e.kind){case GARDEN_CANOPY->isPlant(stack);case ANIMAL_FEEDER->isFeed(stack);case STORAGE_SHELF->true;case STONE_FORGE->slot==0||slot==1&&AbstractFurnaceBlockEntity.canUseAsFuel(stack);default->false;};}
 public ActionResult use(PlayerEntity player,Hand hand){var world=e.getWorld();if(world==null||world.isClient)return ActionResult.SUCCESS;var held=player.getStackInHand(hand);
  switch(e.kind){
   case STORAGE_SHELF,ANIMAL_FEEDER->player.openHandledScreen(e);
   case ANIMAL_TROUGH,QUENCHING_TUB->{if(held.isOf(Items.WATER_BUCKET)&&water<4){water=4;if(!player.isCreative())player.setStackInHand(hand,new ItemStack(Items.BUCKET));e.changed();world.playSound(null,e.getPos(),SoundEvents.ITEM_BUCKET_EMPTY,SoundCategory.BLOCKS,.6f,1);}else if(e.kind==DecorKind.QUENCHING_TUB)quench(player,hand);}
   case GARDEN_CANOPY->{if(player.isSneaking()&&held.isEmpty()){for(int i=e.size()-1;i>=0;i--)if(!e.getStack(i).isEmpty()){var stack=e.removeStack(i);if(!player.getInventory().insertStack(stack))player.dropItem(stack,false);break;}}else if(isPlant(held)){var copy=player.isCreative()?held.copyWithCount(1):held;e.insertDisplay(copy);}}
   case STONE_FORGE->{if(player.isSneaking())player.openHandledScreen(e);else player.openHandledScreen(new SimpleNamedScreenHandlerFactory((id,inv,p)->new AnvilScreenHandler(id,inv,ScreenHandlerContext.create(world,e.getPos())){
    @Override protected boolean canUse(BlockState state){return state.isOf(DecorBlocks.BLOCKS.get(DecorKind.STONE_FORGE));}
    @Override protected void onTakeOutput(PlayerEntity user,ItemStack stack){super.onTakeOutput(user,stack);workTicks=40;updateLit(true);e.changed();}
   },Text.translatable("block.zoldrax.ternor_stone_forge")));}
   case BELLOWS->activateBellows();
   default->{}
  }return ActionResult.CONSUME;
 }
 public ScreenHandler menu(int id,PlayerInventory inventory,PlayerEntity player){return switch(e.kind){case STORAGE_SHELF->GenericContainerScreenHandler.createGeneric9x3(id,inventory,e);case ANIMAL_FEEDER->new Generic3x3ContainerScreenHandler(id,inventory,e);case STONE_FORGE->new FurnaceScreenHandler(id,inventory,e,furnaceProperties);default->null;};}
 public boolean activateBellows(){if(e.kind!=DecorKind.BELLOWS||e.getWorld()==null||e.getWorld().isClient)return false;e.getWorld().addSyncedBlockEvent(e.getPos(),e.getCachedState().getBlock(),3,0);workTicks=200;updateLit(true);e.changed();e.getWorld().playSound(null,e.getPos(),SoundEvents.BLOCK_PISTON_EXTEND,SoundCategory.BLOCKS,.35f,.65f);return true;}
 public int addWater(int levels){if((e.kind!=DecorKind.ANIMAL_TROUGH&&e.kind!=DecorKind.QUENCHING_TUB)||levels<=0)return 0;int added=Math.min(4-water,levels);if(added>0){water+=added;e.changed();}return added;}
 private void updateLit(boolean lit){var world=e.getWorld();var state=e.getCachedState();if(state.get(DecorBaseBlock.LIT)==lit)return;if(e.kind.light>0)e.setLit(lit);else{world.setBlockState(e.getPos(),state.with(DecorBaseBlock.LIT,lit),Block.NOTIFY_ALL);for(int i=1;i<e.kind.cells(1);i++){var pos=e.getPos().add(DecorBlocks.offset(e.kind,i,state.get(DecorBaseBlock.FACING)));if(world.getBlockState(pos).isOf(DecorBlocks.PARTS.get(e.kind)))world.setBlockState(pos,world.getBlockState(pos).with(DecorBaseBlock.LIT,lit),Block.NOTIFY_ALL);}}}
 public void tick(){var world=e.getWorld();if(!(world instanceof ServerWorld sw))return;
  if(workTicks>0){workTicks--;if(workTicks%20==0)e.markDirty();}
  if(e.kind==DecorKind.STONE_FORGE){if(world.getTime()%20==0)boosted=hasBellows();smelt();updateLit(burn>0||workTicks>0);}
  if(e.kind==DecorKind.BELLOWS&&world.getTime()%20==0)updateLit(workTicks>0);
  if((e.kind==DecorKind.ANIMAL_FEEDER||e.kind==DecorKind.ANIMAL_TROUGH)&&world.getTime()%40==0&&world.getTime()>=nextAnimal)careForAnimals(sw);
 }
 private DecorBlockEntity nearestForge(){DecorBlockEntity found=null;double best=Double.MAX_VALUE;var w=e.getWorld();for(var p:BlockPos.iterate(e.getPos().add(-3,-2,-3),e.getPos().add(3,2,3))){if(!w.isChunkLoaded(p))continue;if(w.getBlockEntity(p) instanceof DecorBlockEntity f&&f.kind==DecorKind.STONE_FORGE){double d=p.getSquaredDistance(e.getPos());if(d<best){best=d;found=f;}}}return found;}
 private boolean hasBellows(){var w=e.getWorld();for(var p:BlockPos.iterate(e.getPos().add(-3,-2,-3),e.getPos().add(3,2,3))){if(!w.isChunkLoaded(p))continue;if(w.getBlockEntity(p) instanceof DecorBlockEntity b&&b.kind==DecorKind.BELLOWS&&b.ternor.workTicks>0&&b.ternor.nearestForge()==e)return true;}return false;}
 private void smelt(){var w=e.getWorld();var recipe=w.getRecipeManager().getFirstMatch(RecipeType.SMELTING,e,w);ItemStack result=recipe.isEmpty()?ItemStack.EMPTY:recipe.get().craft(e,w.getRegistryManager());if(result.isEmpty()&&isMetal(e.getStack(0))&&(!e.getStack(0).hasNbt()||e.getStack(0).getNbt().getLong("TernorHotUntil")<=w.getTime()))result=e.getStack(0).copyWithCount(1);var output=e.getStack(2);boolean fits=!result.isEmpty()&&(output.isEmpty()||(ItemStack.canCombine(output,result)||isMetal(result)&&output.isOf(result.getItem())&&output.hasNbt()&&output.getNbt().contains("TernorHotUntil"))&&output.getCount()+result.getCount()<=output.getMaxCount());
  if(burn>0){burn--;e.markDirty();}
  if(fits&&burn==0){var fuel=e.getStack(1);int time=AbstractFurnaceBlockEntity.createFuelTimeMap().getOrDefault(fuel.getItem(),0);if(time>0){burn=fuelTotal=time;Item remainder=fuel.getItem().getRecipeRemainder();fuel.decrement(1);if(fuel.isEmpty())e.setStack(1,remainder==null?ItemStack.EMPTY:new ItemStack(remainder));e.changed();}}
  if(fits&&burn>0){cook+=boosted?2:1;if(cook>=200){cook-=200;e.getStack(0).decrement(1);if(output.isEmpty()){if(isMetal(result))result.getOrCreateNbt().putLong("TernorHotUntil",w.getTime()+600);e.setStack(2,result.copy());}else{output.increment(result.getCount());if(isMetal(output))output.getOrCreateNbt().putLong("TernorHotUntil",w.getTime()+600);}e.changed();}else e.markDirty();}else if(cook!=0){cook=0;e.markDirty();}
 }
 public static boolean isMetal(ItemStack stack){return stack.isOf(Items.IRON_INGOT)||stack.isOf(Items.GOLD_INGOT)||stack.isOf(Items.COPPER_INGOT);}
 public boolean quench(PlayerEntity player,Hand hand){if(!(e.getWorld() instanceof ServerWorld world)||water==0)return false;var held=player.getStackInHand(hand);var inventory=new net.minecraft.inventory.SimpleInventory(held);var recipe=world.getRecipeManager().getFirstMatch(QuenchingRecipe.TYPE,inventory,world);if(recipe.isEmpty()||water<recipe.get().water)return false;var output=recipe.get().craft(inventory,world.getRegistryManager());if(output.isEmpty())return false;held.decrement(1);water-=recipe.get().water;if(held.isEmpty())player.setStackInHand(hand,output);else if(!player.getInventory().insertStack(output))player.dropItem(output,false);e.changed();world.addSyncedBlockEvent(e.getPos(),e.getCachedState().getBlock(),4,0);world.spawnParticles(net.minecraft.particle.ParticleTypes.CLOUD,e.getPos().getX()+.5,e.getPos().getY()+.7,e.getPos().getZ()+.5,12,.2,.15,.2,.025);world.playSound(null,e.getPos(),SoundEvents.BLOCK_FIRE_EXTINGUISH,SoundCategory.BLOCKS,.5f,1.4f);return true;}
 public boolean careForAnimals(ServerWorld w){if(w.getTime()<nextAnimal)return false;if(e.kind==DecorKind.ANIMAL_TROUGH&&water==0||e.kind==DecorKind.ANIMAL_FEEDER&&e.isEmpty())return false;
  for(var animal:w.getEntitiesByClass(AnimalEntity.class,new Box(e.getPos()).expand(6),a->a instanceof CowEntity||a instanceof SheepEntity||a instanceof PigEntity)){
   if(animalCooldown.getOrDefault(animal.getUuid(),0L)>w.getTime())continue;int slot=-1;if(e.kind==DecorKind.ANIMAL_FEEDER){for(int i=0;i<e.size();i++)if(animal.isBreedingItem(e.getStack(i))){slot=i;break;}if(slot<0)continue;}
   var target=DecorBlocks.point(e.getPos(),e.getCachedState().get(DecorBaseBlock.FACING),1,0,1.4);if(animal.squaredDistanceTo(target)>3){if(!animal.isLeashed()&&!animal.hasVehicle())animal.getNavigation().startMovingTo(target.x,target.y,target.z,1);continue;}
   if(e.kind==DecorKind.ANIMAL_FEEDER){e.removeStack(slot,1);animal.heal(2);if(animal.isBaby())animal.growUp(20,true);w.playSound(null,e.getPos(),SoundEvents.ENTITY_GENERIC_EAT,SoundCategory.BLOCKS,.35f,1);}else{water--;w.spawnParticles(net.minecraft.particle.ParticleTypes.SPLASH,target.x,target.y+.3,target.z,5,.2,.1,.2,.01);w.playSound(null,e.getPos(),SoundEvents.ENTITY_GENERIC_DRINK,SoundCategory.BLOCKS,.35f,1);}
   animalCooldown.put(animal.getUuid(),w.getTime()+6000);nextAnimal=w.getTime()+600;e.changed();return true;
  }return false;
 }
 public void write(NbtCompound n){n.putInt("TernorWater",water);n.putInt("TernorBurn",burn);n.putInt("TernorFuel",fuelTotal);n.putInt("TernorCook",cook);n.putInt("TernorWork",workTicks);n.putLong("NextAnimal",nextAnimal);var list=new NbtList();animalCooldown.forEach((id,time)->{var row=new NbtCompound();row.putUuid("Id",id);row.putLong("Until",time);list.add(row);});n.put("AnimalCooldowns",list);}
 public void read(NbtCompound n){water=MathHelper.clamp(n.getInt("TernorWater"),0,4);burn=Math.max(0,n.getInt("TernorBurn"));fuelTotal=Math.max(0,n.getInt("TernorFuel"));cook=MathHelper.clamp(n.getInt("TernorCook"),0,199);workTicks=Math.max(0,n.getInt("TernorWork"));nextAnimal=n.getLong("NextAnimal");animalCooldown.clear();for(var tag:n.getList("AnimalCooldowns",10)){var row=(NbtCompound)tag;if(row.containsUuid("Id"))animalCooldown.put(row.getUuid("Id"),row.getLong("Until"));}}
 public static void particles(DecorKind kind,BlockState state,World world,BlockPos pos,net.minecraft.util.math.random.Random random){if(state.get(DecorBaseBlock.PART)!=0||kind!=DecorKind.STONE_FORGE||!state.get(DecorBaseBlock.LIT))return;var smoke=DecorBlocks.point(pos,state.get(DecorBaseBlock.FACING),1,3,.9);world.addParticle(net.minecraft.particle.ParticleTypes.SMOKE,smoke.x,smoke.y,smoke.z,0,.025,0);if(random.nextInt(5)==0)world.playSound(pos.getX()+1,pos.getY()+.7,pos.getZ()+1,SoundEvents.BLOCK_FURNACE_FIRE_CRACKLE,SoundCategory.BLOCKS,.25f,1,false);var fire=DecorBlocks.point(pos,state.get(DecorBaseBlock.FACING),1,.7,1.5);world.addParticle(net.minecraft.particle.ParticleTypes.SMALL_FLAME,fire.x,fire.y,fire.z,0,.01,0);}
}
