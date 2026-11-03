package nms.atmosphericfauna.modmenu;

import nms.atmosphericfauna.gui.ConfigScreenHelper;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuCompat implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreenHelper::createScreen;
    }
}
