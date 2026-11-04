package ru.zoldrax;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.*;
import net.minecraft.client.render.entity.feature.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
public class MonkeyFollowerRenderer extends MobEntityRenderer<MonkeyFollowerEntity,MonkeyFollowerModel> {
 private final Identifier texture;
 public MonkeyFollowerRenderer(EntityRendererFactory.Context ctx,String variant){super(ctx,new MonkeyFollowerModel(variant),.55f);texture=new Identifier("zoldrax","textures/entity/"+variant+".png");addFeature(new Glow(this));addFeature(new HeldItemFeatureRenderer<>(this,ctx.getHeldItemRenderer()));}
 @Override public Identifier getTexture(MonkeyFollowerEntity entity){return texture;}
 private static class Glow extends FeatureRenderer<MonkeyFollowerEntity,MonkeyFollowerModel>{
  Glow(MonkeyFollowerRenderer context){super(context);}
  @Override public void render(MatrixStack matrices,VertexConsumerProvider vertices,int light,MonkeyFollowerEntity entity,float limbAngle,float limbDistance,float delta,float age,float yaw,float pitch){getContextModel().renderGlow(matrices,vertices,getTexture(entity),light);}
 }
}
