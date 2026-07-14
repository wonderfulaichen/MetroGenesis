package com.metrogenesis.structurize.client;

import com.metrogenesis.structurize.blueprints.v1.Blueprint;
import com.metrogenesis.structurize.util.RotationMirror;

public record RenderingCacheKey(RotationMirror rotationMirror, Blueprint blueprint)
{
}
