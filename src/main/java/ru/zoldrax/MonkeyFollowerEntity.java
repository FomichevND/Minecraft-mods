package ru.zoldrax;

import java.util.*;
import net.minecraft.block.*;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;

/** Persistent neutral workers. Resources are physically collected and carried, never created. */
public class MonkeyFollowerEntity extends PathAwareEntity {
 final SimpleInventory cargo=new SimpleInventory(9);
 private BlockPos shrine;
 private long nextSearch,nextSpeech,nextTrade,lastOrder=-1;
 public MonkeyFollowerEntity(EntityType<? extends PathAwareEntity> type,World world){super(type,world);setPersistent();if(getNavigation() instanceof net.minecraft.entity.ai.pathing.MobNavigation n)n.setCanPathThroughDoors(true);}
 public boolean isHerald(){return getType()==Zoldrax.BANANA_HERALD;}
 public static boolean food(ItemStack stack){return stack.isFood()&&!stack.isOf(Zoldrax.GOLDEN_BANANA);}
 @Override protected void initGoals(){
  goalSelector.add(0,new SwimGoal(this));goalSelector.add(1,new MeleeAttackGoal(this,1.1,false));
  goalSelector.add(2,new LongDoorInteractGoal(this,true));goalSelector.add(3,new MonkeyFoodGoal(this));
  goalSelector.add(4,new WanderAroundFarGoal(this,.7));goalSelector.add(5,new LookAtEntityGoal(this,PlayerEntity.class,8));goalSelector.add(6,new LookAroundGoal(this));targetSelector.add(1,new RevengeGoal(this));
 }
 @Override public void setTarget(LivingEntity target){super.setTarget(target instanceof MonkeyFollowerEntity?null:target);}
 public MonkeyEntity altar(){
  if(!(getWorld() instanceof ServerWorld sw))return null;
  if(shrine!=null){if(!sw.isChunkLoaded(shrine))return null;if(sw.getBlockEntity(shrine) instanceof MonkeyEntity m&&m.stage==2)return m;shrine=null;clearPositionTarget();}
  if(sw.getTime()<nextSearch)return null;nextSearch=sw.getTime()+100;
  MonkeyEntity best=null;double closest=2304;int cx=getBlockX()>>4,cz=getBlockZ()>>4;
  for(int x=cx-3;x<=cx+3;x++)for(int z=cz-3;z<=cz+3;z++){
   var chunk=sw.getChunkManager().getWorldChunk(x,z);if(chunk==null)continue;
   for(var be:chunk.getBlockEntities().values())if(be instanceof MonkeyEntity m&&m.stage==2){double distance=squaredDistanceTo(Vec3d.ofCenter(m.getPos()));if(distance<closest){best=m;closest=distance;}}
  }
  if(best!=null){shrine=best.getPos().toImmutable();setPositionTarget(shrine,22);}return best;
 }
 List<ChestBlockEntity> chests(){
  var out=new ArrayList<ChestBlockEntity>();if(!(getWorld() instanceof ServerWorld sw))return out;
  int cx=getBlockX()>>4,cz=getBlockZ()>>4;
  for(int x=cx-2;x<=cx+2;x++)for(int z=cz-2;z<=cz+2;z++){
   var chunk=sw.getChunkManager().getWorldChunk(x,z);if(chunk==null)continue;
   for(var be:chunk.getBlockEntities().values())if(be instanceof ChestBlockEntity chest&&squaredDistanceTo(Vec3d.ofCenter(chest.getPos()))<1600)out.add(chest);
  }
  out.sort(Comparator.comparingDouble(chest->squaredDistanceTo(Vec3d.ofCenter(chest.getPos()))));return out;
 }
 boolean carrying(){return !cargo.isEmpty();}
 int room(ItemStack stack){int n=0;for(int i=0;i<cargo.size();i++){var s=cargo.getStack(i);if(s.isEmpty())n+=stack.getMaxCount();else if(ItemStack.canCombine(s,stack))n+=s.getMaxCount()-s.getCount();}return n;}
 boolean canDeliver(ChestBlockEntity chest){
  for(int i=0;i<cargo.size();i++)if(!cargo.getStack(i).isEmpty())for(int j=0;j<chest.size();j++){var a=cargo.getStack(i);var b=chest.getStack(j);if(b.isEmpty()||(ItemStack.canCombine(a,b)&&b.getCount()<b.getMaxCount()))return true;}
  return false;
 }
 void deliver(ChestBlockEntity chest){
  for(int i=0;i<cargo.size();i++){
   var held=cargo.getStack(i);if(held.isEmpty())continue;
   for(int j=0;j<chest.size()&&!held.isEmpty();j++){
    var dst=chest.getStack(j);if(dst.isEmpty()){chest.setStack(j,held.copy());held.setCount(0);}
    else if(ItemStack.canCombine(dst,held)){int count=Math.min(held.getCount(),dst.getMaxCount()-dst.getCount());dst.increment(count);held.decrement(count);}
   }
  }cargo.markDirty();chest.markDirty();swingHand(Hand.OFF_HAND);
 }
 private void announce(MonkeyEntity altar){
  long now=getWorld().getTime();if(now<nextSpeech&&(!altar.hasRequest()||lastOrder==altar.requestDeadline()))return;
  // One spokesman per loaded court prevents three heralds repeating each order.
  if(!getWorld().getEntitiesByClass(MonkeyFollowerEntity.class,getBoundingBox().expand(32),e->e.isHerald()&&e.getId()<getId()&&Objects.equals(e.shrine,shrine)).isEmpty())return;
  Text text=null;
  if(altar.hasRequest()&&lastOrder!=altar.requestDeadline()){lastOrder=altar.requestDeadline();text=Text.literal("Слушайте, слушайте! ").append(altar.status());}
  else if(now>=nextSpeech){String[] jokes={"Его Банановое Величество объявляет: завтрак был разминкой!","Король так велик, что его обеду нужен отдельный амбар!","Срочная новость: бананы закончились. Несите всё, что не убежало!","Да здравствует король! И да поспеет его второй ужин!"};text=Text.literal(jokes[random.nextInt(jokes.length)]);}
  if(text!=null){for(var p:((ServerWorld)getWorld()).getPlayers(p->p.squaredDistanceTo(getPos())<576))p.sendMessage(Text.literal("[Банановый глашатай] ").append(text),false);playSound(SoundEvents.GOAT_HORN_SOUNDS.get(0).value(),.6f,1.25f);swingHand(Hand.MAIN_HAND);}
  nextSpeech=now+900+random.nextInt(901);
 }
 @Override public void tick(){super.tick();if(getWorld().isClient)return;
  if(getMainHandStack().isEmpty()){equipStack(EquipmentSlot.MAINHAND,new ItemStack(isHerald()?Zoldrax.HERALD_HORN:Zoldrax.HARVEST_MACHETE));setEquipmentDropChance(EquipmentSlot.MAINHAND,0);}
  if(age%20==0){var altar=altar();if(altar!=null&&isHerald())announce(altar);if(altar!=null&&squaredDistanceTo(Vec3d.ofCenter(altar.getPos()))>900&&getTarget()==null)getNavigation().startMovingTo(altar.getPos().getX()+.5,altar.getPos().getY(),altar.getPos().getZ()+4,1);}
  if(isHerald()&&age%140==0&&isOnGround()&&getTarget()==null&&!isTouchingWater())getJumpControl().setActive();
 }
 @Override protected ActionResult interactMob(PlayerEntity player,Hand hand){
  if(getWorld().isClient)return ActionResult.SUCCESS;
  var altar=altar();var held=player.getStackInHand(hand);
  if(!isHerald()&&food(held)){
   if(getWorld().getTime()<nextTrade)player.sendMessage(Text.literal("[Храмовый добытчик] Казна отдыхает. Загляни через минутку!"),true);
   else if(held.getCount()<8||room(held)<8)player.sendMessage(Text.literal("[Храмовый добытчик] Дай 8 одинаковых продуктов. И оставь мне место в корзине!"),true);
   else{cargo.addStack(held.copyWithCount(8));held.decrement(8);player.giveItemStack(new ItemStack(Items.GOLD_NUGGET));nextTrade=getWorld().getTime()+1200;player.sendMessage(Text.literal("[Храмовый добытчик] Восемь угощений — золотой самородок. Король одобряет!"),false);}
  }else player.sendMessage(isHerald()&&altar!=null?Text.literal("[Банановый глашатай] ").append(altar.status()):Text.literal(isHerald()?"[Банановый глашатай] Где наше Банановое Величество? Позови меня к готовому монументу!":"[Храмовый добытчик] Собираю урожай и припасы для короля. Обменяю 8 продуктов на золотой самородок."),false);
  return ActionResult.SUCCESS;
 }
 @Override public void writeCustomDataToNbt(NbtCompound n){super.writeCustomDataToNbt(n);if(shrine!=null)n.put("MonkeyShrine",NbtHelper.fromBlockPos(shrine));var list=new NbtList();for(int i=0;i<cargo.size();i++)if(!cargo.getStack(i).isEmpty()){var entry=cargo.getStack(i).writeNbt(new NbtCompound());entry.putInt("Slot",i);list.add(entry);}n.put("MonkeyCargo",list);n.putLong("NextTrade",nextTrade);n.putLong("NextSpeech",nextSpeech);n.putLong("LastOrder",lastOrder);}
 @Override public void readCustomDataFromNbt(NbtCompound n){super.readCustomDataFromNbt(n);cargo.clear();for(var el:n.getList("MonkeyCargo",10)){var e=(NbtCompound)el;int slot=e.getInt("Slot");if(slot>=0&&slot<cargo.size())cargo.setStack(slot,ItemStack.fromNbt(e));}shrine=n.contains("MonkeyShrine")?NbtHelper.toBlockPos(n.getCompound("MonkeyShrine")):null;if(shrine!=null)setPositionTarget(shrine,22);nextTrade=n.getLong("NextTrade");nextSpeech=n.getLong("NextSpeech");lastOrder=n.contains("LastOrder")?n.getLong("LastOrder"):-1;setPersistent();}
 @Override protected void dropEquipment(net.minecraft.entity.damage.DamageSource source,int looting,boolean drops){super.dropEquipment(source,looting,drops);for(int i=0;i<cargo.size();i++){if(!cargo.getStack(i).isEmpty())dropStack(cargo.removeStack(i));}}
}
