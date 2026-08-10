package com.nore.cobblebash.integration;

import com.cobblemon.mod.common.api.battles.model.ai.BattleAI;
import com.gitlab.srcmc.rctapi.api.ai.RCTBattleAI;
import com.gitlab.srcmc.rctapi.api.models.PokemonModel;
import com.gitlab.srcmc.rctapi.api.models.PokemonModel.StatsModel;
import com.gitlab.srcmc.rctapi.api.models.TrainerModel;
import com.gitlab.srcmc.rctapi.api.util.JTO;
import com.gitlab.srcmc.rctapi.api.util.Text;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class RctGymTrainerFactory {
    public static Optional<TrainerModel> createTrainer(MinecraftServer server, String gymType, String trainerIdPart, int level) {
        return RctTrainerDataLoader.load(server, gymType, trainerIdPart)
                .map(data -> {
                    RctTrainerDataLoader.BuildData build = selectBuild(data.builds());
                    List<RctTrainerDataLoader.PokemonData> shuffledTeam = new ArrayList<>(build.pokemon());
                    Collections.shuffle(shuffledTeam);

                    return new TrainerModel(
                            Text.literal(data.displayName()),
                            JTO.<BattleAI>of(RCTBattleAI::new),
                            List.of(),
                            shuffledTeam.stream()
                                    .map(pokemon -> createPokemon(pokemon, level))
                                    .toList()
                    );
                });
    }

    public static Optional<String> getTrainerDisplayName(MinecraftServer server, String gymType, String trainerIdPart) {
        return RctTrainerDataLoader.load(server, gymType, trainerIdPart)
                .map(RctTrainerDataLoader.TrainerData::displayName);
    }

    private static PokemonModel createPokemon(RctTrainerDataLoader.PokemonData data, int level) {
        StatsModel ivs = new StatsModel(31, 31, 31, 31, 31, 31);
        StatsModel evs = new StatsModel(0, 0, 0, 0, 0, 0);

        return new PokemonModel(
                data.species(),
                "MALE",
                level,
                "hardy",
                data.ability(),
                new LinkedHashSet<>(data.moves()),
                ivs,
                evs,
                false,
                data.heldItem(),
                Set.of()
        );
    }

    private static RctTrainerDataLoader.BuildData selectBuild(List<RctTrainerDataLoader.BuildData> builds) {
        return builds.get(ThreadLocalRandom.current().nextInt(builds.size()));
    }
}
