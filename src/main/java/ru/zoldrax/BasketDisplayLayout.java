package ru.zoldrax;
import java.util.*;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
/** Deterministic compact layers, proportional to actual container fullness. No inventory mutation. */
public final class BasketDisplayLayout {
 public record Display(ItemStack stack,double x,double y,double z,float size){}
 public static double fullness(Inventory inventory){double sum=0;for(int i=0;i<inventory.size();i++){var s=inventory.getStack(i);if(!s.isEmpty())sum+=(double)s.getCount()/Math.min(inventory.getMaxCountPerStack(),s.getMaxCount());}return inventory.size()==0?0:Math.min(1,sum/inventory.size());}
 public static List<Display> create(Inventory inventory){
  return create(inventory,36);
 }
 public static List<Display> create(Inventory inventory,int maximumDisplays){
  int units=0;var stacks=new ArrayList<ItemStack>();for(int i=0;i<inventory.size();i++){var s=inventory.getStack(i);if(!s.isEmpty()){units+=s.getCount();stacks.add(s);}}
  if(units==0)return List.of();
  // Scale the visible heap with real fullness. Callers can request a denser surface for wide containers.
  int count=Math.min(units,Math.max(stacks.size(),(int)Math.ceil(fullness(inventory)*maximumDisplays)));
  var remaining=new int[stacks.size()];for(int i=0;i<remaining.length;i++)remaining[i]=stacks.get(i).getCount();
  var result=new ArrayList<Display>();int cursor=0;
  for(int j=0;j<count;j++){while(remaining[cursor]==0)cursor=(cursor+1)%stacks.size();var stack=stacks.get(cursor);remaining[cursor]--;cursor=(cursor+1)%stacks.size();int cell=j%9,layer=j/9;
   result.add(new Display(stack,.25+(cell%3)*.25,.14+layer*.22,.25+(cell/3)*.25,.275f));
  }return List.copyOf(result);
 }
}
