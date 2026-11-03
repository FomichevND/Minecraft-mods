package ru.zoldrax;

import java.util.*;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.*;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.effect.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.registry.tag.DamageTypeTags;

public class MonumentEntity extends BlockEntity {
 public int stage=0;
 private long lastOffering=-1;
 private long cultRitualUntil=-1;
 public boolean wasRecentlyFed(){return lastOffering>=0&&world.getTime()-lastOffering<600;}
 private boolean nearbyPriest(){return !world.getEntitiesByClass(CultistEntity.class,new Box(pos).expand(8),e->e.isAlive()&&e.isPriest()).isEmpty();}
 private boolean cultRitual(){return cultRitualUntil>world.getTime();}
 private void recordCultOffering(){lastOffering=world.getTime();if(nearbyPriest())cultRitualUntil=world.getTime()+600;sync();}

 // Transient attribution, scoped to this altar; entries expire or are removed after death.
 private final Map<UUID,Long> burningVictims=new HashMap<>();
 private final Map<UUID,Long> nextItemOffering=new HashMap<>();
 public Vec3d altarPoint(double x,double y,double z){
  var facing=getCachedState().get(MonumentBlock.FACING);
  return new Vec3d(pos.getX()+.5+x*facing.getOffsetZ()+z*facing.getOffsetX(),pos.getY()+y,pos.getZ()+.5-x*facing.getOffsetX()+z*facing.getOffsetZ());
 }
 public Box brazierBounds(){return new Box(altarPoint(-1.15,-.1,.6),altarPoint(1.15,2,2.55));}
 private PlayerEntity recipient(Entity owner) {
  if(owner instanceof PlayerEntity p && p.isAlive() && !p.isSpectator() && p.squaredDistanceTo(Vec3d.ofCenter(pos))<=256) return p;
  return world.getClosestPlayer(pos.getX()+.5,pos.getY(),pos.getZ()+1.25,16,false);
 }

