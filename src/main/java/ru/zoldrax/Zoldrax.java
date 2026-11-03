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
 public static final net.minecraft.entity.EntityType<TernorGuardian> TERNOR_GUARDIAN=net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder.create(net.minecraft.entity.SpawnGroup.MISC,TernorGuardian::new).dimensions(net.minecraft.entity.EntityDimensions.fixed(1.4f,2.7f)).trackRangeBlocks(12).build();
 public void onInitialize() {
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
  ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK).register(e->e.add(GOLDEN_BANANA));
  net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register((entity,source)->{
   if(entity.getWorld() instanceof net.minecraft.server.world.ServerWorld sw){MonkeyRaidState.get(sw).died(entity.getUuid());if(entity instanceof net.minecraft.entity.player.PlayerEntity)MonkeyRaidState.get(sw).playerDied(entity.getUuid(),sw.getTime());}
  });
  ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(e -> {e.add(BLOCK);e.add(MONKEY_BLOCK);e.add(TERNOR_BLOCK);});
 }
}
