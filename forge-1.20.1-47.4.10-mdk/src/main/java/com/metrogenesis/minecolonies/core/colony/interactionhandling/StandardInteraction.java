package com.metrogenesis.minecolonies.core.colony.interactionhandling;

import com.metrogenesis.minecolonies.api.colony.ICitizen;
import com.metrogenesis.minecolonies.api.colony.interactionhandling.IChatPriority;
import com.metrogenesis.minecolonies.api.colony.interactionhandling.IInteractionResponseHandler;
import com.metrogenesis.minecolonies.api.colony.interactionhandling.InteractionValidatorRegistry;
import com.metrogenesis.minecolonies.api.colony.interactionhandling.ModInteractionResponseHandlers;
import com.metrogenesis.minecolonies.api.util.Tuple;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.List;

/**
 * The server side interaction response handler.
 */
public class StandardInteraction extends ServerCitizenInteraction
{
    /**
     * Standard responses
     */
    public static final String INTERACTION_R_OKAY   = "com.metrogenesis.minecolonies.coremod.gui.chat.okay";
    public static final String INTERACTION_R_IGNORE = "com.metrogenesis.minecolonies.coremod.gui.chat.ignore";
    public static final String INTERACTION_R_REMIND = "com.metrogenesis.minecolonies.coremod.gui.chat.remindmelater";
    public static final String INTERACTION_R_SKIP   = "com.metrogenesis.minecolonies.coremod.gui.chat.skipchitchat";

    @SuppressWarnings("unchecked")
    private static final Tuple<Component, Component>[] tuples = (Tuple<Component, Component>[]) new Tuple[] {
      new Tuple<>(Component.translatable(INTERACTION_R_OKAY), null),
      new Tuple<>(Component.translatable(INTERACTION_R_IGNORE), null),
      new Tuple<>(Component.translatable(INTERACTION_R_REMIND), null),
      new Tuple<>(Component.translatable(INTERACTION_R_SKIP), null)};

    /**
     * The server interaction response handler with custom validator.
     *
     * @param inquiry   the client inquiry.
     * @param validator the id of the validator.
     * @param priority  the interaction priority.
     */
    public StandardInteraction(
      final Component inquiry,
      final Component validator,
      final IChatPriority priority)
    {
        super(inquiry, true, priority, InteractionValidatorRegistry.getStandardInteractionValidatorPredicate(validator), validator, tuples);
    }

    /**
     * The server interaction response handler.
     *
     * @param inquiry  the client inquiry.
     * @param priority the interaction priority.
     */
    public StandardInteraction(
      final Component inquiry,
      final IChatPriority priority)
    {
        super(inquiry, true, priority, InteractionValidatorRegistry.getStandardInteractionValidatorPredicate(inquiry), inquiry, tuples);
    }

    /**
     * Way to load the response handler for a citizen.
     *
     * @param data the citizen owning this handler.
     */
    public StandardInteraction(final ICitizen data)
    {
        super(data);
    }

    @Override
    public List<IInteractionResponseHandler> genChildInteractions()
    {
        return Collections.emptyList();
    }

    @Override
    public String getType()
    {
        return ModInteractionResponseHandlers.STANDARD.getPath();
    }
}
