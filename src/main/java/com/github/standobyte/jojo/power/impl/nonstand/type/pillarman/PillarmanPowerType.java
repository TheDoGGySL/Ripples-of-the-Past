package com.github.standobyte.jojo.power.impl.nonstand.type.pillarman;

import com.github.standobyte.jojo.JojoModConfig;
import com.github.standobyte.jojo.action.Action;
import com.github.standobyte.jojo.action.non_stand.PillarmanAction;
import com.github.standobyte.jojo.client.controls.ControlScheme;
import com.github.standobyte.jojo.init.power.non_stand.pillarman.ModPillarmanActions;
import com.github.standobyte.jojo.power.impl.nonstand.INonStandPower;
import com.github.standobyte.jojo.power.impl.nonstand.type.NonStandPowerType;
import com.github.standobyte.jojo.power.impl.nonstand.type.pillarman.PillarmanData.Mode;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.util.general.GeneralUtil;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.Effects;
import net.minecraft.world.World;

public class PillarmanPowerType extends NonStandPowerType<PillarmanData> {
    public static final int COLOR = 0xFFAA00;

    public PillarmanPowerType(PillarmanAction[] startingAttacks, PillarmanAction[] startingAbilities, PillarmanAction defaultMmb) {
        super(startingAttacks, startingAbilities, defaultMmb, PillarmanData::new);
    }

    public PillarmanPowerType(PillarmanAction[] startingAttacks, PillarmanAction[] startingAbilities) {
        super(startingAttacks, startingAbilities, startingAttacks[0], PillarmanData::new);
    }
    
    @Override
    public boolean keepOnDeath(INonStandPower power) {
        return true;
    }
    
    @Override
    public void onClear(INonStandPower power) {
        super.onClear(power);
        power.getTypeSpecificData(this).ifPresent(PillarmanData::onClear);
    }
    
    @Override
    public float getMaxEnergy(INonStandPower power) {
        World world = power.getUser().level;
        return super.getMaxEnergy(power) * GeneralUtil.getOrLast(
                JojoModConfig.getCommonConfigInstance(world.isClientSide()).maxBloodMultiplier.get(), world.getDifficulty().getId())
                .floatValue() * power.getTypeSpecificData(this).get().getEvolutionStage();
    }
    
    @Override
    public float tickEnergy(INonStandPower power) {
        World world = power.getUser().level;
        float inc = -GeneralUtil.getOrLast(
                JojoModConfig.getCommonConfigInstance(world.isClientSide()).bloodTickDown.get(), world.getDifficulty().getId())
                .floatValue();
        if (power.isUserCreative()) {
            inc = Math.max(inc, 0);
        }
        return power.getEnergy() + inc * power.getTypeSpecificData(this).get().getEvolutionStage();
    }
    
    @Override
    public float getMaxStaminaFactor(INonStandPower power, IStandPower standPower) {
        return 1 * power.getTypeSpecificData(this).get().getEvolutionStage();
    }
    
    @Override
    public float getStaminaRegenFactor(INonStandPower power, IStandPower standPower) {
        return 1 * power.getTypeSpecificData(this).get().getEvolutionStage();
    }

    @Override
    public void tickUser(LivingEntity entity, INonStandPower power) {
        super.tickUser(entity, power);
        PillarmanData pillarman = power.getTypeSpecificData(this).get();
        pillarman.tick();
        if (!entity.level.isClientSide()) {
            if (pillarman.getEvolutionStage() > 1) {
                if (entity instanceof PlayerEntity) {
                    ((PlayerEntity) entity).getFoodData().setFoodLevel(17);
                }
                entity.setAirSupply(entity.getMaxAirSupply());
            }
            if (pillarman.needsEffectsRefresh(power)) {
                updatePassiveEffects(entity, power);
            }
        }
    }

    @Override
    protected void initPassiveEffects() {
        initAllPossibleEffects(
                () -> Effects.REGENERATION,
                () -> Effects.NIGHT_VISION);
    }
    
    @Override
    public int getPassiveEffectLevel(Effect effect, INonStandPower power) {
        PillarmanData pillarman = power.getTypeSpecificData(this).get();
        if (pillarman.getEvolutionStage() == 1) return -1;
        
        if (effect == Effects.REGENERATION) {
            float energyRatio = power.getEnergy() / power.getMaxEnergy();
            if (energyRatio >= 0.3f) {
                return 1;
            }
            if (energyRatio >= 0.1f) {
                return 0;
            }
            return -1;
        }
        if (effect == Effects.NIGHT_VISION) {
            return 0;
        }
        return -1;
    }
    
    @Override
    public boolean isReplaceableWith(NonStandPowerType<?> newType) {
        return false;
    }

    // TODO
    @Override
    public float getTargetResolveMultiplier(INonStandPower power, IStandPower attackingStand) {
        LivingEntity entity = power.getUser();
        if (entity != null) {
            return (float) Math.pow(2, Math.max(entity.level.getDifficulty().getId() - 1, 0));
        }
        return 1;
    }
    
