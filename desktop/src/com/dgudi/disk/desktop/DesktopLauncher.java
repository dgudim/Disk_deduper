package com.dgudi.disk.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.dgudi.disk.Main;

public class DesktopLauncher {
    public static void main(String[] arg) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Disk scan");
        config.setWindowedMode(800, 480);

        try {
            new Lwjgl3Application(new Main(), config);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

