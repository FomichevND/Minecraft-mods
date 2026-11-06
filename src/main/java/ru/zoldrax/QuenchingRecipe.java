package ru.zoldrax;
import com.google.gson.*;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.*;
import net.minecraft.registry.*;
import net.minecraft.util.*;
import net.minecraft.world.World;
/** Data-driven, hot-only by default. The output is committed atomically by the tub. */
public final class QuenchingRecipe implements Recipe<Inventory> {
 public static final RecipeType<QuenchingRecipe> TYPE=new RecipeType<>(){};
 public static final RecipeSerializer<QuenchingRecipe> SERIALIZER=new RecipeSerializer<>(){
  public QuenchingRecipe read(Identifier id,JsonObject json){var ingredient=Ingredient.fromJson(json.get("ingredient"));var result=ShapedRecipe.outputFromJson(json.getAsJsonObject("result"));if(json.has("tempered")&&json.get("tempered").getAsBoolean())result.getOrCreateNbt().putBoolean("TernorTempered",true);return new QuenchingRecipe(id,ingredient,result,Math.max(1,Math.min(4,JsonHelper.getInt(json,"water",1))),JsonHelper.getBoolean(json,"require_hot",true));}
  public QuenchingRecipe read(Identifier id,PacketByteBuf buf){return new QuenchingRecipe(id,Ingredient.fromPacket(buf),buf.readItemStack(),buf.readVarInt(),buf.readBoolean());}
  public void write(PacketByteBuf buf,QuenchingRecipe recipe){recipe.ingredient.write(buf);buf.writeItemStack(recipe.output);buf.writeVarInt(recipe.water);buf.writeBoolean(recipe.requireHot);}
 };
 public static void register(){Registry.register(Registries.RECIPE_TYPE,DecorBlocks.id("quenching"),TYPE);Registry.register(Registries.RECIPE_SERIALIZER,DecorBlocks.id("quenching"),SERIALIZER);}
 private final Identifier id;private final Ingredient ingredient;private final ItemStack output;public final int water;private final boolean requireHot;
 public QuenchingRecipe(Identifier id,Ingredient ingredient,ItemStack output,int water,boolean requireHot){this.id=id;this.ingredient=ingredient;this.output=output;this.water=water;this.requireHot=requireHot;}
 public boolean matches(Inventory inventory,World world){var stack=inventory.getStack(0);return ingredient.test(stack)&&(!requireHot||stack.hasNbt()&&stack.getNbt().getLong("TernorHotUntil")>world.getTime());}
 public ItemStack craft(Inventory inventory,DynamicRegistryManager registries){return output.copy();}public ItemStack getOutput(DynamicRegistryManager registries){return output;}public boolean fits(int width,int height){return width*height>=1;}public Identifier getId(){return id;}public RecipeSerializer<?> getSerializer(){return SERIALIZER;}public RecipeType<?> getType(){return TYPE;}
}
