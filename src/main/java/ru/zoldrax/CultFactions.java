package ru.zoldrax;
import net.minecraft.entity.*;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
/** Faction contract; the Ternor follower entity types will be implemented later. */
public final class CultFactions {
 public static final TagKey<EntityType<?>> TERNOR_FOLLOWERS=TagKey.of(RegistryKeys.ENTITY_TYPE,new Identifier("zoldrax","ternor_followers"));
 public static final TagKey<net.minecraft.item.Item> OFFERINGS=TagKey.of(RegistryKeys.ITEM,new Identifier("zoldrax","offerings"));
 public static boolean isEnemy(LivingEntity entity){return entity.isAlive()&&entity.getType().isIn(TERNOR_FOLLOWERS);}
 public static boolean isOffering(ItemStack stack){return stack.isIn(OFFERINGS);}
 private CultFactions(){}
}
