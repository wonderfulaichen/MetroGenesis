package com.metrogenesis.domumornamentum.datagen.wall.vanilla;

import com.metrogenesis.data.LanguageProvider;
import com.metrogenesis.domumornamentum.util.Constants;

public class WallLangEntryProvider implements LanguageProvider.SubProvider
{

    public void addTranslations(LanguageProvider.LanguageAcceptor acceptor) {
        acceptor.add(Constants.MOD_ID + ".wall.name.format", "%s Wall");
    }
}
