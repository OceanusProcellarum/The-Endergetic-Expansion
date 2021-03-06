package com.minecraftabnormals.endergetic.core;

import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.minecraftabnormals.endergetic.client.particle.EEParticles;
import com.minecraftabnormals.endergetic.client.render.entity.*;
import com.minecraftabnormals.endergetic.client.render.entity.booflo.*;
import com.minecraftabnormals.endergetic.client.render.tile.*;
import com.minecraftabnormals.endergetic.common.network.entity.*;
import com.minecraftabnormals.endergetic.common.network.entity.booflo.*;
import com.minecraftabnormals.endergetic.common.network.entity.puffbug.*;
import com.minecraftabnormals.endergetic.common.network.nbt.*;
import com.minecraftabnormals.endergetic.common.world.EEWorldGenHandler;
import com.minecraftabnormals.endergetic.common.world.features.EEFeatures;
import com.minecraftabnormals.endergetic.common.world.surfacebuilders.EESurfaceBuilders;
import com.minecraftabnormals.endergetic.core.config.EEConfig;
import com.minecraftabnormals.endergetic.core.keybinds.KeybindHandler;
import com.minecraftabnormals.endergetic.core.registry.EEBiomes;
import com.minecraftabnormals.endergetic.core.registry.EEEntities;
import com.minecraftabnormals.endergetic.core.registry.EESounds;
import com.minecraftabnormals.endergetic.core.registry.EETileEntities;
import com.minecraftabnormals.endergetic.core.registry.other.EEBlockRegistrars;
import com.minecraftabnormals.endergetic.core.registry.other.EEDataSerializers;
import com.minecraftabnormals.endergetic.core.registry.other.EEDispenserBehaviors;
import com.minecraftabnormals.endergetic.core.registry.other.EEEntityAttributes;
import com.minecraftabnormals.endergetic.core.registry.other.EEFlammables;
import com.minecraftabnormals.endergetic.core.registry.util.EndergeticRegistryHelper;
import com.teamabnormals.abnormals_core.core.library.api.AmbienceMusicPlayer;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EnderCrystalRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DeferredWorkQueue;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

