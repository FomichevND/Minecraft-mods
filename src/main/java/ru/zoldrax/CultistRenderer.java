package ru.zoldrax;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.*;
import net.minecraft.client.render.entity.feature.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
public class CultistRenderer extends MobEntityRenderer<CultistEntity,CultistModel> {
 private final Identifier texture;
 public CultistRenderer(EntityRendererFactory.Context ctx,String variant){super(ctx,new CultistModel(variant),.55f);texture=new Identifier("zoldrax","textures/entity/"+variant+".png");addFeature(new Glow(this));addFeature(new HeldItemFeatureRenderer<>(this,ctx.getHeldItemRenderer()));}
 @Override public Identifier getTexture(CultistEntity entity){return texture;}
 private static class Glow extends FeatureRenderer<CultistEntity,CultistModel>{
  Glow(CultistRenderer context){super(context);}
  @Override public void render(MatrixStack matrices,VertexConsumerProvider vertices,int light,CultistEntity entity,float limbAngle,float limbDistance,float delta,float age,float yaw,float pitch){getContextModel().renderGlow(matrices,vertices,getTexture(entity),light);}
 }
}
