package ru.zoldrax;
import java.util.List;
import net.minecraft.item.*;
import net.minecraft.entity.effect.*;
import net.minecraft.text.Text;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.world.World;
import net.minecraft.util.Rarity;
public class GoldenBananaItem extends Item {
 public GoldenBananaItem(){super(new Item.Settings().maxCount(16).rarity(Rarity.EPIC).food(new FoodComponent.Builder()
  .hunger(6).saturationModifier(1).alwaysEdible()
  .statusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST,12000,255),1)
  .statusEffect(new StatusEffectInstance(StatusEffects.SATURATION,12000,0),1).build()));}
 @Override public void appendTooltip(ItemStack stack,World world,List<Text> tooltip,TooltipContext context){
  tooltip.add(Text.literal("Дар Короля обезьян: Прыгучесть 256 и Насыщение на 10 минут."));
  tooltip.add(Text.literal("Прыжок экстремально высокий. Падение остаётся опасным."));
 }
}
