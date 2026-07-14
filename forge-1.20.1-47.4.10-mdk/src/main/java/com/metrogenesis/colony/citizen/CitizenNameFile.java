package com.metrogenesis.colony.citizen;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 甯傛皯鍛藉悕鏂囦欢 鈥?浠?JSON 鍔犺浇
 * <p>
 * 鍙傝€?MineColonies {@code com.minecolonies.api.colony.CitizenNameFile}
 */
public class CitizenNameFile {

    /**
     * 鍛藉悕椤哄簭
     */
    public enum NameOrder {
        EASTERN,
        WESTERN
    }

    /** 濮撳悕閮ㄥ垎鏁帮紙2 鎴?3锛? 鏃惰タ鏂归鏍煎惈涓棿鍚嶉瀛楁瘝锛?*/
    private final int parts;

    /** 鍛藉悕椤哄簭 */
    private final NameOrder order;

    /** 鐢锋€у悕瀛楀垪琛?*/
    private final List<String> maleFirstNames;

    /** 濂虫€у悕瀛楀垪琛?*/
    private final List<String> femaleFirstNames;

    /** 濮撴皬鍒楄〃 */
    private final List<String> surnames;

    public CitizenNameFile(
            final int parts,
            @NotNull final NameOrder order,
            @NotNull final List<String> maleFirstNames,
            @NotNull final List<String> femaleFirstNames,
            @NotNull final List<String> surnames) {
        this.parts = parts;
        this.order = order;
        this.maleFirstNames = maleFirstNames;
        this.femaleFirstNames = femaleFirstNames;
        this.surnames = surnames;
    }

    public int getParts() {
        return parts;
    }

    public NameOrder getOrder() {
        return order;
    }

    public List<String> getMaleFirstNames() {
        return maleFirstNames;
    }

    public List<String> getFemaleFirstNames() {
        return femaleFirstNames;
    }

    public List<String> getSurnames() {
        return surnames;
    }
}
