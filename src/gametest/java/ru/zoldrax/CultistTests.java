package ru.zoldrax;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.*;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;

public class CultistTests implements FabricGameTest {
 private void check(boolean v,String message){if(!v)throw new IllegalStateException(message);}
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=100,batchId="cultists")
 public void bothCultistsSpawnNeutralAndPersist(TestContext c){
  var pos=c.getAbsolutePos(new BlockPos(3,2,3));
  var punisher=Zoldrax.CHAIN_PUNISHER.create(c.getWorld());var priest=Zoldrax.BLOOD_PRIEST.create(c.getWorld());
  punisher.refreshPositionAndAngles(pos.getX(),pos.getY(),pos.getZ(),0,0);priest.refreshPositionAndAngles(pos.getX()+3,pos.getY(),pos.getZ(),0,0);
  check(c.getWorld().spawnEntity(punisher)&&c.getWorld().spawnEntity(priest),"Both entity types must really spawn");
  check(punisher.getMaxHealth()==60&&priest.getMaxHealth()==36,"Separate NPC attributes must be registered");
  check(!punisher.isPriest()&&priest.isPriest(),"Models/variant identity must differ");
  check(punisher.isPersistent()&&priest.isPersistent(),"Followers must not naturally despawn");
  c.waitAndRun(25,()->{
   check(punisher.getTarget()==null&&priest.getTarget()==null,"NPCs must remain neutral without provocation");
   for(var entity:new CultistEntity[]{punisher,priest}){
    var n=new NbtCompound();entity.writeNbt(n);n.putString("id",Registries.ENTITY_TYPE.getId(entity.getType()).toString());
    var restored=EntityType.loadEntityWithPassengers(n,c.getWorld(),e->e);
    check(restored instanceof CultistEntity&&restored.getType()==entity.getType(),"Entity variant must survive NBT roundtrip");
   }
   punisher.discard();priest.discard();c.complete();
  });
 }
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=80,batchId="cultists")
 public void spawnEggsMatchBothVariants(TestContext c){
  var a=Registries.ITEM.get(new Identifier("zoldrax","chain_punisher_spawn_egg"));var b=Registries.ITEM.get(new Identifier("zoldrax","blood_priest_spawn_egg"));
  check(a instanceof SpawnEggItem&&b instanceof SpawnEggItem,"Both eggs must be registered");
  check(((SpawnEggItem)a).getEntityType(null)==Zoldrax.CHAIN_PUNISHER&&((SpawnEggItem)b).getEntityType(null)==Zoldrax.BLOOD_PRIEST,"Eggs must point to correct custom NPCs");c.complete();
 }
 @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=100,batchId="cultists")
 public void retaliationOnlyAfterDamage(TestContext c){
  var pos=c.getAbsolutePos(new BlockPos(3,2,3));var mob=Zoldrax.CHAIN_PUNISHER.create(c.getWorld());mob.refreshPositionAndAngles(pos.getX(),pos.getY(),pos.getZ(),0,0);c.getWorld().spawnEntity(mob);
  var player=c.createMockCreativeServerPlayerInWorld();player.changeGameMode(GameMode.SURVIVAL);player.refreshPositionAndAngles(pos.getX()+6,pos.getY(),pos.getZ(),0,0);
  c.waitAndRun(5,()->{
   check(mob.getTarget()==null,"No unsolicited player aggression");mob.damage(c.getWorld().getDamageSources().playerAttack(player),1);
  });
  c.waitAndRun(12,()->{check(mob.getTarget()==player,"Follower must retaliate when attacked");check(mob.getAttributeValue(EntityAttributes.GENERIC_ARMOR)==14,"Punisher armour must exist");mob.discard();c.getWorld().getServer().getPlayerManager().remove(player);c.complete();});
 }
}
