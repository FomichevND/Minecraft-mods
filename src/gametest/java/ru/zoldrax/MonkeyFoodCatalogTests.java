package ru.zoldrax;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.*;
public class MonkeyFoodCatalogTests implements FabricGameTest {
 @GameTest(templateName=EMPTY_STRUCTURE) public void catalog(TestContext c){
 var list=new com.google.gson.JsonArray();net.minecraft.registry.Registries.ITEM.forEach(item->{var id=net.minecraft.registry.Registries.ITEM.getId(item);if(id.getNamespace().equals("minecraft")&&item.isFood())list.add(id.toString());});
 try{java.nio.file.Files.writeString(java.nio.file.Path.of("food-catalog.json"),list.toString());}catch(Exception e){throw new RuntimeException(e);}c.complete();}
}
