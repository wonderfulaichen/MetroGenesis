package com.metrogenesis.minecolonies.core.client.render;

import com.metrogenesis.minecolonies.api.colony.ICitizenDataView;
import com.metrogenesis.minecolonies.api.colony.IColonyManager;
import com.metrogenesis.minecolonies.api.colony.IColonyView;
import com.metrogenesis.minecolonies.api.colony.requestsystem.manager.IRequestManager;
import com.metrogenesis.minecolonies.api.colony.requestsystem.resolver.player.IPlayerRequestResolver;
import com.metrogenesis.minecolonies.api.colony.requestsystem.resolver.retrying.IRetryingRequestResolver;
import com.metrogenesis.minecolonies.api.colony.requestsystem.token.IToken;
import com.metrogenesis.minecolonies.api.util.Log;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.IItemDecorator;

import java.util.HashSet;
import java.util.Set;

import static com.metrogenesis.minecolonies.core.items.ItemClipboard.TAG_COLONY;

public class ClipBoardDecorator implements IItemDecorator
{
    private static IColonyView colonyView;
    private static boolean render = false;
    private long lastChange;

    @Override
    public boolean render(GuiGraphics graphics, Font font, ItemStack stack, int xOffset, int yOffset)
    {
        final long gametime = Minecraft.getInstance().level.getGameTime();

        if (lastChange != gametime && gametime % 40 == 0)
        {
            lastChange = gametime;
            render = !render;
        }

        if (render)
        {
            final CompoundTag compoundTag = stack.getTag();
            if (compoundTag != null)
            {
                final int colonyId = compoundTag.getInt(TAG_COLONY);
                colonyView = IColonyManager.getInstance().getColonyView(colonyId, Minecraft.getInstance().level.dimension());

                if (colonyView != null)
                {
                    try
                    {
                        final IRequestManager requestManager = colonyView.getRequestManager();
                        if (requestManager != null)
                        {
                            final IPlayerRequestResolver resolver = requestManager.getPlayerResolver();
                            final IRetryingRequestResolver retryingRequestResolver = requestManager.getRetryingRequestResolver();

                            final Set<IToken<?>> requestTokens = new HashSet<>();
                            requestTokens.addAll(resolver.getAllAssignedRequests());
                            requestTokens.addAll(retryingRequestResolver.getAllAssignedRequests());

                            for (final ICitizenDataView view : colonyView.getCitizens().values())
                            {
                                if (view.getJobView() != null)
                                {
                                    requestTokens.removeAll(view.getJobView().getAsyncRequests());
                                }
                            }

                            if (!requestTokens.isEmpty())
                            {
                                final PoseStack ps = graphics.pose();
                                ps.pushPose();
                                ps.translate(0, 0, 200);
                                graphics.drawCenteredString(font,
                                  Component.literal(requestTokens.size() + ""),
                                  xOffset + 15,
                                  yOffset - 2,
                                  0xFFFF4500);
                                ps.popPose();
                                return true;
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        Log.getLogger().error("Something went wrong with the clipboard item decorator", e);
                    }
                }
            }
        }
        return false;
    }
}