package com.metrogenesis.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * 妯″潡鍖栭厤缃鐞?鈥?鍊熼壌 BlockUI/common/config 妯″紡
 * <p>
 * 鏀寔瀹㈡埛绔?鏈嶅姟绔厤缃垎绂伙紝鑷姩娉ㄥ唽銆? *
 * <h3>浣跨敤绀轰緥锛?/h3>
 * <pre>{@code
 * public class MyConfig extends AbstractConfig {
 *     public IntValue someValue = defineInteger("some.value", 42, 0, 100);
 *
 *     public MyConfig(ForgeConfigSpec.Builder builder, String modId) {
 *         super(builder, modId);
 *     }
 * }
 * }</pre>
 */
public abstract class AbstractConfig
{
    protected final ForgeConfigSpec.Builder builder;
    protected final String modId;

    protected AbstractConfig(final ForgeConfigSpec.Builder builder, final String modId)
    {
        this.builder = builder;
        this.modId = modId;
    }

    /**
     * 鍒涘缓閰嶇疆鍒嗙被
     */
    protected void push(final String key)
    {
        builder.push(key);
    }

    /**
     * 閫€鍑洪厤缃垎绫?     */
    protected void pop()
    {
        builder.pop();
    }

    /**
     * 甯冨皵鍊煎畾涔?     */
    protected ForgeConfigSpec.BooleanValue defineBoolean(final String key, final boolean defaultValue, final String comment)
    {
        builder.comment(comment).translation(modId + ".config." + key);
        return builder.define(key, defaultValue);
    }

    /**
     * 鏁存暟鍊煎畾涔夛紙鏃犺寖鍥撮檺鍒讹級
     */
    protected ForgeConfigSpec.IntValue defineInteger(final String key, final int defaultValue, final String comment)
    {
        builder.comment(comment).translation(modId + ".config." + key);
        return builder.defineInRange(key, defaultValue, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    /**
     * 鏁存暟鍊煎畾涔夛紙鏈夎寖鍥撮檺鍒讹級
     */
    protected ForgeConfigSpec.IntValue defineInteger(final String key, final int defaultValue, final int min, final int max, final String comment)
    {
        builder.comment(comment).translation(modId + ".config." + key);
        return builder.defineInRange(key, defaultValue, min, max);
    }

    /**
     * 鍙岀簿搴﹀€煎畾涔?     */
    protected ForgeConfigSpec.DoubleValue defineDouble(final String key, final double defaultValue, final double min, final double max, final String comment)
    {
        builder.comment(comment).translation(modId + ".config." + key);
        return builder.defineInRange(key, defaultValue, min, max);
    }

    /**
     * 瀛楃涓插畾涔?     */
    protected ForgeConfigSpec.ConfigValue<String> defineString(final String key, final String defaultValue, final String comment)
    {
        builder.comment(comment).translation(modId + ".config." + key);
        return builder.define(key, defaultValue);
    }

    /**
     * 杈呭姪锛氬垱寤?Pair 鍏冪粍
     */
    public static <S, C> Pair<S, C> of(final S server, final C client)
    {
        return Pair.of(server, client);
    }
}
