package ru.zoldrax;
import java.util.*;
import net.minecraft.item.*;
/** Shared immutable local transforms, also usable by NPC interaction code. */
public final class FoodDisplaySlots {
 public record Slot(double x,double y,double z,float rotation,float maxSize,boolean hanging){}
 public static boolean acceptsFood(ItemStack stack){return stack.isFood()||stack.isOf(Items.CAKE);}
 public static List<Slot> forKind(DecorKind kind){var slots=new ArrayList<Slot>();
  switch(kind){
   case FOOD_SHELF->{for(int i=0;i<6;i++)slots.add(new Slot(.6+i*.36,.75,.58,0,.36f,false));}
   case PREPARATION_TABLE->{for(int i=0;i<4;i++)slots.add(new Slot(.55+i*.5,(i==0||i==3?17:17.7)/16,.58,0,.43f,false));}
   case SUPPLY_BASKET->{for(int i=0;i<9;i++)slots.add(new Slot(.27+(i%3)*.23,.15+(i/3)*.22,.3+(i%2)*.3,0,.24f,false));}
   case DRYING_RACK->{for(int i=0;i<5;i++)slots.add(new Slot((7.5+i*8)/16,22/16.0,.58,-90,.38f,true));}
   default->{}
  }return List.copyOf(slots);
 }
}
