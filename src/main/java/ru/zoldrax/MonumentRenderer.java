package ru.zoldrax;
import net.minecraft.client.model.*;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class MonumentRenderer implements BlockEntityRenderer<MonumentEntity> {
 private final ModelPart[] stone=new ModelPart[3];
 private final ModelPart red;
 private static final Identifier STONE=new Identifier("minecraft","textures/block/polished_blackstone_bricks.png");
 private static final Identifier RED=new Identifier("minecraft","textures/block/redstone_block.png");
 public MonumentRenderer(BlockEntityRendererFactory.Context context) {
  for(int stage=0;stage<3;stage++) {
   ModelData d=new ModelData();ModelPartData root=d.getRoot();
   cube(root,"base",-24,0,-24,48,5,48);
   cube(root,"step",-20,5,-21,40,4,40);
   cube(root,"back",-13,9,-17,26,stage==0?37:60,15);
   cube(root,"brazier",-13,9,9,26,5,14);
   cube(root,"leftRim",-15,9,7,4,12,18);
   cube(root,"rightRim",11,9,7,4,12,18);
   cube(root,"frontRim",-12,9,22,24,9,3);
   if(stage==0) { cube(root,"scaffoldL",-22,6,-10,3,43,3);cube(root,"scaffoldR",19,6,-10,3,43,3);cube(root,"scaffoldTop",-22,46,-10,44,3,3); }
   if(stage>=1) {
    cube(root,"head",-11,65,-15,22,17,18);
    cube(root,"snout",-6,64,0,12,9,6);
    cube(root,"shoulderL",-23,48,-15,12,13,20);cube(root,"shoulderR",11,48,-15,12,13,20);
    cube(root,"pillarL",-20,9,-9,7,39,10);cube(root,"pillarR",13,9,-9,7,39,10);
    cube(root,"hornL1",-21,75,-10,10,7,9);cube(root,"hornR1",11,75,-10,10,7,9);
    cube(root,"hornL2",-25,79,-10,6,11,7);cube(root,"hornR2",19,79,-10,6,11,7);
    if(stage==2) {
     cube(root,"hornL3",-23,90,-9,5,5,5);cube(root,"hornR3",18,90,-9,5,5,5);
     cube(root,"crown",-2,82,-9,4,12,6);
     for(int side=-1;side<=1;side+=2) for(int i=0;i<5;i++) cube(root,"chain"+side+i,side*21-1,23+i*5,1,2,3,2);
    }
   }
   stone[stage]=TexturedModelData.of(d,16,16).createModel();
  }
  ModelData d=new ModelData();ModelPartData root=d.getRoot();
  cube(root,"eyeL",-8,73,3.1f,5,3,1);cube(root,"eyeR",3,73,3.1f,5,3,1);
  cube(root,"rune",-1,34,-1.5f,2,21,1);cube(root,"runeCross",-7,42,-1.5f,14,2,1);
  cube(root,"embers",-10,13,10,20,2,12);
  cube(root,"bannerL",-20,20,2,6,22,1);cube(root,"bannerR",14,20,2,6,22,1);
  red=TexturedModelData.of(d,16,16).createModel();
 }
 private static void cube(ModelPartData root,String name,float x,float y,float z,float w,float h,float depth) {
  root.addChild(name,ModelPartBuilder.create().uv(0,0).cuboid(x,-y-h,z,w,h,depth),ModelTransform.NONE);
 }
 @Override public void render(MonumentEntity e,float delta,MatrixStack matrices,VertexConsumerProvider vertices,int light,int overlay) {
  matrices.push();matrices.translate(.5,0,.5);matrices.scale(1,-1,1);
  stone[e.stage].render(matrices,vertices.getBuffer(RenderLayer.getEntitySolid(STONE)),light,overlay);
  if(e.stage==2) red.render(matrices,vertices.getBuffer(RenderLayer.getEntitySolid(RED)),15728880,overlay);
  matrices.pop();
 }
 @Override public boolean rendersOutsideBoundingBox(MonumentEntity e) {return true;}
 @Override public int getRenderDistance() {return 128;}
}
