package com.github.standobyte.jojo.capability.entity;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.entity.mob.rps.RockPaperScissorsGame;
import com.github.standobyte.jojo.network.PacketManager;
import com.github.standobyte.jojo.network.packets.fromserver.TrDoubleShiftPacket;
import com.github.standobyte.jojo.network.packets.fromserver.TrKnivesCountPacket;
import com.github.standobyte.jojo.network.packets.fromserver.TrWalkmanEarbudsPacket;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;

public class PlayerUtilCap {
    private final PlayerEntity player;
    
    private Set<OneTimeNotification> notificationsSent = EnumSet.noneOf(OneTimeNotification.class);
    
    private int knives;
    private int removeKnifeTime;
    
    public int knivesThrewTicks = 0;
    
    private boolean hasClientInput;
    private int noClientInputTimer;
    
    private Optional<RockPaperScissorsGame> currentGame = Optional.empty();
    
    private boolean walkmanEarbuds = false;
    
    private boolean doubleShiftPress = false;
    private boolean shiftSynced = false;
    
    public PlayerUtilCap(PlayerEntity player) {
        this.player = player;
    }
    
    
    
    public void tick() {
        if (!player.level.isClientSide()) {
            tickKnivesRemoval();
            tickVoiceLines();
            tickClientInputTimer();
            
            if (knivesThrewTicks > 0) {
                knivesThrewTicks--;
            }
        }
        
        tickDoubleShift();
    }
    
    
    
    public void setDoubleShiftPress() {
        doubleShiftPress = true;
        shiftSynced = player.isShiftKeyDown();
        if (!player.level.isClientSide()) {
            PacketManager.sendToClientsTracking(new TrDoubleShiftPacket(player.getId()), player);
        }
    }
    
    private void tickDoubleShift() {
        if (doubleShiftPress) {
            if (!shiftSynced) {
                if (player.isShiftKeyDown()) {
                    shiftSynced = true;
                }
            }
            else if (!player.isShiftKeyDown()) {
                doubleShiftPress = false;
            }
        }
    }
    
    public boolean getDoubleShiftPress() {
        return doubleShiftPress;
    }
    
    
    
    public void sendNotification(OneTimeNotification notification, ITextComponent message) {
        if (!sentNotification(notification)) {
            player.sendMessage(message, Util.NIL_UUID);
            setSentNotification(notification, true);
        }
    }
    
    public boolean sentNotification(OneTimeNotification notification) {
        return notificationsSent.contains(notification);
    }
    
    public void setSentNotification(OneTimeNotification notification, boolean sent) {
        if (sent) {
            notificationsSent.add(notification);
        }
        else {
            notificationsSent.remove(notification);
        }
    }
    
    public void moveNotificationsSet(PlayerUtilCap cap) {
        this.notificationsSent = cap.notificationsSent;
    }
    
    public static enum OneTimeNotification {
        POWER_CONTROLS,
        HAMON_WINDOW,
        HIGH_STAND_RANGE
    }
    
    
    
    public void setKnives(int knives) {
        knives = Math.max(knives, 0);
        if (this.knives != knives) {
            this.knives = knives;
            if (!player.level.isClientSide()) {
                PacketManager.sendToClientsTrackingAndSelf(new TrKnivesCountPacket(player.getId(), knives), player);
            }
        }
    }
    
    public void addKnife() {
        setKnives(knives + 1);
    }
    
    public int getKnivesCount() {
        return knives;
    }
    
    private void tickKnivesRemoval() {
        if (knives > 0) {
            if (removeKnifeTime <= 0) {
                removeKnifeTime = 20 * (30 - knives);
            }
            removeKnifeTime--;
            if (removeKnifeTime <= 0) {
                setKnives(knives - 1);
            }
        }
    }
    
    public void onTracking(ServerPlayerEntity tracking) {
        PacketManager.sendToClient(new TrKnivesCountPacket(player.getId(), knives), tracking);
        PacketManager.sendToClient(new TrWalkmanEarbudsPacket(player.getId(), walkmanEarbuds), tracking);
    }
    
    
    
    public void setHasClientInput(boolean hasInput) {
        this.hasClientInput = hasInput;
        if (hasClientInput) {
            noClientInputTimer = 0;
        }
    }
    
    private void tickClientInputTimer() {
        if (!hasClientInput) {
            noClientInputTimer++;
        }
    }
    
    public boolean hasClientInput() {
        return hasClientInput;
    }
    
    public int getNoClientInputTimer() {
        return noClientInputTimer;
    }
    
    
    
    public Optional<RockPaperScissorsGame> getCurrentRockPaperScissorsGame() {
        return currentGame;
    }
    
    public void setCurrentRockPaperScissorsGame(RockPaperScissorsGame game) {
        this.currentGame = game != null ? Optional.of(game) : Optional.empty();
    }
    
    
    
    public void setEarbuds(boolean earbuds) {
        if (this.walkmanEarbuds != earbuds) {
            this.walkmanEarbuds = earbuds;
            if (!player.level.isClientSide()) {
                PacketManager.sendToClientsTrackingAndSelf(new TrWalkmanEarbudsPacket(player.getId(), walkmanEarbuds), player);
            }
        }
    }
    
    public boolean hasEarbuds() {
        return walkmanEarbuds;
    }
    
    
    
    private Map<SoundEvent, Integer> recentlyPlayedVoiceLines = new HashMap<>();
    
    @Nullable
    public boolean checkNotRepeatingVoiceLine(SoundEvent voiceLine, int voiceLineDelay) {
        if (recentlyPlayedVoiceLines.containsKey(voiceLine) && recentlyPlayedVoiceLines.get(voiceLine) > 0) {
            return false;
        }
        recentlyPlayedVoiceLines.put(voiceLine, voiceLineDelay);
        return true;
    }
    
    private void tickVoiceLines() {
        for (Map.Entry<SoundEvent, Integer> voiceLine : recentlyPlayedVoiceLines.entrySet()) {
            int ticks = voiceLine.getValue();
            if (ticks > 0) {
                voiceLine.setValue(--ticks);
            }
        }
    }

}
