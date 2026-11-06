package ru.zoldrax;

import java.util.*;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.*;
import net.minecraft.item.*;
import net.minecraft.registry.*;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.*;
import net.minecraft.world.World;

/** Eight visible items; companion blocks are technical, never independent loot. */
public final class DecorBlocks {
 public static final EnumMap<DecorKind,DecorBlock> BLOCKS=new EnumMap<>(DecorKind.class);
 public static final EnumMap<DecorKind,DecorBaseBlock> PARTS=new EnumMap<>(DecorKind.class);
 public static BlockEntityType<DecorBlockEntity> ENTITY;
 public static final EntityType<DecorSeatEntity> SEAT=FabricEntityTypeBuilder.create(SpawnGroup.MISC,DecorSeatEntity::new).dimensions(EntityDimensions.fixed(.01f,.01f)).trackRangeChunks(6).trackedUpdateRate(1).build();
 public static final TagKey<Item> WEAPONS=TagKey.of(RegistryKeys.ITEM,id("rack_weapons"));
 public static final TagKey<Item> BOOKS=TagKey.of(RegistryKeys.ITEM,id("lectern_books"));
 public static final TagKey<Item> GEMS=TagKey.of(RegistryKeys.ITEM,id("pedestal_gems"));
 public static final SoundEvent CHEST_OPEN=SoundEvent.of(id("decor_chest_open")),CHEST_CLOSE=SoundEvent.of(id("decor_chest_close"));
 public static final RegistryKey<ItemGroup> GROUP=RegistryKey.of(RegistryKeys.ITEM_GROUP,id("cult_decor"));
 private static final Map<World,Set<BlockPos>> CHANGING=new WeakHashMap<>();
 private static final Map<String,VoxelShape> SHAPES=new java.util.concurrent.ConcurrentHashMap<>();
 public static Identifier id(String path){return new Identifier("zoldrax",path);}
 public static void register(){
  QuenchingRecipe.register();
  for(var kind:DecorKind.values()){
   var block=Registry.register(Registries.BLOCK,id(kind.id),new DecorBlock(kind));BLOCKS.put(kind,block);
   PARTS.put(kind,Registry.register(Registries.BLOCK,id(kind.id+"_part"),new DecorBaseBlock(kind)));
   Registry.register(Registries.ITEM,id(kind.id),new BlockItem(block,new Item.Settings()));
  }
  ENTITY=Registry.register(Registries.BLOCK_ENTITY_TYPE,id("cult_decor"),FabricBlockEntityTypeBuilder.create(DecorBlockEntity::new,BLOCKS.values().toArray(Block[]::new)).build());
  Registry.register(Registries.ENTITY_TYPE,id("decor_seat"),SEAT);
  Registry.register(Registries.SOUND_EVENT,CHEST_OPEN.getId(),CHEST_OPEN);Registry.register(Registries.SOUND_EVENT,CHEST_CLOSE.getId(),CHEST_CLOSE);
  Registry.register(Registries.ITEM_GROUP,GROUP,FabricItemGroup.builder().displayName(Text.translatable("itemGroup.zoldrax.cult_decor")).icon(()->new ItemStack(BLOCKS.get(DecorKind.THRONE))).entries((context,entries)->{for(var kind:DecorKind.values())if(!kind.monkey()&&!kind.ternor())entries.add(BLOCKS.get(kind));}).build());
  Registry.register(Registries.ITEM_GROUP,RegistryKey.of(RegistryKeys.ITEM_GROUP,id("monkey_decor")),FabricItemGroup.builder().displayName(Text.translatable("itemGroup.zoldrax.monkey_decor")).icon(()->new ItemStack(BLOCKS.get(DecorKind.FOOD_SHELF))).entries((context,entries)->{for(var kind:DecorKind.values())if(kind.monkey())entries.add(BLOCKS.get(kind));}).build());
  Registry.register(Registries.ITEM_GROUP,RegistryKey.of(RegistryKeys.ITEM_GROUP,id("ternor_decor")),FabricItemGroup.builder().displayName(Text.translatable("itemGroup.zoldrax.ternor_decor")).icon(()->new ItemStack(BLOCKS.get(DecorKind.STONE_FORGE))).entries((context,entries)->{for(var kind:DecorKind.values())if(kind.ternor())entries.add(BLOCKS.get(kind));}).build());
  // Vanilla bypasses block use while sneaking with an item. These retrieval actions
  // must still work with a nonempty hand, without swapping a player's hand stack.
  UseBlockCallback.EVENT.register((player,world,hand,hit)->{
   var state=world.getBlockState(hit.getBlockPos());
   if(player.isSneaking()&&state.getBlock() instanceof DecorBaseBlock block&&(block.kind==DecorKind.LECTERN||block.kind==DecorKind.OFFERING_PEDESTAL||block.kind==DecorKind.CHAIN_LANTERN||block.kind==DecorKind.THRONE||block.kind.monkey()||block.kind.ternor()))return block.onUse(state,world,hit.getBlockPos(),player,hand,hit);
   return ActionResult.PASS;
  });
 }
 public static BlockRotation rotation(Direction facing){return switch(facing){case WEST->BlockRotation.CLOCKWISE_90;case NORTH->BlockRotation.CLOCKWISE_180;case EAST->BlockRotation.COUNTERCLOCKWISE_90;default->BlockRotation.NONE;};}
 public static int degrees(Direction facing){return switch(facing){case WEST->90;case NORTH->180;case EAST->270;default->0;};}
 public static BlockPos offset(DecorKind kind,int part,Direction facing){return new BlockPos(part%kind.width,(kind.hanging()?-1:1)*(part/(kind.width*kind.depth())),(part/kind.width)%kind.depth()).rotate(rotation(facing));}
 public static BlockPos root(BlockPos pos,BlockState state){var block=(DecorBaseBlock)state.getBlock();return pos.subtract(offset(block.kind,state.get(DecorBaseBlock.PART),state.get(DecorBaseBlock.FACING)));}
 public static BlockState partState(DecorKind kind,BlockState master,int part){return PARTS.get(kind).getDefaultState().with(DecorBaseBlock.PART,part).with(DecorBaseBlock.FACING,master.get(DecorBaseBlock.FACING)).with(DecorBaseBlock.LIT,master.get(DecorBaseBlock.LIT)).with(DecorBaseBlock.LENGTH,master.get(DecorBaseBlock.LENGTH));}
 public static Vec3d point(BlockPos root,Direction facing,double x,double y,double z){
  double a=Math.toRadians(degrees(facing)),xx=x-.5,zz=z-.5;
  return new Vec3d(root.getX()+.5+xx*Math.cos(a)-zz*Math.sin(a),root.getY()+y,root.getZ()+.5+xx*Math.sin(a)+zz*Math.cos(a));
 }
 public static boolean changing(World world,BlockPos root){return CHANGING.getOrDefault(world,Set.of()).contains(root);}
 public static void beginChange(World world,BlockPos root){CHANGING.computeIfAbsent(world,w->new HashSet<>()).add(root.toImmutable());}
 public static void endChange(World world,BlockPos root){var set=CHANGING.get(world);if(set!=null){set.remove(root);if(set.isEmpty())CHANGING.remove(world);}}
 public static DecorBlockEntity entity(World world,BlockPos pos,BlockState state){return world.getBlockEntity(root(pos,state)) instanceof DecorBlockEntity entity?entity:null;}
 public static VoxelShape shape(DecorKind kind,BlockState state,boolean collision){
  String key=kind.ordinal()+":"+state.get(DecorBaseBlock.PART)+":"+state.get(DecorBaseBlock.FACING)+":"+state.get(DecorBaseBlock.LENGTH)+":"+collision;
  return SHAPES.computeIfAbsent(key,k->{
   int part=state.get(DecorBaseBlock.PART),length=state.get(DecorBaseBlock.LENGTH);var facing=state.get(DecorBaseBlock.FACING);var offset=offset(kind,part,facing);
   VoxelShape result=VoxelShapes.empty();
   var boxes=new ArrayList<double[]>();
   if(kind==DecorKind.CHAIN_LANTERN){
    for(var b:collision?DecorGeometry.LANTERN_STATIC_COLLISION:DecorGeometry.LANTERN_STATIC)boxes.add(b);
    for(var b:collision?DecorGeometry.LANTERN_CAGE_COLLISION:DecorGeometry.LANTERN_CAGE){var c=b.clone();c[1]-=16*(length-1);c[4]-=16*(length-1);boxes.add(c);}
    if(!collision&&length==2)for(var b:DecorGeometry.LANTERN_EXTENSION)boxes.add(b);
   }else Collections.addAll(boxes,kind.ternor()?(collision?TernorDecorGeometry.COLLISION[kind.ordinal()-16]:TernorDecorGeometry.OUTLINE[kind.ordinal()-16]):kind.monkey()?(collision?MonkeyDecorGeometry.COLLISION[kind.ordinal()-8]:MonkeyDecorGeometry.OUTLINE[kind.ordinal()-8]):(collision?DecorGeometry.COLLISION[kind.ordinal()]:DecorGeometry.OUTLINE[kind.ordinal()]));
   for(var b:boxes){double minX=99,minZ=99,maxX=-99,maxZ=-99;
    for(double x:new double[]{b[0]/16,b[3]/16})for(double z:new double[]{b[2]/16,b[5]/16}){var p=point(BlockPos.ORIGIN,facing,x,0,z);minX=Math.min(minX,p.x);maxX=Math.max(maxX,p.x);minZ=Math.min(minZ,p.z);maxZ=Math.max(maxZ,p.z);}
    double x0=Math.max(0,minX-offset.getX()),x1=Math.min(1,maxX-offset.getX()),y0=Math.max(0,b[1]/16-offset.getY()),y1=Math.min(1,b[4]/16-offset.getY()),z0=Math.max(0,minZ-offset.getZ()),z1=Math.min(1,maxZ-offset.getZ());
    if(x1>x0&&y1>y0&&z1>z0)result=VoxelShapes.union(result,VoxelShapes.cuboid(x0,y0,z0,x1,y1,z1));
   }
   return result.simplify();
  });
 }
 private DecorBlocks(){}
}
