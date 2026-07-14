package com.metrogenesis.minecolonies.api.colony.buildings;

import com.metrogenesis.minecolonies.api.colony.ICitizenData;
import com.metrogenesis.minecolonies.api.colony.jobs.IJob;
import com.metrogenesis.minecolonies.api.colony.jobs.registry.JobEntry;
import com.metrogenesis.minecolonies.api.entity.citizen.Skill;
import org.jetbrains.annotations.NotNull;

public interface IBuildingWorkerModule
{
    /**
     * The abstract method which creates a job for the building.
     *
     * @param citizen the citizen to take the job.
     * @return the Job.
     */
    @NotNull
    IJob<?> createJob(ICitizenData citizen);

    /**
     * Method which defines if a worker should be allowed to work during the rain.
     *
     * @return true if so.
     */
    boolean canWorkDuringTheRain();

    /**
     * Primary skill getter.
     *
     * @return the primary skill.
     */
    @NotNull
    Skill getPrimarySkill();

    /**
     * Secondary skill getter.
     *
     * @return the secondary skill.
     */
    @NotNull
    Skill getSecondarySkill();

    /**
     * Getter for the job entry.
     * @return the entry.
     */
    JobEntry getJobEntry();
}
