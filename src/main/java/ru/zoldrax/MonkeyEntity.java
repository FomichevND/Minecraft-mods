package ru.zoldrax;

import java.util.*;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.*;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.boss.*;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.world.*;

public class MonkeyEntity extends BlockEntity {
 public int stage;
 private static final Item[][] MATERIALS={{Items.MOSSY_COBBLESTONE,Items.CHISELED_STONE_BRICKS,Items.JUNGLE_LOG,Items.GOLD_NUGGET},{Items.CHISELED_STONE_BRICKS,Items.JUNGLE_LOG,Items.VINE,Items.GOLD_INGOT}};
 private static final int[][] COST={{48,24,16,16},{32,16,16,8}};
 public static final Item[] FOODS={Items.BREAD,Items.CARROT,Items.POTATO,Items.BAKED_POTATO,Items.APPLE,Items.MELON_SLICE,Items.COOKED_CHICKEN,Items.DRIED_KELP};
 private Item food=Items.AIR;
 private int amount;
 private long deadline=-1,nextRequest=-1,nextJoke=-1;
 private UUID customer;
 private boolean awakened;
 private int raidWave;
 private long nextWave;
 private boolean waveStarted;
 private final Set<UUID> raiders=new HashSet<>();
 private final ServerBossBar bar=new ServerBossBar(Text.literal("Король обезьян"),BossBar.Color.YELLOW,BossBar.Style.PROGRESS);
 public MonkeyEntity(BlockPos p,BlockState s){super(Zoldrax.MONKEY_ENTITY,p,s);}
 private void sync(){markDirty();if(world!=null)world.updateListeners(pos,getCachedState(),getCachedState(),3);}
 private List<Inventory> chests(){
  List<Inventory> out=new ArrayList<>();if(world==null)return out;
  for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)for(int y=-1;y<=1;y++){
   BlockPos p=pos.add(x,y,z);if(acceptsChest(p)&&world.isChunkLoaded(p)&&world.getBlockEntity(p) instanceof ChestBlockEntity c)out.add(c);
  }return out;
 }
 public boolean acceptsChest(BlockPos p){
  var d=p.subtract(pos);var f=getCachedState().get(MonkeyBlock.FACING);int x=d.getX()*f.getOffsetZ()-d.getZ()*f.getOffsetX(),z=d.getX()*f.getOffsetX()+d.getZ()*f.getOffsetZ();return x>=-3&&x<=4&&Math.abs(z)<=3&&Math.abs(d.getY())<=1;
 }
 private int count(List<Inventory> invs,Item item){int n=0;for(var inv:invs)for(int i=0;i<inv.size();i++)if(inv.getStack(i).isOf(item))n+=inv.getStack(i).getCount();return n;}
 private void consume(List<Inventory> invs,Item item,int n){for(var inv:invs){for(int i=0;i<inv.size()&&n>0;i++)if(inv.getStack(i).isOf(item)){int take=Math.min(n,inv.getStack(i).getCount());inv.removeStack(i,take);n-=take;}inv.markDirty();}}
 private void reward(List<Inventory> invs){
  ItemStack banana=new ItemStack(Zoldrax.GOLDEN_BANANA);
  for(var inv:invs)for(int i=0;i<inv.size();i++){
   ItemStack stack=inv.getStack(i);
   if(stack.isEmpty()){inv.setStack(i,banana);inv.markDirty();return;}
   if(stack.isOf(Zoldrax.GOLDEN_BANANA)&&stack.getCount()<stack.getMaxCount()){stack.increment(1);inv.markDirty();return;}
  }
  world.spawnEntity(new ItemEntity(world,pos.getX()+.5,pos.getY()+1,pos.getZ()+2.5,banana));
 }
 private List<ServerPlayerEntity> viewers(){return ((ServerWorld)world).getPlayers(p->p.isAlive()&&!p.isSpectator()&&p.squaredDistanceTo(Vec3d.ofCenter(pos))<=576);}
 private void say(Text text){for(var p:viewers())p.sendMessage(Text.literal("[Король обезьян] ").append(text),false);}
 private void say(String text){say(Text.literal(text));}
 private void updateBar(){
  if(food==Items.AIR&&raidWave==0){bar.clearPlayers();return;}
  var players=viewers();for(var p:new ArrayList<>(bar.getPlayers()))if(!players.contains(p))bar.removePlayer(p);
  for(var p:players)bar.addPlayer(p);
  if(raidWave>0){bar.setColor(BossBar.Color.RED);bar.setName(Text.literal("Рейд Короля обезьян — волна "+raidWave+"/3, врагов: "+raiders.size()));bar.setPercent(MathHelper.clamp(raiders.size()/(float)(raidWave+1),0,1));}
  else{bar.setColor(BossBar.Color.YELLOW);bar.setName(Text.literal("Голодный король: "+amount+" × ").append(Text.translatable(food.getTranslationKey())).append(" — "+Math.max(0,(deadline-world.getTime()+19)/20)+" сек."));bar.setPercent(MathHelper.clamp((deadline-world.getTime())/2400f,0,1));}
 }
 public Text status(){
  if(stage<2){var text=Text.literal("Король обезьян: этап "+(stage+1)+"/3. Найдено сундуков: "+chests().size()+"\n");for(int i=0;i<MATERIALS[stage].length;i++)text.append(Text.translatable(MATERIALS[stage][i].getTranslationKey())).append(": "+count(chests(),MATERIALS[stage][i])+" / "+COST[stage][i]+"\n");return text;}
  if(raidWave>0)return Text.literal("Королевский рейд: волна "+raidWave+"/3. Врагов осталось: "+raiders.size());
  if(food==Items.AIR&&!awakened)return Text.literal("Король обезьян ждёт первого гостя. Подойдите ближе или нажмите ПКМ, чтобы получить испытание.");
  if(food==Items.AIR)return Text.literal("Король пока сыт. Поставьте сундук у основания и ждите голодного зова!");
  return Text.literal("Нужно "+amount+" × ").append(Text.translatable(food.getTranslationKey())).append(". В сундуках: "+count(chests(),food)+". Осталось "+Math.max(0,(deadline-world.getTime()+19)/20)+" сек. За просрочку — рейд!");
 }
 /** A persisted request; also used by the integration tests to control the random trigger. */
 void beginRequest(Item wanted,int count,UUID player){awakened=true;food=wanted;amount=count;customer=player;deadline=world.getTime()+2400;sync();say(Text.literal("Моё величество требует "+amount+" × ").append(Text.translatable(food.getTranslationKey())).append(" за 2 минуты! Положите еду в сундук. Не успеете — пришлю разбойников!"));updateBar();}
 /** First contact is independent of who placed the monument or when its chunk loaded. */
 public boolean tryFirstEncounter(ServerPlayerEntity player){
  if(world==null||world.isClient||stage<2||awakened||food!=Items.AIR||raidWave>0)return false;
  if(player.getWorld()!=world||!player.isAlive()||player.isSpectator()||player.squaredDistanceTo(Vec3d.ofCenter(pos))>576)return false;
  beginRequest(FOODS[world.random.nextInt(FOODS.length)],16+world.random.nextInt(49),player.getUuid());
  return true;
 }
 public boolean hasRequest(){return food!=Items.AIR;}
 public long requestDeadline(){return deadline;}
 private void schedule(){nextRequest=world.getTime()+3600+world.random.nextInt(3601);}
 public static void tick(World w,BlockPos p,BlockState state,MonkeyEntity m){
  if(w.getTime()%20!=0)return;long now=w.getTime();
  if(m.stage<2){var invs=m.chests();for(int i=0;i<MATERIALS[m.stage].length;i++)if(m.count(invs,MATERIALS[m.stage][i])<COST[m.stage][i])return;
   for(int i=0;i<MATERIALS[m.stage].length;i++)m.consume(invs,MATERIALS[m.stage][i],COST[m.stage][i]);m.stage++;m.sync();return;}
  if(m.raidWave>0){m.tickRaid();m.updateBar();return;}
  if(m.nextRequest<0){m.nextRequest=now+1200+w.random.nextInt(2401);m.nextJoke=now+1200;m.markDirty();}
  if(!m.awakened&&m.food==Items.AIR){
   var guests=m.viewers();if(!guests.isEmpty()&&m.tryFirstEncounter(guests.get(0)))return;
  }
  if(m.food!=Items.AIR){
   if(now>=m.deadline){m.food=Items.AIR;m.raidWave=1;m.nextWave=now;m.say("Мой живот объявляет войну! Начинается рейд!");m.sync();m.tickRaid();}
   else{var invs=m.chests();if(m.count(invs,m.food)>=m.amount){m.consume(invs,m.food,m.amount);m.reward(invs);m.food=Items.AIR;m.schedule();m.say("Достойная трапеза! Золотой банан ждёт в сундуке. Я велик, но вы тоже ничего!");m.sync();}}
  }else if(now>=m.nextRequest&&!m.viewers().isEmpty()){
   var players=m.viewers();var player=players.get(w.random.nextInt(players.size()));m.beginRequest(FOODS[w.random.nextInt(FOODS.length)],16+w.random.nextInt(49),player.getUuid());
  }
  if(now>=m.nextJoke&&m.food==Items.AIR){String[] jokes={"Я настолько велик, что мне нужен второй обед после первого завтрака!","Бананы — символ власти. Но хлеб тоже подойдёт.","Не поклоняйтесь на пустой желудок. Особенно на мой!","Моя корона золотая. Мой аппетит — бесценный."};if(!m.viewers().isEmpty())m.say(jokes[w.random.nextInt(jokes.length)]);m.nextJoke=now+2400+w.random.nextInt(2401);m.markDirty();}
  m.updateBar();
 }
 private void tickRaid(){
  ServerWorld sw=(ServerWorld)world;long now=world.getTime();
  var history=MonkeyRaidState.get(sw);
  boolean losses=raiders.removeIf(id->{Entity e=sw.getEntity(id);boolean dead=history.isDead(id)||(e!=null&&!e.isAlive());if(dead)history.forget(id);return dead;});
  if(losses)markDirty();
  if(world.getDifficulty()==Difficulty.PEACEFUL){for(UUID id:raiders){if(sw.getEntity(id)!=null)sw.getEntity(id).discard();history.forget(id);}finishRaid();return;}
  if(waveStarted&&raiders.isEmpty()){
   if(raidWave>=3){finishRaid();return;}
   raidWave++;waveStarted=false;nextWave=now+100;sync();
  }
  // Missing UUIDs may belong to unloaded chunks; never treat unloading as a victory.
  var target=customer==null?null:sw.getServer().getPlayerManager().getPlayer(customer);
  if(target==null||target.getWorld()!=sw||!target.isAlive()||target.isSpectator())return;
  if(!raiders.isEmpty()){
   for(UUID id:raiders)if(sw.getEntity(id) instanceof MobEntity mob)mob.setTarget(target);
   return;
  }
  if(now<nextWave)return;
  if(raidWave>3){finishRaid();return;}
  int spawned=0;
  for(int i=0;i<raidWave+1;i++){
   MobEntity mob=(i==0&&raidWave>=2?EntityType.VINDICATOR:EntityType.PILLAGER).create(sw);
   if(mob==null)continue;
   BlockPos spawn=null;
   for(int attempt=0;attempt<24;attempt++){
    double a=world.random.nextDouble()*Math.PI*2;
    BlockPos near=target.getBlockPos().add((int)(Math.cos(a)*10),0,(int)(Math.sin(a)*10));
    // Search around the player's level so caves and ordinary terrain work as well.
    for(int dy=5;dy>=-7;dy--){BlockPos q=near.up(dy);if(sw.isInBuildLimit(q)&&sw.getWorldBorder().contains(q)&&sw.getBlockState(q.down()).isSolidBlock(sw,q.down())&&sw.isAir(q)&&sw.isAir(q.up())){spawn=q;break;}}
    if(spawn!=null)break;
   }
   if(spawn==null)continue;
   mob.refreshPositionAndAngles(spawn.getX()+.5,spawn.getY(),spawn.getZ()+.5,0,0);
   mob.initialize(sw,sw.getLocalDifficulty(spawn),SpawnReason.EVENT,null,null);mob.setPersistent();mob.setTarget(target);
   if(sw.spawnEntity(mob)){raiders.add(mob.getUuid());history.track(mob.getUuid());spawned++;}
  }
  if(spawned>0){waveStarted=true;say("Волна "+raidWave+"! Так бывает, когда короля оставляют без обеда!");nextWave=now+100;markDirty();}
 }
 private void finishRaid(){raidWave=0;waveStarted=false;raiders.clear();schedule();say("Рейд окончен! В следующий раз начните с закуски.");bar.clearPlayers();sync();}
 @Override public void markRemoved(){bar.clearPlayers();super.markRemoved();}
 @Override protected void writeNbt(NbtCompound n){super.writeNbt(n);n.putInt("Stage",stage);n.putBoolean("Awakened",awakened);n.putString("Food",Registries.ITEM.getId(food).toString());n.putInt("Amount",amount);n.putLong("Deadline",deadline);n.putLong("NextRequest",nextRequest);n.putLong("NextJoke",nextJoke);if(customer!=null)n.putUuid("Customer",customer);n.putInt("RaidWave",raidWave);n.putBoolean("WaveStarted",waveStarted);n.putLong("NextWave",nextWave);NbtList ids=new NbtList();for(UUID id:raiders){NbtCompound e=new NbtCompound();e.putUuid("Id",id);ids.add(e);}n.put("Raiders",ids);}
 @Override public void readNbt(NbtCompound n){super.readNbt(n);stage=MathHelper.clamp(n.getInt("Stage"),0,2);Identifier id=Identifier.tryParse(n.getString("Food"));food=id==null?Items.AIR:Registries.ITEM.get(id);if(!Arrays.asList(FOODS).contains(food))food=Items.AIR;amount=MathHelper.clamp(n.getInt("Amount"),16,64);deadline=n.contains("Deadline")?n.getLong("Deadline"):-1;nextRequest=n.contains("NextRequest")?n.getLong("NextRequest"):-1;nextJoke=n.contains("NextJoke")?n.getLong("NextJoke"):-1;customer=n.containsUuid("Customer")?n.getUuid("Customer"):null;raidWave=MathHelper.clamp(n.getInt("RaidWave"),0,3);awakened=n.getBoolean("Awakened")||customer!=null||food!=Items.AIR||raidWave>0;waveStarted=n.getBoolean("WaveStarted");nextWave=n.getLong("NextWave");raiders.clear();for(var el:n.getList("Raiders",10)){var e=(NbtCompound)el;if(e.containsUuid("Id"))raiders.add(e.getUuid("Id"));}}
 @Override public NbtCompound toInitialChunkDataNbt(){return createNbt();}
 @Override public BlockEntityUpdateS2CPacket toUpdatePacket(){return BlockEntityUpdateS2CPacket.create(this);}
}
