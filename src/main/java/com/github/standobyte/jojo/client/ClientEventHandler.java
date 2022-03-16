package com.github.standobyte.jojo.client;

import static net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType.AIR;
import static net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType.FOOD;

import java.util.Random;

import com.github.standobyte.jojo.JojoMod;
import com.github.standobyte.jojo.JojoModConfig;
import com.github.standobyte.jojo.client.sound.StandOstSound;
import com.github.standobyte.jojo.client.ui.hud.ActionsOverlayGui;
import com.github.standobyte.jojo.init.ModActions;
import com.github.standobyte.jojo.init.ModEffects;
import com.github.standobyte.jojo.init.ModNonStandPowers;
import com.github.standobyte.jojo.power.IPower.ActionType;
import com.github.standobyte.jojo.power.nonstand.INonStandPower;
import com.github.standobyte.jojo.power.stand.IStandPower;
import com.github.standobyte.jojo.util.TimeHandler;
import com.github.standobyte.jojo.util.reflection.ClientReflection;
import com.google.common.base.MoreObjects;
import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.renderer.FirstPersonRenderer;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.Timer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderNameplateEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.RenderTickEvent;
import net.minecraftforge.event.entity.living.PotionEvent.PotionAddedEvent;
import net.minecraftforge.event.entity.living.PotionEvent.PotionExpiryEvent;
import net.minecraftforge.event.entity.living.PotionEvent.PotionRemoveEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = JojoMod.MOD_ID, value = Dist.CLIENT)
public class ClientEventHandler {
    private static ClientEventHandler instance = null;
    
    private Minecraft mc;

    private Timer clientTimer;
    private boolean isTimeStopped = false;
    private boolean canSeeInStoppedTime = true;
    private boolean canMoveInStoppedTime = true;
    private float partialTickStoppedAt;
    private static final ResourceLocation SHADER_TIME_STOP = new ResourceLocation("shaders/post/desaturate.json");
    
    private ResourceLocation resolveShader = null;
    private StandOstSound ost;
    
    private boolean resetShader;
    private double zoomModifier;
    public boolean isZooming;
    
    private int deathScreenTick;

    private ClientEventHandler(Minecraft mc) {
        this.mc = mc;
        this.clientTimer = ClientReflection.getTimer(mc);
    }

    public static void init(Minecraft mc) {
        if (instance == null) {
            instance = new ClientEventHandler(mc);
            MinecraftForge.EVENT_BUS.register(instance);
        }
    }
    
    public static ClientEventHandler getInstance() {
        return instance;
    }
    
    
    
    private boolean isTimeStopped(BlockPos blockPos) {
        return isTimeStopped(new ChunkPos(blockPos));
    }
    
    private boolean isTimeStopped(ChunkPos chunkPos) {
        return mc.level != null && TimeHandler.isTimeStopped(mc.level, chunkPos);
    }
    
    public void setTimeStopClientState(int ticks, ChunkPos chunkPos, boolean canSee, boolean canMove) {
        if (ticks > 0) {
            canSeeInStoppedTime = canSee;
            canMoveInStoppedTime = canSee && canMove;
            partialTickStoppedAt = canMove ? mc.getFrameTime() : 0.0F;
        }
        else {
            canSeeInStoppedTime = true;
            canMoveInStoppedTime = true;
            resetShader = true;
        }
    }
    