 private final Map<UUID,Long> devotees=new HashMap<>();
 private static final Item[][] MATERIALS={{Items.OBSIDIAN,Items.POLISHED_BLACKSTONE_BRICKS,Items.REDSTONE},{Items.OBSIDIAN,Items.POLISHED_BLACKSTONE_BRICKS,Items.REDSTONE,Items.REDSTONE_BLOCK}};
 private static final int[][] COUNTS={{32,24,8},{24,16,12,4}};
 private static final StatusEffect[] GIFTS={StatusEffects.STRENGTH,StatusEffects.REGENERATION,StatusEffects.SPEED,StatusEffects.RESISTANCE,StatusEffects.FIRE_RESISTANCE};
 private static final StatusEffect[] CURSES={StatusEffects.WEAKNESS,StatusEffects.SLOWNESS,StatusEffects.HUNGER};
 public MonumentEntity(BlockPos p,BlockState s) { super(Zoldrax.ENTITY,p,s); }
 /** Scan the visible footprint and its perimeter, including chests one block lower. */
 private List<Inventory> constructionChests() {
  List<Inventory> result=new ArrayList<>();
  if(world==null) return result;
  for(int y=-1;y<=1;y++) for(int x=-3;x<=3;x++) for(int z=-3;z<=3;z++) {
   if(x*x+z*z>9) continue;
   BlockPos q=pos.add(x,y,z);
   if(world.isChunkLoaded(q) && world.getBlockEntity(q) instanceof ChestBlockEntity chest) result.add(chest);
  }
  return result;
 }
 private boolean matches(ItemStack stack, Item required) {
  if(required!=Items.POLISHED_BLACKSTONE_BRICKS) return stack.isOf(required);
  return stack.isOf(Items.BLACKSTONE)||stack.isOf(Items.POLISHED_BLACKSTONE)||stack.isOf(Items.POLISHED_BLACKSTONE_BRICKS)
    ||stack.isOf(Items.CRACKED_POLISHED_BLACKSTONE_BRICKS)||stack.isOf(Items.CHISELED_POLISHED_BLACKSTONE);
 }
 private int available(List<Inventory> chests, Item item) {
  int count=0;
  for(Inventory inv:chests) for(int i=0;i<inv.size();i++) if(matches(inv.getStack(i),item)) count+=inv.getStack(i).getCount();
  return count;
 }
 public net.minecraft.text.Text status() {
  if(stage==2) return Text.literal("Золдракс: активен. Бросьте алмаз, изумруд, золото или незерит в жаровню с пламенем (на стороне пламени). Один предмет из стопки в секунду. Подожжённый здесь моб тоже считается жертвой. Последнее подношение: "+(lastOffering<0?"ещё не было":((world.getTime()-lastOffering)/20)+" сек. назад"));
  List<Inventory> chests=constructionChests();
  var text=Text.literal("Этап "+(stage+1)+"/3. Найдено сундуков: "+chests.size()+". Радиус: 3 блока, высота ±1.\n");
  for(int i=0;i<MATERIALS[stage].length;i++) {
   Item item=MATERIALS[stage][i];
   text.append(item==Items.POLISHED_BLACKSTONE_BRICKS ? Text.literal("Чернокамень / полированный / кирпичи") : Text.translatable(item.getTranslationKey()));
   int count=available(chests,item),need=COUNTS[stage][i];
   text.append(Text.literal(": "+count+" / "+need+(count<need?" — не хватает "+(need-count):" — готово")+"\n"));
  }
  return text;
 }
 private void sync() { markDirty(); if(world!=null) world.updateListeners(pos,getCachedState(),getCachedState(),3); }
 public static void tick(World world,BlockPos pos,BlockState state,MonumentEntity m) {
  if(m.stage<2) { if(world.getTime()%20==0) m.build(); return; }
  ServerWorld sw=(ServerWorld)world;
  long now=world.getTime();
  Box fire=m.brazierBounds();
  if(now%4==0){var flame=m.altarPoint(0,.9,1.55);sw.spawnParticles(ParticleTypes.FLAME,flame.x,flame.y,flame.z,8,.45,.2,.45,.015);}
  // Scan every tick: a thrown item can pass through the bowl in less than a second.
  m.nextItemOffering.entrySet().removeIf(entry -> entry.getValue()+100<now);
  for(ItemEntity item:world.getEntitiesByClass(ItemEntity.class,fire,e->!e.isRemoved())) {
   ItemStack stack=item.getStack();
   if(CultFactions.isOffering(stack)
      && now>=m.nextItemOffering.getOrDefault(item.getUuid(),Long.MIN_VALUE)) {
    PlayerEntity p=m.recipient(item.getOwner());
    boolean cult=stack.hasNbt()&&stack.getNbt().getBoolean("ZoldraxCultOffering")&&stack.getNbt().getLong("ZoldraxAltar")==pos.asLong();
    if(p!=null||cult) {
     stack.decrement(1); if(stack.isEmpty()) item.discard();
     m.nextItemOffering.put(item.getUuid(),now+20);if(p!=null)m.offer(p);else m.recordCultOffering();
    }
   }
  }
  for(MobEntity mob:world.getEntitiesByClass(MobEntity.class,fire,e->e.isAlive()&&!(e instanceof CultistEntity)&&!e.isFireImmune()&&!e.hasStatusEffect(StatusEffects.FIRE_RESISTANCE))) {
   boolean firstContact=!m.burningVictims.containsKey(mob.getUuid());
   m.burningVictims.put(mob.getUuid(),now+100);
   mob.setOnFireFor(4);
   if(firstContact || now%20==0) mob.damage(world.getDamageSources().inFire(),m.cultRitual()?6:4);
  }
  // Death can occur between direct-damage ticks, or after the mob walks out burning.
  var victims=m.burningVictims.entrySet().iterator();
  while(victims.hasNext()) {
   var entry=victims.next(); Entity entity=sw.getEntity(entry.getKey());
   if(entity instanceof MobEntity mob && !mob.isAlive()) {
    if(entry.getValue()>=now && mob.getRecentDamageSource()!=null && mob.getRecentDamageSource().isIn(DamageTypeTags.IS_FIRE)) {
     PlayerEntity p=m.recipient(mob.getAttacker());if(p!=null) m.offer(p);
    }
    victims.remove(); // Exactly once, including if no eligible player is present.
   } else if(entity==null || entry.getValue()<now) victims.remove();
  }
  for(var entry:m.devotees.entrySet()) {
   PlayerEntity p=world.getPlayerByUuid(entry.getKey());
   if(p!=null && now-entry.getValue()>=24000 && now%200==0) {
    p.addStatusEffect(new StatusEffectInstance(CURSES[world.random.nextInt(CURSES.length)],240,0));
    p.sendMessage(Text.literal("Золдракс требует нового подношения!"),true);
   }
  }
 }
 private void offer(PlayerEntity p) {
  devotees.put(p.getUuid(),world.getTime());recordCultOffering();
  for(StatusEffect e:CURSES) p.removeStatusEffect(e);
  int minutes=nearbyPriest()?6:3;
  p.addStatusEffect(new StatusEffectInstance(GIFTS[world.random.nextInt(GIFTS.length)],minutes*1200,0));
  p.sendMessage(Text.literal("Золдракс принял жертву и даровал благословение на "+minutes+" мин."+(minutes==6?" Жрец усилил ритуал!":"")),false); sync();
 }
 private void build() {
  List<Inventory> inventories=constructionChests();
  Item[] items=MATERIALS[stage]; int[] required=COUNTS[stage];
  for(int j=0;j<items.length;j++) if(available(inventories,items[j])<required[j]) return;
  for(int j=0;j<items.length;j++) {
   int remaining=required[j];
   for(Inventory inv:inventories) {
    for(int i=0;i<inv.size()&&remaining>0;i++) if(matches(inv.getStack(i),items[j])) {
     int n=Math.min(remaining,inv.getStack(i).getCount()); inv.removeStack(i,n); remaining-=n;
    }
    inv.markDirty();
   }
  }
  stage++;sync();
  ((ServerWorld)world).spawnParticles(ParticleTypes.POOF,pos.getX()+.5,pos.getY()+2,pos.getZ()+.5,30,1,1,1,.02);
 }
 @Override protected void writeNbt(NbtCompound n) { super.writeNbt(n);n.putInt("Stage",stage);n.putLong("LastOffering",lastOffering);n.putLong("CultRitualUntil",cultRitualUntil); NbtList list=new NbtList();devotees.forEach((id,time)->{NbtCompound e=new NbtCompound();e.putUuid("Player",id);e.putLong("Time",time);list.add(e);});n.put("Devotees",list); }
 @Override public void readNbt(NbtCompound n) {super.readNbt(n);stage=MathHelper.clamp(n.getInt("Stage"),0,2);lastOffering=n.getLong("LastOffering");cultRitualUntil=n.getLong("CultRitualUntil");devotees.clear();for(NbtElement el:n.getList("Devotees",10)){NbtCompound e=(NbtCompound)el;devotees.put(e.getUuid("Player"),e.getLong("Time"));}}
 @Override public NbtCompound toInitialChunkDataNbt() {return createNbt();}
 @Override public Packet<ClientPlayPacketListener> toUpdatePacket() {return BlockEntityUpdateS2CPacket.create(this);}
}
