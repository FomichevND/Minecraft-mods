package ru.zoldrax;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.*;
import net.minecraft.registry.*;
import net.minecraft.structure.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.biome.source.MultiNoiseBiomeSource;
import net.minecraft.world.biome.source.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.gen.chunk.*;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.chunk.placement.RandomSpreadStructurePlacement;
import net.minecraft.entity.*;
import net.minecraft.item.*;
import net.minecraft.block.entity.ChestBlockEntity;

public class CultSiteTests implements FabricGameTest {
 private void check(boolean value,String message){if(!value)throw new IllegalStateException(message);}
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=100,batchId="cult_sites")
 public void customMobsTrackBeyondOldSixteenBlockLimit(TestContext c){
  for(var type:new EntityType<?>[]{Zoldrax.CHAIN_PUNISHER,Zoldrax.BLOOD_PRIEST,Zoldrax.TERNOR_GUARDIAN}){
   check(type.getMaxTrackDistance()>=10,"All three custom mobs must track across ten chunks");
   var entity=type.create(c.getWorld());
   check(entity.shouldRender(64*64),"Custom mobs must allow rendering at 64 blocks");
   check(entity instanceof net.minecraft.entity.mob.MobEntity mob&&mob.isPersistent(),"NPC/guardian persistence must survive tracking fix");
  }
  c.complete();
 }
 private void site(TestContext c,String name,BlockPos origin,BlockRotation rotation,int priests,BlockPos localAltar){
  var world=c.getWorld();var template=world.getStructureTemplateManager().getTemplate(new Identifier("zoldrax",name)).orElseThrow();
  check(template.getSize().getX()==31&&template.getSize().getZ()==31,"Full buildings must have a 31x31 footprint");
  var data=new StructurePlacementData().setRotation(rotation).setIgnoreEntities(false).setInitializeMobs(true);
  var box=template.calculateBoundingBox(data,origin);
  for(int x=box.getMinX()>>4;x<=box.getMaxX()>>4;x++)for(int z=box.getMinZ()>>4;z<=box.getMaxZ()>>4;z++){world.setChunkForced(x,z,true);world.getChunk(x,z);}
  check(template.place(world,origin,origin,data,Random.create(10),2),"Native template must place successfully");
  var anchor=StructureTemplate.transform(data,localAltar).add(origin);
  check(world.getBlockEntity(anchor) instanceof MonumentEntity altar&&altar.stage==2,"Site must contain a completed working altar");
  check(world.getBlockState(anchor).get(MonumentBlock.FACING)==rotation.rotate(Direction.SOUTH),"Altar facing must rotate with buildings");
  c.waitAndRun(60,()->{
   var area=new Box(box.getMinX(),box.getMinY(),box.getMinZ(),box.getMaxX()+1,box.getMaxY()+1,box.getMaxZ()+1);
   var npcs=world.getEntitiesByClass(CultistEntity.class,area,e->true);
   check(npcs.size()==5,"Exactly five followers must spawn once, found "+npcs.size());
   check(npcs.stream().filter(CultistEntity::isPriest).count()==priests,"Correct priest/punisher population");
   check(npcs.stream().allMatch(CultistEntity::isPersistent),"Generated followers must persist");
   boolean loot=false;
   for(var pos:BlockPos.iterate(box.getMinX(),box.getMinY(),box.getMinZ(),box.getMaxX(),box.getMaxY(),box.getMaxZ())){
    if(world.getBlockEntity(pos) instanceof ChestBlockEntity chest&&chest.createNbt().getString("LootTable").startsWith("zoldrax:chests/"))loot=true;
   }
   check(loot,"Buildings must contain real loot-table chests");
   npcs.forEach(Entity::discard);
   for(int x=box.getMinX()>>4;x<=box.getMaxX()>>4;x++)for(int z=box.getMinZ()>>4;z<=box.getMaxZ()>>4;z++)world.setChunkForced(x,z,false);
   c.complete();
  });
 }
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=200,batchId="cult_sites")
 public void shrinePlacesFiveFollowersAndRealLoot(TestContext c){site(c,"blood_shrine",new BlockPos(1000,70,1000),BlockRotation.NONE,3,new BlockPos(15,4,9));}
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=200,batchId="cult_sites")
 public void rotatedCampPlacesFiveFollowersAndAlignedAltar(TestContext c){site(c,"chain_camp",new BlockPos(1200,70,1200),BlockRotation.CLOCKWISE_90,2,new BlockPos(15,3,7));}
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=200,batchId="cult_sites")
 public void nativeOverworldGeneratorFindsBothStructures(TestContext c){
  var world=c.getWorld();var registries=world.getRegistryManager();
  var biomes=MultiNoiseBiomeSource.create(registries.get(RegistryKeys.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST).entryOf(MultiNoiseBiomeSourceParameterLists.OVERWORLD));
  var settings=registries.get(RegistryKeys.CHUNK_GENERATOR_SETTINGS).entryOf(ChunkGeneratorSettings.OVERWORLD);
  var generator=new NoiseChunkGenerator(biomes,settings);
  var noise=NoiseConfig.create(settings.value(),registries.get(RegistryKeys.NOISE_PARAMETERS).getReadOnlyWrapper(),42L);
  var set=registries.get(RegistryKeys.STRUCTURE_SET).get(new Identifier("zoldrax","cult_sites"));
  check(set!=null&&set.structures().size()==2,"Both structures must be registered in a native structure set");
  check(set.placement() instanceof RandomSpreadStructurePlacement,"Sites must use native random generation");
  var placement=(RandomSpreadStructurePlacement)set.placement();
  check(placement.getSpacing()==32&&placement.getSeparation()==12,"Sites must be spaced apart");
  for(var name:new String[]{"blood_shrine","chain_camp"}){
   var structure=registries.get(RegistryKeys.STRUCTURE).get(new Identifier("zoldrax",name));
   check(structure!=null,"Native jigsaw structure registry must contain "+name);
   boolean found=false;
   for(int i=0;i<64&&!found;i++){
    var chunk=placement.getStartChunk(42L,(i%8)*32,(i/8)*32);
    var start=structure.createStructureStart(registries,generator,biomes,noise,world.getStructureTemplateManager(),42L,chunk,0,world,structure.getValidBiomes()::contains);
    if(start.hasChildren()){
     check(start.getChildren().size()==1,"Entire multi-building site must be one clipped template");
     var bounds=start.getChildren().get(0).getBoundingBox();
     check(bounds.getBlockCountX()==31&&bounds.getBlockCountZ()==31,"Natural generation must preserve full site footprint");
     found=true;
    }
   }
   check(found,"A valid natural Overworld start must exist for "+name);
  }
  c.complete();
 }
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=100,batchId="cult_sites")
 public void rotatedBrazierAcceptsOfferingAtVisibleFire(TestContext c){
  var root=c.getAbsolutePos(new BlockPos(3,2,3));var world=c.getWorld();
  world.setBlockState(root,Zoldrax.BLOCK.getDefaultState().with(MonumentBlock.FACING,Direction.WEST));
  var altar=(MonumentEntity)world.getBlockEntity(root);altar.stage=2;
  var player=c.createMockCreativeServerPlayerInWorld();var p=altar.altarPoint(0,.5,1.55);
  player.refreshPositionAndAngles(root.getX()-4,root.getY()+2,root.getZ(),0,0);
  var item=new ItemEntity(world,p.x,p.y,p.z,new ItemStack(Items.DIAMOND));item.setNoGravity(true);item.setThrower(player.getUuid());item.setPickupDelayInfinite();world.spawnEntity(item);
  c.waitAndRun(8,()->{check(item.isRemoved(),"Offering at rotated visible brazier must be accepted");check(!player.getActiveStatusEffects().isEmpty(),"Donor must receive blessing");world.getServer().getPlayerManager().remove(player);c.complete();});
 }
}
