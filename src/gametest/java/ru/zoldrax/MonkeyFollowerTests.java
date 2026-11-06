package ru.zoldrax;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.*;
import net.minecraft.block.*;
import net.minecraft.block.entity.*;
import net.minecraft.entity.*;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.registry.*;
import net.minecraft.structure.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameRules;
import net.minecraft.world.biome.source.*;
import net.minecraft.world.gen.chunk.*;
import net.minecraft.world.gen.noise.NoiseConfig;
public class MonkeyFollowerTests implements FabricGameTest {
 private void check(boolean b,String message){if(!b)throw new IllegalStateException(message);}
 private MonkeyFollowerEntity worker(TestContext c,boolean herald,boolean ai){
  var root=new BlockPos(3,2,3);for(int x=0;x<12;x++)for(int z=0;z<12;z++)c.setBlockState(new BlockPos(x,1,z),Blocks.STONE);
  c.setBlockState(root,Zoldrax.MONKEY_BLOCK);((MonkeyEntity)c.getWorld().getBlockEntity(c.getAbsolutePos(root))).stage=2;
  var entity=(herald?Zoldrax.BANANA_HERALD:Zoldrax.TEMPLE_GATHERER).create(c.getWorld());var p=c.getAbsolutePos(root.add(6,0,1));entity.refreshPositionAndAngles(p.getX()+.5,p.getY(),p.getZ()+.5,0,0);entity.setAiDisabled(!ai);c.getWorld().spawnEntity(entity);return entity;
 }
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=100,batchId="monkey_workers")
 public void neutralWorkersEquipmentAndPersistentCargo(TestContext c){
  var npc=worker(c,false,false);npc.cargo.addStack(new ItemStack(Items.CARROT,13));
  c.waitAndRun(5,()->{check(npc.getTarget()==null&&npc.isPersistent(),"Workers must be neutral and persistent");check(npc.getMainHandStack().isEmpty(),"Gatherer must keep empty hands");var n=new NbtCompound();npc.writeCustomDataToNbt(n);var copy=Zoldrax.TEMPLE_GATHERER.create(c.getWorld());copy.readCustomDataFromNbt(n);check(copy.cargo.count(Items.CARROT)==13,"Carried food must survive reload without duplication");check(Zoldrax.BANANA_HERALD.getMaxTrackDistance()==10,"Long-range tracking must be preserved");npc.discard();c.complete();});
 }
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=1200,batchId="monkey_delivery")
 public void heraldPhysicallyPicksUpAndDeliversFood(TestContext c){
  // The 12x12 navigation floor extends beyond the 1x1 empty GameTest template.
  // Its neighbouring chunks must tick too, regardless of the test's grid position.
  var min=c.getAbsolutePos(BlockPos.ORIGIN);var max=c.getAbsolutePos(new BlockPos(12,0,12));
  for(int x=min.getX()>>4;x<=max.getX()>>4;x++)for(int z=min.getZ()>>4;z<=max.getZ()>>4;z++){c.getWorld().setChunkForced(x,z,true);c.getWorld().getChunk(x,z);}
  var npc=worker(c,true,true);var p=c.getAbsolutePos(new BlockPos(4,2,6));c.getWorld().setBlockState(p,Blocks.CHEST.getDefaultState());var chest=(ChestBlockEntity)c.getWorld().getBlockEntity(p);
  var fullPos=c.getAbsolutePos(new BlockPos(6,2,6));c.getWorld().setBlockState(fullPos,Blocks.CHEST.getDefaultState());var full=(ChestBlockEntity)c.getWorld().getBlockEntity(fullPos);for(int i=0;i<full.size();i++)full.setStack(i,new ItemStack(Items.STONE,64));
  var e=new ItemEntity(c.getWorld(),npc.getX(),npc.getY(),npc.getZ()+1,new ItemStack(Items.BREAD,11));e.setPickupDelay(0);c.getWorld().spawnEntity(e);
  c.addInstantFinalTask(()->{
   c.assertTrue(npc.age>0&&e.isRemoved(),"Herald must really collect dropped food");
   c.assertTrue(full.count(Items.STONE)==1728,"Full chest must remain intact");
   c.assertTrue(chest.count(Items.BREAD)==11&&npc.cargo.isEmpty(),"All eleven food items must physically reach the altar chest once");
   npc.discard();for(int x=min.getX()>>4;x<=max.getX()>>4;x++)for(int z=min.getZ()>>4;z<=max.getZ()>>4;z++)c.getWorld().setChunkForced(x,z,false);
  });
 }
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=100,batchId="monkey_workers")
 public void foodSearchWorksWithOddAndEvenAiPhases(TestContext c){
  var npc=worker(c,true,false);var item=new ItemEntity(c.getWorld(),npc.getX(),npc.getY(),npc.getZ()+1,new ItemStack(Items.BREAD,11));item.setPickupDelay(0);c.getWorld().spawnEntity(item);
  npc.age=1;var odd=new MonkeyFoodGoal(npc);check(odd.canStart(),"Food scan must start with an odd AI phase");check(!odd.canStart(),"Repeated scans must still respect the cooldown");
  npc.age=2;check(new MonkeyFoodGoal(npc).canStart(),"Food scan must start with an even AI phase");
  item.discard();npc.discard();c.complete();
 }
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=100,batchId="monkey_farming")
 public void harvestReplantsAndHonoursMobGriefing(TestContext c){
  var npc=worker(c,false,false);var pos=c.getAbsolutePos(new BlockPos(8,2,7));c.getWorld().setBlockState(pos.down(),Blocks.FARMLAND.getDefaultState());c.getWorld().setBlockState(pos,((CropBlock)Blocks.CARROTS).withAge(7));var goal=new MonkeyFoodGoal(npc);
  c.getWorld().getGameRules().get(GameRules.DO_MOB_GRIEFING).set(false,c.getWorld().getServer());goal.harvest(pos);check(npc.cargo.isEmpty()&&c.getWorld().getBlockState(pos).get(CarrotsBlock.AGE)==7,"Protected crops must remain intact");
  c.getWorld().getGameRules().get(GameRules.DO_MOB_GRIEFING).set(true,c.getWorld().getServer());goal.harvest(pos);check(c.getWorld().getBlockState(pos).get(CarrotsBlock.AGE)==0&&npc.cargo.count(Items.CARROT)>0,"Harvest must consume a seed/carrot to replant and carry real drops");npc.discard();c.complete();
 }
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=100,batchId="monkey_workers")
 public void barterConsumesFoodAndHasPersistentCooldown(TestContext c){
  var npc=worker(c,false,false);var p=c.createMockCreativeServerPlayerInWorld();p.setStackInHand(Hand.MAIN_HAND,new ItemStack(Items.APPLE,16));npc.interactMob(p,Hand.MAIN_HAND);
  check(p.getMainHandStack().getCount()==8&&npc.cargo.count(Items.APPLE)==8&&p.getInventory().count(Items.GOLD_NUGGET)==1,"Trade must exchange eight real food items for one nugget");var n=new NbtCompound();npc.writeCustomDataToNbt(n);npc.readCustomDataFromNbt(n);npc.interactMob(p,Hand.MAIN_HAND);check(p.getMainHandStack().getCount()==8&&p.getInventory().count(Items.GOLD_NUGGET)==1,"Reloading must not bypass trade cooldown");npc.discard();c.getWorld().getServer().getPlayerManager().remove(p);c.complete();
 }
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=100,batchId="monkey_workers")
 public void legacyMacheteRemovedWithoutLosingCargoOrOtherEquipment(TestContext c){
  var npc=worker(c,false,false);npc.cargo.addStack(new ItemStack(Items.BREAD,17));
  npc.equipStack(EquipmentSlot.MAINHAND,new ItemStack(Zoldrax.HARVEST_MACHETE));
  npc.equipStack(EquipmentSlot.OFFHAND,new ItemStack(Zoldrax.HARVEST_MACHETE));
  npc.equipStack(EquipmentSlot.HEAD,new ItemStack(Items.LEATHER_HELMET));
  var n=new NbtCompound();npc.writeCustomDataToNbt(n);npc.readCustomDataFromNbt(n);
  check(npc.getMainHandStack().isEmpty()&&npc.getOffHandStack().isEmpty(),"Old equipment must be removed immediately on load");
  check(npc.cargo.count(Items.BREAD)==17&&npc.getEquippedStack(EquipmentSlot.HEAD).isOf(Items.LEATHER_HELMET),"Migration must preserve cargo and unrelated gear");
  npc.equipStack(EquipmentSlot.MAINHAND,new ItemStack(Zoldrax.HARVEST_MACHETE));
  c.waitAndRun(60,()->{check(npc.age>0&&npc.getMainHandStack().isEmpty(),"Already loaded workers must also retire the machete: age="+npc.age+", alive="+npc.isAlive()+", removed="+npc.isRemoved()+", loaded="+c.getWorld().isChunkLoaded(npc.getBlockPos()));npc.discard();c.complete();});
 }
 private void site(TestContext c,String name,BlockPos root,BlockRotation rotation,int heralds,int width,int length,BlockPos altar){
  var w=c.getWorld();var t=w.getStructureTemplateManager().getTemplate(new Identifier("zoldrax",name)).orElseThrow();check(t.getSize().getX()==width&&t.getSize().getZ()==length,"Reference site dimensions must match");
  var data=new StructurePlacementData().setRotation(rotation).setIgnoreEntities(false).setInitializeMobs(true);var bounds=t.calculateBoundingBox(data,root);
  for(int x=bounds.getMinX()>>4;x<=bounds.getMaxX()>>4;x++)for(int z=bounds.getMinZ()>>4;z<=bounds.getMaxZ()>>4;z++){w.setChunkForced(x,z,true);w.getChunk(x,z);}
  check(t.place(w,root,root,data,Random.create(6),2),"Structure must place");var anchor=StructureTemplate.transform(data,altar).add(root);check(w.getBlockEntity(anchor) instanceof MonkeyEntity m&&m.stage==2,"A completed working Monkey King monument must be present");
  check(w.getBlockState(anchor).get(MonkeyBlock.FACING)==rotation.rotate(Direction.SOUTH),"Monument must rotate with settlement");
  for(var p:BlockPos.iterate(bounds.getMinX(),bounds.getMinY(),bounds.getMinZ(),bounds.getMaxX(),bounds.getMaxY(),bounds.getMaxZ())){var s=w.getBlockState(p);if(s.getBlock() instanceof LanternBlock||s.getBlock() instanceof LadderBlock||s.getBlock() instanceof WallBannerBlock)check(s.canPlaceAt(w,p),"Unsupported detail "+p+" "+s);}
  c.waitAndRun(65,()->{var entities=w.getEntitiesByClass(MonkeyFollowerEntity.class,new Box(bounds.getMinX(),bounds.getMinY(),bounds.getMinZ(),bounds.getMaxX()+1,bounds.getMaxY()+1,bounds.getMaxZ()+1),e->true);check(entities.size()==5,"Each court must have exactly five residents: "+entities.size());check(entities.stream().filter(MonkeyFollowerEntity::isHerald).count()==heralds,"Resident roles must match the reference");for(var e:entities){check(e.isPersistent(),"Residents must not despawn");check(!w.getBlockCollisions(e,e.getBoundingBox().contract(.001)).iterator().hasNext(),"Resident intersects blocks: "+e.getPos());e.discard();}for(int x=bounds.getMinX()>>4;x<=bounds.getMaxX()>>4;x++)for(int z=bounds.getMinZ()>>4;z<=bounds.getMaxZ()>>4;z++)w.setChunkForced(x,z,false);c.complete();});
 }
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=200,batchId="monkey_sites")
 public void bananaCourtHasFiveResidentsAndSupportedDetails(TestContext c){site(c,"monkey/court_temple",new BlockPos(3000,70,3000),BlockRotation.NONE,3,17,17,new BlockPos(8,12,6));}
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=200,batchId="monkey_sites")
 public void rotatedValleyHasFiveResidentsAndWorkingMonument(TestContext c){site(c,"monkey/valley_temple",new BlockPos(3200,70,3200),BlockRotation.CLOCKWISE_90,2,17,17,new BlockPos(8,12,6));}
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=1200,batchId="monkey_sites")
 public void bothSitesGenerateNativelyOnlyInJunglesAndSavanna(TestContext c){
  var w=c.getWorld();var r=w.getRegistryManager();var biomes=MultiNoiseBiomeSource.create(r.get(RegistryKeys.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST).entryOf(MultiNoiseBiomeSourceParameterLists.OVERWORLD));var settings=r.get(RegistryKeys.CHUNK_GENERATOR_SETTINGS).entryOf(ChunkGeneratorSettings.OVERWORLD);var gen=new NoiseChunkGenerator(biomes,settings);var noise=NoiseConfig.create(settings.value(),r.get(RegistryKeys.NOISE_PARAMETERS).getReadOnlyWrapper(),42L);
  for(var name:new String[]{"banana_court","great_appetite_valley"}){var s=r.get(RegistryKeys.STRUCTURE).get(new Identifier("zoldrax",name));check(s!=null,"Native structure must be registered");check(s.getValidBiomes().stream().allMatch(b->{String id=b.getKey().orElseThrow().getValue().getPath();return id.contains("jungle")||id.startsWith("savanna");}),"No structures outside requested biomes");boolean valid=false;for(int i=0;i<512&&!valid;i++){
   var start=s.createStructureStart(r,gen,biomes,noise,w.getStructureTemplateManager(),42L,new ChunkPos(i%32*30,i/32*30),0,w,s.getValidBiomes()::contains);
   if(!start.hasChildren())continue;
   var children=start.getChildren();check(children.size()>=(name.equals("banana_court")?4:5)&&children.size()<=(name.equals("banana_court")?8:10),"A complete core settlement must generate");
   for(String required:new String[]{"temple","hall","storehouse","kitchen"})check(children.stream().anyMatch(p->((net.minecraft.structure.PoolStructurePiece)p).getPoolElement().toString().contains(required)),"Missing core building: "+required);
   if(name.equals("great_appetite_valley"))check(children.stream().anyMatch(p->((net.minecraft.structure.PoolStructurePiece)p).getPoolElement().toString().contains("farm")),"Valley must keep a working farm");
   for(int a=0;a<children.size();a++)for(int b=a+1;b<children.size();b++)check(!children.get(a).getBoundingBox().intersects(children.get(b).getBoundingBox()),"Separate buildings must not overlap");
   valid=children.stream().map(p->p.getBoundingBox().getMinY()).distinct().count()>1;
   if(valid){
    var evidence=new com.google.gson.JsonArray();
    for(var child:children){var piece=(net.minecraft.structure.PoolStructurePiece)child;var item=new com.google.gson.JsonObject();item.addProperty("template",piece.getPoolElement().toString());item.addProperty("x",piece.getPos().getX());item.addProperty("y",piece.getPos().getY());item.addProperty("z",piece.getPos().getZ());item.addProperty("rotation",piece.getRotation().name());evidence.add(item);}
    try{java.nio.file.Files.writeString(java.nio.file.Path.of(name+"-generated.json"),evidence.toString());}catch(java.io.IOException e){throw new IllegalStateException(e);}
   }
  }check(valid,"Native Overworld must generate independent terrain heights for "+name);}
  check(Registries.BANNER_PATTERN.containsId(new Identifier("zoldrax","banana")),"Custom banana banner motif must be registered");c.complete();
 }
}
