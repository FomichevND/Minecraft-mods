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
 public void onInitialize() {
  Registry.register(Registries.BLOCK, new Identifier("zoldrax", "monument"), BLOCK);
  Registry.register(Registries.ITEM, new Identifier("zoldrax", "monument"), new BlockItem(BLOCK, new Item.Settings().maxCount(1)));
  ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier("zoldrax", "monument"), FabricBlockEntityTypeBuilder.create(MonumentEntity::new, BLOCK).build());
  Registry.register(Registries.BLOCK,new Identifier("zoldrax","monkey_monument"),MONKEY_BLOCK);
  Registry.register(Registries.ITEM,new Identifier("zoldrax","monkey_monument"),new BlockItem(MONKEY_BLOCK,new Item.Settings().maxCount(1)));
  Registry.register(Registries.ITEM,new Identifier("zoldrax","golden_banana"),GOLDEN_BANANA);
  MONKEY_ENTITY=Registry.register(Registries.BLOCK_ENTITY_TYPE,new Identifier("zoldrax","monkey_monument"),FabricBlockEntityTypeBuilder.create(MonkeyEntity::new,MONKEY_BLOCK).build());
  ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK).register(e->e.add(GOLDEN_BANANA));
  net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register((entity,source)->{
   if(entity.getWorld() instanceof net.minecraft.server.world.ServerWorld sw)MonkeyRaidState.get(sw).died(entity.getUuid());
  });
  ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(e -> {e.add(BLOCK);e.add(MONKEY_BLOCK);});
 }
}
