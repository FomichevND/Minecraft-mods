package ru.zoldrax;
import java.util.EnumSet;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ItemEntity;
/** Real navigation, pickup and throwing, rather than giving the altar free resources. */
public class CultOfferingGoal extends Goal {
 private final CultistEntity npc;
 private ItemEntity item;
 private long nextAttempt;
 public CultOfferingGoal(CultistEntity npc){this.npc=npc;setControls(EnumSet.of(Control.MOVE,Control.LOOK));}
 @Override public boolean canStart(){
  if(!npc.isPriest()||npc.getTarget()!=null||npc.getWorld().getTime()<nextAttempt)return false;
  nextAttempt=npc.getWorld().getTime()+20;
  if(npc.altar()==null)return false;
  if(npc.hasOffering())return true;
  item=npc.findOffering();return item!=null;
 }
 @Override public boolean shouldContinue(){return npc.getTarget()==null&&npc.altar()!=null&&(npc.hasOffering()||(item!=null&&item.isAlive())) ;}
 @Override public void tick(){
  var altar=npc.altar();if(altar==null)return;
  if(!npc.hasOffering()){
   if(item==null||!item.isAlive())return;
   npc.getLookControl().lookAt(item,30,30);npc.getNavigation().startMovingTo(item,1);
   if(npc.squaredDistanceTo(item)<2.25)npc.collectOffering(item);
  }else{
   var pos=altar.getPos();var point=altar.altarPoint(0,0,3.2);double x=point.x,z=point.z;
   npc.getNavigation().startMovingTo(x,pos.getY(),z,.85);var flame=altar.altarPoint(0,1,1.55);npc.getLookControl().lookAt(flame.x,flame.y,flame.z,30,30);
   if(npc.squaredDistanceTo(x,pos.getY(),z)<2.5)npc.throwOffering();
  }
 }
 @Override public void stop(){npc.getNavigation().stop();item=null;nextAttempt=npc.getWorld().getTime()+40;}
}
