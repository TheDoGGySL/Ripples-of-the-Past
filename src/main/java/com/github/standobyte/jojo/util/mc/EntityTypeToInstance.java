package com.github.standobyte.jojo.util.mc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.github.standobyte.jojo.JojoMod;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.monster.SlimeEntity;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistries;

/* 
 * A map to hold entity instances which aren't added into a world, 
 * for cases where something specific for entity types
 * requires actual entity instances to exist by the vanilla code design
 * (entity texture paths, AI goals, etc.)
 */
public class EntityTypeToInstance {
    private static EntityTypeToInstance instance;
    
    public static void init(World world) {
        if (instance == null) {
            Iterable<EntityType<?>> entityTypes = ForgeRegistries.ENTITIES.getValues();
            instance = new EntityTypeToInstance(entityTypes, world);
        }
    }
    
    private final Map<EntityType<?>, Entity> entityMap;
    private final Set<EntityType<?>> tryLazyInit = new HashSet<>();
    private EntityTypeToInstance(Iterable<EntityType<?>> entityTypes, World world) {
        entityMap = new HashMap<>();
        for (EntityType<?> type : entityTypes) {
            try {
                Entity entity = createInstance(type, world);
                entityMap.put(type, entity);
            }
            catch (Exception e) {
                tryLazyInit.add(type);
                JojoMod.getLogger().warn("Failed to initialize an entity of type {} on world load. Will try lazy initialization.", type.getRegistryName());
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    public static <T extends Entity> T getEntityInstance(EntityType<T> type, World world) {
        if (instance == null) {
            JojoMod.getLogger().error("An operation with {} entity type needed an Entity instance, but the map for them hasn't been created yet!", type.getRegistryName());
            return null;
        }
        Entity entity = null;
        if (!instance.entityMap.containsKey(type) && instance.tryLazyInit.remove(type)) {
            entity = createInstance(type, world);
            if (entity != null) {
                instance.entityMap.put(type, entity);
            }
        }
        else {
            entity = instance.entityMap.get(type);
        }
        return (T) entity;
    }
    
    private static <T extends Entity> T createInstance(EntityType<T> type, World world) {
        T entity = type.create(world);
        if (entity instanceof SlimeEntity) {
            entity.refreshDimensions();
        }
        return entity;
    }
    
}
