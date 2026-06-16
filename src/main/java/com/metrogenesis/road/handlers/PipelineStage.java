package com.metrogenesis.road.handlers;

import net.minecraft.network.chat.Component;

public enum PipelineStage {
    INITIALISATION(Component.translatable("metrogenesis.stage.initialisation")),
    SCANNING_STRUCTURES(Component.translatable("metrogenesis.stage.scanning")),
    PATH_FINDING(Component.translatable("metrogenesis.stage.pathfinding")),
    POST_PROCESSING(Component.translatable("metrogenesis.stage.postprocess")),
    COMPLETE(Component.translatable("metrogenesis.stage.complete"));

    private final Component label;

    PipelineStage(Component label) {
        this.label = label;
    }

    public Component label() {
        return label;
    }
}
