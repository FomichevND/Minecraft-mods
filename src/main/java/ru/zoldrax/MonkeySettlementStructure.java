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

/** Small independent rigid templates, each fitted to its own patch of natural terrain.
 * Uses vanilla pool pieces for chunk clipping, rotation, entity placement and save/load.
 * No world-wide clearing pass, connecting foundation or enclosing perimeter. */
public class MonkeySettlementStructure extends Structure {
 public static final Codec<MonkeySettlementStructure> CODEC=RecordCodecBuilder.create(i->i.group(
  configCodecBuilder(i),Codec.BOOL.fieldOf("valley").forGetter(s->s.valley)
 ).apply(i,MonkeySettlementStructure::new));
 public static final StructureType<MonkeySettlementStructure> TYPE=()->CODEC;
 private final boolean valley;
 public MonkeySettlementStructure(Config config,boolean valley){super(config);this.valley=valley;}
 private record Plot(String name,int x,int z){}
 private static final List<Plot> COURT=List.of(
  new Plot("court_temple",0,0),new Plot("herald_hall",-17,10),
  new Plot("storehouse",18,-12),new Plot("kitchen",16,9),
  new Plot("watch",-24,-12),new Plot("festival",0,19),
  new Plot("garden",-9,-17),new Plot("rest",24,26));
 private static final List<Plot> VALLEY=List.of(
  new Plot("valley_temple",0,0),new Plot("gatherer_hall",-19,-10),
  new Plot("storehouse",17,-19),new Plot("kitchen",18,2),
  new Plot("market",-17,7),new Plot("watch",22,20),
  new Plot("farm",-8,21),new Plot("farm",8,19),new Plot("garden",-3,-24),
  new Plot("rest",-23,21));
 private static int height(Context c,int x,int z){return c.chunkGenerator().getHeight(x,z,Heightmap.Type.OCEAN_FLOOR_WG,c.world(),c.noiseConfig());}
 @Override protected Optional<StructurePosition> getStructurePosition(Context c){
  int cx=c.chunkPos().getStartX()+8,cz=c.chunkPos().getStartZ()+8;
  var rotation=BlockRotation.random(c.random());var pieces=new ArrayList<PoolStructurePiece>();
  var manager=c.structureTemplateManager();
  for(var plot:valley?VALLEY:COURT){
   var offset=new BlockPos(plot.x,0,plot.z).rotate(rotation);
   var element=StructurePoolElement.ofSingle("zoldrax:monkey/"+plot.name).apply(StructurePool.Projection.RIGID);
   var bounds=element.getBoundingBox(manager,BlockPos.ORIGIN,rotation);
   // Seeded irregular spacing; temple remains exactly at the settlement anchor.
   int jitterX=plot.x==0&&plot.z==0?0:c.random().nextInt(3)-1;
   int jitterZ=plot.x==0&&plot.z==0?0:c.random().nextInt(3)-1;
   int ox=cx+offset.getX()-(bounds.getMinX()+bounds.getMaxX())/2+jitterX;
   int oz=cz+offset.getZ()-(bounds.getMinZ()+bounds.getMaxZ())/2+jitterZ;
   int low=Integer.MAX_VALUE,high=Integer.MIN_VALUE;
   // Sample corners, edge midpoints and centre without loading any world chunks.
   for(int x:new int[]{bounds.getMinX(),(bounds.getMinX()+bounds.getMaxX())/2,bounds.getMaxX()})for(int z:new int[]{bounds.getMinZ(),(bounds.getMinZ()+bounds.getMaxZ())/2,bounds.getMaxZ()}){
    int y=height(c,ox+x,oz+z);low=Math.min(low,y);high=Math.max(high,y);
   }
   if(low<c.chunkGenerator().getSeaLevel()||high-low>5||high+24>=c.world().getTopY())return Optional.empty();
   // Match the front approach, so workers can enter from the natural ground.
   var approach=new BlockPos((element.getStart(manager,BlockRotation.NONE).getX()-1)/2,0,element.getStart(manager,BlockRotation.NONE).getZ()-2).rotate(rotation);
   int ground=height(c,ox+approach.getX(),oz+approach.getZ());
   var pos=new BlockPos(ox,ground-10,oz);var box=element.getBoundingBox(manager,pos,rotation);
   if(pieces.stream().anyMatch(p->p.getBoundingBox().intersects(box)))return Optional.empty();
   pieces.add(new PoolStructurePiece(manager,element,pos,10,rotation,box));
  }
  return Optional.of(new StructurePosition(new BlockPos(cx,height(c,cx,cz),cz),collector->pieces.forEach(collector::addPiece)));
 }
 @Override public StructureType<?> getType(){return TYPE;}
}
