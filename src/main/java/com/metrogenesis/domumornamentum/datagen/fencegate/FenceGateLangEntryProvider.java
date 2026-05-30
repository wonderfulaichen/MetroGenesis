package com.metrogenesis.domumornamentum.datagen.fencegate;

import com.metrogenesis.data.LanguageProvider;
import com.metrogenesis.domumornamentum.util.Constants;

public class FenceGateLangEntryProvider implements LanguageProvider.SubProvider {
    @Override
    public void addTranslations(LanguageProvider.LanguageAcceptor acceptor) {
        acceptor.add(Constants.MOD_ID + ".fence-gate.name.format", "%s Fence gate");
    }
}
