package ru.zoldrax;
import java.util.*;
import com.google.gson.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.World;
/** Extensible, cached real cuboid food meshes. Unknown mod foods retain their item renderer. */
public final class FoodDisplayModelRegistry {
 private static final Map<Identifier,String> MODELS=new HashMap<>();private static final Map<String,DecorRenderer.Mesh> CACHE=new HashMap<>();private static boolean loaded;private static final Set<Identifier> CUSTOM=new HashSet<>();
 private static final Map<Identifier,Identifier[]> EXTERNAL=new HashMap<>();
 public static void register(Item item,Identifier model,Identifier texture){EXTERNAL.put(Registries.ITEM.getId(item),new Identifier[]{model,texture});}
 public static void register(Item item,String modelName){CUSTOM.add(Registries.ITEM.getId(item));MODELS.put(Registries.ITEM.getId(item),modelName);CACHE.remove(modelName);}
 public static void reload(){CACHE.clear();MODELS.keySet().removeIf(id->!CUSTOM.contains(id));loaded=false;}
 private static void load(){if(loaded)return;loaded=true;try(var reader=new java.io.InputStreamReader(MinecraftClient.getInstance().getResourceManager().getResource(DecorBlocks.id("food_models.json")).orElseThrow().getInputStream(),java.nio.charset.StandardCharsets.UTF_8)){var json=JsonParser.parseReader(reader).getAsJsonObject();for(var e:json.entrySet())MODELS.putIfAbsent(new Identifier(e.getKey()),e.getValue().getAsString());}catch(java.io.IOException e){throw new IllegalStateException(e);}}
 public static boolean hasModel(Item item){load();return MODELS.containsKey(Registries.ITEM.getId(item))||EXTERNAL.containsKey(Registries.ITEM.getId(item));}
 public static void renderBasket(ItemStack stack,double x,double y,double z,float size,MatrixStack matrices,VertexConsumerProvider vertices,int light,int overlay,World world){
  load();var name=MODELS.get(Registries.ITEM.getId(stack.getItem()));var external=EXTERNAL.get(Registries.ITEM.getId(stack.getItem()));
  if(name==null&&external==null){render(stack,new FoodDisplaySlots.Slot(x,y,z,0,size,false),matrices,vertices,light,overlay,world);return;}
  var mesh=external!=null?CACHE.computeIfAbsent(external[0].toString(),n->new DecorRenderer.Mesh(external[0],external[1])):CACHE.computeIfAbsent(name,n->new DecorRenderer.Mesh("food",n));
  matrices.push();matrices.translate(x,y,z);float horizontal=size/Math.max(mesh.width,mesh.depth);float vertical=Math.min(horizontal*1.8f,.36f/(mesh.maxY-mesh.minY));matrices.scale(size/mesh.width,vertical,size/mesh.depth);matrices.translate(-mesh.cx,-mesh.minY,-mesh.cz);
  for(var group:mesh.groups.keySet())mesh.render(group,matrices,vertices,light,overlay,false);matrices.pop();
 }
 public static void render(ItemStack stack,FoodDisplaySlots.Slot slot,MatrixStack matrices,VertexConsumerProvider vertices,int light,int overlay,World world){
  load();matrices.push();matrices.translate(slot.x(),slot.y(),slot.z());var name=MODELS.get(Registries.ITEM.getId(stack.getItem()));
  var external=EXTERNAL.get(Registries.ITEM.getId(stack.getItem()));
  if(name!=null||external!=null){var mesh=external!=null?CACHE.computeIfAbsent(external[0].toString(),n->new DecorRenderer.Mesh(external[0],external[1])):CACHE.computeIfAbsent(name,n->new DecorRenderer.Mesh("food",n));float scale=slot.maxSize()/mesh.extent;matrices.scale(scale,scale,scale);
   if(slot.hanging()){matrices.translate(0,-mesh.extent/2,0);matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(mesh.maxY-mesh.minY>mesh.extent*.7f?0:slot.rotation()));matrices.translate(-mesh.cx,-mesh.cy,-mesh.cz);}else matrices.translate(-mesh.cx,-mesh.minY,-mesh.cz);
   for(var group:mesh.groups.keySet())mesh.render(group,matrices,vertices,light,overlay,false);
  }else {matrices.translate(0,slot.hanging()?-.18:.18,0);matrices.scale(slot.maxSize(),slot.maxSize(),slot.maxSize());MinecraftClient.getInstance().getItemRenderer().renderItem(stack,ModelTransformationMode.FIXED,light,overlay,matrices,vertices,world,0);}
  matrices.pop();
 }
}
