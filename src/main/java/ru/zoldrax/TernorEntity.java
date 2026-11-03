package ru.zoldrax;
import java.util.*;
import net.minecraft.block.*;
import net.minecraft.block.entity.*;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.boss.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;
import net.minecraft.world.*;

public class TernorEntity extends BlockEntity {
 public int stage;
 private static final Item[] MATERIALS={Items.IRON_INGOT,Items.STONE,Items.OBSIDIAN,Items.LANTERN,Items.MOSS_BLOCK};
 private static final int[] COST={12,32,4,4,8};
 private final Set<UUID> enemies=new HashSet<>();
 private UUID challenger,guardian;
 private int wave,farmCursor;
 private boolean spawned;
 private long nextWave,cooldown,trialStarted;
 private final ServerBossBar bar=new ServerBossBar(Text.literal("Испытание Тернора"),BossBar.Color.BLUE,BossBar.Style.PROGRESS);
 public TernorEntity(BlockPos p,BlockState s){super(Zoldrax.TERNOR_ENTITY,p,s);}
 private void sync(){markDirty();if(world!=null)world.updateListeners(pos,getCachedState(),getCachedState(),3);}
 private List<Inventory> chests(){var out=new ArrayList<Inventory>();for(int x=-2;x<=3;x++)for(int z=-2;z<=3;z++)for(int y=-1;y<=1;y++){BlockPos p=pos.add(x,y,z);if(world.isChunkLoaded(p)&&world.getBlockEntity(p) instanceof ChestBlockEntity c)out.add(c);}return out;}
 private int count(List<Inventory> invs,Item item){int n=0;for(var inv:invs)for(int i=0;i<inv.size();i++)if(inv.getStack(i).isOf(item))n+=inv.getStack(i).getCount();return n;}
 private void consume(List<Inventory> invs,Item item,int n){for(var inv:invs){for(int i=0;i<inv.size()&&n>0;i++)if(inv.getStack(i).isOf(item)){int t=Math.min(n,inv.getStack(i).getCount());inv.removeStack(i,t);n-=t;}inv.markDirty();}}
 private void say(String text){for(var p:((ServerWorld)world).getPlayers(p->p.squaredDistanceTo(Vec3d.ofCenter(pos))<=1024))p.sendMessage(Text.literal("[Тернор] "+text),false);}
 public Text status(){
  if(stage<2){var t=Text.literal("Тернор: этап "+(stage+1)+"/3. Сундуков: "+chests().size()+"\n");for(int i=0;i<MATERIALS.length;i++)t.append(Text.translatable(MATERIALS[i].getTranslationKey())).append(": "+count(chests(),MATERIALS[i])+" / "+COST[i]+"\n");return t;}
  if(wave>0)return Text.literal("Испытание Тернора: волна "+wave+"/3. Осталось врагов: "+enemies.size());
  if(guardian!=null)return Text.literal("Страж Тернора охраняет земли в радиусе 50 блоков. Днём благословение растений действует в радиусе 30 блоков.");
  return Text.literal(world.isDay()?"Дневное благословение: рост растений и увлажнение пашни в радиусе 30. Вернитесь ночью для испытания.":"Ночь испытаний: ПКМ запускает три волны зомби, скелетов и пауков. Победа призовёт стража.");
 }
 public void interact(PlayerEntity player){
  if(stage<2||wave>0){player.sendMessage(status(),false);return;}
  refreshGuardian();
  if(guardian!=null){player.sendMessage(status(),false);return;}
  if(!world.isNight()){player.sendMessage(status(),false);return;}
  if(world.getDifficulty()==Difficulty.PEACEFUL){player.sendMessage(Text.literal("Испытание требует сложности выше Мирной."),false);return;}
  if(world.getTime()<cooldown){player.sendMessage(Text.literal("Тернор отдыхает. Подождите "+((cooldown-world.getTime()+19)/20)+" сек."),false);return;}
  if(!player.isAlive()||player.isSpectator()||player.squaredDistanceTo(Vec3d.ofCenter(pos))>64)return;
  challenger=player.getUuid();trialStarted=world.getTime();wave=1;spawned=false;nextWave=world.getTime();sync();say("Поднимите оружие. Начинается испытание защитника!");
 }
 private void refreshGuardian(){if(guardian!=null&&MonkeyRaidState.get((ServerWorld)world).isDead(guardian)){MonkeyRaidState.get((ServerWorld)world).forget(guardian);guardian=null;sync();}}
 public static void tick(World w,BlockPos p,BlockState s,TernorEntity m){
  if(w.getTime()%20!=0)return;
  if(m.stage<2){var invs=m.chests();for(int i=0;i<MATERIALS.length;i++)if(m.count(invs,MATERIALS[i])<COST[i])return;for(int i=0;i<MATERIALS.length;i++)m.consume(invs,MATERIALS[i],COST[i]);m.stage++;m.sync();return;}
  m.refreshGuardian();if(w.isDay())m.farmPulse();if(m.wave>0)m.trial();m.updateBar();
 }
 /** Bounded work: 128 columns per second, only already-loaded chunks, sphere radius 30. */
 void farmPulse(){
  if(!world.isDay())return;var sw=(ServerWorld)world;
  for(int n=0;n<128;n++){
   int index=farmCursor++%3721,x=index%61-30,z=index/61-30;int square=900-x*x-z*z;if(square<0)continue;
   BlockPos column=pos.add(x,0,z);if(!world.isChunkLoaded(column))continue;
   int dy=(int)Math.sqrt(square);
   for(int y=-dy;y<=dy;y++){
    BlockPos p=column.up(y);if(!world.isInBuildLimit(p))continue;BlockState state=world.getBlockState(p);Block block=state.getBlock();
    if(block instanceof FarmlandBlock){if(state.get(FarmlandBlock.MOISTURE)<7)world.setBlockState(p,state.with(FarmlandBlock.MOISTURE,7),2);continue;}
    if(world.random.nextInt(4)!=0)continue;
    if((block instanceof PlantBlock||block instanceof CocoaBlock||block instanceof BambooBlock)&&block instanceof Fertilizable f&&f.isFertilizable(world,p,state,false)&&f.canGrow(world,world.random,p,state))f.grow(sw,world.random,p,state);
    else if(block instanceof SugarCaneBlock||block instanceof CactusBlock||block instanceof NetherWartBlock)state.randomTick(sw,p,world.random);
   }
  }
  farmCursor%=3721;markDirty();
 }
 private BlockPos findSpawn(Vec3d center,int distance,int height){
  var sw=(ServerWorld)world;
  for(int attempt=0;attempt<32;attempt++){
   double angle=world.random.nextDouble()*Math.PI*2;BlockPos near=BlockPos.ofFloored(center).add((int)(Math.cos(angle)*distance),0,(int)(Math.sin(angle)*distance));
   if(!world.isChunkLoaded(near))continue;
   for(int dy=6;dy>=-8;dy--){BlockPos q=near.up(dy);if(!world.isInBuildLimit(q)||!sw.getWorldBorder().contains(q)||!world.getBlockState(q.down()).isSolidBlock(world,q.down()))continue;
    boolean clear=true;for(int y=0;y<height;y++)if(!world.isAir(q.up(y)))clear=false;if(clear)return q;
   }
  }return null;
 }
 private void trial(){
  var sw=(ServerWorld)world;var history=MonkeyRaidState.get(sw);
  if(challenger!=null&&history.lastPlayerDeath(challenger)>=trialStarted){fail("Испытание проиграно. Новая попытка через минуту.");return;}
  if(world.getDifficulty()==Difficulty.PEACEFUL){fail("Испытание прекращено на Мирной сложности.");return;}
  var player=challenger==null?null:sw.getServer().getPlayerManager().getPlayer(challenger);
  if(player!=null&&!player.isAlive()){fail("Испытание проиграно. Вернитесь за новой попыткой через минуту.");return;}
  if(player==null||player.getWorld()!=sw||player.isSpectator())return;
  boolean lost=enemies.removeIf(id->{Entity e=sw.getEntity(id);boolean dead=history.isDead(id)||(e!=null&&!e.isAlive());if(dead)history.forget(id);return dead;});if(lost)markDirty();
  if(spawned&&enemies.isEmpty()){
   if(wave==3){summonGuardian();return;}
   wave++;spawned=false;nextWave=world.getTime()+100;sync();
  }
  if(!enemies.isEmpty()){for(UUID id:enemies)if(sw.getEntity(id) instanceof MobEntity mob)mob.setTarget(player);return;}
  if(world.getTime()<nextWave)return;
  for(int i=0;i<2*wave+1;i++){
   BlockPos q=findSpawn(player.getPos(),10,2);if(q==null)continue;
   EntityType<? extends HostileEntity> type=switch(i%3){case 0->EntityType.ZOMBIE;case 1->EntityType.SKELETON;default->EntityType.SPIDER;};
   var mob=type.create(sw);if(mob==null)continue;
   mob.refreshPositionAndAngles(q.getX()+.5,q.getY(),q.getZ()+.5,0,0);mob.initialize(sw,sw.getLocalDifficulty(q),SpawnReason.EVENT,null,null);mob.setPersistent();mob.setTarget(player);
   if(sw.spawnEntity(mob)){enemies.add(mob.getUuid());history.track(mob.getUuid());}
  }
  if(!enemies.isEmpty()){spawned=true;say("Волна "+wave+" из 3. Отстоять землю — значит отстоять друг друга.");sync();}
 }
 private void summonGuardian(){
  BlockPos q=findSpawn(Vec3d.ofCenter(pos),5,3);if(q==null)return;var sw=(ServerWorld)world;var entity=Zoldrax.TERNOR_GUARDIAN.create(sw);if(entity==null)return;
  entity.refreshPositionAndAngles(q.getX()+.5,q.getY(),q.getZ()+.5,0,0);entity.setHome(pos);
  if(sw.spawnEntity(entity)){guardian=entity.getUuid();MonkeyRaidState.get(sw).track(guardian);wave=0;spawned=false;bar.clearPlayers();sync();say("Вы доказали мужество. Страж с венком будет охранять эти земли!");}
 }
 private void fail(String reason){var sw=(ServerWorld)world;var history=MonkeyRaidState.get(sw);for(UUID id:enemies){if(sw.getEntity(id)!=null)sw.getEntity(id).discard();history.forget(id);}enemies.clear();wave=0;spawned=false;cooldown=world.getTime()+1200;bar.clearPlayers();sync();say(reason);}
 private void updateBar(){if(wave==0){bar.clearPlayers();return;}var list=((ServerWorld)world).getPlayers(p->!p.isSpectator()&&p.squaredDistanceTo(Vec3d.ofCenter(pos))<=1024);for(var p:new ArrayList<>(bar.getPlayers()))if(!list.contains(p))bar.removePlayer(p);for(var p:list)bar.addPlayer(p);bar.setName(Text.literal("Испытание Тернора: волна "+wave+"/3 — "+enemies.size()+" врагов"));bar.setPercent(MathHelper.clamp(enemies.size()/(float)(2*wave+1),0,1));}
 @Override public void markRemoved(){bar.clearPlayers();super.markRemoved();}
 @Override protected void writeNbt(NbtCompound n){super.writeNbt(n);n.putInt("Stage",stage);n.putInt("Wave",wave);n.putBoolean("Spawned",spawned);n.putLong("NextWave",nextWave);n.putLong("Cooldown",cooldown);n.putLong("TrialStarted",trialStarted);n.putInt("FarmCursor",farmCursor);if(challenger!=null)n.putUuid("Challenger",challenger);if(guardian!=null)n.putUuid("Guardian",guardian);var list=new NbtList();for(UUID id:enemies){var e=new NbtCompound();e.putUuid("Id",id);list.add(e);}n.put("Enemies",list);}
 @Override public void readNbt(NbtCompound n){super.readNbt(n);stage=MathHelper.clamp(n.getInt("Stage"),0,2);wave=MathHelper.clamp(n.getInt("Wave"),0,3);spawned=n.getBoolean("Spawned");nextWave=n.getLong("NextWave");cooldown=n.getLong("Cooldown");trialStarted=n.getLong("TrialStarted");farmCursor=Math.floorMod(n.getInt("FarmCursor"),3721);challenger=n.containsUuid("Challenger")?n.getUuid("Challenger"):null;guardian=n.containsUuid("Guardian")?n.getUuid("Guardian"):null;enemies.clear();for(var el:n.getList("Enemies",10)){var e=(NbtCompound)el;if(e.containsUuid("Id"))enemies.add(e.getUuid("Id"));}}
 @Override public NbtCompound toInitialChunkDataNbt(){return createNbt();}
 @Override public BlockEntityUpdateS2CPacket toUpdatePacket(){return BlockEntityUpdateS2CPacket.create(this);}
}
