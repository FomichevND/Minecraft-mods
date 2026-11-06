package ru.zoldrax;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.*;
import net.minecraft.registry.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.source.*;
import net.minecraft.world.gen.chunk.*;
import net.minecraft.world.gen.chunk.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.gen.noise.NoiseConfig;

/** Samples actual random-spread candidates, rather than arbitrary chunks. */
public class MonkeyGenerationTests implements FabricGameTest {
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=1200,batchId="monkey_frequency")
 public void naturalPlacementFrequency(TestContext c){
  var w=c.getWorld();var r=w.getRegistryManager();
  var biomes=MultiNoiseBiomeSource.create(r.get(RegistryKeys.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST).entryOf(MultiNoiseBiomeSourceParameterLists.OVERWORLD));
  var settings=r.get(RegistryKeys.CHUNK_GENERATOR_SETTINGS).entryOf(ChunkGeneratorSettings.OVERWORLD);
  var gen=new NoiseChunkGenerator(biomes,settings);
  var set=r.get(RegistryKeys.STRUCTURE_SET).get(new Identifier("zoldrax","monkey_sites"));
  var nativePlacement=(RandomSpreadStructurePlacement)set.placement();
  var report=new com.google.gson.JsonArray();int allEligible=0,allGenerated=0;
  for(var placement:java.util.List.of(new RandomSpreadStructurePlacement(30,12,net.minecraft.world.gen.chunk.placement.SpreadType.LINEAR,52601928),nativePlacement)){
  int eligibleForPlacement=0,generatedForPlacement=0;
  for(long seed:new long[]{42L,12345L,8675309L}){
   var noise=NoiseConfig.create(settings.value(),r.get(RegistryKeys.NOISE_PARAMETERS).getReadOnlyWrapper(),seed);
   int eligible=0,generated=0;var examples=new com.google.gson.JsonArray();
   for(int rx=-10;rx<10;rx++)for(int rz=-10;rz<10;rz++){
    var chunk=placement.getStartChunk(seed,rx*placement.getSpacing(),rz*placement.getSpacing());
    int x=chunk.getStartX()+8,z=chunk.getStartZ()+8;
    int y=gen.getHeight(x,z,Heightmap.Type.OCEAN_FLOOR_WG,w,noise);
    var biome=biomes.getBiome(x>>2,y>>2,z>>2,noise.getMultiNoiseSampler());
    var entries=set.structures();if(!entries.get(0).structure().value().getValidBiomes().contains(biome))continue;
    eligible++;boolean success=false;
    // Both entries have equal weights; native selection retries the other on failure.
    for(var entry:entries){var structure=entry.structure().value();
     var start=structure.createStructureStart(r,gen,biomes,noise,w.getStructureTemplateManager(),seed,chunk,0,w,structure.getValidBiomes()::contains);
     if(!start.hasChildren())continue;
     var children=start.getChildren();
     for(int a=0;a<children.size();a++)for(int b=a+1;b<children.size();b++)if(children.get(a).getBoundingBox().intersects(children.get(b).getBoundingBox()))throw new IllegalStateException("Overlapping buildings at "+chunk);
     success=true;
     if(examples.size()<3){var example=new com.google.gson.JsonObject();example.addProperty("structure",entry.structure().getKey().orElseThrow().getValue().toString());example.addProperty("x",x);example.addProperty("z",z);example.addProperty("pieces",children.size());examples.add(example);}
     break;
    }
    if(success)generated++;
   }
   var row=new com.google.gson.JsonObject();row.addProperty("seed",seed);row.addProperty("spacing",placement.getSpacing());row.addProperty("eligible",eligible);row.addProperty("generated",generated);row.add("examples",examples);report.add(row);allEligible+=eligible;allGenerated+=generated;eligibleForPlacement+=eligible;generatedForPlacement+=generated;
   System.out.println("MONKEY_FREQUENCY "+row);
  }
  if(generatedForPlacement*2<eligibleForPlacement)throw new IllegalStateException("Settlement generation too rare at spacing "+placement.getSpacing()+": "+generatedForPlacement+" / "+eligibleForPlacement);
  }
  try{java.nio.file.Files.writeString(java.nio.file.Path.of("monkey-frequency.json"),report.toString());}catch(java.io.IOException e){throw new IllegalStateException(e);}
  if(allEligible<20)throw new IllegalStateException("Insufficient native biome sample: "+allEligible);
  // A whole settlement must not disappear just because one optional garden hits a slope.
  if(allGenerated*2<allEligible)throw new IllegalStateException("Settlement generation too rare: "+allGenerated+" / "+allEligible);
  c.complete();
 }
}
