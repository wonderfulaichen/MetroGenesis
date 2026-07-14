package com.metrogenesis.domumornamentum.datagen.allbrick;

import com.metrogenesis.domumornamentum.block.ModBlocks;
import com.metrogenesis.domumornamentum.block.decorative.AllBrickBlock;
import com.metrogenesis.domumornamentum.datagen.MateriallyTexturedModelBuilder;
import com.metrogenesis.domumornamentum.datagen.utils.ModelBuilderUtils;
import com.metrogenesis.domumornamentum.util.Constants;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class AllBrickBlockStateProvider extends BlockStateProvider
{

    public AllBrickBlockStateProvider(DataGenerator gen, ExistingFileHelper exFileHelper) {
        super(gen.getPackOutput(), Constants.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        ModBlocks.getInstance().getAllBrickBlocks().forEach(this::registerStatesAndModelsFor);
    }

    private void registerStatesAndModelsFor(AllBrickBlock allBrickBlock) {
        final ModelFile blockModel = models().withExistingParent(
                        "block/allbrick/" + allBrickBlock.getRegistryName().getPath(),
                        modLoc("block/allbrick/" + Objects.requireNonNull(allBrickBlock.getRegistryName()).getPath() + "_spec"))
                .customLoader(MateriallyTexturedModelBuilder::new)
                .end();
        simpleBlock(allBrickBlock, blockModel);

        ModelBuilderUtils.applyDefaultItemTransforms(itemModels().getBuilder(allBrickBlock.getRegistryName().getPath())
                .parent(blockModel));
    }

    @NotNull
    @Override
    public String getName()
    {
        return "All Brick BlockStates Provider";
    }
}
