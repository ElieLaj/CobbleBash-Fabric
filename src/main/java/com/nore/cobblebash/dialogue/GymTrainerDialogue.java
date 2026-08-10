package com.nore.cobblebash.dialogue;

import com.cobblemon.mod.common.api.dialogue.Dialogue;
import com.cobblemon.mod.common.api.dialogue.DialogueAction;
import com.cobblemon.mod.common.api.dialogue.DialogueManager;
import com.cobblemon.mod.common.api.dialogue.DialoguePage;
import com.cobblemon.mod.common.api.dialogue.DialoguePredicate;
import com.cobblemon.mod.common.api.dialogue.DialogueText;
import com.cobblemon.mod.common.api.dialogue.WrappedDialogueText;
import com.cobblemon.mod.common.api.dialogue.DialogueSpeaker;
import com.cobblemon.mod.common.api.dialogue.input.DialogueOption;
import com.cobblemon.mod.common.api.dialogue.input.DialogueOptionSetInput;
import com.nore.cobblebash.gym.GymTrainerUnit;
import com.nore.cobblebash.integration.RctApiProbe;
import com.nore.cobblebash.integration.RctTrainerDataLoader;
import com.nore.cobblebash.command.GymCommand;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Map;

public class GymTrainerDialogue {
    private static final ResourceLocation DEFAULT_BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            "cobblemon",
            "textures/gui/dialogue/dialogue_box.png"
    );
    private static final DialogueAction NO_OP_ACTION = (activeDialogue, value) -> {
    };
    private static final DialoguePredicate ALWAYS_TRUE = activeDialogue -> true;

    public static boolean open(ServerPlayer player, RctApiProbe.GymTrainerRef trainerRef) {
        GymTrainerUnit unit = GymTrainerUnit.fromTrainerIdPart(trainerRef.trainerIdPart());
        if (unit == null) {
            return false;
        }

        RctTrainerDataLoader.TrainerData trainerData = RctTrainerDataLoader
                .load(player.server, trainerRef.gymType(), trainerRef.trainerIdPart())
                .orElse(null);

        if (trainerData == null) {
            player.sendSystemMessage(Component.literal("No trainer JSON found for " + trainerRef.gymType() + " " + trainerRef.trainerIdPart() + "."));
            return true;
        }

        RctTrainerDataLoader.DialogueData dialogueData = trainerData.dialogue();
        DialogueManager.startDialogue(player, createDialogue(player, trainerRef, unit, trainerData, dialogueData));
        return true;
    }

    private static Dialogue createDialogue(
            ServerPlayer player,
            RctApiProbe.GymTrainerRef trainerRef,
            GymTrainerUnit unit,
            RctTrainerDataLoader.TrainerData trainerData,
            RctTrainerDataLoader.DialogueData dialogueData
    ) {
        DialogueAction battleAction = (activeDialogue, value) -> {
            activeDialogue.close();
            GymCommand.startTrainerBattle(player, trainerRef.gymType(), trainerRef.slotId(), unit);
        };

        DialogueAction cancelAction = (activeDialogue, value) -> {
            activeDialogue.close();
        };

        DialogueOptionSetInput input = new DialogueOptionSetInput(
                List.of(
                        new DialogueOption(text(dialogueData.battleText()), "battle", battleAction, ALWAYS_TRUE, ALWAYS_TRUE),
                        new DialogueOption(text(dialogueData.cancelText()), "cancel", cancelAction, ALWAYS_TRUE, ALWAYS_TRUE)
                ),
                null,
                false
        );

        DialoguePage page = new DialoguePage(
                "intro",
                "trainer",
                dialogueData.lines().stream().map(GymTrainerDialogue::text).toList(),
                null,
                input,
                null,
                null,
                List.of(),
                NO_OP_ACTION
        );

        return new Dialogue(
                List.of(page),
                DEFAULT_BACKGROUND,
                NO_OP_ACTION,
                Map.of("trainer", new DialogueSpeaker(text(trainerData.displayName()), null, null)),
                NO_OP_ACTION
        );
    }

    private static DialogueText text(String value) {
        return new WrappedDialogueText(Component.literal(value));
    }
}
