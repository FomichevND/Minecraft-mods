package ru.zoldrax;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
public class ZoldraxClient implements ClientModInitializer {
 public void onInitializeClient() { BlockEntityRendererFactories.register(Zoldrax.ENTITY,MonumentRenderer::new); }
}
