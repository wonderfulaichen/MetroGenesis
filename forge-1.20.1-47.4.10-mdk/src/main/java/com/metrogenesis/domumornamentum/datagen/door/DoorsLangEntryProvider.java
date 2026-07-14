package com.metrogenesis.domumornamentum.datagen.door;

import com.metrogenesis.data.LanguageProvider;
import com.metrogenesis.domumornamentum.block.types.DoorType;
import com.metrogenesis.domumornamentum.util.Constants;

public class DoorsLangEntryProvider implements LanguageProvider.SubProvider
{
    @Override
    public void addTranslations(LanguageProvider.LanguageAcceptor acceptor) {
        acceptor.add(Constants.MOD_ID + ".door.name.format", "%s Door");
        acceptor.add(Constants.MOD_ID + ".door.type.format", "Variant: %s");
        acceptor.add(Constants.MOD_ID + ".door.block.format", "Material: %s");

        for (final DoorType value : DoorType.values())
        {
            acceptor.add(Constants.MOD_ID + ".door.type.name." + value.getTranslationKeySuffix(), value.getDefaultEnglishTranslation());
        }
    }
}
