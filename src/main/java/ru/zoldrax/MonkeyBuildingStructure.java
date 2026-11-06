package ru.zoldrax;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.pool.*;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.*;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.structure.*;

/** One compact building, independent of the larger Monkey King settlements. */
public class MonkeyBuildingStructure extends Structure {
 public static final Codec<MonkeyBuildingStructure> CODEC=RecordCodecBuilder.create(i->i.group(configCodecBuilder(i),Codec.STRING.fieldOf("template").forGetter(s->s.template)).apply(i,MonkeyBuildingStructure::new));
 public static final StructureType<MonkeyBuildingStructure> TYPE=()->CODEC;
 private final String template;
 public MonkeyBuildingStructure(Config config,String template){super(config);this.template=template;}
 @Override protected Optional<StructurePosition> getStructurePosition(Context c){
  var rotation=BlockRotation.random(c.random());var manager=c.structureTemplateManager();
  var element=StructurePoolElement.ofSingle("zoldrax:monkey_buildings/"+template).apply(StructurePool.Projection.RIGID);
  var box=element.getBoundingBox(manager,BlockPos.ORIGIN,rotation);
  int cx=c.chunkPos().getStartX()+8,cz=c.chunkPos().getStartZ()+8;
  var offsets=new ArrayList<BlockPos>();for(int x=-8;x<=8;x+=4)for(int z=-8;z<=8;z+=4)offsets.add(new BlockPos(x,0,z));offsets.sort(Comparator.comparingInt(p->p.getX()*p.getX()+p.getZ()*p.getZ()));
  var heights=new HashMap<Long,Integer>();
  java.util.function.BiFunction<Integer,Integer,Integer> height=(x,z)->heights.computeIfAbsent(BlockPos.asLong(x,0,z),p->c.chunkGenerator().getHeight(x,z,Heightmap.Type.OCEAN_FLOOR_WG,c.world(),c.noiseConfig()));
  for(var offset:offsets){
   int ox=cx-(box.getMinX()+box.getMaxX())/2+offset.getX(),oz=cz-(box.getMinZ()+box.getMaxZ())/2+offset.getZ();
   int min=Integer.MAX_VALUE,max=Integer.MIN_VALUE;
   for(int x:new int[]{box.getMinX(),(box.getMinX()+box.getMaxX())/2,box.getMaxX()})for(int z:new int[]{box.getMinZ(),(box.getMinZ()+box.getMaxZ())/2,box.getMaxZ()}){int y=height.apply(ox+x,oz+z);min=Math.min(min,y);max=Math.max(max,y);}
   if(min<c.chunkGenerator().getSeaLevel()||max-min>4||max+20>=c.world().getTopY())continue;
   var size=element.getStart(manager,BlockRotation.NONE);var entrance=new BlockPos(size.getX()/2,0,size.getZ()-2).rotate(rotation);
   int ground=height.apply(ox+entrance.getX(),oz+entrance.getZ());if(ground<min||ground>max)continue;
   var pos=new BlockPos(ox,ground-8,oz);
   var piece=new PoolStructurePiece(manager,element,pos,8,rotation,element.getBoundingBox(manager,pos,rotation));
   return Optional.of(new StructurePosition(new BlockPos(cx+offset.getX(),ground,cz+offset.getZ()),collector->collector.addPiece(piece)));
  }
  return Optional.empty();
 }
 @Override public StructureType<?> getType(){return TYPE;}
}
