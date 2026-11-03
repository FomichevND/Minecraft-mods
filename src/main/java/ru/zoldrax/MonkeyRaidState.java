package ru.zoldrax;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
/** Retains raider deaths even while their altar chunk is unloaded. */
public class MonkeyRaidState extends PersistentState {
 private final Map<UUID,Boolean> tracked=new HashMap<>();
 public static MonkeyRaidState get(ServerWorld w){return w.getPersistentStateManager().getOrCreate(MonkeyRaidState::read,MonkeyRaidState::new,"zoldrax_monkey_raids");}
 public void track(UUID id){tracked.put(id,false);markDirty();}
 public void died(UUID id){if(tracked.containsKey(id)){tracked.put(id,true);markDirty();}}
 public boolean isDead(UUID id){return Boolean.TRUE.equals(tracked.get(id));}
 public void forget(UUID id){tracked.remove(id);markDirty();}
 private static MonkeyRaidState read(NbtCompound n){var out=new MonkeyRaidState();for(var el:n.getList("Tracked",10)){var e=(NbtCompound)el;if(e.containsUuid("Id"))out.tracked.put(e.getUuid("Id"),e.getBoolean("Dead"));}return out;}
 @Override public NbtCompound writeNbt(NbtCompound n){NbtList list=new NbtList();tracked.forEach((id,dead)->{var e=new NbtCompound();e.putUuid("Id",id);e.putBoolean("Dead",dead);list.add(e);});n.put("Tracked",list);return n;}
}
