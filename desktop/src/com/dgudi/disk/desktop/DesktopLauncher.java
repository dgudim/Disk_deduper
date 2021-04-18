package com.dgudi.disk.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.dgudi.disk.Main;

public class DesktopLauncher {
    public static void main(String[] arg) {
        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.title = "Disk scan";
        config.width = 800;
        config.height = 480;
        config.foregroundFPS = 60;

        try {
            new LwjglApplication(new Main(), config);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

