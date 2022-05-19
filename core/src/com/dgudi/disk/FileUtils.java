package com.dgudi.disk;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import javax.swing.JFileChooser;

import static com.dgudi.disk.Main.filterFolder;

public class FileUtils {

    final static String saveDirectory = "DiskScanPreferences" + File.separator;
    final static FileHandle paramsFile = Gdx.files.external(saveDirectory + "params.txt");
    final static long moveThreadDelay = 50;
    final static int emptyDirectorySize = 5000;
    final static String deletedMessage = "_DELETED_";

    public static long getDirectoriesSizeFromPaths(ArrayList<String> paths) {
        long dirSize = 0;
        for (String path : paths) {
            dirSize += getDirectorySize(path);
        }
        return dirSize;
    }

    public static long getDirectorySize(String path, int sizeLimit) {
        File root = new File(path);
        File[] list = root.listFiles();
        long dirSize = 0;

        if (list == null || !filterFolder(root)) return 0;

        for (final File f : list) {
            if (f.isDirectory()) {
                dirSize += getDirectorySize(f.getAbsolutePath(), sizeLimit);
            } else {
                Main.currentFile = f.toString();
                dirSize += f.length();
            }
            if(dirSize > sizeLimit){
                return dirSize;
            }
        }
        return dirSize;
    }

    public static long getDirectorySize(String path) {
        File root = new File(path);
        File[] list = root.listFiles();
        long dirSize = 0;

        if (list == null || !filterFolder(root)) return 0;

        for (final File f : list) {
            if (f.isDirectory()) {
                dirSize += getDirectorySize(f.getAbsolutePath());
            } else {
                Main.currentFile = f.toString();
                dirSize += f.length();
            }
        }
        return dirSize;
    }

    public static ArrayList<File> getAllFiles(String path) {
        File root = new File(path);
        File[] list = root.listFiles();
        ArrayList<File> foundFiles = new ArrayList<>();

        if (list == null || !filterFolder(root)) return new ArrayList<>();

        for (final File f : list) {
            if (f.isDirectory()) {
                foundFiles.addAll(getAllFiles(f.getAbsolutePath()));
            } else {
                Main.currentFile = f.toString();
                foundFiles.add(f.getAbsoluteFile());
            }
        }

        return foundFiles;
    }

    public static ArrayList<File> getAllDirectories(String path) {
        File root = new File(path);
        File[] list = root.listFiles();
        ArrayList<File> foundDirectories = new ArrayList<>();

        if (list == null || !filterFolder(root)) return new ArrayList<>();

        for (final File f : list) {
            if (f.isDirectory()) {
                foundDirectories.add(f);
                foundDirectories.addAll(getAllDirectories(f.getAbsolutePath()));
            }
        }

        return foundDirectories;
    }

    public static String moveAccordingToFolderStructure(File getFrom, String clonePath) {
        String fullPath = clonePath + File.separator + (getFrom.getAbsolutePath().substring(3));
        int lastDirectoryIndex = fullPath.lastIndexOf(File.separator);
        String path = fullPath.substring(0, lastDirectoryIndex);
        boolean success = true;
        if (!new File(path).exists()) {
            success = new File(path).mkdirs();
        }
        success = success && getFrom.renameTo(new File(fullPath));
        sleep();
        if (success) {
            return fullPath;
        } else {
            Main.currentErrorMessage = "[#FF0000]Error moving to " + fullPath;
            return "Error moving to " + fullPath;
        }
    }

    static void sleep() {
        try {
            Thread.sleep(moveThreadDelay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static String getFilePathWithoutName(String fullFilePath) {
        return fullFilePath.substring(0, fullFilePath.lastIndexOf(File.separator)).trim();
    }

    static String markAsDeleted(File fileToDelete) {
        String fullPath = fileToDelete.getAbsolutePath();
        String name = fileToDelete.getName();
        String pathWithoutName = getFilePathWithoutName(fullPath);
        String newName = pathWithoutName + File.separator + deletedMessage + name;
        boolean success = fileToDelete.renameTo(new File(newName));
        sleep();
        if (success) {
            return newName;
        } else {
            Main.currentErrorMessage = "[#FF0000]Error renaming to " + fullPath;
            return "Error renaming";
        }
    }

    public static Object loadObject(String path) throws IOException, ClassNotFoundException {
        FileInputStream f = new FileInputStream(Gdx.files.external(saveDirectory + path).file());
        ObjectInputStream s = new ObjectInputStream(f);
        Object object = s.readObject();
        s.close();
        return object;
    }

    public static void saveObject(String path, Object object) throws IOException {
        FileOutputStream f = new FileOutputStream(Gdx.files.external(saveDirectory + path).file());
        ObjectOutputStream s = new ObjectOutputStream(f);
        s.writeObject(object);
        s.close();
    }

    static void callFileChooser(FileDialogueAction action, boolean multiSelect) {
        JFileChooser jFileChooser = new JFileChooser(new File(""));
        jFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        jFileChooser.setMultiSelectionEnabled(multiSelect);

        int returnVal = jFileChooser.showOpenDialog(null);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            if (multiSelect) {
                action.performAction(jFileChooser.getSelectedFiles());
            } else {
                action.performAction(new File[]{jFileChooser.getSelectedFile()});
            }
        }

    }

}

class FileDialogueAction {
    void performAction(File[] selectedFiles) {

    }
}
