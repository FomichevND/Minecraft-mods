package ru.zoldrax;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.*;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.*;
import net.minecraft.util.math.BlockPos;

public class ConstructionTests implements FabricGameTest {
 private static final BlockPos ROOT=new BlockPos(3,2,3);
 private MonumentEntity altar(TestContext c) {
  c.setBlockState(ROOT,Zoldrax.BLOCK);
  return (MonumentEntity)c.getWorld().getBlockEntity(c.getAbsolutePos(ROOT));
 }
 private ChestBlockEntity chest(TestContext c,int x,int y,int z) {
  BlockPos p=ROOT.add(x,y,z);c.setBlockState(p,Blocks.CHEST);
  return (ChestBlockEntity)c.getWorld().getBlockEntity(c.getAbsolutePos(p));
 }
 private void fill(ChestBlockEntity inv,int obsidian,int stone,int redstone,Item kind) {
  inv.setStack(0,new ItemStack(Items.OBSIDIAN,obsidian));
  inv.setStack(1,new ItemStack(kind,stone));
  inv.setStack(2,new ItemStack(Items.REDSTONE,redstone));inv.markDirty();
 }
 private void check(boolean value,String message) {if(!value) throw new IllegalStateException(message);}
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=100)
 public void chestAtVisibleEdgeAndLowerLevel(TestContext c) {
  MonumentEntity m=altar(c);ChestBlockEntity inv=chest(c,-2,-1,1);
  fill(inv,32,24,8,Items.POLISHED_BLACKSTONE_BRICKS);
  c.waitAndRun(45,()->{
   check(m.stage==1,"Chest at visible edge and one block lower must advance construction automatically");
   check(inv.isEmpty(),"Exact requirements must be consumed");c.complete();
  });
 }
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=100)
 public void twoChestHalvesAndAlternativeStone(TestContext c) {
  MonumentEntity m=altar(c);
  ChestBlockEntity a=chest(c,2,0,0),b=chest(c,2,0,1);
  fill(a,16,12,4,Items.POLISHED_BLACKSTONE);fill(b,16,12,4,Items.BLACKSTONE);
  c.waitAndRun(45,()->{check(m.stage==1,"Both chest halves and mixed blackstone variants must count");check(a.isEmpty()&&b.isEmpty(),"Both inventories consumed once");c.complete();});
 }
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=130)
 public void missingMaterialDoesNotConsumeAnything(TestContext c) {
  MonumentEntity m=altar(c);ChestBlockEntity inv=chest(c,2,0,0);
  fill(inv,32,24,7,Items.POLISHED_BLACKSTONE_BRICKS);
  c.waitAndRun(40,()->{
   check(m.stage==0,"Incomplete set must not build");
   check(inv.getStack(0).getCount()==32&&inv.getStack(2).getCount()==7,"Incomplete set must not be consumed");
   check(m.status().getString().contains("не хватает 1"),"Status must explain missing resource");
   inv.setStack(2,new ItemStack(Items.REDSTONE,8));inv.markDirty();
  });
  c.waitAndRun(85,()->{check(m.stage==1,"Adding final resource must start building");c.complete();});
 }
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=130)
 public void bothStagesAndPersistence(TestContext c) {
  MonumentEntity m=altar(c);ChestBlockEntity inv=chest(c,0,0,-2);
  fill(inv,63,40,20,Items.POLISHED_BLACKSTONE_BRICKS);
  inv.setStack(3,new ItemStack(Items.REDSTONE_BLOCK,4));
  c.waitAndRun(85,()->{
   check(m.stage==2,"Both building stages must complete");
   check(inv.getStack(0).getCount()==7,"Extra items must be retained");
   check(inv.getStack(1).isEmpty()&&inv.getStack(2).isEmpty()&&inv.getStack(3).isEmpty(),"Stage costs must be exact");
   MonumentEntity restored=new MonumentEntity(m.getPos(),Zoldrax.BLOCK.getDefaultState());
   restored.readNbt(m.createNbt());check(restored.stage==2,"Completed stage must survive NBT roundtrip");c.complete();
  });
 }
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=100)
 public void distantChestIsNotConsumed(TestContext c) {
  MonumentEntity m=altar(c);ChestBlockEntity inv=chest(c,3,0,2);
  fill(inv,32,24,8,Items.POLISHED_BLACKSTONE_BRICKS);
  c.waitAndRun(45,()->{check(m.stage==0,"Out of range chest must be ignored");check(inv.getStack(0).getCount()==32,"Out of range inventory must be preserved");c.complete();});
 }
}
