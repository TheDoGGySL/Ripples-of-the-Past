package com.github.standobyte.jojo.action.non_stand;

import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.player.ContinuousActionInstance;
import com.github.standobyte.jojo.action.player.IPlayerAction;
import com.github.standobyte.jojo.capability.entity.PlayerUtilCap;
import com.github.standobyte.jojo.client.ClientUtil;
import com.github.standobyte.jojo.client.playeranim.anim.ModPlayerAnimations;
import com.github.standobyte.jojo.init.ModSounds;
import com.github.standobyte.jojo.power.impl.nonstand.INonStandPower;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class PillarmanHeavyPunch extends PillarmanAction implements IPlayerAction<PillarmanHeavyPunch.Instance, INonStandPower> {

    public PillarmanHeavyPunch(PillarmanAction.Builder builder) {
        super(builder);
    }
    
    
    @Override
    protected void perform(World world, LivingEntity user, INonStandPower power, ActionTarget target) {
        if (!world.isClientSide()) {
            setPlayerAction(user, power);
        }
    }
    
    @Override
    public PillarmanHeavyPunch.Instance createContinuousActionInstance(
            LivingEntity user, PlayerUtilCap userCap, INonStandPower power) {
        if (user.level.isClientSide() && user instanceof PlayerEntity) {
            ModPlayerAnimations.pillarManPunch.setAnimEnabled((PlayerEntity) user, true);
        }
        return new Instance(user, userCap, power, this);
    }
    
    
    @Override
    public void setCooldownOnUse(INonStandPower power) {}
    
    @Override
    protected void consumeEnergy(World world, LivingEntity user, INonStandPower power, ActionTarget target) {}
    
    
    public static class Instance extends ContinuousActionInstance<PillarmanHeavyPunch, INonStandPower> {
        
        public Instance(LivingEntity user, PlayerUtilCap userCap, 
                INonStandPower playerPower, PillarmanHeavyPunch action) {
            super(user, userCap, playerPower, action);
        }
        
        @Override
        public void playerTick() {
            switch (getTick()) {
            case 3:
                if (user.level.isClientSide()) {
                    user.level.playSound(ClientUtil.getClientPlayer(), user.getX(), user.getEyeY(), user.getZ(), 
                            ModSounds.PILLAR_MAN_SWING.get(), user.getSoundSource(), 1.0f, 1.25f);
                    user.swing(Hand.MAIN_HAND, true);
                }
                break;
            case 5:
                if (!user.level.isClientSide()) {
                    ActionTarget target = playerPower.getMouseTarget();
                    VampirismClawLacerate.punchPerform(user.level, user, playerPower, target, ModSounds.PILLAR_MAN_PUNCH.get(), 1.2F, 0.8F);
                }
                break;
            case 8:
                stopAction();
                break;
            }
        }
        
        @Override
        public boolean updateTarget() {
            return true;
        }
        
        
        @Override
        public float getWalkSpeed() {
            return 0.5f;
        }
        
        @Override
        public void onStop() {
            super.onStop();
            if (user.level.isClientSide() && user instanceof PlayerEntity) {
                ModPlayerAnimations.pillarManPunch.setAnimEnabled((PlayerEntity) user, false);
            }
        }
        
    }

}
