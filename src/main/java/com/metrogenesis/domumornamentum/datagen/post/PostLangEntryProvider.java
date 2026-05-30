package com.metrogenesis.domumornamentum.datagen.post;

import com.metrogenesis.data.LanguageProvider;
import com.metrogenesis.domumornamentum.block.types.PostType;
import com.metrogenesis.domumornamentum.util.Constants;

public class PostLangEntryProvider implements LanguageProvider.SubProvider
{
    @Override
    public void addTranslations(LanguageProvider.LanguageAcceptor acceptor) {
        acceptor.add(Constants.MOD_ID + ".post.name.format", "%s Post");
        acceptor.add(Constants.MOD_ID + ".post.type.format", "Variant: %s");
        acceptor.add(Constants.MOD_ID + ".post.block.format", "Material: %s");

        /*
          IE "Oak Planks Double Post"
         */
        for (final PostType value : PostType.values())
        {
            acceptor.add(Constants.MOD_ID + ".post.type.name." + value.getTranslationKeySuffix(), value.getDefaultEnglishTranslation());
        }
    }
}
