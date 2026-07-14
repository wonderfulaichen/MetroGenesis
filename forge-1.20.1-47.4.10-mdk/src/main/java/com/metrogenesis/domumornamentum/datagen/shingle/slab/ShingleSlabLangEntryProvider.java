package com.metrogenesis.domumornamentum.datagen.shingle.slab;

import com.metrogenesis.data.LanguageProvider;
import com.metrogenesis.domumornamentum.util.Constants;

public class ShingleSlabLangEntryProvider implements LanguageProvider.SubProvider
{
    @Override
    public void addTranslations(LanguageProvider.LanguageAcceptor acceptor) {
        acceptor.add(Constants.MOD_ID + ".shingle_slab.name.format", "%s Shingles");
        acceptor.add(Constants.MOD_ID + ".shingle_slab.support.format", "Supported by: %s");
        acceptor.add(Constants.MOD_ID + ".shingle_slab.cover.format", "Covered by: %s");
        acceptor.add(Constants.MOD_ID + ".shingle_slab.main.format", "Main Material: %s");
    }
}
