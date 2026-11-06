package ru.zoldrax;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.resource.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.*;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.*;
import net.minecraft.resource.*;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.*;

/** Cuboids/UVs are read directly from the editable .bbmodel, including single-axis rotations. */
public final class DecorRenderer implements BlockEntityRenderer<DecorBlockEntity> {
 private static final Map<DecorKind,Mesh> CACHE=new EnumMap<>(DecorKind.class);
 private final net.minecraft.client.render.item.ItemRenderer items;
 public DecorRenderer(BlockEntityRendererFactory.Context context){items=context.getItemRenderer();}
 public static void register(){
  BlockEntityRendererFactories.register(DecorBlocks.ENTITY,DecorRenderer::new);
  net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(DecorBlocks.SEAT,net.minecraft.client.render.entity.EmptyEntityRenderer::new);
  for(var kind:DecorKind.values())BuiltinItemRendererRegistry.INSTANCE.register(DecorBlocks.BLOCKS.get(kind).asItem(),(stack,mode,matrices,vertices,light,overlay)->renderItem(kind,matrices,vertices,light,overlay));
  ItemTooltipCallback.EVENT.register((stack,context,lines)->{if(stack.hasNbt()&&stack.getNbt().contains("TernorHotUntil")&&MinecraftClient.getInstance().world!=null&&stack.getNbt().getLong("TernorHotUntil")>MinecraftClient.getInstance().world.getTime())lines.add(Text.translatable("tooltip.zoldrax.hot_blank").formatted(Formatting.GOLD));if(stack.hasNbt()&&stack.getNbt().getBoolean("TernorTempered"))lines.add(Text.translatable("tooltip.zoldrax.tempered").formatted(Formatting.GRAY));if(stack.getItem() instanceof BlockItem item&&item.getBlock() instanceof DecorBlock block)lines.add(Text.translatable("tooltip.zoldrax."+block.kind.id).formatted(Formatting.GRAY));});
  ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener(){public Identifier getFabricId(){return DecorBlocks.id("decor_meshes");}public void reload(ResourceManager manager){CACHE.clear();FoodDisplayModelRegistry.reload();TernorDecorRenderer.registerModels();}});
 }
 private static Mesh mesh(DecorKind kind){return CACHE.computeIfAbsent(kind,Mesh::new);}
 private static void renderItem(DecorKind kind,MatrixStack matrices,VertexConsumerProvider vertices,int light,int overlay){
  var model=mesh(kind);matrices.push();matrices.translate(.5,.5,.5);float scale=.76f/model.extent;matrices.scale(scale,scale,scale);matrices.translate(-model.cx,-model.cy,-model.cz);
  drawModel(kind,model,matrices,vertices,light,overlay,true,1,0,true,true,0);matrices.pop();
 }
 @Override public void render(DecorBlockEntity entity,float tickDelta,MatrixStack matrices,VertexConsumerProvider vertices,int light,int overlay){
  var state=entity.getCachedState();if(!(state.getBlock() instanceof DecorBlock))return;
  var kind=entity.kind;var model=mesh(kind);float time=entity.getWorld()==null?0:entity.getWorld().getTime()+tickDelta;
  float open=MathHelper.lerp(tickDelta,entity.previousLid,entity.lid);open=1-(1-open)*(1-open)*(1-open);
  matrices.push();matrices.translate(.5,0,.5);matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-DecorBlocks.degrees(state.get(DecorBaseBlock.FACING))));matrices.translate(-.5,0,-.5);
  boolean vanillaBook=kind==DecorKind.LECTERN&&!entity.getStack(0).isEmpty()&&(entity.getStack(0).isOf(Items.BOOK)||entity.getStack(0).isOf(Items.WRITTEN_BOOK)||entity.getStack(0).isOf(Items.WRITABLE_BOOK));
  drawModel(kind,model,matrices,vertices,light,overlay,kind.light==0||state.get(DecorBaseBlock.LIT),state.get(DecorBaseBlock.LENGTH),open,vanillaBook,kind!=DecorKind.OFFERING_PEDESTAL||entity.getStack(0).isEmpty(),kind==DecorKind.COURTYARD_GONG||kind==DecorKind.FESTIVAL_DRUM||kind==DecorKind.BELLOWS?entity.strikeTicks:time);
  if(kind.ternor())TernorDecorRenderer.render(entity,model,matrices,vertices,light,overlay,time);
  if(kind==DecorKind.SUPPLY_BASKET){for(var display:BasketDisplayLayout.create(entity))FoodDisplayModelRegistry.renderBasket(display.stack(),display.x(),display.y(),display.z(),display.size(),matrices,vertices,light,overlay,entity.getWorld());}
  else if(kind.foodDisplay()){var slots=FoodDisplaySlots.forKind(kind);for(int i=0;i<Math.min(slots.size(),entity.size());i++)if(!entity.getStack(i).isEmpty())FoodDisplayModelRegistry.render(entity.getStack(i),slots.get(i),matrices,vertices,light,overlay,entity.getWorld());}
  if(kind==DecorKind.NOTICE_BOARD)renderDocuments(entity,matrices,vertices,light,overlay);
  if(kind==DecorKind.WEAPON_RACK){for(int i=0;i<3;i++)if(!entity.getStack(i).isEmpty()){
   matrices.push();matrices.translate(.5+i*.5,.98,.9);matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(135));matrices.scale(1.05f,1.05f,1.05f);items.renderItem(entity.getStack(i),ModelTransformationMode.FIXED,light,overlay,matrices,vertices,entity.getWorld(),entity.getPos().hashCode()+i);matrices.pop();
  }}
  if(kind==DecorKind.OFFERING_PEDESTAL&&!entity.getStack(0).isEmpty()){
   matrices.push();matrices.translate(.5,1.4+Math.sin(time*.08)*.04,.5);matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(time*1.2f));matrices.scale(.65f,.65f,.65f);items.renderItem(entity.getStack(0),ModelTransformationMode.FIXED,light,overlay,matrices,vertices,entity.getWorld(),entity.getPos().hashCode());matrices.pop();
  }
  if(kind==DecorKind.LECTERN&&!entity.getStack(0).isEmpty()&&!vanillaBook){
   matrices.push();matrices.translate(.5,1.36,.5);matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(65));matrices.scale(.55f,.55f,.55f);items.renderItem(entity.getStack(0),ModelTransformationMode.FIXED,light,overlay,matrices,vertices,entity.getWorld(),entity.getPos().hashCode());matrices.pop();
  }
  matrices.pop();
 }
 private void renderDocuments(DecorBlockEntity entity,MatrixStack matrices,VertexConsumerProvider vertices,int light,int overlay){
  double[][] positions={{.9,1.75},{2.1,1.75},{1.5,.85}};
  for(int i=0;i<3;i++){var stack=entity.getStack(i);if(stack.isEmpty())continue;if(stack.isOf(Items.PAPER)){mesh(entity.kind).render("paper"+i,matrices,vertices,light,overlay,false);continue;}matrices.push();matrices.translate(positions[i][0],positions[i][1],.435);
   var id=net.minecraft.item.FilledMapItem.getMapId(stack);var map=id==null?null:net.minecraft.item.FilledMapItem.getMapState(id,entity.getWorld());
   if(stack.isOf(Items.FILLED_MAP)&&map!=null){matrices.scale(.0055f,-.0055f,.0055f);matrices.translate(-64,-64,0);MinecraftClient.getInstance().gameRenderer.getMapRenderer().draw(matrices,vertices,id,map,false,light);}
   else {matrices.scale(.65f,.65f,.65f);items.renderItem(stack,ModelTransformationMode.FIXED,light,overlay,matrices,vertices,entity.getWorld(),i);}
   matrices.pop();
  }
 }
 private static void drawModel(DecorKind kind,Mesh model,MatrixStack matrices,VertexConsumerProvider vertices,int light,int overlay,boolean lit,int length,float open,boolean book,boolean crystal,float time){
  for(String group:model.groups.keySet()){
   if(group.startsWith("paper")||group.equals("water")||group.equals("greenery"))continue;
   if(group.equals("book")&&!book||group.equals("crystal")&&!crystal||group.equals("flame")&&!lit||group.equals("extension")&&length==1)continue;
   matrices.push();
   if(group.equals("pump")){float compression=time>0?1-.4f*(float)Math.sin(Math.PI*(1-time/16f)):1;matrices.translate(0,.25,0);matrices.scale(1,compression,1);matrices.translate(0,-.25,0);}
   if(group.equals("gong")){matrices.translate(1.5,2.5,.5);matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)Math.sin(time*1.7)*Math.min(7,time)));matrices.translate(-1.5,-2.5,-.5);}
   if(group.equals("drum"))matrices.translate(0,0,Math.sin(time*2)*Math.min(.045,time*.004));
   if(group.equals("lid")){matrices.translate(.5,14/16f,3/16f);matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90*open));matrices.translate(-.5,-14/16f,-3/16f);}
   if(group.equals("cage")){matrices.translate(.5,-(length-1),.5);matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)Math.sin(time*.035)*1.7f));matrices.translate(-.5,0,-.5);}
   if(group.equals("flame")){matrices.translate(.5,1,.5);matrices.scale(1,1+(float)Math.sin(time*.17)*.025f,1);matrices.translate(-.5,-1,-.5);}
   model.render(group,matrices,vertices,light,overlay,lit);matrices.pop();
  }
 }
 @Override public boolean rendersOutsideBoundingBox(DecorBlockEntity entity){return true;}
 @Override public int getRenderDistance(){return 80;}
 private record Quad(float[] xyz,float[] uv,float nx,float ny,float nz,boolean emissive){}
 static final class Mesh {
  final Map<String,List<Quad>> groups=new LinkedHashMap<>();final Identifier texture;float cx,cy,cz,extent,minY,maxY,width,depth;
  Mesh(DecorKind kind){this("decor",kind.id);}
  Mesh(String folder,String name){this(DecorBlocks.id("models/blockbench/"+folder+"/"+name+".bbmodel"),DecorBlocks.id("textures/block/zoldrax/"+name+".png"));}
  Mesh(Identifier resource,Identifier modelTexture){
   texture=modelTexture;float[] min={Float.MAX_VALUE,Float.MAX_VALUE,Float.MAX_VALUE},max={-Float.MAX_VALUE,-Float.MAX_VALUE,-Float.MAX_VALUE};
   try(var reader=new InputStreamReader(MinecraftClient.getInstance().getResourceManager().getResource(resource).orElseThrow().getInputStream(),StandardCharsets.UTF_8)){
    var data=JsonParser.parseReader(reader).getAsJsonObject();float tw=data.getAsJsonObject("resolution").get("width").getAsFloat(),th=data.getAsJsonObject("resolution").get("height").getAsFloat();
    var byUuid=new HashMap<String,String>();indexGroups(data.getAsJsonArray("outliner"),"body",byUuid);
    for(var value:data.getAsJsonArray("elements")){
     var e=value.getAsJsonObject();float[] lo=vector(e.getAsJsonArray("from")),hi=vector(e.getAsJsonArray("to")),origin=vector(e.getAsJsonArray("origin")),rotation=vector(e.getAsJsonArray("rotation"));
     var transform=new Matrix4f().translate(origin[0],origin[1],origin[2]).rotateZ((float)Math.toRadians(rotation[2])).rotateY((float)Math.toRadians(rotation[1])).rotateX((float)Math.toRadians(rotation[0])).translate(-origin[0],-origin[1],-origin[2]);
     var faces=e.getAsJsonObject("faces");var list=groups.computeIfAbsent(e.has("zoldrax_group")?e.get("zoldrax_group").getAsString():byUuid.getOrDefault(e.get("uuid").getAsString(),"body"),s->new ArrayList<>());
     for(String face:faces.keySet()){
      if(!faces.get(face).isJsonObject())continue;
      float x=lo[0],y=lo[1],z=lo[2],X=hi[0],Y=hi[1],Z=hi[2];float[] points;var normal=new Vector3f();
      switch(face){
       case "south"->{points=new float[]{x,Y,Z,x,y,Z,X,y,Z,X,Y,Z};normal.set(0,0,1);}
       case "north"->{points=new float[]{X,Y,z,X,y,z,x,y,z,x,Y,z};normal.set(0,0,-1);}
       case "east"->{points=new float[]{X,Y,Z,X,y,Z,X,y,z,X,Y,z};normal.set(1,0,0);}
       case "west"->{points=new float[]{x,Y,z,x,y,z,x,y,Z,x,Y,Z};normal.set(-1,0,0);}
       case "up"->{points=new float[]{x,Y,z,x,Y,Z,X,Y,Z,X,Y,z};normal.set(0,1,0);}
       default->{points=new float[]{x,y,Z,x,y,z,X,y,z,X,y,Z};normal.set(0,-1,0);}
      }
      for(int i=0;i<4;i++){var p=transform.transformPosition(new Vector3f(points[i*3],points[i*3+1],points[i*3+2]));points[i*3]=p.x/16;points[i*3+1]=p.y/16;points[i*3+2]=p.z/16;min[0]=Math.min(min[0],p.x/16);min[1]=Math.min(min[1],p.y/16);min[2]=Math.min(min[2],p.z/16);max[0]=Math.max(max[0],p.x/16);max[1]=Math.max(max[1],p.y/16);max[2]=Math.max(max[2],p.z/16);}
      transform.transformDirection(normal).normalize();var uv=faces.getAsJsonObject(face).getAsJsonArray("uv");float u=uv.get(0).getAsFloat()/tw,v=uv.get(1).getAsFloat()/th,U=uv.get(2).getAsFloat()/tw,V=uv.get(3).getAsFloat()/th;
      list.add(new Quad(points,new float[]{u,v,u,V,U,V,U,v},normal.x,normal.y,normal.z,e.has("zoldrax_emissive")?e.get("zoldrax_emissive").getAsBoolean():((int)(v*th/64)*4+(int)(u*tw/64)>=9&&(int)(v*th/64)*4+(int)(u*tw/64)<=11)));
     }
    }
   }catch(Exception e){throw new IllegalStateException("Cannot load editable decor mesh "+resource,e);}
   minY=min[1];maxY=max[1];width=max[0]-min[0];depth=max[2]-min[2];cx=(min[0]+max[0])/2;cy=(min[1]+max[1])/2;cz=(min[2]+max[2])/2;extent=Math.max(max[0]-min[0],Math.max(max[1]-min[1],max[2]-min[2]));
  }
  void render(String group,MatrixStack matrices,VertexConsumerProvider vertices,int light,int overlay,boolean lit){
   var entry=matrices.peek();var matrix=entry.getPositionMatrix();var normal=entry.getNormalMatrix();
   for(int pass=0;pass<2;pass++){
    final boolean emissivePass=pass==1;if(emissivePass&&!lit)continue;
    if(groups.get(group).stream().noneMatch(q->(q.emissive&&lit)==emissivePass))continue;
    var consumer=vertices.getBuffer(emissivePass?RenderLayer.getEntityTranslucentEmissive(texture):RenderLayer.getEntityCutoutNoCull(texture));
    for(var q:groups.get(group)){
     boolean emission=q.emissive&&lit;if(emission!=emissivePass)continue;
     int brightness=emission?LightmapTextureManager.MAX_LIGHT_COORDINATE:light;float color=q.emissive&&!lit?.28f:1;
     for(int i=0;i<4;i++)consumer.vertex(matrix,q.xyz[i*3],q.xyz[i*3+1],q.xyz[i*3+2]).color(color,color,color,1).texture(q.uv[i*2],q.uv[i*2+1]).overlay(overlay).light(brightness).normal(normal,q.nx,q.ny,q.nz).next();
    }
   }
  }

  private static void indexGroups(JsonArray nodes,String group,Map<String,String> byUuid){
   if(nodes==null)return;for(var node:nodes){if(node.isJsonPrimitive())byUuid.put(node.getAsString(),group);else if(node.isJsonObject()){var object=node.getAsJsonObject();String name=object.get("name").getAsString();String animated=Set.of("body","lid","flame","cage","chain","extension","book","crystal","pump","greenery","water").contains(name)?name:group;indexGroups(object.getAsJsonArray("children"),animated,byUuid);}}
  }
  private static float[] vector(JsonArray a){if(a==null)return new float[]{0,0,0};return new float[]{a.get(0).getAsFloat(),a.get(1).getAsFloat(),a.get(2).getAsFloat()};}
 }
}
