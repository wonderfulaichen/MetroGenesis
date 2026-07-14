package com.metrogenesis.domumornamentum.datagen.frames.dynamic;

import com.metrogenesis.data.LanguageProvider;
import com.metrogenesis.domumornamentum.block.types.TimberFrameType;
import com.metrogenesis.domumornamentum.util.Constants;

public class DynamicFramesLangEntryProvider implements LanguageProvider.SubProvider {

    @Override
    public void addTranslations(LanguageProvider.LanguageAcceptor acceptor) {
        acceptor.add(Constants.MOD_ID + ".dynamic.frame.name.format", "Dynamic Framed %s");
    }
}