    public void updateCanMoveInStoppedTime(boolean canMove, ChunkPos chunkPos) {
        if (isTimeStopped(chunkPos)) {
            this.canMoveInStoppedTime = canMove;
        }
    }
    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public <T extends LivingEntity, M extends EntityModel<T>> void onRenderLiving(RenderLivingEvent.Pre<T, M> event) {
        LivingEntity entity = event.getEntity();
        if (isTimeStopped(entity.blockPosition())) {
            if (!entity.canUpdate()) {
                if (event.getPartialRenderTick() != partialTickStoppedAt) {
                    event.getRenderer().render((T) entity, MathHelper.lerp(partialTickStoppedAt, entity.yRotO, entity.yRot), 
                            partialTickStoppedAt, event.getMatrixStack(), event.getBuffers(), event.getLight());
                    event.setCanceled(true);
                }
                return;
            }
        }
        INonStandPower.getNonStandPowerOptional(entity).ifPresent(power -> {
            if (power.getHeldAction(true) == ModActions.ZEPPELI_TORNADO_OVERDRIVE.get()) {
                event.getMatrixStack().mulPose(Vector3f.YP.rotation((power.getHeldActionTicks() + event.getPartialRenderTick()) * 2F % 360F));
            }
            if (power.isActionOnCooldown(ModActions.HAMON_ZOOM_PUNCH.get())) {
                M model = event.getRenderer().getModel();
                if (model instanceof BipedModel) {
                    ModelRenderer arm = entity.getMainArm() == HandSide.LEFT ? ((BipedModel<?>) model).leftArm : ((BipedModel<?>) model).rightArm;
                    arm.visible = false;
                    if (model instanceof PlayerModel) {
                        arm = entity.getMainArm() == HandSide.LEFT ? ((PlayerModel<?>) model).leftArm : ((PlayerModel<?>) model).rightArm;
                        arm.visible = false;
                    }
                }
            }
        });
    }
    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public <T extends LivingEntity, M extends EntityModel<T>> void onRenderNameplate(RenderNameplateEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof LivingEntity) {
            INonStandPower.getNonStandPowerOptional((LivingEntity) entity).ifPresent(power -> {
                if (power.getHeldAction(true) == ModActions.ZEPPELI_TORNADO_OVERDRIVE.get()) {
                    event.getMatrixStack().mulPose(Vector3f.YP.rotation((power.getHeldActionTicks() + event.getPartialTicks()) * -2F % 360F));
                }
            });
        }
    }
    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderTick(RenderTickEvent event) {
        if (mc.level != null && mc.player.isAlive() && isTimeStopped(mc.player.blockPosition()) && event.phase == TickEvent.Phase.START) {
            if (!canSeeInStoppedTime) {
                clientTimer.partialTick = 0.0F;
            }
        }
    }
    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onClientTick(ClientTickEvent event) {
        if (mc.level != null) {
            if (event.phase == TickEvent.Phase.START) {
                ActionsOverlayGui.getInstance().tick();
            }
            isTimeStopped = isTimeStopped(mc.player.blockPosition());
            
            switch (event.phase) {
            case START:
                if (isTimeStopped && !canSeeInStoppedTime) {
                    ClientReflection.pauseClient(mc);
                }
                tickResolveEffect();
                break;
            case END:
                if (resetShader) {
                    mc.gameRenderer.checkEntityPostEffect(mc.options.getCameraType().isFirstPerson() ? mc.getCameraEntity() : null);
                    resetShader = false;
                }
                break;
            }
            
            if (mc.gameRenderer.currentEffect() == null) {
                ResourceLocation shader = getCurrentShader();
                if (shader != null) {
                    mc.gameRenderer.loadEffect(shader);
                }
            }
            
            if (mc.screen instanceof DeathScreen) {
                deathScreenTick++;
            }
            else {
                deathScreenTick = 0;
            }
        }
    }
    
    private ResourceLocation getCurrentShader() {
        if (JojoModConfig.CLIENT.resolveShaders.get() && resolveShader != null) {
            return resolveShader;
        }
        if (isTimeStopped && canSeeInStoppedTime) {
            return SHADER_TIME_STOP;
        }
        return null;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onResolveEffectStart(PotionAddedEvent event) {
        if (event.getOldPotionEffect() == null && event.getPotionEffect().getEffect() == ModEffects.RESOLVE.get()) {
            if (resolveShader == null) {
                resolveShader = HueShiftShaders.SHADERS_HUE_SHIFT[new Random().nextInt(HueShiftShaders.SHADERS_HUE_SHIFT.length)];
            }
            
            if (ost == null || ost.isStopped()) {
                SoundEvent ostSound = IStandPower.getStandPowerOptional(mc.player).map(stand -> {
                    if (stand.hasPower()) {
                        return stand.getType().getOst();
                    }
                    return null;
                }).orElse(null);
                if (ostSound != null) {
                    ost = new StandOstSound(ostSound, mc);
                    mc.getSoundManager().play(ost);
                }
            }
        }
    }
    
    private void tickResolveEffect() {
        if (mc.player.isAlive() && mc.player.hasEffect(ModEffects.RESOLVE.get())) {
            if (mc.player.getEffect(ModEffects.RESOLVE.get()).getDuration() == 100) {
                fadeAwayOst(200);
            }
        }
        else {
            stopResolveShader();
            fadeAwayOst(0);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onResolveEffectOver(PotionExpiryEvent event) {
        if (event.getPotionEffect().getEffect() == ModEffects.RESOLVE.get()) {
            stopResolveShader();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onResolveEffectRemoved(PotionRemoveEvent event) {
        if (event.getPotionEffect() != null && event.getPotionEffect().getEffect() == ModEffects.RESOLVE.get()) {
            stopResolveShader();
            fadeAwayOst(50);
        }
    }
    
    private void stopResolveShader() {
        if (resolveShader != null) {
            resetShader = true;
            resolveShader = null;
        }
    }
    
    private void fadeAwayOst(int fadeAwayTicks) {
        if (ost != null) {
            ost.setFadeAway(fadeAwayTicks);
            ost = null;
        }
    }
    
    
    
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void zoom(EntityViewRenderEvent.FOVModifier event) {
        if (isZooming) {
            zoomModifier = Math.min(zoomModifier + mc.getDeltaFrameTime() / 3F, 60);
        }
        else if (zoomModifier > 1) {
            zoomModifier = Math.max(zoomModifier - mc.getDeltaFrameTime() * 2F, 1);
        }
        if (zoomModifier > 1) {
            event.setFOV(event.getFOV() / zoomModifier);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void disableFoodBar(RenderGameOverlayEvent.Pre event) {
        if (event.getType() == FOOD || event.getType() == AIR) {
            INonStandPower.getNonStandPowerOptional(mc.player).ifPresent(power -> {
                if (power.getType() == ModNonStandPowers.VAMPIRISM.get()) {
                    event.setCanceled(true);
                }
            });
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onRenderHand(RenderHandEvent event) {
        if (event.getHand() == Hand.MAIN_HAND) {
            Entity entity = Minecraft.getInstance().getCameraEntity();
            if (entity instanceof LivingEntity) {
                INonStandPower.getNonStandPowerOptional((LivingEntity) entity).ifPresent(power -> {
                    if (power.isActionOnCooldown(ModActions.HAMON_ZOOM_PUNCH.get())) {
                        event.setCanceled(true);
                    }
                    else {
                        ActionsOverlayGui hud = ActionsOverlayGui.getInstance();
                        if (hud.getSelectedAction(ActionType.ATTACK) == ModActions.JONATHAN_OVERDRIVE_BARRAGE.get()) {
                            FirstPersonRenderer renderer = mc.getItemInHandRenderer();
                            ClientPlayerEntity player = mc.player;
                            Hand swingingArm = MoreObjects.firstNonNull(player.swingingArm, Hand.MAIN_HAND);
                            float f6 = swingingArm == Hand.OFF_HAND ? player.getAttackAnim(event.getPartialTicks()) : 0.0F;
                            float f7 = 1.0F - MathHelper.lerp(event.getPartialTicks(), ClientReflection.getOffHandHeightPrev(renderer), ClientReflection.getOffHandHeight(renderer));
                            MatrixStack matrixStack = event.getMatrixStack();
                            matrixStack.pushPose();
                            ClientReflection.renderPlayerArm(matrixStack, event.getBuffers(), event.getLight(), f7, f6, player.getMainArm().getOpposite(), renderer);
                            matrixStack.popPose();
                            // i've won... but at what cost?
                        }
                    }
                });
            }
        }
    }
    
    
    
    private static final ResourceLocation ADDITIONAL_UI = new ResourceLocation(JojoMod.MOD_ID, "textures/gui/additional.png");
    @SubscribeEvent
    public void afterScreenRender(DrawScreenEvent.Post event) {
        if (event.getGui() instanceof DeathScreen) {
            ITextComponent title = event.getGui().getTitle();
            if (title instanceof TranslationTextComponent && ((TranslationTextComponent) title).getKey().endsWith(".hardcore")) {
                return;
            }
            int x = event.getGui().width - 5 - 
                    (int) ((event.getGui().width - 10) * Math.min(deathScreenTick + event.getRenderPartialTicks(), 20F) / 20F);
            int y = event.getGui().height - 29;
            mc.textureManager.bind(ADDITIONAL_UI);
            event.getGui().blit(event.getMatrixStack(), x, y, 0, 0, 130, 25);
            AbstractGui.drawCenteredString(event.getMatrixStack(), mc.font, new TranslationTextComponent("jojo.to_be_continued"), x + 61, y + 8, 0x525544);
        }
    }
}
