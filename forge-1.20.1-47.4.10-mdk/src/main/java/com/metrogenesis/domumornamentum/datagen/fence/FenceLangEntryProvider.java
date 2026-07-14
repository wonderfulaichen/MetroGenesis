package com.metrogenesis.domumornamentum.datagen.fence;

import com.metrogenesis.data.LanguageProvider;
import com.metrogenesis.domumornamentum.util.Constants;

public class FenceLangEntryProvider implements LanguageProvider.SubProvider
{
    @Override
    public void addTranslations(LanguageProvider.LanguageAcceptor acceptor) {
        acceptor.add(Constants.MOD_ID + ".fence.name.format", "%s Fence");
    }
}
