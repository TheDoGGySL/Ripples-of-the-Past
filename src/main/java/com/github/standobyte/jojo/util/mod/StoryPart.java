package com.github.standobyte.jojo.util.mod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

public enum StoryPart {
    PHANTOM_BLOOD           (new TranslationTextComponent("jojo.story_part.1").withStyle(TextFormatting.DARK_BLUE)),
    BATTLE_TENDENCY         (new TranslationTextComponent("jojo.story_part.2").withStyle(TextFormatting.GREEN)),
    STARDUST_CRUSADERS      (new TranslationTextComponent("jojo.story_part.3").withStyle(TextFormatting.DARK_PURPLE)),
    DIAMOND_IS_UNBREAKABLE  (new TranslationTextComponent("jojo.story_part.4").withStyle(TextFormatting.RED)),
    GOLDEN_WIND             (new TranslationTextComponent("jojo.story_part.5").withStyle(TextFormatting.GOLD)),
    STONE_OCEAN             (new TranslationTextComponent("jojo.story_part.6").withStyle(TextFormatting.AQUA)),
    STEEL_BALL_RUN          (new TranslationTextComponent("jojo.story_part.7").withStyle(TextFormatting.LIGHT_PURPLE)),
    JOJOLION                (new TranslationTextComponent("jojo.story_part.8").withStyle(TextFormatting.WHITE)),
    THE_JOJOLANDS           (new TranslationTextComponent("jojo.story_part.9").withStyle(TextFormatting.BLUE)),
    OTHER                   (new TranslationTextComponent("jojo.story_part.none").withStyle(TextFormatting.GRAY));
    
    private final ITextComponent name;
    
    private StoryPart(ITextComponent name) {
        this.name = name;
    }
    
    public ITextComponent getName() {
        return name;
    }
    
    
    public static final List<ITextComponent> PART_NAMES_SORTED = Arrays.stream(StoryPart.values())
            .map(StoryPart::getName)
            .collect(Collectors.toCollection(ArrayList::new));
    
    public static Comparator<ITextComponent> partNamesComparator() {
        return (n1, n2) -> {
            int index1 = PART_NAMES_SORTED.indexOf(n1);
            int index2 = PART_NAMES_SORTED.indexOf(n2);
            if (index2 < 0) return -1;
            if (index1 < 0) return 1;
            return index1 - index2;
        };
    }
    
    /**
     * Example:<br><br>
     * 
     * <code>public static final ITextComponent PHF_NAME = new TranslationTextComponent("purple_haze_feedback_name");<br>
     * static { StoryPart.addToOrder(PHF_NAME, StoryPart.GOLDEN_WIND.getName()); }</code>
     */
    public static void addToOrder(ITextComponent storyPartName, ITextComponent putAfter) {
        if (!PART_NAMES_SORTED.contains(storyPartName)) {
            int index = PART_NAMES_SORTED.indexOf(putAfter);
            if (index > -1) {
                PART_NAMES_SORTED.add(index + 1, storyPartName);
            }
        }
    }
}
