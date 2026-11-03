package ru.zoldrax;
import net.minecraft.client.model.*;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.*;
import net.minecraft.client.render.entity.feature.*;
import net.minecraft.client.render.entity.model.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.util.Identifier;
public class TernorGuardianRenderer extends IronGolemEntityRenderer {
 public TernorGuardianRenderer(EntityRendererFactory.Context context){super(context);addFeature(new Wreath(this));}
 private static class Wreath extends FeatureRenderer<IronGolemEntity,IronGolemEntityModel<IronGolemEntity>> {
  private final ModelPart leaves,flowers;
  Wreath(FeatureRendererContext<IronGolemEntity,IronGolemEntityModel<IronGolemEntity>> context){
   super(context);var data=new ModelData();var root=data.getRoot();
   cube(root,"front",-5,-13,-6.5f,10,2,2);cube(root,"back",-5,-13,2,10,2,2);
   cube(root,"left",-5,-13,-5,2,2,8);cube(root,"right",3,-13,-5,2,2,8);
   leaves=TexturedModelData.of(data,64,64).createModel();data=new ModelData();root=data.getRoot();
   for(int x:new int[]{-4,0,3})cube(root,"flower"+x,x,-14,-7,2,2,1);
   flowers=TexturedModelData.of(data,64,64).createModel();
  }
  private void cube(ModelPartData root,String n,float x,float y,float z,float w,float h,float d){root.addChild(n,ModelPartBuilder.create().uv(0,0).cuboid(x,y,z,w,h,d),ModelTransform.NONE);}
  @Override public void render(MatrixStack matrices,VertexConsumerProvider vertices,int light,IronGolemEntity entity,float limbAngle,float limbDistance,float delta,float animation,float yaw,float pitch){
   matrices.push();getContextModel().getPart().getChild("head").rotate(matrices);
   leaves.render(matrices,vertices.getBuffer(RenderLayer.getEntityCutoutNoCull(new Identifier("zoldrax","textures/entity/guardian_wreath.png"))),light,OverlayTexture.DEFAULT_UV);
   flowers.render(matrices,vertices.getBuffer(RenderLayer.getEntityCutoutNoCull(new Identifier("zoldrax","textures/entity/guardian_flowers.png"))),light,OverlayTexture.DEFAULT_UV);
   matrices.pop();
  }
 }
}