@SuppressWarnings("deprecation")
@Mod(value = EndergeticExpansion.MOD_ID)
public class EndergeticExpansion {
	public static final String MOD_ID = "endergetic";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID.toUpperCase());
	public static final String NETWORK_PROTOCOL = "EE1";
	public static EndergeticExpansion instance;
	public static final EndergeticRegistryHelper REGISTRY_HELPER = new EndergeticRegistryHelper(MOD_ID);
	
	public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder.named(new ResourceLocation(MOD_ID, "net"))
		.networkProtocolVersion(() -> NETWORK_PROTOCOL)
		.clientAcceptedVersions(NETWORK_PROTOCOL::equals)
		.serverAcceptedVersions(NETWORK_PROTOCOL::equals)
		.simpleChannel();
    
	public EndergeticExpansion() {
		instance = this;
		
		this.setupMessages();
		EEDataSerializers.registerSerializers();
		
		final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		
		REGISTRY_HELPER.getDeferredItemRegister().register(modEventBus);
		REGISTRY_HELPER.getDeferredBlockRegister().register(modEventBus);
		REGISTRY_HELPER.getDeferredSoundRegister().register(modEventBus);
		REGISTRY_HELPER.getDeferredTileEntityRegister().register(modEventBus);
		REGISTRY_HELPER.getDeferredEntityRegister().register(modEventBus);
		EEParticles.PARTICLES.register(modEventBus);
		EESurfaceBuilders.SURFACE_BUILDERS.register(modEventBus);
		EEFeatures.FEATURES.register(modEventBus);
		EEBiomes.BIOMES.register(modEventBus);
		
		modEventBus.addListener((ModConfig.ModConfigEvent event) -> {
			final ModConfig config = event.getConfig();
			if (config.getSpec() == EEConfig.COMMON_SPEC) {
				EEConfig.ValuesHolder.updateCommonValuesFromConfig(config);
			}
		});
		
		DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
			modEventBus.addListener(EventPriority.LOWEST, this::registerItemColors);
			modEventBus.addListener(EventPriority.LOWEST, this::setupClient);
		});
		
		modEventBus.addListener(EventPriority.LOWEST, this::setupCommon);
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, EEConfig.COMMON_SPEC);
	}
	
	void setupCommon(final FMLCommonSetupEvent event) {
		DeferredWorkQueue.runLater(() -> {
			EEDispenserBehaviors.registerAll();
			EEFlammables.registerFlammables();
			EEBiomes.applyBiomeInfo();
			EEBlockRegistrars.registerFireInfo();
			EEWorldGenHandler.overrideFeatures();
			EEEntityAttributes.putAttributes();
		});
	}
	
	@OnlyIn(Dist.CLIENT)
	void setupClient(final FMLClientSetupEvent event) {
		EEBlockRegistrars.setupRenderLayers();
		
		ClientRegistry.bindTileEntityRenderer(EETileEntities.FRISBLOOM_STEM.get(), FrisbloomStemTileEntityRenderer::new);
		ClientRegistry.bindTileEntityRenderer(EETileEntities.CORROCK_CROWN.get(), CorrockCrownTileEntityRenderer::new);
		ClientRegistry.bindTileEntityRenderer(EETileEntities.ENDSTONE_COVER.get(), EndStoneCoverTileEntityRenderer::new);
		ClientRegistry.bindTileEntityRenderer(EETileEntities.BOLLOOM_BUD.get(), BolloomBudTileEntityRenderer::new);
		ClientRegistry.bindTileEntityRenderer(EETileEntities.PUFFBUG_HIVE.get(), PuffBugHiveTileEntityRenderer::new);
		ClientRegistry.bindTileEntityRenderer(EETileEntities.BOOF_BLOCK_DISPENSED.get(), BoofBlockTileEntityRenderer::new);
	
		RenderingRegistry.registerEntityRenderingHandler(EEEntities.BOLLOOM_FRUIT.get(), BolloomFruitRenderer::new);
		RenderingRegistry.registerEntityRenderingHandler(EEEntities.POISE_CLUSTER.get(), PoiseClusterRender::new);
		RenderingRegistry.registerEntityRenderingHandler(EEEntities.BOOF_BLOCK.get(), BoofBlockRenderer::new);
		RenderingRegistry.registerEntityRenderingHandler(EEEntities.BOLLOOM_KNOT.get(), BolloomKnotRenderer::new);
		RenderingRegistry.registerEntityRenderingHandler(EEEntities.BOLLOOM_BALLOON.get(), BolloomBalloonRenderer::new);
		RenderingRegistry.registerEntityRenderingHandler(EEEntities.PUFF_BUG.get(), PuffBugRenderer::new);
		RenderingRegistry.registerEntityRenderingHandler(EEEntities.BOOFLO_BABY.get(), BoofloBabyRenderer::new);
		RenderingRegistry.registerEntityRenderingHandler(EEEntities.BOOFLO_ADOLESCENT.get(), BoofloAdolescentRenderer::new);
		RenderingRegistry.registerEntityRenderingHandler(EEEntities.BOOFLO.get(), BoofloRenderer::new);
		
		KeybindHandler.registerKeys();
		
		AmbienceMusicPlayer.registerBiomeAmbientSoundPlayer(Arrays.asList(() -> EEBiomes.POISE_FOREST.get()), () -> EESounds.POISE_FOREST_LOOP.get(), () -> EESounds.POISE_FOREST_ADDITIONS.get(), () -> EESounds.POISE_FOREST_MOOD.get());
		EnderCrystalRenderer.field_229046_e_ = RenderType.getEntityCutoutNoCull(new ResourceLocation(MOD_ID, "textures/entity/end_crystal.png"));
	}
	
	@OnlyIn(Dist.CLIENT)
	private void registerItemColors(ColorHandlerEvent.Item event) {
		REGISTRY_HELPER.processSpawnEggColors(event);
	}
    
	private void setupMessages() {
		int id = -1;
		CHANNEL.registerMessage(id++, SUpdateNBTTagMessage.class, SUpdateNBTTagMessage::serialize, SUpdateNBTTagMessage::deserialize, SUpdateNBTTagMessage::handle);
		CHANNEL.registerMessage(id++, SSetCooldownMessage.class, SSetCooldownMessage::serialize, SSetCooldownMessage::deserialize, SSetCooldownMessage::handle);
		CHANNEL.registerMessage(id++, SBoofEntityMessage.class, SBoofEntityMessage::serialize, SBoofEntityMessage::deserialize, SBoofEntityMessage::handle);
		CHANNEL.registerMessage(id++, SInflateMessage.class, SInflateMessage::serialize, SInflateMessage::deserialize, SInflateMessage::handle);
		CHANNEL.registerMessage(id++, SIncrementBoostDelayMessage.class, SIncrementBoostDelayMessage::serialize, SIncrementBoostDelayMessage::deserialize, SIncrementBoostDelayMessage::handle);
		CHANNEL.registerMessage(id++, SSetPlayerNotBoostingMessage.class, SSetPlayerNotBoostingMessage::serialize, SSetPlayerNotBoostingMessage::deserialize, SSetPlayerNotBoostingMessage::handle);
		CHANNEL.registerMessage(id++, SSlamMessage.class, SSlamMessage::serialize, SSlamMessage::deserialize, SSlamMessage::handle);
		CHANNEL.registerMessage(id++, RotateMessage.class, RotateMessage::serialize, RotateMessage::deserialize, RotateMessage::handle);
		CHANNEL.registerMessage(id++, S2CRemoveBalloonFromOrderMap.class, S2CRemoveBalloonFromOrderMap::serialize, S2CRemoveBalloonFromOrderMap::deserialize, S2CRemoveBalloonFromOrderMap::handle);
	}
}