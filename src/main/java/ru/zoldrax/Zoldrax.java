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
 public void onInitialize() {
  Registry.register(Registries.BLOCK, new Identifier("zoldrax", "monument"), BLOCK);
  Registry.register(Registries.ITEM, new Identifier("zoldrax", "monument"), new BlockItem(BLOCK, new Item.Settings().maxCount(1)));
  ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier("zoldrax", "monument"), FabricBlockEntityTypeBuilder.create(MonumentEntity::new, BLOCK).build());
  ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(e -> e.add(BLOCK));
 }
}
