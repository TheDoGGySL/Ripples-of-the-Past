package com.github.standobyte.jojo.entity.damaging.projectile;

import com.github.standobyte.jojo.client.render.item.CustomIconItem;
import com.github.standobyte.jojo.client.render.item.CustomIconItem.RegularIcon;
import com.github.standobyte.jojo.init.ModBlocks;
import com.github.standobyte.jojo.init.ModEntityTypes;
import com.github.standobyte.jojo.util.mc.damage.DamageUtil;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.IRendersAsItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.ForgeEventFactory;

@OnlyIn(value = Dist.CLIENT, _interface = IRendersAsItem.class) // without this OnlyIn annotation dedicated server will crash
public class MRFireballEntity extends ModdedProjectileEntity implements IRendersAsItem {
    private static ItemStack MR_FIREBALL_SPRITE_ITEM = ItemStack.EMPTY;
    
    public MRFireballEntity(LivingEntity shooter, World world) {
        super(ModEntityTypes.MR_FIREBALL.get(), shooter, world);
    }

    public MRFireballEntity(EntityType<? extends MRFireballEntity> type, World world) {
        super(type, world);
    }

    @Override
    public boolean standDamage() {
        return true;
    }
    
    @Override
    public float getBaseDamage() {
        return 2.0F;
    }
    
    @Override
    protected boolean hurtTarget(Entity target, LivingEntity owner) {
        return DamageUtil.dealDamageAndSetOnFire(target, 
                entity -> super.hurtTarget(entity, owner), 10, true);
    }
    
    @Override
    protected void afterBlockHit(BlockRayTraceResult blockRayTraceResult, boolean blockDestroyed) {
        if (!level.isClientSide) {
            if (ForgeEventFactory.getMobGriefingEvent(level, getEntity())) {
                BlockPos blockPos = blockDestroyed ? blockRayTraceResult.getBlockPos() : 
                    blockRayTraceResult.getBlockPos().relative(blockRayTraceResult.getDirection());
                if (level.isEmptyBlock(blockPos)) {
                    level.setBlockAndUpdate(blockPos, ModBlocks.MAGICIANS_RED_FIRE.get().getStateForPlacement(level, blockPos));
                }
            }
        }
    }

//    @Override
//    public void tick() {
//        if (isInWaterOrRain()) {
//            clearFire();
//        }
//        else {
//            super.tick();
//        }
//    }
//    
//    @Override
//    public void clearFire() {
//        super.clearFire();
//        if (!level.isClientSide()) {
//            JojoModUtil.extinguishFieryStandEntity(this, (ServerWorld) level);
//        }
//    }

    @Override
    public ItemStack getItem() {
        if (MR_FIREBALL_SPRITE_ITEM.isEmpty()) {
            MR_FIREBALL_SPRITE_ITEM = CustomIconItem.makeIconItem(RegularIcon.MR_FIREBALL);
        }
        return MR_FIREBALL_SPRITE_ITEM;
    }

    @Override
    public boolean isOnFire() {
        return true;
    }
    
    @Override
    public boolean isFiery() {
        return true;
    }
    
    @Override
    protected float getMaxHardnessBreakable() {
        return 5F;
    }
    
    @Override
    public int ticksLifespan() {
        return 100;
    }
}
