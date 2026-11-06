package ru.zoldrax;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
public class ZoldraxClient implements ClientModInitializer {
 public void onInitializeClient() {
  DecorRenderer.register();
  net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(Zoldrax.BANANA_HERALD,ctx->new MonkeyFollowerRenderer(ctx,"banana_herald"));
  net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(Zoldrax.TEMPLE_GATHERER,ctx->new MonkeyFollowerRenderer(ctx,"temple_gatherer"));
  net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(Zoldrax.CHAIN_PUNISHER,ctx->new CultistRenderer(ctx,"chain_punisher"));
  net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(Zoldrax.BLOOD_PRIEST,ctx->new CultistRenderer(ctx,"blood_priest"));
  net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback.EVENT.register(context ->
   context.register(new net.minecraft.util.Identifier("zoldrax","monument"),
    net.minecraft.client.render.VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
    shader -> MonumentRenderLayer.shader=shader)); BlockEntityRendererFactories.register(Zoldrax.ENTITY,MonumentRenderer::new); BlockEntityRendererFactories.register(Zoldrax.MONKEY_ENTITY,MonkeyRenderer::new); BlockEntityRendererFactories.register(Zoldrax.TERNOR_ENTITY,TernorRenderer::new); net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(Zoldrax.TERNOR_GUARDIAN,TernorGuardianRenderer::new); }
}
