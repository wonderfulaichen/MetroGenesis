package com.metrogenesis.entity.client;

import com.metrogenesis.entity.MetroGenesisCitizen;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * 甯傛皯娓叉煋鍣?鈥?澶嶇敤鍘熺増鏉戞皯妯″瀷锛岀汗鐞嗘牴鎹?textureId + textureSuffix 鍙樺寲
 * <p>
 * 浣跨敤鍘熺増鏉戞皯鐨勮亴涓氱汗鐞嗕綔涓哄鏍锋€ф潵婧愶細
 * <ul>
 *   <li>textureId 0-15 鈫?鍚勮亴涓氱汗鐞嗗惊鐜?/li>
 *   <li>textureSuffix 鈫?涓嶉澶栧奖鍝嶇汗鐞嗚矾寰勶紙澶嶇敤鍘熺増绾圭悊锛?/li>
 * </ul>
 */
public class CitizenRenderer extends MobRenderer<MetroGenesisCitizen, VillagerModel<MetroGenesisCitizen>> {

    /** 鍙敤鐨勬潙姘戠汗鐞嗗垪琛紙16 绉嶈亴涓氱汗鐞嗭級 */
    private static final ResourceLocation[] TEXTURES = {
            tex("villager"),          // 0
            tex("farmer"),            // 1
            tex("fisherman"),         // 2
            tex("fletcher"),          // 3
            tex("butcher"),           // 4
            tex("shepherd"),          // 5
            tex("leatherworker"),     // 6
            tex("armorer"),           // 7
            tex("weaponsmith"),       // 8
            tex("toolsmith"),         // 9
            tex("librarian"),         // 10
            tex("cartographer"),      // 11
            tex("cleric"),            // 12
            tex("mason"),             // 13
            tex("nitwit"),            // 14
            tex("unemployed")         // 15
    };

    private static ResourceLocation tex(String name) {
        return new ResourceLocation("minecraft", "textures/entity/villager/" + name + ".png");
    }

    public CitizenRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new VillagerModel<>(ctx.bakeLayer(ModelLayers.VILLAGER)), 0.5f);
    }

    @Override
    @NotNull
    public ResourceLocation getTextureLocation(MetroGenesisCitizen entity) {
        int id = entity.getTextureId();
        return TEXTURES[Math.abs(id) % TEXTURES.length];
    }
}
