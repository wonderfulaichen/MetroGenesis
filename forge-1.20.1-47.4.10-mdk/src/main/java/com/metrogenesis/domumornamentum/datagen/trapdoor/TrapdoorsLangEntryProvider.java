package com.metrogenesis.domumornamentum.datagen.trapdoor;

import com.metrogenesis.data.LanguageProvider;
import com.metrogenesis.domumornamentum.block.types.TrapdoorType;
import com.metrogenesis.domumornamentum.util.Constants;

public class TrapdoorsLangEntryProvider implements LanguageProvider.SubProvider
{
    @Override
    public void addTranslations(LanguageProvider.LanguageAcceptor acceptor) {
        acceptor.add(Constants.MOD_ID + ".trapdoor.name.format", "%s Trapdoor");
        acceptor.add(Constants.MOD_ID + ".trapdoor.type.format", "Variant: %s");
        acceptor.add(Constants.MOD_ID + ".trapdoor.block.format", "Material: %s");

        for (final TrapdoorType value : TrapdoorType.values())
        {
            acceptor.add(Constants.MOD_ID + ".trapdoor.type.name." + value.getTranslationKeySuffix(), value.getDefaultEnglishTranslation());
        }
    }
}
