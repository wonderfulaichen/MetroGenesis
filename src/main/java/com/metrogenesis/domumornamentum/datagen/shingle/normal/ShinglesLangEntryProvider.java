package com.metrogenesis.domumornamentum.datagen.shingle.normal;

import com.metrogenesis.data.LanguageProvider;
import com.metrogenesis.domumornamentum.util.Constants;

public class ShinglesLangEntryProvider implements LanguageProvider.SubProvider
{
    @Override
    public void addTranslations(LanguageProvider.LanguageAcceptor acceptor) {
        acceptor.add(Constants.MOD_ID + ".shingle.name.format.block.metrogenesis_domum.shingle_flat_lower", "%s Flat Lower Shingles");
        acceptor.add(Constants.MOD_ID + ".shingle.support.format", "Supported by: %s");
        acceptor.add(Constants.MOD_ID + ".shingle.main.format", "Main Material: %s");

        acceptor.add(Constants.MOD_ID + ".shingle.name.format.block.metrogenesis_domum.shingle", "%s Shingles");
        acceptor.add(Constants.MOD_ID + ".shingle.name.format.block.metrogenesis_domum.shingle_flat", "%s Flat Shingles");


        acceptor.add(Constants.MOD_ID + ".shingle.name.format.block.metrogenesis_domum.shingle_steep_lower", "%s Steep Lower Shingles");
        acceptor.add(Constants.MOD_ID + ".shingle.name.format.block.metrogenesis_domum.shingle_steep", "%s Steep Shingles");
    }
}
