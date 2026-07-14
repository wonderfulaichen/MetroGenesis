package com.metrogenesis.minecolonies.core.colony.buildings.moduleviews;

import com.metrogenesis.minecolonies.api.colony.ICitizenDataView;
import com.metrogenesis.minecolonies.api.colony.requestsystem.token.IToken;
import com.metrogenesis.minecolonies.core.colony.jobs.views.CrafterJobView;

import java.util.ArrayList;
import java.util.List;

/**
 * Crafter task module to display tasks in the UI.
 */
public class CrafterRequestTaskModuleView extends RequestTaskModuleView
{
    @Override
    public List<IToken<?>> getTasks()
    {
        final List<IToken<?>> tasks = new ArrayList<>();
        for (final WorkerBuildingModuleView moduleView : buildingView.getModuleViews(WorkerBuildingModuleView.class))
        {
            for (final int citizenId : moduleView.getAssignedCitizens())
            {
                ICitizenDataView citizen = buildingView.getColony().getCitizen(citizenId);
                if (citizen != null && citizen.getJobView() instanceof CrafterJobView)
                {
                    tasks.addAll(((CrafterJobView) citizen.getJobView()).getDataStore().getQueue());
                }
            }
        }

        return tasks;
    }
}
