package com.metrogenesis.domumornamentum.datagen.frames.light;

import com.metrogenesis.data.LanguageProvider;
import com.metrogenesis.domumornamentum.block.types.FramedLightType;
import com.metrogenesis.domumornamentum.util.Constants;

public class FramedLightLangEntryProvider implements LanguageProvider.SubProvider
{
    @Override
    public void addTranslations(LanguageProvider.LanguageAcceptor acceptor) {
        for (final FramedLightType type : FramedLightType.values())
        {
            final String reference = Constants.MOD_ID + ".light.frame.type." + type.getName();
            final String value = type.getLangName();

            acceptor.add(reference, value);
        }

        acceptor.add(Constants.MOD_ID + ".light.frame.name.format", "Framed %s");
        acceptor.add(Constants.MOD_ID + ".light.frame.header", "Framing:");
        acceptor.add(Constants.MOD_ID + ".light.frame.type.format", "  - Type:     %s");
        acceptor.add(Constants.MOD_ID + ".light.frame.block.format", "  - Material: %s");
        acceptor.add(Constants.MOD_ID + ".light.center.header", "Center:");
        acceptor.add(Constants.MOD_ID + ".light.center.block.format", "  - Material: %s");
    }
}
