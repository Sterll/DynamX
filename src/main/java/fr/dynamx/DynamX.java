package fr.dynamx;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

// TODO port:1.20.1 - Constants/logger holder. The actual @Mod entrypoint is
// fr.dynamx.common.DynamXMain (modId = dynamxmod from DynamXConstants.ID).
public class DynamX {
    public static final String MOD_ID = "dynamxmod";
    public static final Logger LOGGER = LogUtils.getLogger();
}
