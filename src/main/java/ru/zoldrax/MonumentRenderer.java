package ru.zoldrax;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.*;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.world.LightType;

/** Renders the actual editable Blockbench files shipped in the resource pack. */
public class MonumentRenderer implements BlockEntityRenderer<MonumentEntity> {
 private final ModelPart[] body=new ModelPart[3],glow=new ModelPart[3];
 public MonumentRenderer(BlockEntityRendererFactory.Context context) {
  for(int stage=0;stage<3;stage++) {
   ModelData regular=new ModelData(),emissive=new ModelData();
   Identifier id=new Identifier("zoldrax","models/blockbench/stage_"+(stage+1)+".bbmodel");
   try(var reader=new InputStreamReader(MinecraftClient.getInstance().getResourceManager().getResource(id).orElseThrow().getInputStream(),StandardCharsets.UTF_8)) {
    JsonObject model=JsonParser.parseReader(reader).getAsJsonObject();
    for(JsonElement element:model.getAsJsonArray("elements")) {
     JsonObject cube=element.getAsJsonObject();String name=cube.get("name").getAsString();
     JsonArray from=cube.getAsJsonArray("from"),to=cube.getAsJsonArray("to"),uv=cube.getAsJsonArray("uv_offset");
     float x=from.get(0).getAsFloat(),y=from.get(1).getAsFloat(),z=from.get(2).getAsFloat();
     float w=to.get(0).getAsFloat()-x,h=to.get(1).getAsFloat()-y,d=to.get(2).getAsFloat()-z;
     ModelPartData root=(name.startsWith("glow_")?emissive:regular).getRoot();
     root.addChild(name,ModelPartBuilder.create().uv(uv.get(0).getAsInt(),uv.get(1).getAsInt()).cuboid(x,y,z,w,h,d),ModelTransform.NONE);
    }
    int width=model.getAsJsonObject("resolution").get("width").getAsInt();
    int height=model.getAsJsonObject("resolution").get("height").getAsInt();
    body[stage]=TexturedModelData.of(regular,width,height).createModel();
    glow[stage]=TexturedModelData.of(emissive,width,height).createModel();
   } catch(Exception e) {throw new IllegalStateException("Cannot load Blockbench model "+id,e);}
  }
 }
 @Override public void render(MonumentEntity e,float delta,MatrixStack matrices,VertexConsumerProvider vertices,int light,int overlay) {
  Identifier texture=new Identifier("zoldrax","textures/entity/stage_"+(e.stage+1)+".png");
  matrices.push();matrices.translate(.5,0,.5);
  // This shader has no view-relative entity diffuse lighting. Apply world ambient
  // brightness as vertex colour, so camera pitch cannot brighten/darken the altar.
  float ambient=.35f;
  if(e.getWorld()!=null) {
   var sample=e.getPos().up(3);
   int sky=Math.max(0,e.getWorld().getLightLevel(LightType.SKY,sample)-e.getWorld().getAmbientDarkness());
   int block=e.getWorld().getLightLevel(LightType.BLOCK,sample);
   ambient=.3f+.7f*Math.max(sky,block)/15f;
  }
  var layer=MonumentRenderLayer.get(texture);
  body[e.stage].render(matrices,vertices.getBuffer(layer),LightmapTextureManager.MAX_LIGHT_COORDINATE,overlay,ambient,ambient,ambient,1);
  glow[e.stage].render(matrices,vertices.getBuffer(layer),LightmapTextureManager.MAX_LIGHT_COORDINATE,overlay);
  matrices.pop();
 }
 @Override public boolean rendersOutsideBoundingBox(MonumentEntity e) {return true;}
 @Override public int getRenderDistance() {return 128;}
}
