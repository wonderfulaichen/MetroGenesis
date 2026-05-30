package com.metrogenesis.domumornamentum.datagen.trapdoor.fancy;

import com.metrogenesis.data.LanguageProvider;
import com.metrogenesis.domumornamentum.block.types.FancyTrapdoorType;
import com.metrogenesis.domumornamentum.util.Constants;

public class FancyTrapdoorsLangEntryProvider implements LanguageProvider.SubProvider
{
    @Override
    public void addTranslations(LanguageProvider.LanguageAcceptor acceptor) {
        acceptor.add(Constants.MOD_ID + ".fancytrapdoor.name.format", "Fancy %s Trapdoor");
        acceptor.add(Constants.MOD_ID + ".fancytrapdoor.type.format", "Variant: %s");
        acceptor.add(Constants.MOD_ID + ".fancytrapdoor.frame.header", "Frame:");
        acceptor.add(Constants.MOD_ID + ".fancytrapdoor.center.header", "Center:");
        acceptor.add(Constants.MOD_ID + ".fancytrapdoor.center.block.format", "  - Material: %s");
        acceptor.add(Constants.MOD_ID + ".fancytrapdoor.frame.block.format", "  - Material: %s");

        for (final FancyTrapdoorType value : FancyTrapdoorType.values())
        {
            acceptor.add(Constants.MOD_ID + ".fancytrapdoor.type.name." + value.getTranslationKeySuffix(), value.getDefaultEnglishTranslation());
        }
    }
}
