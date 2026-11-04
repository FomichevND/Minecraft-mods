package ru.zoldrax;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.*;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
/** Shared loader for editable, bone-assigned Blockbench followers. */
public class MonkeyFollowerModel extends SinglePartEntityModel<MonkeyFollowerEntity> implements net.minecraft.client.render.entity.model.ModelWithArms {
 private final ModelPart root,glow;
 private final boolean priest;
 private static final String[] BONES={"head","body","rightArm","leftArm","rightLeg","leftLeg","cape"};
 public MonkeyFollowerModel(String variant){
  priest=variant.equals("banana_herald");var mainData=new ModelData();var glowData=new ModelData();
  try(var reader=new InputStreamReader(MinecraftClient.getInstance().getResourceManager().getResource(new Identifier("zoldrax","models/blockbench/"+variant+".bbmodel")).orElseThrow().getInputStream(),StandardCharsets.UTF_8)){
   var model=JsonParser.parseReader(reader).getAsJsonObject();var pivots=model.getAsJsonObject("zoldrax_bones");
   Map<String,ModelPartData> bodies=new HashMap<>(),lights=new HashMap<>();
   for(String bone:BONES){var p=pivots.getAsJsonArray(bone);var transform=ModelTransform.pivot(p.get(0).getAsFloat(),24-p.get(1).getAsFloat(),-p.get(2).getAsFloat());bodies.put(bone,mainData.getRoot().addChild(bone,ModelPartBuilder.create(),transform));lights.put(bone,glowData.getRoot().addChild(bone,ModelPartBuilder.create(),transform));}
   for(var el:model.getAsJsonArray("elements")){
    var c=el.getAsJsonObject();String name=c.get("name").getAsString(),bone=c.get("zoldrax_bone").getAsString();var p=pivots.getAsJsonArray(bone);var f=c.getAsJsonArray("from");var t=c.getAsJsonArray("to");var uv=c.getAsJsonArray("uv_offset");
    float x=f.get(0).getAsFloat(),y=f.get(1).getAsFloat(),z=f.get(2).getAsFloat(),w=t.get(0).getAsFloat()-x,h=t.get(1).getAsFloat()-y,d=t.get(2).getAsFloat()-z;
    (name.startsWith("glow_")?lights:bodies).get(bone).addChild(name,ModelPartBuilder.create().uv(uv.get(0).getAsInt(),uv.get(1).getAsInt()).cuboid(x-p.get(0).getAsFloat(),p.get(1).getAsFloat()-y-h,p.get(2).getAsFloat()-z-d,w,h,d,new Dilation(c.has("inflate")?c.get("inflate").getAsFloat():0)),ModelTransform.NONE);
   }
   int width=model.getAsJsonObject("resolution").get("width").getAsInt(),height=model.getAsJsonObject("resolution").get("height").getAsInt();
   root=TexturedModelData.of(mainData,width,height).createModel();glow=TexturedModelData.of(glowData,width,height).createModel();
  }catch(Exception e){throw new IllegalStateException("Cannot load cultist "+variant,e);}
 }
 @Override public ModelPart getPart(){return root;}
 @Override public void setAngles(MonkeyFollowerEntity entity,float limbAngle,float limbDistance,float age,float headYaw,float headPitch){
  for(var tree:List.of(root,glow)){
   tree.traverse().forEach(ModelPart::resetTransform);
   tree.getChild("head").yaw=headYaw*MathHelper.RADIANS_PER_DEGREE;tree.getChild("head").pitch=headPitch*MathHelper.RADIANS_PER_DEGREE;
   tree.getChild("rightLeg").pitch=MathHelper.cos(limbAngle*.6662f)*.85f*limbDistance;tree.getChild("leftLeg").pitch=MathHelper.cos(limbAngle*.6662f+(float)Math.PI)*.85f*limbDistance;
   tree.getChild("rightArm").pitch=MathHelper.cos(limbAngle*.6662f+(float)Math.PI)*(priest?.13f:.5f)*limbDistance;
   tree.getChild("leftArm").pitch=MathHelper.cos(limbAngle*.6662f)*.5f*limbDistance;
   tree.getChild("rightArm").pitch-=MathHelper.sin(handSwingProgress*(float)Math.PI)*1.1f;
   tree.getChild("cape").pitch=.06f+limbDistance*.22f+MathHelper.sin(age*.07f)*.02f;
  }
 }
 @Override public void setArmAngle(net.minecraft.util.Arm arm,MatrixStack matrices){root.getChild(arm==net.minecraft.util.Arm.RIGHT?"rightArm":"leftArm").rotate(matrices);}
 public void renderGlow(MatrixStack matrices,VertexConsumerProvider vertices,Identifier texture,int light){glow.render(matrices,vertices.getBuffer(MonumentRenderLayer.get(texture)),LightmapTextureManager.MAX_LIGHT_COORDINATE,OverlayTexture.DEFAULT_UV);}
}
