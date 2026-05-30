package com.metrogenesis.minecolonies.core.items;

import static com.metrogenesis.minecolonies.api.util.constant.Constants.STACKSIZE;

/**
 * Class handling Cookie Dough.
 */
public class ItemCookieDough extends AbstractItemMinecolonies
{
    /**
     * Sets the name, creative tab, and registers the Cookie Dough item.
     *
     * @param properties the properties.
     */
    public ItemCookieDough(final Properties properties)
    {
        super("cookie_dough", properties.stacksTo(STACKSIZE));
    }
}
