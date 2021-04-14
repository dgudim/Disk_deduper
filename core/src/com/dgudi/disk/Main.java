package com.dgudi.disk;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import javax.swing.JFileChooser;

import de.tomgrill.gdxdialogs.core.GDXDialogs;
import de.tomgrill.gdxdialogs.core.GDXDialogsSystem;
import de.tomgrill.gdxdialogs.core.dialogs.GDXButtonDialog;
import de.tomgrill.gdxdialogs.core.listener.ButtonClickListener;

import static com.dgudi.disk.EXIFUtils.getImageCameraModel;
import static com.dgudi.disk.EXIFUtils.getImageCreationDateAndCameraModel;
import static com.dgudi.disk.FileUtils.getAllDirectories;
import static com.dgudi.disk.FileUtils.getAllFiles;
import static com.dgudi.disk.FileUtils.getFilePathWithoutName;
import static com.dgudi.disk.FileUtils.loadObject;
import static com.dgudi.disk.FileUtils.markAsDeleted;
import static com.dgudi.disk.FileUtils.moveAccordingToFolderStructure;
import static com.dgudi.disk.FileUtils.paramsFile;
import static com.dgudi.disk.FileUtils.saveDirectory;
import static com.dgudi.disk.FileUtils.saveObject;
import static com.dgudi.disk.FileUtils.sleep;
import static com.dgudi.disk.GeneralUtils.calculatePercentage;
import static com.dgudi.disk.GeneralUtils.convertToGigabytes;
import static com.dgudi.disk.GeneralUtils.formatNumber;
import static com.dgudi.disk.GeneralUtils.getCurrentDate;
import static com.dgudi.disk.GeneralUtils.getCurrentTime;
import static com.dgudi.disk.GeneralUtils.getElapsedTime;
import static com.dgudi.disk.GeneralUtils.normaliseLength;
import static com.dgudi.disk.HashParser.currentCommits;
import static com.dgudi.disk.HashParser.getFileChecksum;
import static com.dgudi.disk.HashParser.readHashBase;
import static com.dgudi.disk.HashParser.saveHashBase;
import static com.dgudi.disk.TextureUtils.constructFilledImageWithColor;
import static com.dgudi.disk.TextureUtils.createThumbnail;
import static com.dgudi.disk.TextureUtils.excludedFormats;
import static com.dgudi.disk.TextureUtils.loadPreview;
import static com.dgudi.disk.TextureUtils.unloadAllTextures;
import static java.lang.Math.min;

public class Main extends ApplicationAdapter {

    OrthographicCamera camera;
    Viewport viewport;
    SpriteBatch batch;
    BitmapFont font;
    TextureAtlas uiAtlas;
    TextureAtlas uiAtlas_buttons;
    CheckBox.CheckBoxStyle checkBoxStyle;
    TextButton.TextButtonStyle buttonStyle;
    Label.LabelStyle labelStyle;
    Skin uiTextures;
    Stage stage;
    Stage stage_results;
    ShapeRenderer renderer;

    TextButton startScan;
    TextButton changeMode;
    TextButton commit;
    TextButton addFolder;
    TextButton removeAllFolders;
    TextButton setMasterPath;
    TextButton addSlavePath;
    TextButton setClonePath;
    TextButton removeAllExtensions;
    TextButton addExtension;

    public static String currentFile = "";
    public static String currentHash = "";
    public static String currentErrorMessage = "";
    int allFilesCalculated = 1;
    final HashMap<String, String> hashedFiles = new HashMap<>();
    int allFiles = 0;
    int prevFiles = 0;
    final int filesPerSecSmoothingFrame = 15; //Min
    float[] filesPerSecSmoothing;
    int comparedFiles = 0;
    int duplicatesOriginal = 0;
    int duplicates = 0;
    float dupesSize = 0;
    ArrayList<String> dupes_merged = new ArrayList<>();
    ArrayList<String> dupes_first_part = new ArrayList<>();
    ArrayList<String> dupes_second_part = new ArrayList<>();
    long startTime;
    boolean extensionFilterEnabled = true;

    final String NAME_SEARCH = "searchNames";
    final String SEARCH = "search";
    final String COMPARE_2_FOLDERS = "comp2f";
    final String COMPARE_2_FOLDERS_RENAME = "comp2fR";
    final String EXIF_SORT = "exifSort";
    final String STATS = "stats";

    int emptyDirectorySize = 5000;

    String comparisonMode = SEARCH;
    String comparisonMode_humanReadable;

    String masterPath, clonePath;

    ArrayList<String> slaveFolders;
    ArrayList<String> foldersToSearch;
    ArrayList<String> extensionsToSearch;

    String foldersToSearch_string;
    String extensionsToSearch_string;

    int commitCount;
    long lastCommitTime;

    boolean resultsAvailable;
    boolean currentlyLoadingPreviews;
    boolean currentlyPreLoadingPreviews;
    int preloadGroupIndex = 0;
    boolean currentlySortingFolders;
    int currentlySelectedGroup = 0;
    int currentlySelectedGroup_fingerprint = 0;
    TextButton nextGroup;
    TextButton prevGroup;
    TextButton deleteLeft;
    TextButton deleteRight;
    TextButton switchGroupMode;
    int currentGroupMode = 0;
    String currentlyViewedPaths;
    ArrayList<Integer> dupesLeft = new ArrayList<>();
    ArrayList<String> fileHashes = new ArrayList<>();
    ArrayList<Integer> indexMap = new ArrayList<>();
    ArrayList<ArrayList<Integer>> sortedFilesIndexMap = new ArrayList<>();

    ArrayList<ArrayList<String>> sortedFilesArray_leftPart = new ArrayList<>();
    ArrayList<ArrayList<String>> sortedFilesArray_rightPart = new ArrayList<>();
    ArrayList<ArrayList<Integer>> sortedFilesArray_fingerprintMatch = new ArrayList<>();
    ArrayList<String> notSortedFilesArray_leftPart = new ArrayList<>();
    ArrayList<String> notSortedFilesArray_rightPart = new ArrayList<>();

    final static String deletedMessage = "_DELETED_";

    final String fontChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890\"!`?'.,;:()[]{}<>|/@\\^$€-%+=#_&~*ёйцукенгшщзхъэждлорпавыфячсмитьбюЁЙЦУКЕНГШЩЗХЪЭЖДЛОРПАВЫФЯЧСМИТЬБЮ";
    long maxMemUsage = 0;
    long availableMem = 0;
    double memDisplayFactor = 0;

    JsonValue modeNamesMap;

    GDXDialogs dialogs;

