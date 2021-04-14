package com.dgudi.disk;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.ExifThumbnailDirectory;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Calendar;
import java.util.TimeZone;

import javax.imageio.ImageIO;

public class EXIFUtils {

    static JsonValue cameraNamesMap = new JsonReader().parse(Gdx.files.internal("cameraNamesMap.json"));

    static String mapCameraModel(String originalName) {
        try {
            return cameraNamesMap.getString(originalName.trim());
        } catch (Exception e) {
            Main.currentErrorMessage = "[#DDDD00]Warning: no alternative name specified for [#DDDD99]" + originalName + "[#DDDD00], using default name";
            return originalName.trim();
        }
    }

    static String getImageCameraModel(File file) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);
            String model = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class).getString(ExifSubIFDDirectory.TAG_MODEL);
            if (!(model == null)) {
                return removeIllegalCharacters(mapCameraModel(model));
            } else {
                Main.currentErrorMessage = "[#DDDD00]Warning: camera model is null for [#DDDD99]" + file;
                return "";
            }
        } catch (Exception e) {
            Main.currentErrorMessage = "[#DDDD00]Warning: could not get image camera model for [#DDDD99]" + file;
            return "";
        }
    }

    static void preloadUnknownFormatThumbnail(File file, File thumbnailFile) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);
            ExifThumbnailDirectory dir = metadata.getFirstDirectoryOfType(ExifThumbnailDirectory.class);
            int offset = dir.getInteger(ExifThumbnailDirectory.TAG_THUMBNAIL_OFFSET);
            int length = dir.getInteger(ExifThumbnailDirectory.TAG_THUMBNAIL_LENGTH);
            byte[] contents = Files.readAllBytes(file.toPath());
            byte[] imageData = new byte[length];
            for (int i = offset; i < offset + length; i++) {
                imageData[i - offset] = contents[i];
            }
            ByteArrayInputStream bis = new ByteArrayInputStream(imageData);
            BufferedImage bImage2 = ImageIO.read(bis);
            ImageIO.write(bImage2, "jpg", thumbnailFile);
        } catch (Exception e) {
            Main.currentErrorMessage = "[#DDDD00]Warning: could not get image thumbnail for [#DDDD99]" + file;
        }
    }

    static String removeIllegalCharacters(String input) {
        return input.replace("*", "")
                .replace("\\", "")
                .replace("/", "")
                .replace("\"", "")
                .replace(":", "")
                .replace("?", "")
                .replace("|", "")
                .replace("<", "")
                .replace(">", "")
                .replace(",", "");
    }

    static String getImageCreationDate(File file) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);
            String date = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class).getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
            if (!(date == null)) {
                String numberStr = date.trim().replaceAll("[^0-9]", "");
                int dateSum = 0;
                for (int i = 0; i < numberStr.length(); i++) {
                    dateSum += Integer.parseInt(String.valueOf(numberStr.charAt(i)));
                }
                if (dateSum > 0) {
                    return date.trim().replace(":", "").replace(" ", "-");
                }
            }
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeZone(TimeZone.getTimeZone("GMT+3"));
            calendar.setTimeInMillis(Math.min(
                    Files.readAttributes(file.toPath(), BasicFileAttributes.class).lastModifiedTime().toMillis(),
                    Files.readAttributes(file.toPath(), BasicFileAttributes.class).creationTime().toMillis()));

            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);
            int second = calendar.get(Calendar.SECOND);
            return year + "" + month + "" + day + "-" + hour + "" + minute + "" + second;
        } catch (Exception e) {
            Main.currentErrorMessage = "[#DDDD00]Warning: could not get image creation date for [#DDDD99]" + file;
            return "";
        }
    }

    static String getImageCreationDateAndCameraModel(File file) {
        String extension = file.getName().substring(file.getName().length() - 4);
        String origFileName = file.getName().toLowerCase().substring(0, file.getName().length() - 4);

        if (origFileName.replaceAll("[^0-9]", "").length() > 1) {
            origFileName = origFileName.replaceAll("[^0-9]", "");
        }

        String additionalData = getImageCreationDate(file) + "_" + getImageCameraModel(file);
        String toReturn = origFileName + extension;
        if (additionalData.length() > 1) {
            toReturn = additionalData + "_" + toReturn;
        }
        return toReturn;
    }

}
