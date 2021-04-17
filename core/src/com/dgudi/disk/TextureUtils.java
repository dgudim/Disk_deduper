package com.dgudi.disk;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import java.io.File;
import java.util.ArrayList;

import static com.dgudi.disk.EXIFUtils.preloadUnknownFormatThumbnail;
import static com.dgudi.disk.FileUtils.deletedMessage;
import static com.dgudi.disk.GeneralUtils.convertToMegabytes;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class TextureUtils {

    final static int previewFileSizeLimit = 35; // Mb
    final static String[] supportedFormats = new String[]{".jpg", ".png", ".jpeg", ".mov", ".bmp", ".cr2", ".gif"};
    final static String[] excludedFormats = new String[]{".bup", ".ifo"};
    final static int previewImageSizeLimit = 200; // Px
    final static String thumbnailDirectory = "DiskScanThumbnails\\";

    static ArrayList<Texture> currentPreviewTextures = new ArrayList<>();

    public static Texture loadPreview(String inputPath) {
        Texture toReturn;
        Pixmap outputPixmap = null;
        boolean outputPixmapCreated = false;
        try {
            FileHandle file;
            boolean createThumbnail = false;

            if (!new File(inputPath).exists()) {
                return new Texture(Gdx.files.internal("blank.png"));
            } else if (!canLoadPreview(inputPath)) {
                file = Gdx.files.internal("formatError.png");
            } else if (new File(inputPath).length() / 1024f / 1024f > previewFileSizeLimit) {
                file = Gdx.files.internal("tooBig.png");
            } else {
                createThumbnail = true;
                file = Gdx.files.absolute(inputPath);
            }

            outputPixmap = createThumbnail(file, inputPath, false, createThumbnail);
            outputPixmapCreated = createThumbnail;

            if (inputPath.contains(deletedMessage)) {
                outputPixmap.setColor(Color.RED);
                int radius = (int) (min(outputPixmap.getWidth(), outputPixmap.getHeight()) / 5f);
                outputPixmap.fillCircle(radius, radius, radius);
                outputPixmap.setColor(Color.ORANGE);
                outputPixmap.fillCircle(radius, radius, (int) (radius * 0.8f));
            }

            toReturn = new Texture(outputPixmap);

            if (outputPixmapCreated) {
                outputPixmap.dispose();
            }
        } catch (Exception e) {
            Main.currentErrorMessage = "[#FF0000]Error loading preview: " + e.getMessage();
            if (outputPixmapCreated) {
                outputPixmap.dispose();
            }
            toReturn = new Texture(Gdx.files.internal("error.png"));
        }
        currentPreviewTextures.add(toReturn);
        return toReturn;
    }

    static void createThumbnail(String sourcePath) {
        try {
            if ((convertToMegabytes(new File(sourcePath).length()) <= previewFileSizeLimit) && canLoadPreview(sourcePath) && !sourcePath.toLowerCase().endsWith(".cr2")) {
                createThumbnail(Gdx.files.absolute(sourcePath), sourcePath, true, true);
            } else {
                Main.currentErrorMessage = "[#FFCC00]Warning: unsupported format";
            }
        } catch (Exception e) {
            Main.currentErrorMessage = "[#FF0000]Error preloading thumbnail: " + e.getMessage();
        }
    }

    static Pixmap createThumbnail(FileHandle pixmapFile, String sourcePath, boolean dispose, boolean createThumbnail) {
        FileHandle thumbnailFile = Gdx.files.external(thumbnailDirectory + sourcePath.replace("\\", "_").replace(":", "_") + ".png");
        if (!createThumbnail) {
            return new Pixmap(pixmapFile);
        }
        if (!thumbnailFile.exists()) {
            if (pixmapFile.file().getName().toLowerCase().endsWith(".cr2")) {
                preloadUnknownFormatThumbnail(new File(sourcePath), thumbnailFile.file());
                return new Pixmap(thumbnailFile);
            }
            Pixmap outputPixmap;
            Pixmap sourcePixmap = new Pixmap(pixmapFile);
            int sHeight = sourcePixmap.getHeight();
            int sWidth = sourcePixmap.getWidth();
            double scaleFactor = max(max(sHeight, sWidth) / (double) previewImageSizeLimit, 1);
            outputPixmap = new Pixmap((int) (sWidth / scaleFactor), (int) (sHeight / scaleFactor), sourcePixmap.getFormat());
            outputPixmap.drawPixmap(sourcePixmap,
                    0, 0, sWidth, sHeight,
                    0, 0, (int) Math.ceil(sWidth / scaleFactor), (int) Math.ceil(sHeight / scaleFactor)
            );
            try {
                PixmapIO.writePNG(thumbnailFile, outputPixmap, 9, false);
            } catch (Exception e) {
                Main.currentErrorMessage = "[#FF0000]Error saving thumbnail: " + e.getMessage();
            }
            sourcePixmap.dispose();
            if (dispose) {
                outputPixmap.dispose();
            }
            return outputPixmap;
        } else {
            if (dispose) {
                return null;
            }
            return new Pixmap(thumbnailFile);
        }
    }

    static void unloadAllTextures() {
        for (Texture texture : currentPreviewTextures) {
            texture.dispose();
        }
        currentPreviewTextures.clear();
    }

    public static boolean canLoadPreview(String inputPath) {
        for (String extension : supportedFormats) {
            if (inputPath.toLowerCase().endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    public static TextureRegionDrawable constructFilledImageWithColor(int width, int height, Color color) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        TextureRegionDrawable image = new TextureRegionDrawable(new TextureRegion(new Texture(pixmap)));
        pixmap.dispose();
        return image;
    }

}
