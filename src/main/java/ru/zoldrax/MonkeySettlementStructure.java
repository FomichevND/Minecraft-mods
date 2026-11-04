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
 /** Try nearby patches instead of treating every plot as a veto on the whole town. */
 private static final List<BlockPos> PATCHES;
 static {
  var offsets=new ArrayList<BlockPos>();
  for(int x=-8;x<=8;x+=4)for(int z=-8;z<=8;z+=4)offsets.add(new BlockPos(x,0,z));
  offsets.sort(Comparator.comparingInt(p->p.getX()*p.getX()+p.getZ()*p.getZ()));
  PATCHES=List.copyOf(offsets);
 }
 private static class Terrain {
  final Context context;
  final Map<Long,Integer> heights=new HashMap<>();
  Terrain(Context context){this.context=context;}
  int height(int x,int z){return heights.computeIfAbsent(BlockPos.asLong(x,0,z),key->context.chunkGenerator().getHeight(x,z,Heightmap.Type.OCEAN_FLOOR_WG,context.world(),context.noiseConfig()));}
 }
 private static PoolStructurePiece fit(Context c,Terrain terrain,Plot plot,int cx,int cz,BlockRotation rotation,List<PoolStructurePiece> pieces){
  var manager=c.structureTemplateManager();
  var element=StructurePoolElement.ofSingle("zoldrax:monkey/"+plot.name).apply(StructurePool.Projection.RIGID);
  var bounds=element.getBoundingBox(manager,BlockPos.ORIGIN,rotation);
  var offset=new BlockPos(plot.x,0,plot.z).rotate(rotation);
  int jitterX=plot.x==0&&plot.z==0?0:c.random().nextInt(3)-1;
  int jitterZ=plot.x==0&&plot.z==0?0:c.random().nextInt(3)-1;
  int desiredX=cx+offset.getX()-(bounds.getMinX()+bounds.getMaxX())/2+jitterX;
  int desiredZ=cz+offset.getZ()-(bounds.getMinZ()+bounds.getMaxZ())/2+jitterZ;
  var size=element.getStart(manager,BlockRotation.NONE);
  var approach=new BlockPos((size.getX()-1)/2,0,size.getZ()-2).rotate(rotation);
  for(var patch:PATCHES){
   int ox=desiredX+patch.getX(),oz=desiredZ+patch.getZ();
   // Reject horizontal overlap before doing the more expensive terrain sampling.
   int minX=ox+bounds.getMinX(),maxX=ox+bounds.getMaxX(),minZ=oz+bounds.getMinZ(),maxZ=oz+bounds.getMaxZ();
   if(pieces.stream().anyMatch(p->{var b=p.getBoundingBox();return minX<=b.getMaxX()+1&&maxX>=b.getMinX()-1&&minZ<=b.getMaxZ()+1&&maxZ>=b.getMinZ()-1;}))continue;
   int low=Integer.MAX_VALUE,high=Integer.MIN_VALUE;
   for(int x:new int[]{bounds.getMinX(),(bounds.getMinX()+bounds.getMaxX())/2,bounds.getMaxX()})for(int z:new int[]{bounds.getMinZ(),(bounds.getMinZ()+bounds.getMaxZ())/2,bounds.getMaxZ()}){
    int y=terrain.height(ox+x,oz+z);low=Math.min(low,y);high=Math.max(high,y);
   }
   if(low<c.chunkGenerator().getSeaLevel()||high-low>6||high+24>=c.world().getTopY())continue;
   int ground=terrain.height(ox+approach.getX(),oz+approach.getZ());
   if(ground<low||ground>high)continue;
   var pos=new BlockPos(ox,ground-10,oz);
   return new PoolStructurePiece(manager,element,pos,10,rotation,element.getBoundingBox(manager,pos,rotation));
  }
  return null;
 }
 @Override protected Optional<StructurePosition> getStructurePosition(Context c){
  int cx=c.chunkPos().getStartX()+8,cz=c.chunkPos().getStartZ()+8;
  var terrain=new Terrain(c);var rotation=BlockRotation.random(c.random());
  var pieces=new ArrayList<PoolStructurePiece>();var plan=valley?VALLEY:COURT;
  var temple=fit(c,terrain,plan.get(0),cx,cz,rotation,pieces);
  if(temple==null)return Optional.empty();
  pieces.add(temple);
  // Keep the other plots centred on the actual fitted shrine.
  var anchor=temple.getPos().add(new BlockPos(8,10,8).rotate(rotation));cx=anchor.getX();cz=anchor.getZ();
  int farms=0;
  for(var plot:plan.subList(1,plan.size())){
   var piece=fit(c,terrain,plot,cx,cz,rotation,pieces);
   if(piece!=null){pieces.add(piece);if(plot.name.equals("farm"))farms++;}
   else if(plot.name.endsWith("hall")||plot.name.equals("storehouse")||plot.name.equals("kitchen"))return Optional.empty();
   // A steep optional garden, tower or rest area no longer cancels all buildings.
  }
  if(valley&&farms==0)return Optional.empty();
  return Optional.of(new StructurePosition(anchor,collector->pieces.forEach(collector::addPiece)));
 }
 @Override public StructureType<?> getType(){return TYPE;}
}
