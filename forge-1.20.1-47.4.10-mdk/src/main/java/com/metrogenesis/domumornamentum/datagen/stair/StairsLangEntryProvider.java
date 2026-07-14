package com.metrogenesis.domumornamentum.datagen.stair;

import com.metrogenesis.data.LanguageProvider;
import com.metrogenesis.domumornamentum.util.Constants;

public class StairsLangEntryProvider implements LanguageProvider.SubProvider
{
    @Override
    public void addTranslations(LanguageProvider.LanguageAcceptor acceptor) {
        acceptor.add(Constants.MOD_ID + ".stair.name.format", "%s Stairs");
    }
}