    @Override
    @SuppressWarnings("unchecked")
    public void create() {

        filesPerSecSmoothing = new float[filesPerSecSmoothingFrame * 60];

        availableMem = Runtime.getRuntime().maxMemory();
        memDisplayFactor = 400 / (double) availableMem;

        readHashBase();

        dialogs = GDXDialogsSystem.install();

        modeNamesMap = new JsonReader().parse(Gdx.files.internal("modesMap.json"));

        batch = new SpriteBatch();
        renderer = new ShapeRenderer();
        renderer.setAutoShapeType(true);

        camera = new OrthographicCamera(800, 480);
        viewport = new ScreenViewport(camera);

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("font.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 12;
        parameter.characters = fontChars;
        font = generator.generateFont(parameter);
        generator.dispose();
        font.getData().markupEnabled = true;

        uiAtlas_buttons = new TextureAtlas(Gdx.files.internal("workshop.atlas"));
        uiAtlas = new TextureAtlas(Gdx.files.internal("ui.atlas"));
        uiTextures = new Skin();
        uiTextures.addRegions(uiAtlas);
        uiTextures.addRegions(uiAtlas_buttons);

        checkBoxStyle = new CheckBox.CheckBoxStyle();
        checkBoxStyle.checkboxOff = uiTextures.getDrawable("checkBox_disabled");
        checkBoxStyle.checkboxOn = uiTextures.getDrawable("checkBox_enabled");
        checkBoxStyle.checkboxOver = uiTextures.getDrawable("checkBox_disabled_over");
        checkBoxStyle.checkboxOnOver = uiTextures.getDrawable("checkBox_enabled_over");

        checkBoxStyle.checkboxOff.setMinHeight(30);
        checkBoxStyle.checkboxOff.setMinWidth(30);
        checkBoxStyle.checkboxOver.setMinHeight(30);
        checkBoxStyle.checkboxOver.setMinWidth(30);
        checkBoxStyle.checkboxOn.setMinHeight(30);
        checkBoxStyle.checkboxOn.setMinWidth(30);
        checkBoxStyle.checkboxOnOver.setMinHeight(30);
        checkBoxStyle.checkboxOnOver.setMinWidth(30);

        checkBoxStyle.font = font;
        checkBoxStyle.fontColor = Color.WHITE;

        buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.up = uiTextures.getDrawable("blank_shopButton_disabled");
        buttonStyle.down = uiTextures.getDrawable("blank_shopButton_enabled");
        buttonStyle.over = uiTextures.getDrawable("blank_shopButton_over");
        buttonStyle.font = font;
        buttonStyle.fontColor = Color.WHITE;

        labelStyle = new Label.LabelStyle();
        labelStyle.font = font;
        labelStyle.fontColor = Color.WHITE;
        labelStyle.background = constructFilledImageWithColor(10, 10, Color.DARK_GRAY);

        stage = new Stage(viewport, batch);
        stage_results = new Stage(viewport, batch);

        loadParams();

        startScan = makeTextButton("Start Scanning", 110, 10, 150);
        changeMode = makeTextButton("Change mode", 110, 10, 405);
        commit = makeTextButton("Commit now", 110, 10, 450);
        addFolder = makeTextButton("Add a folder to scan", 150, 625, 450);
        removeAllFolders = makeTextButton("Remove all folders", 150, 625, 420);
        setMasterPath = makeTextButton("Set master folder path", 170, 615, 390);
        addSlavePath = makeTextButton("Add slave folder path", 170, 615, 360);
        setClonePath = makeTextButton("Set clone folder path", 170, 615, 330);
        removeAllExtensions = makeTextButton("Remove all extensions", 170, 415, 420);
        addExtension = makeTextButton("Add an extension to scan", 170, 415, 450);

        nextGroup = makeTextButton("Next Group", 110, 680, 450, false);
        prevGroup = makeTextButton("Previous Group", 110, 10, 450, false);
        deleteLeft = makeTextButton("Delete Group", 110, 10, 5, false);
        deleteRight = makeTextButton("Delete Group", 110, 680, 5, false);
        switchGroupMode = makeTextButton("Switch group mode", 150, 325, 10, false);
        deleteLeft.setColor(Color.RED);
        deleteRight.setColor(Color.RED);
        switchGroupMode.setColor(Color.SKY);

        final CheckBox extensionFilterCheckBox = new CheckBox("Extension filter", checkBoxStyle);
        extensionFilterCheckBox.setPosition(415, 380);
        extensionFilterCheckBox.setChecked(extensionFilterEnabled);
        stage.addActor(extensionFilterCheckBox);

        prevGroup.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                reloadSelectedGroup(-1);
            }
        });

        nextGroup.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                reloadSelectedGroup(1);
            }
        });

        deleteLeft.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                callGroupDeletionDialogue(false);
            }
        });

        deleteRight.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                callGroupDeletionDialogue(true);
            }
        });

        switchGroupMode.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (currentGroupMode == 0) {
                    currentGroupMode = 1;
                } else {
                    currentGroupMode = 0;
                }
                currentlyLoadingPreviews = true;
            }
        });

        changeMode.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                switch (comparisonMode) {
                    case SEARCH:
                        comparisonMode = NAME_SEARCH;
                        break;
                    case NAME_SEARCH:
                        comparisonMode = COMPARE_2_FOLDERS;
                        break;
                    case COMPARE_2_FOLDERS:
                        comparisonMode = COMPARE_2_FOLDERS_RENAME;
                        break;
                    case COMPARE_2_FOLDERS_RENAME:
                        comparisonMode = EXIF_SORT;
                        break;
                    case EXIF_SORT:
                        comparisonMode = STATS;
                        break;
                    case STATS:
                        comparisonMode = SEARCH;
                        break;
                }
                updateModeDescription();
                saveParams();
            }
        });

        commit.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                commitChanges();
            }
        });

        extensionFilterCheckBox.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                extensionFilterEnabled = extensionFilterCheckBox.isChecked();
                saveParams();
                reloadExtensionsString();
            }
        });

        startScan.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                startScan();
                changeUiTouchableState(Touchable.disabled);
            }
        });

        addExtension.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                CustomTextInputListener listener = new CustomTextInputListener() {
                    @Override
                    public void input(String text) {
                        extensionsToSearch.add(text);
                        saveParams();
                        reloadExtensionsString();
                    }
                };
                Gdx.input.getTextInput(listener, "Add an extension", ".", "");
            }
        });

        addFolder.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                callFileChooser(new FileDialogueAction() {
                    @Override
                    void performAction(File[] selectedFiles) {
                        for (File file : selectedFiles) {
                            foldersToSearch.add(file.toString());
                        }
                        saveParams();
                        reloadFoldersString();
                    }
                }, true);
            }
        });

        removeAllFolders.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                foldersToSearch.clear();
                slaveFolders.clear();
                saveParams();
                reloadFoldersString();
            }
        });

        removeAllExtensions.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                extensionsToSearch.clear();
                saveParams();
                reloadExtensionsString();
            }
        });

        setMasterPath.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                callFileChooser(new FileDialogueAction() {
                    @Override
                    void performAction(File[] selectedFiles) {
                        masterPath = selectedFiles[0].toString();
                        saveParams();
                        reloadFoldersString();
                    }
                }, false);
            }
        });

        addSlavePath.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                callFileChooser(new FileDialogueAction() {
                    @Override
                    void performAction(File[] selectedFiles) {
                        for (File selectedFile : selectedFiles) {
                            slaveFolders.add(selectedFile.toString());
                        }
                        saveParams();
                        reloadFoldersString();
                    }
                }, true);
            }
        });

        setClonePath.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                callFileChooser(new FileDialogueAction() {
                    @Override
                    void performAction(File[] selectedFiles) {
                        clonePath = selectedFiles[0].toString();
                        saveParams();
                        reloadFoldersString();
                    }
                }, false);
            }
        });

        if (Gdx.files.external(saveDirectory + "dupesSize").file().exists()) {
            callDialogue("Previous results available", "Load?",
                    "Yes", "No", null, new GeneralDialogueAction() {
                        @Override
                        void performAction(int button) {
                            if (button == 0) {
                                try {
                                    sortedFilesArray_leftPart = (ArrayList<ArrayList<String>>) loadObject("sortedFilesArray_leftPart");
                                    sortedFilesArray_rightPart = (ArrayList<ArrayList<String>>) loadObject("sortedFilesArray_rightPart");
                                    sortedFilesIndexMap = (ArrayList<ArrayList<Integer>>) loadObject("sortedFilesIndexMap");
                                    dupesLeft = (ArrayList<Integer>) loadObject("dupesLeft");
                                    fileHashes = (ArrayList<String>) loadObject("fileHashes");
                                    duplicatesOriginal = (int) loadObject("duplicates");
                                    duplicates = (int) loadObject("duplicatesOriginal");
                                    dupesSize = (float) loadObject("dupesSize");
                                    dupes_first_part = (ArrayList<String>) loadObject("dupes_first_part");
                                    dupes_second_part = (ArrayList<String>) loadObject("dupes_second_part");
                                    sortedFilesArray_fingerprintMatch = (ArrayList<ArrayList<Integer>>) loadObject("sortedFilesArray_fingerprintMatch");
                                    currentlyLoadingPreviews = true;
                                    currentlySelectedGroup = 0;
                                } catch (Exception e) {
                                    currentErrorMessage = "[#FF0000]ERROR reading save files \n" + e.getMessage() + "[#00FF00]";
                                }
                            }
                        }
                    });
        }

        reloadFoldersString();
        reloadExtensionsString();
        updateModeDescription();

        Gdx.input.setInputProcessor(stage);
    }

    void changeUiTouchableState(Touchable touchable) {
        if (touchable.equals(Touchable.disabled)) {
            startScan.setColor(Color.GRAY);
            changeMode.setColor(Color.GRAY);
            addFolder.setColor(Color.GRAY);
            removeAllFolders.setColor(Color.GRAY);
            setMasterPath.setColor(Color.GRAY);
            addSlavePath.setColor(Color.GRAY);
            setClonePath.setColor(Color.GRAY);
            removeAllExtensions.setColor(Color.GRAY);
            addExtension.setColor(Color.GRAY);
        } else {
            startScan.setColor(Color.WHITE);
            changeMode.setColor(Color.WHITE);
            addFolder.setColor(Color.WHITE);
            removeAllFolders.setColor(Color.WHITE);
            setMasterPath.setColor(Color.WHITE);
            addSlavePath.setColor(Color.WHITE);
            setClonePath.setColor(Color.WHITE);
            removeAllExtensions.setColor(Color.WHITE);
            addExtension.setColor(Color.WHITE);
        }
        startScan.setTouchable(touchable);
        changeMode.setTouchable(touchable);
        addFolder.setTouchable(touchable);
        removeAllFolders.setTouchable(touchable);
        setMasterPath.setTouchable(touchable);
        addSlavePath.setTouchable(touchable);
        setClonePath.setTouchable(touchable);
        removeAllExtensions.setTouchable(touchable);
        addExtension.setTouchable(touchable);
    }

    void callDialogue(String title, String message, String button1, String button2, String button3, final GeneralDialogueAction onClickAction) {
        GDXButtonDialog bDialog = dialogs.newDialog(GDXButtonDialog.class);
        bDialog.setTitle(title);
        bDialog.setMessage(message);

        bDialog.setClickListener(new ButtonClickListener() {
            @Override
            public void click(int button) {
                onClickAction.performAction(button);
            }
        });

        if (!(button1 == null)) {
            bDialog.addButton(button1);
        }
        if (!(button2 == null)) {
            bDialog.addButton(button2);
        }
        if (!(button3 == null)) {
            bDialog.addButton(button3);
        }

        bDialog.build().show();
    }

    void callGroupDeletionDialogue(final boolean right) {
        if (right) {
            callDialogue("Delete right group?",
                    sortedFilesArray_leftPart.get(currentlySelectedGroup).size() + " files",
                    "Safe delete", "Yes", "No",
                    new GeneralDialogueAction() {
                        @Override
                        void performAction(int button) {
                            if (button == 0) {
                                deleteFilesFromGroup(sortedFilesArray_rightPart, true);
                            } else if (button == 1) {
                                deleteFilesFromGroup(sortedFilesArray_rightPart, false);
                            }
                        }
                    });
        } else {
            callDialogue("Delete left group?",
                    sortedFilesArray_leftPart.get(currentlySelectedGroup).size() + " files",
                    "Safe delete", "Yes", "No",
                    new GeneralDialogueAction() {
                        @Override
                        void performAction(int button) {
                            if (button == 0) {
                                deleteFilesFromGroup(sortedFilesArray_leftPart, true);
                            } else if (button == 1) {
                                deleteFilesFromGroup(sortedFilesArray_leftPart, false);
                            }
                        }
                    });
        }
    }

    void callFileChooser(FileDialogueAction action, boolean multiSelect) {
        JFileChooser jFileChooser = new JFileChooser(new File("C:\\"));
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

    void updateModeDescription() {
        comparisonMode_humanReadable = modeNamesMap.getString(comparisonMode);
    }

    void deleteFilesFromGroup(ArrayList<ArrayList<String>> group, boolean safeDelete) {
        ArrayList<String> filesToDelete = group.get(currentlySelectedGroup);
        ArrayList<Integer> indexMap = sortedFilesIndexMap.get(currentlySelectedGroup);
        for (int i = 0; i < filesToDelete.size(); i++) {
            deleteFileAndDecrementDupes(filesToDelete.get(i), safeDelete, indexMap.get(i));
        }
        saveDupeCountAndRefreshPreviews();
    }

    void saveDupeCountAndRefreshPreviews(boolean refresh) {
        if (refresh) {
            currentlyLoadingPreviews = true;
            try {
                saveObject("dupesLeft", dupesLeft);
                saveObject("duplicates", duplicates);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void saveDupeCountAndRefreshPreviews() {
        saveDupeCountAndRefreshPreviews(true);
    }

    void deleteFileAndDecrementDupes(String path, boolean safeDelete, int index) {
        if (dupesLeft.get(index) > 0) {
            File fileToDelete = new File(path);
            if (fileToDelete.exists()) {
                boolean success;
                if (safeDelete) {
                    success = !markAsDeleted(fileToDelete).contains("Error");
                } else {
                    success = fileToDelete.delete();
                }
                if (success) {
                    dupesLeft.set(index, dupesLeft.get(index) - 1);
                    duplicates--;
                } else {
                    currentErrorMessage = "[#FF0000]Error deleting " + fileToDelete;
                }
            }
        }
    }

    void reloadSelectedGroup(int increment) {
        if (currentGroupMode == 0) {
            float prevGroup = currentlySelectedGroup;
            currentlySelectedGroup = MathUtils.clamp(currentlySelectedGroup + increment, 0, sortedFilesArray_leftPart.size() - 1);
            currentlyViewedPaths =
                    "[#00FF00]" + getFilePathWithoutName(sortedFilesArray_leftPart.get(currentlySelectedGroup).get(0))
                            + "\n[#FFFFFF] and [#33FF10]" +
                            getFilePathWithoutName(sortedFilesArray_rightPart.get(currentlySelectedGroup).get(0))
                            + "\nGroup: " + (currentlySelectedGroup + 1) + "/" + (sortedFilesArray_leftPart.size())
                            + "\nDupes all/left: " + duplicatesOriginal + "(" + formatNumber(dupesSize) + "Gb)/" + duplicates
                            + "\nFiles in group: " + sortedFilesArray_leftPart.get(currentlySelectedGroup).size() * 2;
            currentlyLoadingPreviews = !(prevGroup == currentlySelectedGroup);
            resultsAvailable = prevGroup == currentlySelectedGroup;

        } else {
            float prevGroup = currentlySelectedGroup_fingerprint;
            currentlySelectedGroup_fingerprint = MathUtils.clamp(currentlySelectedGroup_fingerprint + increment, 0, sortedFilesArray_fingerprintMatch.size() - 1);
            currentlyViewedPaths = "[#00FF00]" + sortedFilesArray_fingerprintMatch.get(currentlySelectedGroup_fingerprint).size() + " files grouped"
                    + "\n[]Group: " + (currentlySelectedGroup_fingerprint + 1) + "/" + sortedFilesArray_fingerprintMatch.size()
                    + "\n[#00FF00]Dupes all/left: " + duplicatesOriginal + "(" + formatNumber(dupesSize) + "Gb)/" + duplicates;
            currentlyLoadingPreviews = !(prevGroup == currentlySelectedGroup_fingerprint);
            resultsAvailable = prevGroup == currentlySelectedGroup_fingerprint;
        }
    }

    TextButton makeTextButton(String text, float width, float x, float y, boolean addToStage) {
        TextButton button = new TextButton(text, buttonStyle);
        button.setWidth(width);
        button.setPosition(x, y);
        if (addToStage) {
            stage.addActor(button);
        }
        return button;
    }

    Label makeLabel(String text) {
        Label label = new Label(text, labelStyle);
        label.setWrap(true);
        return label;
    }

    TextButton makeTextButton(String text, float width, float x, float y) {
        return makeTextButton(text, width, x, y, true);
    }

    void commitChanges() {
        String fileName = saveDirectory + "results_" + comparisonMode + getCurrentDate() + "\\commit" + getCurrentTime() + "." + commitCount + ".txt";
        if (comparisonMode.equals(SEARCH)) {
            dupes_merged.clear();
            for (int i = 0; i < dupes_first_part.size(); i++) {
                dupes_merged.add("\n\n\n" + dupes_first_part.get(i) + "\n and \n" + dupes_second_part.get(i));
            }
        }
        FileHandle results = Gdx.files.external(fileName);
        results.writeString(dupes_merged.toString(), false);
        commitCount++;
        lastCommitTime = System.currentTimeMillis();
    }

    void loadParams() {
        String[] allParams = new String[]{};
        foldersToSearch = new ArrayList<>();
        extensionsToSearch = new ArrayList<>();
        slaveFolders = new ArrayList<>();

        if (!paramsFile.exists()) {
            paramsFile.writeString("c_mode_search", false);
        } else {
            allParams = paramsFile.readString().split("\r\n");
        }

        for (String param : allParams) {
            if (param.startsWith("c_mode_")) {
                comparisonMode = param.substring(7);
            }
            if (param.startsWith("foldMaster_")) {
                masterPath = param.substring(11);
            }
            if (param.startsWith("foldSlave_")) {
                slaveFolders.add(param.substring(10));
            }
            if (param.startsWith("foldDupe_")) {
                clonePath = param.substring(9);
            }
            if (param.startsWith("ext_")) {
                extensionsToSearch.add(param.substring(4));
            }
            if (param.startsWith("extension_filter_off")) {
                extensionFilterEnabled = false;
            }
            if (param.startsWith("fold_")) {
                foldersToSearch.add(param.substring(5));
            }
        }
    }

    void saveParams() {
        StringBuilder paramsToWrite = new StringBuilder();
        paramsToWrite.append("c_mode_").append(comparisonMode).append("\r\n");
        for (String folderPath : foldersToSearch) {
            paramsToWrite.append("fold_").append(folderPath).append("\r\n");
        }
        for (String extension : extensionsToSearch) {
            paramsToWrite.append("ext_").append(extension).append("\r\n");
        }
        for (String slavePath : slaveFolders) {
            paramsToWrite.append("foldSlave_").append(slavePath).append("\r\n");
        }
        paramsToWrite.append("foldMaster_").append(masterPath).append("\r\n");
        paramsToWrite.append("foldDupe_").append(clonePath).append("\r\n");
        if (!extensionFilterEnabled) {
            paramsToWrite.append("extension_filter_off\r\n");
        }
        paramsFile.writeString(paramsToWrite.toString(), false);
    }

    void reloadFoldersString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[#00FF00]Folders to search: (").append(foldersToSearch.size()).append(")\n");
        for (String folderPath : foldersToSearch) {
            builder.append(folderPath).append("\n");
        }
        builder.append("[#00DDFF]Master folder:").append(masterPath).append("\n");
        builder.append("[#FF5555]Slave folders: (").append(slaveFolders.size()).append(")\n");
        for (String slavePath : slaveFolders) {
            builder.append(slavePath).append("\n");
        }
        builder.append("[#FFDD00]Clone folder:").append(clonePath);
        foldersToSearch_string = builder.toString();
    }

    void reloadExtensionsString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[#00FF00]Extensions to search: (").append(extensionsToSearch.size()).append(")\n");
        if (extensionFilterEnabled) {
            builder.append("ENABLED\n");
        } else {
            builder.append("[#FF5555]DISABLED(search all)[#00FF00]\n");
        }
        for (int i = 0; i < extensionsToSearch.size(); i++) {
            builder.append(extensionsToSearch.get(i)).append("\n");
        }
        extensionsToSearch_string = builder.toString();
    }

    String calculateFileGroupFingerprint(int index) {
        StringBuilder currentFingerprint = new StringBuilder(getFilePathWithoutName(dupes_first_part.get(index)));
        String[] secondPart = dupes_second_part.get(index).split("\n");
        for (String s : secondPart) {
            currentFingerprint.append(getFilePathWithoutName(s));
        }
        return currentFingerprint.toString();
    }

    void startScan() {
        allFiles = 0;
        comparedFiles = 0;
        prevFiles = 0;
        new Thread(new Runnable() {
            public void run() {
                startTime = System.currentTimeMillis();
                switch (comparisonMode) {
                    case COMPARE_2_FOLDERS:
                    case COMPARE_2_FOLDERS_RENAME:
                        ArrayList<File> files_master = getAllFiles(masterPath);
                        allFilesCalculated = files_master.size();
                        for (File master_file : files_master) {
                            if (filterExtension(master_file)) {
                                String hash = getFileChecksum(master_file);
                                if (hash.length() > 1) {
                                    hashedFiles.put(hash, master_file.toString());
                                }
                            }
                            allFiles++;
                        }
                        for (String slavePath : slaveFolders) {
                            ArrayList<File> files_slave = getAllFiles(slavePath);
                            allFilesCalculated += files_slave.size();

                            for (File slave_file : files_slave) {
                                if (filterExtension(slave_file)) {
                                    String hash = getFileChecksum(slave_file);
                                    if (hashedFiles.containsKey(hash)) {
                                        String fullPath;
                                        long originalFileSize = slave_file.length();
                                        if (comparisonMode.equals(COMPARE_2_FOLDERS)) {
                                            fullPath = moveAccordingToFolderStructure(slave_file, clonePath);
                                        } else {
                                            fullPath = markAsDeleted(slave_file);
                                        }
                                        commitDupe(originalFileSize, "moved " + slave_file + " to " + fullPath + " master: " + hashedFiles.get(hash) + "\n");
                                    }
                                    currentFile = slave_file.toString();
                                    comparedFiles++;
                                }
                                allFiles++;
                            }
                        }
                        break;
                    case SEARCH:
                    case NAME_SEARCH:
                        new Thread(new Runnable() {
                            public void run() {
                                int filesCalculated = 0;
                                for (String folderPath : foldersToSearch) {
                                    filesCalculated += getAllFiles(folderPath).size();
                                }
                                allFilesCalculated = filesCalculated;
                            }
                        }).start();
                        for (String folderPath : foldersToSearch) {
                            walk(folderPath);
                            commitChanges();
                        }
                        new Thread(new Runnable() {
                            public void run() {

                                currentlySortingFolders = true;
                                currentHash = "Sorting folders";

                                int doneCounter = 0;
                                int nextStartFrom = 0;
                                boolean previousDone;
                                String currentPath_left = "";
                                String currentPath_right = "";

                                while (doneCounter < notSortedFilesArray_leftPart.size()) {
                                    for (int i = nextStartFrom; i < notSortedFilesArray_leftPart.size(); i++) {
                                        if (!notSortedFilesArray_leftPart.get(i).equals("DONE")) {
                                            currentPath_left = getFilePathWithoutName(notSortedFilesArray_leftPart.get(i));
                                            currentPath_right = getFilePathWithoutName(notSortedFilesArray_rightPart.get(i));
                                            currentFile = "Set new path " + currentPath_left;
                                            break;
                                        }
                                    }
                                    ArrayList<String> leftPart = new ArrayList<>();
                                    ArrayList<String> rightPart = new ArrayList<>();
                                    ArrayList<Integer> indexMap_sorted = new ArrayList<>();
                                    previousDone = true;

                                    for (int i = nextStartFrom; i < notSortedFilesArray_leftPart.size(); i++) {
                                        if (!notSortedFilesArray_leftPart.get(i).equals("DONE")) {
                                            if (getFilePathWithoutName(notSortedFilesArray_leftPart.get(i)).equals(currentPath_left) &&
                                                    getFilePathWithoutName(notSortedFilesArray_rightPart.get(i)).equals(currentPath_right)) {
                                                leftPart.add(notSortedFilesArray_leftPart.get(i));
                                                rightPart.add(notSortedFilesArray_rightPart.get(i));
                                                indexMap_sorted.add(indexMap.get(i));
                                                currentFile = "Added file to a group " + notSortedFilesArray_leftPart.get(i);
                                                notSortedFilesArray_leftPart.set(i, "DONE");
                                                if (previousDone) {
                                                    nextStartFrom = i;
                                                }
                                                doneCounter++;
                                            }
                                            previousDone = false;
                                        }
                                    }
                                    sortedFilesArray_leftPart.add(leftPart);
                                    sortedFilesArray_rightPart.add(rightPart);
                                    sortedFilesIndexMap.add(indexMap_sorted);
                                    currentHash = "Sorting folders" + " (" + doneCounter + " / " + notSortedFilesArray_leftPart.size() + ", " + nextStartFrom + ")";
                                }

                                int completed = 0;
                                nextStartFrom = 0;
                                boolean[] doneFlags = new boolean[dupes_first_part.size()];
                                while (completed < dupes_first_part.size() - 1) {
                                    String currentFingerprint = "";
                                    for (int i = nextStartFrom; i < dupes_first_part.size(); i++) {
                                        if (!doneFlags[i]) {
                                            currentFingerprint = calculateFileGroupFingerprint(i);
                                            break;
                                        }
                                    }

                                    ArrayList<Integer> group = new ArrayList<>();
                                    previousDone = true;

                                    for (int i = nextStartFrom; i < dupes_first_part.size(); i++) {
                                        if (!doneFlags[i]) {
                                            if (currentFingerprint.equals(calculateFileGroupFingerprint(i))) {
                                                group.add(i);
                                                doneFlags[i] = true;
                                                if (previousDone) {
                                                    nextStartFrom = i;
                                                }
                                                completed++;
                                            }
                                            previousDone = false;
                                        }
                                    }
                                    sortedFilesArray_fingerprintMatch.add(group);
                                }

                                currentlySortingFolders = false;
                                currentlyPreLoadingPreviews = true;
                                currentHash = "Preloading previews";
                                if (sortedFilesArray_leftPart.size() == 0) {
                                    currentlyPreLoadingPreviews = false;
                                    currentHash = "No dupes found";
                                }

                                if (sortedFilesArray_leftPart.size() > 0) {
                                    new Thread(new Runnable() {
                                        public void run() {
                                            currentHash = "Autosaving...";
                                            try {
                                                saveObject("sortedFilesArray_leftPart", sortedFilesArray_leftPart);
                                                saveObject("sortedFilesArray_rightPart", sortedFilesArray_rightPart);
                                                saveObject("sortedFilesIndexMap", sortedFilesIndexMap);
                                                saveObject("dupesLeft", dupesLeft);
                                                saveObject("fileHashes", fileHashes);
                                                saveObject("duplicatesOriginal", duplicatesOriginal);
                                                duplicates = duplicatesOriginal;
                                                saveObject("duplicates", duplicates);
                                                saveObject("dupesSize", dupesSize);
                                                saveObject("dupes_first_part", dupes_first_part);
                                                saveObject("dupes_second_part", dupes_second_part);
                                                saveObject("dupesSize", dupesSize);
                                                saveObject("sortedFilesArray_fingerprintMatch", sortedFilesArray_fingerprintMatch);
                                                currentHash = "Saved!";
                                            } catch (Exception e) {
                                                currentErrorMessage = "[#FF0000]ERROR while trying to autosave[#00FF00]";
                                            }
                                        }
                                    }).start();
                                }
                            }
                        }).start();
                        break;
                    case EXIF_SORT: {

                        ArrayList<File> files = new ArrayList<>();
                        for (int i = 0; i < foldersToSearch.size(); i++) {
                            files.addAll(getAllFiles(foldersToSearch.get(i)));
                        }

                        allFilesCalculated = files.size();

                        for (int i = 0; i < files.size(); i++) {
                            String fileName = files.get(i).getName().toLowerCase().trim();
                            if (fileName.endsWith(".cr2")
                                    || fileName.endsWith(".jpg")
                                    || fileName.endsWith(".mp4")
                                    || fileName.endsWith(".tiff")) {
                                String newFileName = getFilePathWithoutName(files.get(i).getAbsolutePath()) + "\\" + getImageCreationDateAndCameraModel(files.get(i));
                                currentFile = files.get(i).toString();
                                currentHash = newFileName;
                                boolean success = files.get(i).renameTo(new File(newFileName));
                                sleep();
                                comparedFiles++;
                                String message = files.get(i) + " to " + newFileName + "\n";
                                String additionalMessage = "renamed ";
                                if (!success) {
                                    additionalMessage = "Error renaming ";
                                    currentErrorMessage = "[#FF0000]Error renaming " + files.get(i) + " to " + newFileName;
                                }
                                dupes_merged.add(additionalMessage + message);
                            }
                            allFiles++;
                        }
                        break;
                    }
                    case STATS: {

                        ArrayList<String> extensions_sorted_size = new ArrayList<>();
                        ArrayList<String> extensions_sorted_quantity = new ArrayList<>();
                        ArrayList<String> cameraModels_sorted = new ArrayList<>();
                        int allCameraModels = 0;
                        int allExtensions = 0;
                        HashMap<String, Integer> extensions_quantity = new HashMap<>();
                        HashMap<String, Long> extensions_size = new HashMap<>();
                        HashMap<String, Integer> cameraModels = new HashMap<>();
                        HashMap<String, String> emptyFolders = new HashMap<>();

                        ArrayList<File> filesAndFolders = new ArrayList<>();
                        for (int i = 0; i < foldersToSearch.size(); i++) {
                            filesAndFolders.addAll(getAllFiles(foldersToSearch.get(i)));
                        }
                        for (int i = 0; i < foldersToSearch.size(); i++) {
                            filesAndFolders.addAll(getAllDirectories(foldersToSearch.get(i)));
                        }
                        allFilesCalculated = filesAndFolders.size();
                        long allFileSizes = 0;
                        for (File file : filesAndFolders) {
                            currentFile = file.getAbsolutePath();
                            if (file.isFile()) {
                                allFileSizes += file.length();
                                int index = file.getName().lastIndexOf(".");
                                if (index > -1) {
                                    String extension = file.getName().substring(index).toLowerCase();
                                    if (extension.length() > 15) {
                                        extension = extension.substring(0, 11) + "...";
                                    }
                                    if (!extensions_quantity.containsKey(extension)) {
                                        extensions_quantity.put(extension, 1);
                                        extensions_size.put(extension, file.length());
                                    } else {
                                        extensions_quantity.put(extension, extensions_quantity.get(extension) + 1);
                                        extensions_size.put(extension, extensions_size.get(extension) + file.length());
                                    }
                                    allExtensions++;
                                    currentHash = extension + " " + extensions_quantity.get(extension);
                                }
                                String cameraModel = getImageCameraModel(file);
                                if (cameraModel.length() > 0) {
                                    if (!cameraModels.containsKey(cameraModel)) {
                                        cameraModels.put(cameraModel, 1);
                                    } else {
                                        cameraModels.put(cameraModel, cameraModels.get(cameraModel) + 1);
                                    }
                                    allCameraModels++;
                                    currentHash = cameraModel + " " + cameraModels.get(cameraModel);
                                }
                            } else {
                                if (file.length() < emptyDirectorySize) {
                                    ArrayList<File> allFoldersAndFiles = getAllFiles(file.getAbsolutePath());
                                    ArrayList<String> allFilesAndFolders_onlyNames = new ArrayList<>();
                                    for (File contentEntry : allFoldersAndFiles) {
                                        allFilesAndFolders_onlyNames.add(contentEntry.getAbsolutePath().replace(file.getAbsolutePath(), ""));
                                    }
                                    emptyFolders.put(file.getAbsolutePath(), ", Files in folder:" + allFilesAndFolders_onlyNames + "\n");
                                    currentHash = file.getAbsolutePath() + " " + emptyFolders.size();
                                }
                            }
                            comparedFiles++;
                            allFiles++;
                        }
                        StringBuilder results = new StringBuilder();
                        results.append("Detected camera models:").append(cameraModels.size()).append("\n");
                        results.append("Detected extensions:").append(extensions_quantity.size()).append("\n");
                        results.append("All empty folders:").append(emptyFolders.size()).append("\n");
                        results.append("All files scanned:").append(allFilesCalculated).append("\n");
                        results.append("Total size:").append(formatNumber(convertToGigabytes(allFileSizes))).append("Gb\n\n");
                        results.append("File extension stats:\n");

                        ArrayList<String> cameraModels_keySet = new ArrayList<>(cameraModels.keySet());
                        ArrayList<String> extension_keySet = new ArrayList<>(extensions_quantity.keySet());

                        ArrayList<String> indexes_extensions_quantity = new ArrayList<>();
                        ArrayList<String> indexes_extensions_size = new ArrayList<>();
                        ArrayList<String> indexes_cameraModels = new ArrayList<>();
                        for (int i = 0; i < cameraModels_keySet.size(); i++) {
                            indexes_cameraModels.add(cameraModels.get(cameraModels_keySet.get(i)) + "." + i);
                        }
                        for (int i = 0; i < extension_keySet.size(); i++) {
                            indexes_extensions_quantity.add(extensions_quantity.get(extension_keySet.get(i)) + "." + i);
                        }
                        for (int i = 0; i < extension_keySet.size(); i++) {
                            indexes_extensions_size.add(extensions_size.get(extension_keySet.get(i)) + "." + i);
                        }

                        Collections.sort(indexes_extensions_quantity, new CustomIndexComparator());
                        Collections.sort(indexes_extensions_size, new CustomIndexComparator());
                        Collections.sort(indexes_cameraModels, new CustomIndexComparator());

                        for (String index : indexes_extensions_quantity) {
                            int actualIndex = Integer.parseInt(String.valueOf(index).split("\\.")[1]);
                            extensions_sorted_quantity.add(extension_keySet.get(actualIndex));
                        }

                        for (String index : indexes_extensions_size) {
                            int actualIndex = Integer.parseInt(String.valueOf(index).split("\\.")[1]);
                            extensions_sorted_size.add(extension_keySet.get(actualIndex));
                        }

                        for (String index : indexes_cameraModels) {
                            int actualIndex = Integer.parseInt(String.valueOf(index).split("\\.")[1]);
                            cameraModels_sorted.add(cameraModels_keySet.get(actualIndex));
                        }

                        int longestExtension = 0;
                        int longestQuantity = 0;
                        int longestSize = 0;

                        for (int i = 0; i < extension_keySet.size(); i++) {
                            int currentExtension = extension_keySet.get(i).length();
                            int currentQuantity = String.valueOf(extensions_quantity.get(extension_keySet.get(i))).length();
                            int currentSize = (formatNumber(convertToGigabytes(extensions_size.get(extension_keySet.get(i)))) + "Gb").length();
                            if (currentExtension > longestExtension) {
                                longestExtension = currentExtension;
                            }
                            if (currentSize > longestSize) {
                                longestSize = currentSize;
                            }
                            if (currentQuantity > longestQuantity) {
                                longestQuantity = currentQuantity;
                            }
                        }

                        ArrayList<String> extensions_sorted_results = new ArrayList<>();
                        for (int i = 0; i < extensions_sorted_quantity.size(); i++) {

                            String ext_sort1 = extensions_sorted_quantity.get(i);
                            String ext_sort2 = extensions_sorted_size.get(i);

                            StringBuilder itemToAdd = new StringBuilder();
                            for (int i2 = 0; i2 < 2; i2++) {
                                String ext;
                                String endingChar;
                                if (i2 == 0) {
                                    ext = ext_sort1;
                                    endingChar = "   ||   ";
                                } else {
                                    ext = ext_sort2;
                                    endingChar = "\n";
                                }
                                itemToAdd.append(normaliseLength(ext, longestExtension)).append(" ").append(normaliseLength("" + extensions_quantity.get(ext), longestQuantity));
                                String percentage = calculatePercentage(allExtensions, extensions_quantity.get(ext));
                                itemToAdd.append(" ").append(normaliseLength(percentage, 6)).append(" ").append(normaliseLength(formatNumber(convertToGigabytes(extensions_size.get(ext))) + "Gb", longestSize));
                                String percentage_size = calculatePercentage(allFileSizes, extensions_size.get(ext));
                                itemToAdd.append(" ").append(normaliseLength(percentage_size, 6)).append(endingChar);
                            }
                            extensions_sorted_results.add(itemToAdd.toString());
                        }

                        for (String item : extensions_sorted_results) {
                            results.append(item);
                        }

                        results.append("\nCamera model stats:\n");
                        for (String model : cameraModels_sorted) {
                            results.append(normaliseLength(model, 15)).append(" ").append(cameraModels.get(model)).append(" ");
                            if (formatNumber(cameraModels.get(model) / (double) allCameraModels * 100) == 0) {
                                results.append("<.01%\n");
                            } else {
                                results.append(formatNumber(cameraModels.get(model) / (double) allCameraModels * 100)).append("%\n");
                            }
                        }
                        results.append("\nEmpty directory stats:\n");
                        for (String folder : emptyFolders.keySet()) {
                            results.append(folder).append(emptyFolders.get(folder));
                        }
                        dupes_merged.clear();
                        dupes_merged.add(results.toString());
                    }
                }
                currentFile = "Finished! took " + getElapsedTime(startTime) + " min";
                currentHash = "";
                commitChanges();
                changeUiTouchableState(Touchable.enabled);
                if (currentCommits > 0) {
                    saveHashBase();
                }
                Arrays.fill(filesPerSecSmoothing, 0);
            }
        }).start();
    }

    void commitDupe(File file) {
        duplicatesOriginal++;
        dupesSize += convertToGigabytes(file.length());
    }

    void commitDupe(long fileSize, String commitMessage) {
        dupes_merged.add(commitMessage);
        duplicatesOriginal++;
        dupesSize += convertToGigabytes(fileSize);
    }

    boolean filterExtension(File file) {
        boolean addToList = !extensionFilterEnabled;
        if (extensionFilterEnabled) {
            for (String ext : extensionsToSearch) {
                if (file.getAbsoluteFile().toString().toLowerCase().endsWith(ext.toLowerCase())) {
                    addToList = true;
                    break;
                }
            }
        }
        for (String ext : excludedFormats) {
            if (file.getAbsoluteFile().toString().toLowerCase().endsWith(ext.toLowerCase())) {
                return false;
            }
        }
        return addToList;
    }

    public void walk(String path) {

        File[] list = new File(path).listFiles();

        if (list == null) return;

        for (final File file : list) {
            if (file.isDirectory()) {
                walk(file.getAbsolutePath());
            } else {
                allFiles++;
                if (filterExtension(file)) {
                    currentFile = file.getAbsoluteFile().toString();
                    comparedFiles++;
                    String currentHash;
                    boolean isValidHash;
                    if (comparisonMode.equals(SEARCH)) {
                        currentHash = getFileChecksum(file.getAbsoluteFile());
                        isValidHash = currentHash.length() > 1;
                    } else {
                        String fullName = file.getName().toLowerCase();
                        int index = fullName.lastIndexOf(".");
                        currentHash = fullName;
                        if (!(index == -1)) {
                            currentHash = fullName.substring(0, index).trim();
                        }
                        isValidHash = true;
                    }
                    if (isValidHash) {
                        if (hashedFiles.containsKey(currentHash)) {
                            commitDupe(file);
                            String currentDupeFromList = hashedFiles.get(currentHash);
                            String currentDupeFromDisk = file.getAbsoluteFile().toString();

                            notSortedFilesArray_leftPart.add(currentDupeFromList);
                            notSortedFilesArray_rightPart.add(currentDupeFromDisk);

                            if (!dupes_first_part.contains(currentDupeFromList)) {
                                dupes_first_part.add(currentDupeFromList);
                                dupes_second_part.add(currentDupeFromDisk);
                                dupesLeft.add(1);
                                fileHashes.add(currentHash);
                                indexMap.add(dupesLeft.size() - 1);
                            } else {
                                int index = dupes_first_part.indexOf(currentDupeFromList);
                                dupes_second_part.set(index, dupes_second_part.get(index) + "\n" + currentDupeFromDisk);
                                dupesLeft.set(index, dupesLeft.get(index) + 1);
                                indexMap.add(index);
                            }
                        } else {
                            hashedFiles.put(currentHash, file.getAbsoluteFile().toString());
                        }
                    }
                    Main.currentHash = currentHash;
                }
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        camera.position.set(400, 240, 0);
        float tempScaleH = height / 480.0f;
        float tempScaleW = width / 800.0f;
        float zoom = min(tempScaleH, tempScaleW);
        camera.zoom = 1 / zoom;
        camera.update();
    }

    String addFileStateSymbol(String filePath) {
        if (new File(filePath).exists()) {
            return "⬛" + filePath;
        } else {
            return "☐" + filePath;
        }
    }

    CheckBox loadPreviewEntry(final String path, Table addTo, final int indexInArray) {

        CheckBox checkBox = null;

        Table entryTable = new Table();
        final Image preview;

        final File imageFile = new File(path);

        preview = new Image(loadPreview(path));

        preview.setScaling(Scaling.fit);
        entryTable.add(preview).size(150).row();
        TextButton name = makeTextButton(imageFile.getName() + ", \n"
                + dupesLeft.get(indexInArray) + " dupes left, hash: " + fileHashes.get(indexInArray).substring(0, 10) + "...", 150, 65, 0);

        name.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {

                String[] secondPartPaths = dupes_second_part.get(indexInArray).split("\n");
                for (int i = 0; i < secondPartPaths.length; i++) {
                    secondPartPaths[i] = addFileStateSymbol(secondPartPaths[i]);
                }

                StringBuilder rightPaths = new StringBuilder();
                for (String rightPath_modified : secondPartPaths) {
                    rightPaths.append(rightPath_modified).append("\n");
                }

                String message = addFileStateSymbol(dupes_first_part.get(indexInArray)) + "\nand\n" + rightPaths;

                String message2 = dupesLeft.get(indexInArray) + " dupes left";
                if (dupesLeft.get(indexInArray) == 0) {
                    message2 = "\n\n\nWARNING: " + message2 + ", think twice before deleting!!!";
                }
                callDialogue("Delete " + imageFile.getName() + "?", message + "\n" + message2, "Safe delete", "Yes", "No",
                        new GeneralDialogueAction() {
                            @Override
                            void performAction(int button) {
                                if (!(button == 2)) {
                                    deleteFileAndDecrementDupes(path, button == 0, indexInArray);
                                    preview.setDrawable(new Image(loadPreview(path)).getDrawable());
                                    saveDupeCountAndRefreshPreviews(false);
                                }
                            }
                        });
            }
        });
        if (currentGroupMode == 1) {
            checkBox = new CheckBox("", checkBoxStyle);
            entryTable.add(checkBox).row();
        }
        entryTable.add(name).pad(5);
        addTo.add(entryTable).pad(7).padTop(27).align(Align.center).row();
        return checkBox;
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float filesPerSec_current = (allFiles - prevFiles) / Gdx.graphics.getDeltaTime();

        float filesPerSec = 0;
        for (int i = 0; i < filesPerSecSmoothing.length; i++) {
            filesPerSec += filesPerSecSmoothing[i];
            if (i > 0) {
                filesPerSecSmoothing[i - 1] = filesPerSecSmoothing[i];
            }
        }
        filesPerSecSmoothing[filesPerSecSmoothing.length - 1] = filesPerSec_current;
        filesPerSec = (float) formatNumber(filesPerSec / (float) filesPerSecSmoothing.length);

        float minutesLeft = (float) formatNumber((allFilesCalculated - allFiles) / filesPerSec / 60f);
        prevFiles = allFiles;

        if (currentlyPreLoadingPreviews) {
            ArrayList<String> loadFrom_left = sortedFilesArray_leftPart.get(preloadGroupIndex);
            ArrayList<String> loadFrom_right = sortedFilesArray_rightPart.get(preloadGroupIndex);
            currentFile = "Preloading group " + (preloadGroupIndex + 1) + "/" + sortedFilesArray_leftPart.size();
            for (int i = 0; i < loadFrom_left.size(); i++) {
                createThumbnail(loadFrom_left.get(i));
                createThumbnail(loadFrom_right.get(i));
            }
            if (preloadGroupIndex < sortedFilesArray_leftPart.size() - 1) {
                preloadGroupIndex++;
            } else {
                currentlyPreLoadingPreviews = false;
                currentlyLoadingPreviews = true;
            }
        }

        if (currentlyLoadingPreviews) {
            stage_results.clear();
            Table container = new Table();
            TextButton deleteAllButton = null;
            if (currentGroupMode == 0) {
                ArrayList<String> loadFrom_left = sortedFilesArray_leftPart.get(currentlySelectedGroup);
                ArrayList<String> loadFrom_right = sortedFilesArray_rightPart.get(currentlySelectedGroup);
                ArrayList<Integer> loadFromIndexMap = sortedFilesIndexMap.get(currentlySelectedGroup);
                Table rightTable = new Table();
                Table leftTable = new Table();

                unloadAllTextures();
                for (int i = 0; i < loadFrom_left.size(); i++) {
                    loadPreviewEntry(loadFrom_left.get(i), leftTable, loadFromIndexMap.get(i));
                    loadPreviewEntry(loadFrom_right.get(i), rightTable, loadFromIndexMap.get(i));
                }

                container.add(leftTable).width(400);
                container.add(rightTable).width(400);
            } else {

                final ArrayList<Integer> loadFrom = sortedFilesArray_fingerprintMatch.get(currentlySelectedGroup_fingerprint);
                final ArrayList<ArrayList<String>> dupesMerged = new ArrayList<>();
                final ArrayList<ArrayList<Boolean>> dupesMerged_checked = new ArrayList<>();

                for (int i = 0; i < loadFrom.size(); i++) {
                    ArrayList<String> merged = new ArrayList<>();
                    ArrayList<Boolean> merged_checked = new ArrayList<>();
                    merged.add(dupes_first_part.get(loadFrom.get(i)));
                    merged.addAll(Arrays.asList(dupes_second_part.get(loadFrom.get(i)).split("\n")));
                    for (int i2 = 0; i2 < merged.size(); i2++) {
                        merged_checked.add(!(i2 == 0));
                    }
                    dupesMerged_checked.add(merged_checked);
                    dupesMerged.add(merged);
                }

                for (int i = 0; i < loadFrom.size(); i++) {
                    Table column = new Table();
                    ArrayList<String> loadGroupFrom = dupesMerged.get(i);
                    for (int i2 = 0; i2 < loadGroupFrom.size(); i2++) {
                        if (i == 0) {
                            TextButton deleteButton = makeTextButton("Delete", 10, 10, 10);
                            deleteButton.setColor(Color.RED);
                            final int finalI = i2;
                            deleteButton.addListener(new ClickListener() {
                                @Override
                                public void clicked(InputEvent event, float x, float y) {
                                    callDialogue("Delete group?", loadFrom.size() + " files",
                                            "Safe delete", "Yes", "No",
                                            new GeneralDialogueAction() {
                                                @Override
                                                void performAction(int button) {
                                                    if (!(button == 2)) {
                                                        for (int k = 0; k < dupesMerged.size(); k++) {
                                                            deleteFileAndDecrementDupes(dupesMerged.get(k).get(finalI), button == 0, loadFrom.get(k));
                                                        }
                                                        saveDupeCountAndRefreshPreviews();
                                                    }
                                                }
                                            });

                                }
                            });
                            Table container_small = new Table();
                            container_small.add(makeLabel(getFilePathWithoutName(loadGroupFrom.get(i2)))).align(Align.center).width(200).padLeft(15).row();
                            container_small.add(deleteButton).width(90).align(Align.center);
                            column.add(container_small);
                        }
                        final CheckBox deletionCheckBox = loadPreviewEntry(loadGroupFrom.get(i2), column, loadFrom.get(i));
                        deletionCheckBox.setChecked(dupesMerged_checked.get(i).get(i2));
                        final int finalI = i;
                        final int finalI2 = i2;
                        deletionCheckBox.addListener(new ClickListener() {
                            @Override
                            public void clicked(InputEvent event, float x, float y) {
                                dupesMerged_checked.get(finalI).set(finalI2, deletionCheckBox.isChecked());
                            }
                        });
                    }
                    container.add(column);
                }
                deleteAllButton = makeTextButton("Apply (Delete all selected)", 190, 580, 410);
                deleteAllButton.setColor(Color.RED);
                deleteAllButton.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        callDialogue("Apply changes?", "Delete selected files",
                                "Safe delete", "Yes", "No",
                                new GeneralDialogueAction() {
                                    @Override
                                    void performAction(int button) {
                                        if (!(button == 2)) {
                                            for (int i = 0; i < dupesMerged.size(); i++) {
                                                for (int k = dupesMerged.get(i).size() - 1; k >= 0; k--) {
                                                    deleteFileAndDecrementDupes(dupesMerged.get(i).get(k), button == 0, loadFrom.get(i));
                                                }
                                            }
                                            saveDupeCountAndRefreshPreviews();
                                        }
                                    }
                                });
                    }
                });
            }

            ScrollPane.ScrollPaneStyle scrollStyle = new ScrollPane.ScrollPaneStyle();
            scrollStyle.vScroll = constructFilledImageWithColor(10, 50, Color.DARK_GRAY);
            scrollStyle.vScrollKnob = constructFilledImageWithColor(10, 50, Color.RED);
            if (currentGroupMode == 1) {
                scrollStyle.hScroll = constructFilledImageWithColor(50, 10, Color.DARK_GRAY);
                scrollStyle.hScrollKnob = constructFilledImageWithColor(50, 10, Color.RED);
            }
            ScrollPane scroll = new ScrollPane(container, scrollStyle);
            float scale = 0.7f;
            if (currentGroupMode == 1) {
                scale = 0.5f;
            }
            scroll.setScale(scale);
            scroll.setBounds(0, 0, 800 / scale, 480 / scale);
            scroll.setScrollbarsVisible(true);
            scroll.setFadeScrollBars(false);
            stage_results.addActor(scroll);
            stage_results.addActor(prevGroup);
            stage_results.addActor(nextGroup);
            if (currentGroupMode == 0) {
                stage_results.addActor(deleteLeft);
                stage_results.addActor(deleteRight);
            } else {
                stage_results.addActor(deleteAllButton);
            }
            stage_results.addActor(switchGroupMode);
            reloadSelectedGroup(0);
            Gdx.input.setInputProcessor(stage_results);
        }

        batch.setProjectionMatrix(camera.combined);
        renderer.setProjectionMatrix(camera.combined);

        renderer.begin();
        renderer.set(ShapeRenderer.ShapeType.Filled);
        long cachedMem = Runtime.getRuntime().totalMemory();
        long freeMem = Runtime.getRuntime().freeMemory();
        long usedMem = cachedMem - freeMem;

        renderer.setColor(Color.GOLDENROD);
        renderer.rect(400, 0, (float) (cachedMem * memDisplayFactor), 20);
        renderer.setColor(Color.valueOf("#007700"));
        renderer.rect(400, 0, (float) (usedMem * memDisplayFactor), 20);
        renderer.setColor(Color.FIREBRICK);
        renderer.rect((float) (400 + maxMemUsage * memDisplayFactor), 0, 3, 20);
        if (usedMem > maxMemUsage) {
            maxMemUsage = usedMem;
        }
        renderer.set(ShapeRenderer.ShapeType.Line);
        renderer.setColor(Color.WHITE);
        renderer.rect(400, 0, 400, 20);
        renderer.end();
        batch.begin();
        font.draw(batch, "[#FFFFFF]Memory usage: " + formatNumber(convertToGigabytes(usedMem)) + "/" + formatNumber(convertToGigabytes(availableMem)) + "Gb, max: " + formatNumber(convertToGigabytes(maxMemUsage)) + "Gb", 400, 16, 400, -1, false);
        batch.end();

        if (currentlySortingFolders) {
            renderer.begin();
            float heightIncrement = 480 / (float) notSortedFilesArray_leftPart.size();
            for (int i = 0; i < notSortedFilesArray_leftPart.size(); i++) {
                if (notSortedFilesArray_leftPart.get(i).equals("DONE")) {
                    renderer.setColor(Color.valueOf("#003300"));
                } else {
                    renderer.setColor(Color.RED);
                }
                renderer.line(230, i * heightIncrement, 255, i * heightIncrement);
            }
            renderer.end();
        }

        batch.begin();

        if (!resultsAvailable) {
            font.draw(batch, "[#00FF00]" + currentFile, 10, 68, 800, -1, false);
            font.draw(batch, "[#00FF00]" + currentHash, 10, 52, 800, -1, false);
            font.draw(batch, "[#00FF00]" + currentErrorMessage, 10, 85, 800, -1, false);
            font.draw(batch, "[#00FF00]" + comparedFiles + " files compared, " + (int) filesPerSec + " Files per sec, " + minutesLeft + " min left", 10, 35, 800, -1, false);
            font.draw(batch, "[#00FF00]" + allFiles + " files scanned(" + normaliseLength(formatNumber(allFiles / (float) allFilesCalculated * 100) + "%", 6) + ") out of " + allFilesCalculated, 10, 16, 800, -1, false);
            font.draw(batch, "[#00FF00]" + duplicatesOriginal + " dupes found", 10, 121, 800, -1, false);
            font.draw(batch, "[#00FF00]" + formatNumber(dupesSize) + "Gb", 10, 102, 800, -1, false);
            font.draw(batch, "[#00FF00]" + getElapsedTime(startTime) + " minutes passed", 10, 141, 800, -1, false);
            font.draw(batch, foldersToSearch_string, 600, 320, 200, 1, true);
            font.draw(batch, extensionsToSearch_string, 400, 370, 200, 1, true);
            font.draw(batch, "[#00FF00]Last commit: " + getElapsedTime(lastCommitTime) + " min ago(" + commitCount + " commits)", 10, 445, 400, -1, false);
            font.draw(batch, "[#FFFF00]Mode: [#55FF55]" + comparisonMode_humanReadable, 10, 400, 400, -1, true);
        }

        batch.end();

        if (!resultsAvailable) {
            stage.draw();
            stage.act();
        } else {
            stage_results.draw();
            stage_results.act();
        }

        if (resultsAvailable) {
            batch.begin();
            font.draw(batch, currentlyViewedPaths, 75, 473, 650, 1, true);
            batch.end();
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        renderer.dispose();
        uiTextures.dispose();
        uiAtlas_buttons.dispose();
        uiAtlas.dispose();
        font.dispose();
        stage.dispose();
    }
}

class CustomTextInputListener implements Input.TextInputListener {
    @Override
    public void input(String text) {
    }

    @Override
    public void canceled() {
    }
}

class FileDialogueAction {
    void performAction(File[] selectedFiles) {

    }
}

class GeneralDialogueAction {
    void performAction(int button) {

    }
}

class CustomIndexComparator implements Comparator<String> {
    @Override
    public int compare(String s, String t1) {
        long actualValueToCompare = Long.parseLong(s.split("\\.")[0]);
        long actualValueToCompare2 = Long.parseLong(t1.split("\\.")[0]);
        if (actualValueToCompare2 - actualValueToCompare == 0) {
            return 0;
        }
        if (actualValueToCompare2 - actualValueToCompare > 0) {
            return 1;
        } else {
            return -1;
        }
    }
}