    @Override
    public boolean isLeapUnlocked(INonStandPower power) {
        return true;
    }
    
    @Override
    public float getLeapStrength(INonStandPower power) {
        float leapStrength = 2F + Math.min(power.getTypeSpecificData(this).get().getEvolutionStage(), 2.25F) / 2;
        return leapStrength * 0.6F;
    }
    
    @Override
    public int getLeapCooldownPeriod() {
        return 20;
    }
    
    @Override
    public float getLeapEnergyCost() {
        return 0;
    }
    
    public boolean isHighLifeForce(LivingEntity entity) {
        if (INonStandPower.getNonStandPowerOptional(entity).resolve().flatMap(power -> power.getTypeSpecificData(this)).map(
                pillarman -> pillarman.getEvolutionStage() > 1).orElse(false)) {
            return true;
        }
        return false;
    }
    
    
    @Override
    public void clAddMissingActions(ControlScheme controlScheme, INonStandPower power) {
        super.clAddMissingActions(controlScheme, power);
        
        PillarmanData pillarman = power.getTypeSpecificData(this).get();
        
        if (pillarman.getEvolutionStage() > 1) {
            controlScheme.addIfMissing(ControlScheme.Hotbar.LEFT_CLICK, ModPillarmanActions.PILLARMAN_ABSORPTION.get());
            controlScheme.addIfMissing(ControlScheme.Hotbar.LEFT_CLICK, ModPillarmanActions.PILLARMAN_HORN_ATTACK.get());
            controlScheme.addIfMissing(ControlScheme.Hotbar.LEFT_CLICK, ModPillarmanActions.PILLARMAN_RIBS_BLADES.get());
            controlScheme.addIfMissing(ControlScheme.Hotbar.RIGHT_CLICK, ModPillarmanActions.PILLARMAN_REGENERATION.get());
            controlScheme.addIfMissing(ControlScheme.Hotbar.RIGHT_CLICK, ModPillarmanActions.PILLARMAN_ENHANCED_SENSES.get());
            controlScheme.addIfMissing(ControlScheme.Hotbar.RIGHT_CLICK, ModPillarmanActions.PILLARMAN_HIDE_IN_ENTITY.get());
            controlScheme.addIfMissing(ControlScheme.Hotbar.RIGHT_CLICK, ModPillarmanActions.PILLARMAN_EVASION.get());
        }
        switch (pillarman.getMode()) {
        case WIND:
            controlScheme.addIfMissing(ControlScheme.Hotbar.LEFT_CLICK, ModPillarmanActions.PILLARMAN_SMALL_SANDSTORM.get());
            controlScheme.addIfMissing(ControlScheme.Hotbar.LEFT_CLICK, ModPillarmanActions.PILLARMAN_ATMOSPHERIC_RIFT.get());
            controlScheme.addIfMissing(ControlScheme.Hotbar.RIGHT_CLICK, ModPillarmanActions.PILLARMAN_WIND_CLOAK.get());
            break;
        case HEAT:
            controlScheme.addIfMissing(ControlScheme.Hotbar.LEFT_CLICK, ModPillarmanActions.PILLARMAN_ERRATIC_BLAZE_KING.get());
            controlScheme.addIfMissing(ControlScheme.Hotbar.LEFT_CLICK, ModPillarmanActions.PILLARMAN_GIANT_CARTHWHEEL_PRISON.get());
            controlScheme.addIfMissing(ControlScheme.Hotbar.LEFT_CLICK, ModPillarmanActions.PILLARMAN_SELF_DETONATION.get());
            break;
        case LIGHT:
            controlScheme.addIfMissing(ControlScheme.Hotbar.RIGHT_CLICK, ModPillarmanActions.PILLARMAN_LIGHT_FLASH.get());
            controlScheme.addIfMissing(ControlScheme.Hotbar.LEFT_CLICK, ModPillarmanActions.PILLARMAN_BLADE_DASH_ATTACK.get());
            controlScheme.addIfMissing(ControlScheme.Hotbar.LEFT_CLICK, ModPillarmanActions.PILLARMAN_BLADE_BARRAGE.get());
            break;
        default:
            break;
        }
    }
    
    @Override
    public boolean isActionLegalInHud(Action<INonStandPower> action, INonStandPower power) {
        if (super.isActionLegalInHud(action, power)) {
            return true;
        }
        
        if (action instanceof PillarmanAction) {
            PillarmanAction pmAction = (PillarmanAction) action;
            PillarmanData pillarman = power.getTypeSpecificData(this).get();
            return (pmAction.getPillarManStage() == -1 || pmAction.getPillarManStage() <= pillarman.getEvolutionStage())
                    && (pmAction.getPillarManMode() == Mode.NONE || pmAction.getPillarManMode() == pillarman.getMode());
        }
        
        return false;
    }
}
