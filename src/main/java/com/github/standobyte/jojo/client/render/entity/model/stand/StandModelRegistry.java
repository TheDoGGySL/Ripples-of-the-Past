package com.github.standobyte.jojo.client.render.entity.model.stand;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.JojoMod;
import com.github.standobyte.jojo.client.render.entity.model.animnew.stand.GeckoStandAnimator;
import com.github.standobyte.jojo.entity.stand.StandEntity;

import net.minecraft.util.ResourceLocation;

public class StandModelRegistry {
    private static final Map<ResourceLocation, StandModelRegistryObj> STAND_MODELS = new HashMap<>();
    
    
    public static <T extends StandEntity, M extends StandEntityModel<T>> M registerModel(
            ResourceLocation modelId, Supplier<? extends M> constructor) {
        try {
            M model = constructor.get();
            registerStandModel(model, modelId, constructor);
            return model;
        }
        catch (Exception e) {
            JojoMod.getLogger().error(e);
            throw e;
        }
    }
    
    @Nullable
    public static StandModelRegistryObj getRegisteredModel(ResourceLocation id) {
        return STAND_MODELS.get(id);
    }
    
    @Nullable
    public static <T extends StandEntity, M extends StandEntityModel<T>> M getBaseModel(ResourceLocation id) {
        return (M) STAND_MODELS.get(id).baseModel;
    }
    
    private static final void registerStandModel(StandEntityModel<?> baseModel, ResourceLocation modelId, Supplier<? extends StandEntityModel<?>> constructor) {
        synchronized (STAND_MODELS) {
            if (baseModel.modelId != null) {
                JojoMod.getLogger().error("Tried to register {} Stand model object twice!", modelId);
            }
            else if (STAND_MODELS.values().stream().anyMatch(modelEntry -> modelEntry.id.equals(modelId))) {
                JojoMod.getLogger().error("Using duplicate id {} for a Stand model!", modelId);
            }
            else {
                StandModelRegistryObj registryObj = new StandModelRegistryObj(modelId, baseModel, constructor);
                STAND_MODELS.put(modelId, registryObj);
                baseModel.modelId = modelId;
                baseModel.registryObj = registryObj;
            }
        }
    }
    
    public static Collection<StandModelRegistryObj> values() {
        return STAND_MODELS.values();
    }
    
    
    
    public static final class StandModelRegistryObj {
        public final ResourceLocation id;
        public final StandEntityModel<?> baseModel;
        private final Supplier<? extends StandEntityModel<?>> modelConstructor;
        private GeckoStandAnimator anims;
        
        StandModelRegistryObj(ResourceLocation id, StandEntityModel<?> baseModel, Supplier<? extends StandEntityModel<?>> modelConstructor) {
            this.id = id;
            this.baseModel = baseModel;
            this.modelConstructor = modelConstructor;
        }
        
        public StandEntityModel<?> createNewModelCopy() {
            StandEntityModel<?> model = modelConstructor.get();
            model.modelId = id;
            model.registryObj = baseModel.registryObj;
            return model;
        }
        
        
        @Nullable
        public GeckoStandAnimator getDefaultGeckoAnims() {
            return anims;
        }
        
        
        public void beforeGeckoAnimReload() {
            this.anims = null;
        }
        
        public void onGeckoAnimLoaded(GeckoStandAnimator anims) {
            this.anims = anims;
        }
        
//        @Nullable
//        public StandActionAnimation getAnim(String animName) {
//            return anims != null ? anims.getNamedAnim(animName) : null;
//        }
    }
}
