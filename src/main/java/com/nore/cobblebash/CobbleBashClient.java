package com.nore.cobblebash;

import net.fabricmc.api.ClientModInitializer;

/**
 * Cote client.
 *
 * <p>L'original enregistre ici un {@code IConfigScreenFactory}, l'ecran de
 * configuration integre a NeoForge. Fabric n'a pas d'equivalent en API de base
 * (ModMenu le fournit, mais c'est une dependance optionnelle qu'on n'impose
 * pas) : la configuration se modifie dans {@code config/cobblebash.json}.
 */
public class CobbleBashClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CobbleBash.LOGGER.info("CobbleBash (Fabric) : initialisation client.");
    }
}
