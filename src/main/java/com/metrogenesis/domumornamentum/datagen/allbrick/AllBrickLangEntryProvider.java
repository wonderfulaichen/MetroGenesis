package com.metrogenesis.domumornamentum.datagen.allbrick;

import com.metrogenesis.data.LanguageProvider;
import com.metrogenesis.domumornamentum.util.Constants;

public class AllBrickLangEntryProvider implements LanguageProvider.SubProvider
{
    @Override
    public void addTranslations(LanguageProvider.LanguageAcceptor acceptor) {
        acceptor.add(Constants.MOD_ID + ".dark_brick.name.format", "Dark %s Brick");
        acceptor.add(Constants.MOD_ID + ".light_brick.name.format", "Light %s Brick");

        acceptor.add(Constants.MOD_ID + ".dark_brick_stair.name.format", "Dark %s Brick Stair");
        acceptor.add(Constants.MOD_ID + ".light_brick_stair.name.format", "Light %s Brick Stair");

        acceptor.add(Constants.MOD_ID + ".allbrick.column.format", "Main Material: %s");
    }
}
