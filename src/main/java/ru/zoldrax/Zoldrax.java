package ru.zoldrax;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.*;
import net.minecraft.registry.*;
import net.minecraft.util.Identifier;

public class Zoldrax implements ModInitializer {
 public static final MonumentBlock BLOCK = new MonumentBlock();
 public static BlockEntityType<MonumentEntity> ENTITY;
 public static final MonkeyBlock MONKEY_BLOCK=new MonkeyBlock();
 public static final GoldenBananaItem GOLDEN_BANANA=new GoldenBananaItem();
 public static BlockEntityType<MonkeyEntity> MONKEY_ENTITY;
 public static final TernorBlock TERNOR_BLOCK=new TernorBlock();
 public static BlockEntityType<TernorEntity> TERNOR_ENTITY;
 public static final net.minecraft.entity.EntityType<TernorGuardian> TERNOR_GUARDIAN=net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder.create(net.minecraft.entity.SpawnGroup.MISC,TernorGuardian::new).dimensions(net.minecraft.entity.EntityDimensions.fixed(1.4f,2.7f)).trackRangeChunks(10).build();
 public static final net.minecraft.entity.EntityType<CultistEntity> CHAIN_PUNISHER=cultistType(.85f,2.35f);
 public static final net.minecraft.entity.EntityType<CultistEntity> BLOOD_PRIEST=cultistType(.7f,2.2f);
 private static net.minecraft.entity.EntityType<CultistEntity> cultistType(float width,float height){return net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder.create(net.minecraft.entity.SpawnGroup.CREATURE,CultistEntity::new).dimensions(net.minecraft.entity.EntityDimensions.fixed(width,height)).trackRangeChunks(10).build();}
 public static final Item RITUAL_STAFF=new Item(new Item.Settings().maxCount(1).rarity(net.minecraft.util.Rarity.RARE));
 public static final net.minecraft.entity.EntityType<MonkeyFollowerEntity> BANANA_HERALD=monkeyFollower();
 public static final net.minecraft.entity.EntityType<MonkeyFollowerEntity> TEMPLE_GATHERER=monkeyFollower();
 private static net.minecraft.entity.EntityType<MonkeyFollowerEntity> monkeyFollower(){return net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder.create(net.minecraft.entity.SpawnGroup.CREATURE,MonkeyFollowerEntity::new).dimensions(net.minecraft.entity.EntityDimensions.fixed(.65f,1.95f)).trackRangeChunks(10).build();}
 public static final Item HERALD_HORN=new Item(new Item.Settings().maxCount(1));
 public static final Item HARVEST_MACHETE=new Item(new Item.Settings().maxCount(1));
 public void onInitialize() {
  Registry.register(Registries.BANNER_PATTERN,new Identifier("zoldrax","banana"),new net.minecraft.block.entity.BannerPattern("zbanana"));
  Registry.register(Registries.ITEM,new Identifier("zoldrax","herald_horn"),HERALD_HORN);
  Registry.register(Registries.ITEM,new Identifier("zoldrax","harvest_machete"),HARVEST_MACHETE);
  for(var type:java.util.List.of(BANANA_HERALD,TEMPLE_GATHERER)){
   boolean herald=type==BANANA_HERALD;String id=herald?"banana_herald":"temple_gatherer";
   Registry.register(Registries.ENTITY_TYPE,new Identifier("zoldrax",id),type);
   net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(type,net.minecraft.entity.mob.MobEntity.createMobAttributes().add(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH,herald?24:28).add(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MOVEMENT_SPEED,herald?.32:.25).add(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE,4).add(net.minecraft.entity.attribute.EntityAttributes.GENERIC_FOLLOW_RANGE,24));
   var egg=Registry.register(Registries.ITEM,new Identifier("zoldrax",id+"_spawn_egg"),new SpawnEggItem(type,herald?0xb52628:0xcaa36b,0xffcf31,new Item.Settings()));
   ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(e->e.add(egg));
  }
  ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(e->{e.add(HERALD_HORN);e.add(HARVEST_MACHETE);});
  Registry.register(Registries.BLOCK, new Identifier("zoldrax", "monument"), BLOCK);
  Registry.register(Registries.ITEM, new Identifier("zoldrax", "monument"), new BlockItem(BLOCK, new Item.Settings().maxCount(1)));
  ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier("zoldrax", "monument"), FabricBlockEntityTypeBuilder.create(MonumentEntity::new, BLOCK).build());
  Registry.register(Registries.BLOCK,new Identifier("zoldrax","monkey_monument"),MONKEY_BLOCK);
  Registry.register(Registries.ITEM,new Identifier("zoldrax","monkey_monument"),new BlockItem(MONKEY_BLOCK,new Item.Settings().maxCount(1)));
  Registry.register(Registries.ITEM,new Identifier("zoldrax","golden_banana"),GOLDEN_BANANA);
  MONKEY_ENTITY=Registry.register(Registries.BLOCK_ENTITY_TYPE,new Identifier("zoldrax","monkey_monument"),FabricBlockEntityTypeBuilder.create(MonkeyEntity::new,MONKEY_BLOCK).build());
  Registry.register(Registries.BLOCK,new Identifier("zoldrax","ternor_monument"),TERNOR_BLOCK);
  Registry.register(Registries.ITEM,new Identifier("zoldrax","ternor_monument"),new BlockItem(TERNOR_BLOCK,new Item.Settings().maxCount(1)));
  TERNOR_ENTITY=Registry.register(Registries.BLOCK_ENTITY_TYPE,new Identifier("zoldrax","ternor_monument"),FabricBlockEntityTypeBuilder.create(TernorEntity::new,TERNOR_BLOCK).build());
  Registry.register(Registries.ENTITY_TYPE,new Identifier("zoldrax","ternor_guardian"),TERNOR_GUARDIAN);
  net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(TERNOR_GUARDIAN,
   net.minecraft.entity.passive.IronGolemEntity.createIronGolemAttributes().add(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH,150).add(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE,22));
  Registry.register(Registries.ENTITY_TYPE,new Identifier("zoldrax","chain_punisher"),CHAIN_PUNISHER);
  Registry.register(Registries.ENTITY_TYPE,new Identifier("zoldrax","blood_priest"),BLOOD_PRIEST);
  Registry.register(Registries.ITEM,new Identifier("zoldrax","ritual_staff"),RITUAL_STAFF);
  var punisherEgg=Registry.register(Registries.ITEM,new Identifier("zoldrax","chain_punisher_spawn_egg"),new SpawnEggItem(CHAIN_PUNISHER,0x29232e,0xa22423,new Item.Settings()));
  var priestEgg=Registry.register(Registries.ITEM,new Identifier("zoldrax","blood_priest_spawn_egg"),new SpawnEggItem(BLOOD_PRIEST,0x261d26,0xba3928,new Item.Settings()));
  ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(e->{e.add(punisherEgg);e.add(priestEgg);});
  net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(CHAIN_PUNISHER,net.minecraft.entity.mob.MobEntity.createMobAttributes().add(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH,60).add(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MOVEMENT_SPEED,.23).add(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE,12).add(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ARMOR,14).add(net.minecraft.entity.attribute.EntityAttributes.GENERIC_FOLLOW_RANGE,24));
  net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(BLOOD_PRIEST,net.minecraft.entity.mob.MobEntity.createMobAttributes().add(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH,36).add(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MOVEMENT_SPEED,.25).add(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE,6).add(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ARMOR,4).add(net.minecraft.entity.attribute.EntityAttributes.GENERIC_FOLLOW_RANGE,24));
  ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK).register(e->e.add(GOLDEN_BANANA));
  net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register((entity,source)->{
   if(entity.getWorld() instanceof net.minecraft.server.world.ServerWorld sw){MonkeyRaidState.get(sw).died(entity.getUuid());if(entity instanceof net.minecraft.entity.player.PlayerEntity)MonkeyRaidState.get(sw).playerDied(entity.getUuid(),sw.getTime());}
  });
  ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(e -> {e.add(BLOCK);e.add(MONKEY_BLOCK);e.add(TERNOR_BLOCK);e.add(RITUAL_STAFF);});
 }
}
