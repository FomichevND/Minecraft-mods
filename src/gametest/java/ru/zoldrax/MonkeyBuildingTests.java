package ru.zoldrax;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.*;
import net.minecraft.block.*;
import net.minecraft.registry.*;
import net.minecraft.structure.*;
import net.minecraft.structure.pool.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.source.*;
import net.minecraft.world.gen.chunk.*;
import net.minecraft.world.gen.chunk.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.gen.noise.NoiseConfig;

public class MonkeyBuildingTests implements FabricGameTest {
 private void check(boolean b,String message){if(!b)throw new IllegalStateException(message);}
 private void building(TestContext c,String name,int count,int heralds,int index,BlockRotation rotation){
  var w=c.getWorld();var root=new BlockPos(4000+index*128,75,4400);
  var element=StructurePoolElement.ofSingle("zoldrax:monkey_buildings/"+name).apply(StructurePool.Projection.RIGID);
  var box=element.getBoundingBox(w.getStructureTemplateManager(),root,rotation);
  for(int x=box.getMinX()>>4;x<=box.getMaxX()>>4;x++)for(int z=box.getMinZ()>>4;z<=box.getMaxZ()>>4;z++){w.setChunkForced(x,z,true);w.getChunk(x,z);}
  var piece=new PoolStructurePiece(w.getStructureTemplateManager(),element,root,8,rotation,box);
  for(int x=box.getMinX()>>4;x<=box.getMaxX()>>4;x++)for(int z=box.getMinZ()>>4;z<=box.getMaxZ()>>4;z++){
   var clip=new BlockBox(x*16,w.getBottomY(),z*16,x*16+15,w.getTopY()-1,z*16+15);
   piece.generate(w,w.getStructureAccessor(),w.getChunkManager().getChunkGenerator(),Random.create(1),clip,new ChunkPos(x,z),root);
  }
  var area=new Box(box.getMinX(),box.getMinY(),box.getMinZ(),box.getMaxX()+1,box.getMaxY()+1,box.getMaxZ()+1);
  c.addInstantFinalTask(()->{
  var residents=w.getEntitiesByClass(MonkeyFollowerEntity.class,area,e->true);
  c.assertTrue(residents.size()==count,"Wrong resident count for "+name+": "+residents.size());check(residents.stream().filter(MonkeyFollowerEntity::isHerald).count()==heralds,"Wrong roles for "+name);
  for(var npc:residents){npc.setAiDisabled(true);check(npc.isPersistent(),"Residents must be persistent");check(w.isAir(npc.getBlockPos().up(2)),"Crown/hat needs headroom above the NPC: "+name);check(!w.getBlockCollisions(npc,npc.getBoundingBox().contract(.001)).iterator().hasNext(),"NPC intersects furniture: "+name+" "+npc.getPos());}
  int containers=0,furnaces=0,lecterns=0,beds=0;
  for(var p:BlockPos.iterate(box.getMinX(),box.getMinY(),box.getMinZ(),box.getMaxX(),box.getMaxY(),box.getMaxZ())){
   var state=w.getBlockState(p);var block=state.getBlock();check(block!=Zoldrax.MONKEY_BLOCK,"Small decorative buildings must not contain a quest monument");
   if(block instanceof LanternBlock||block instanceof WallBannerBlock||block instanceof CandleBlock)check(state.canPlaceAt(w,p),"Unsupported detail in "+name+": "+p+" "+state);
   if(block instanceof ChestBlock||block instanceof BarrelBlock)containers++;
   if(block instanceof AbstractFurnaceBlock)furnaces++;
   if(block instanceof LecternBlock)lecterns++;
   if(block instanceof BedBlock)beds++;
  }
  if(name.equals("kitchen"))check(furnaces==3&&containers>=3,"Kitchen must contain two furnaces, a smoker and storage");
  if(name.equals("storehouse"))check(containers>=12,"Storehouse must have substantial real storage");
  if(name.equals("herald_house"))check(lecterns==1&&beds==2,"Herald house must have lectern and a complete bed");
  for(var npc:residents)c.assertTrue(npc.age>0&&(npc.isHerald()?npc.getMainHandStack().isOf(Zoldrax.HERALD_HORN):npc.getMainHandStack().isEmpty()),"Resident equipment must preserve horn / empty gatherer hands");
  for(var npc:residents){
   var saved=new net.minecraft.nbt.NbtCompound();npc.writeCustomDataToNbt(saved);check(saved.getBoolean("HomeResident")&&saved.contains("MonkeyHome"),"Standalone residents must save their home");
   var home=net.minecraft.nbt.NbtHelper.toBlockPos(saved.getCompound("MonkeyHome"));check(home.equals(npc.getBlockPos()),"Home must use the rotated world spawn position, not template coordinates");
   var copy=(npc.isHerald()?Zoldrax.BANANA_HERALD:Zoldrax.TEMPLE_GATHERER).create(w);copy.readCustomDataFromNbt(saved);check(copy.getPositionTarget().equals(home),"Home restriction must survive reload");npc.discard();
  }
   for(var e:w.getEntitiesByClass(net.minecraft.entity.decoration.DisplayEntity.ItemDisplayEntity.class,area,e->true))e.discard();
   for(int x=box.getMinX()>>4;x<=box.getMaxX()>>4;x++)for(int z=box.getMinZ()>>4;z<=box.getMaxZ()>>4;z++)w.setChunkForced(x,z,false);});
 }
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=120,batchId="monkey_small_buildings")
 public void kitchenHasCookAndRealWorkstations(TestContext c){building(c,"kitchen",1,0,0,BlockRotation.NONE);}
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=120,batchId="monkey_small_buildings")
 public void rotatedStorehouseHasTwoGatherers(TestContext c){building(c,"storehouse",2,0,1,BlockRotation.CLOCKWISE_90);}
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=120,batchId="monkey_small_buildings")
 public void heraldHouseHasBedAndLectern(TestContext c){building(c,"herald_house",1,1,2,BlockRotation.CLOCKWISE_180);}
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=120,batchId="monkey_small_buildings")
 public void pavilionHasTwoFollowersAndDecorativeAltar(TestContext c){building(c,"pavilion",2,1,3,BlockRotation.COUNTERCLOCKWISE_90);}
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=1200,batchId="monkey_small_generation")
 public void allFourBuildingsGenerateInAllowedBiomes(TestContext c){
  var w=c.getWorld();var r=w.getRegistryManager();
  var biomes=MultiNoiseBiomeSource.create(r.get(RegistryKeys.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST).entryOf(MultiNoiseBiomeSourceParameterLists.OVERWORLD));var settings=r.get(RegistryKeys.CHUNK_GENERATOR_SETTINGS).entryOf(ChunkGeneratorSettings.OVERWORLD);var gen=new NoiseChunkGenerator(biomes,settings);var noise=NoiseConfig.create(settings.value(),r.get(RegistryKeys.NOISE_PARAMETERS).getReadOnlyWrapper(),42L);
  var set=r.get(RegistryKeys.STRUCTURE_SET).get(new Identifier("zoldrax","monkey_buildings"));var placement=(RandomSpreadStructurePlacement)set.placement();check(set.structures().size()==4&&placement.getSpacing()==18,"Separate building structure set must register all four variants");
  for(String name:new String[]{"kitchen","storehouse","herald_house","pavilion"}){
   var structure=r.get(RegistryKeys.STRUCTURE).get(new Identifier("zoldrax","monkey_"+name));check(structure!=null,"Missing standalone structure "+name);
   check(structure.getValidBiomes().stream().allMatch(b->{String id=b.getKey().orElseThrow().getValue().getPath();return id.contains("jungle")||id.startsWith("savanna");}),"Unexpected building biome");boolean found=false;
   for(int i=0;i<256&&!found;i++){
    var chunk=placement.getStartChunk(42L,(i%16-8)*18,(i/16-8)*18);int x=chunk.getStartX()+8,z=chunk.getStartZ()+8,y=gen.getHeight(x,z,Heightmap.Type.OCEAN_FLOOR_WG,w,noise);
    if(!structure.getValidBiomes().contains(biomes.getBiome(x>>2,y>>2,z>>2,noise.getMultiNoiseSampler())))continue;
    var start=structure.createStructureStart(r,gen,biomes,noise,w.getStructureTemplateManager(),42L,chunk,0,w,structure.getValidBiomes()::contains);
    found=start.hasChildren();if(found)check(start.getChildren().size()==1,"Each site must be exactly one building, not a new settlement");
   }
   check(found,"No natural generation found for "+name);
  }
  c.complete();
 }
}
