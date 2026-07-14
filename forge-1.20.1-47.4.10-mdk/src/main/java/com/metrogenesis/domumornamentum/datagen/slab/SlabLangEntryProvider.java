package com.metrogenesis.domumornamentum.datagen.slab;

import com.metrogenesis.data.LanguageProvider;
import com.metrogenesis.domumornamentum.util.Constants;

public class SlabLangEntryProvider implements LanguageProvider.SubProvider
{
    @Override
    public void addTranslations(LanguageProvider.LanguageAcceptor acceptor) {
        acceptor.add(Constants.MOD_ID + ".slab.name.format", "%s Slab");
    }
}
