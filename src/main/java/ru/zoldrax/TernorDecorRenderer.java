package ru.zoldrax;
import java.util.*;
import com.google.gson.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.util.*;
/** Reuses real volumetric food meshes for flowers, grain and inventory representatives. */
public final class TernorDecorRenderer {
 public static void registerModels(){try(var reader=new java.io.InputStreamReader(MinecraftClient.getInstance().getResourceManager().getResource(DecorBlocks.id("ternor_display_models.json")).orElseThrow().getInputStream())){var json=JsonParser.parseReader(reader).getAsJsonObject();for(var entry:json.entrySet()){var id=new Identifier(entry.getKey());if(Registries.ITEM.containsId(id)){String name=entry.getValue().getAsJsonObject().get("model").getAsString();FoodDisplayModelRegistry.register(Registries.ITEM.get(id),DecorBlocks.id("models/blockbench/ternor_props/"+name+".bbmodel"),DecorBlocks.id("textures/block/zoldrax/"+name+".png"));}}}catch(java.io.IOException ex){throw new IllegalStateException(ex);}}
 public static void render(DecorBlockEntity e,DecorRenderer.Mesh model,MatrixStack matrices,VertexConsumerProvider vertices,int light,int overlay,float time){
  if(e.kind==DecorKind.GARDEN_CANOPY){double[][] positions={{.4,.38,.4},{.7,.38,.4},{2.3,.38,.4},{2.6,.38,.4},{1.2,.38,2.5},{1.8,.38,2.5}};for(int i=0;i<6;i++)if(!e.getStack(i).isEmpty()){var p=positions[i];FoodDisplayModelRegistry.render(e.getStack(i),new FoodDisplaySlots.Slot(p[0],p[1],p[2],0,.4f,false),matrices,vertices,light,overlay,e.getWorld());}
   if(java.util.stream.IntStream.range(0,e.size()).anyMatch(i->e.getStack(i).isOf(Items.VINE)||e.getStack(i).isOf(Items.GLOW_BERRIES)))model.render("greenery",matrices,vertices,light,overlay,false);
  }
  if(e.kind==DecorKind.STORAGE_SHELF){int shown=0;for(int i=0;i<e.size()&&shown<9;i++)if(!e.getStack(i).isEmpty()){int cell=shown++;var slot=new FoodDisplaySlots.Slot(.6+(cell%3)*.9,(5+(cell/3)*15)/16.0,.55,0,.55f,false);FoodDisplayModelRegistry.render(e.getStack(i),slot,matrices,vertices,light,overlay,e.getWorld());}}
  if(e.kind==DecorKind.ANIMAL_FEEDER){var displays=BasketDisplayLayout.create(e,63);for(int i=0;i<displays.size();i++){var d=displays.get(i);int column=i%9,row=i/9;double mound=.012*Math.min(row,6-row)+.008*(4-Math.abs(column-4));var slot=new FoodDisplaySlots.Slot(.15+column*.21,.425+mound,.86-row*.105,0,.22f,false);FoodDisplayModelRegistry.render(d.stack(),slot,matrices,vertices,light,overlay,e.getWorld());}}
  if((e.kind==DecorKind.ANIMAL_TROUGH||e.kind==DecorKind.QUENCHING_TUB)&&e.ternor.water>0){matrices.push();double step=e.kind==DecorKind.QUENCHING_TUB?.14:.075;matrices.translate(0,(e.ternor.water-1)*step,0);model.render("water",matrices,vertices,light,overlay,false);matrices.pop();}
 }
}
