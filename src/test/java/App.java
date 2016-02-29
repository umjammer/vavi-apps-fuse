/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.google.common.collect.Maps;

import asg.cliche.Command;
import asg.cliche.Param;
import asg.cliche.ShellFactory;
import de.tuberlin.onedrivesdk.OneDriveException;
import de.tuberlin.onedrivesdk.OneDriveFactory;
import de.tuberlin.onedrivesdk.OneDriveSDK;
import de.tuberlin.onedrivesdk.common.OneDriveScope;
import de.tuberlin.onedrivesdk.common.OneItem;
import de.tuberlin.onedrivesdk.file.OneFile;
import de.tuberlin.onedrivesdk.folder.OneFolder;
import de.tuberlin.onedrivesdk.networking.OneDriveAuthenticationException;
import de.tuberlin.onedrivesdk.uploadFile.OneUploadFile;


/**
 * Example Client for testing the OneDrive SDK
 */
public class App {
    private static OneFolder currentFolder;

    ExecutorService executor = Executors.newFixedThreadPool(5);
    private OneDriveSDK api;
    private Map<String, OneFile> currentFolderFiles = Maps.newHashMap();
    private Map<String, OneFolder> currentFolderFolders = Maps.newHashMap();
    private Map<String, OneItem> currentFolderItems = Maps.newHashMap();

    static final String clientId = "CLIENT_ID";
    static final String clientSecret = "CLIENT_SECRET";

//    File file = new File(System.getProperty("user.home"), ".vavifuse");
//    Properties props = new Properties();

    public App() throws IOException, InterruptedException, OneDriveException {

        String email = "onedrive@id";
        String password = "password";

        //
//        if (!file.exists()) {
//            props.store(new FileOutputStream(file), new Date().toString());
//        } else {
//            props.load(new FileInputStream(file));
//        }
        
        //
        String redirectUrl = "https://vast-plateau-97564.herokuapp.com/onedrive_set";
        api = OneDriveFactory.createOneDriveSDK(clientId,
                                                clientSecret,
                                                redirectUrl,
                                                OneDriveScope.OFFLINE_ACCESS);

        String url = api.getAuthenticationURL();

        //
        String
//        code = props.getProperty("onedrive." + email + ".code");

//        if (code == null) {
            code = getCode(url, email, password, redirectUrl);
//        } else {
//            System.err.println("reuse code: " + code);
//        }
        
//        try {
            System.err.println("authenticate 1: " + code);
            api.authenticate(code);
//        } catch (OneDriveException e) {
//            System.err.println("may be timeout, relogin...");
//            code = getCode(url, email, password, redirectUrl);
//            try {
//                System.err.println("authenticate 2: " + code);
//                api.authenticate(code);
//            } catch (OneDriveException f) {
//                throw new IllegalStateException("could not login");
//            }
//        }

        currentFolder = api.getRootFolder();

        api.startSessionAutoRefresh();
    }

    String getCode(String url, String email, String password, String redirectUrl) throws IOException {
        Getter getter = new FxGetter(email, password, redirectUrl);
        String code = getter.get(url);
        
//        props.setProperty("onedrive." + email + ".code", code);
//        props.store(new FileOutputStream(file), new Date().toString());
        
        return code;
    }
    
    public static void main(String[] args) throws IOException, InterruptedException, OneDriveException {
        ShellFactory.createConsoleShell("OneDrive",
                                        "To list all available commands enter ?list or ?list-all, the latter will also show you system commands.\nTo get detailed info on a command enter ?help command-name",
                                        new App())
                .commandLoop();
    }

    private static String printCurrentFolder() {
        if (currentFolder != null) {
            return currentFolder.toString();
        } else {
            return "/";
        }
    }

    @Command(description = "Change the current directory")
    public void changeDirectory(@Param(name = "index",
                                       description = "Index of folder you want to switch to, OR '..' to go back") String index) throws IOException,
                                                                                                                               OneDriveException {

        OneFolder newCurrentFolder;
        if (index.equals("..")) {
            newCurrentFolder = currentFolder.getParentFolder();
        } else {
            newCurrentFolder = currentFolderFolders.get(index);
        }

        if (newCurrentFolder != null) {
            currentFolder = newCurrentFolder;
        }

        System.out.println("Changing folder to: " + currentFolder.getName());
    }

    @Command(name = "list children", abbrev = "ls")
    public void listSubItems() throws IOException, OneDriveException {
        System.out.println("Listing children");

        this.currentFolderFiles = new HashMap<String, OneFile>();
        this.currentFolderFolders = new HashMap<String, OneFolder>();
        this.currentFolderItems = convertToMap(currentFolder.getChildren(), OneFile.class);

        for (String s : this.currentFolderItems.keySet()) {
            OneItem item = this.currentFolderItems.get(s);
            if (item.isFile())
                this.currentFolderFiles.put(s, (OneFile) item);
            if (item.isFolder())
                this.currentFolderFolders.put(s, (OneFolder) item);
        }

        printItemList(currentFolderItems);
    }

    @Command(name = "list Directories", abbrev = "ls-d")
    public void listSubFolders() throws IOException, OneDriveException {
        System.out.println("Listing sub Folders");
        this.currentFolderFolders = convertToMap(currentFolder.getChildFolder(), OneFolder.class);
        printItemList(currentFolderFolders);
    }

    @Command(name = "list Files", abbrev = "ls-f")
    public void listSubFiles() throws IOException, OneDriveException {
        System.out.println("Listing sub files");
        this.currentFolderFiles = convertToMap(currentFolder.getChildFiles(), OneFile.class);
        printItemList(currentFolderFiles);
    }

    @Command
    public void uploadFile(@Param(name = "path", description = "Path of the File you want to upload to the current Folder") String path) throws IOException,
                                                                                                                                        InterruptedException,
                                                                                                                                        ExecutionException,
                                                                                                                                        OneDriveException {
        File file = new File(path);
        OneUploadFile upload = currentFolder.uploadFile(file);
        Future<OneFile> futureUpload = executor.submit(upload);
        System.out.println(futureUpload.get().getCreatedDateTime());
    }

    @Command(name = "remove", abbrev = "rm", description = "Deletes a file")
    public void deleteItem(@Param(name = "index", description = "Index of file you want to delete") String index) throws IOException,
                                                                                                                 OneDriveException {
        OneItem item = null;

        if (this.currentFolderFiles.containsKey(index))
            item = (OneItem) this.currentFolderFiles.get(index);

        if (this.currentFolderFolders.containsKey(index))
            item = (OneItem) this.currentFolderFolders.get(index);

        if (item != null) {
            System.out.println(String.format("Deleting %s", item.getName()));
            item.delete();
        } else {
            System.out.println("Can not find item with index '" + index + "'");
        }
    }

    @Command(description = "Creates a subfolder in the currentFolder")
    public void createFolder(@Param(name = "folderName", description = "The name of the new Folder that should be created") String folderName) throws IOException,
                                                                                                                                              OneDriveException {
        System.out.println(String.format("Creating %s in %s", folderName, currentFolder.getName()));
        currentFolder.createFolder(folderName);
    }

    @Command
    public void downloadItem(@Param(name = "index", description = "Index of the file you want to download") String index,
                             @Param(name = "targetFileName", description = "path where to download to") String pathToDownload) throws IOException {
        System.out.println(String.format("Downloading %s to %s", currentFolderFiles.get(index).getName(), pathToDownload));
        OneFile tmpFile = currentFolderFiles.get(index);
        try {
            tmpFile.download(new File(pathToDownload)).startDownload();
        } catch (OneDriveAuthenticationException e) {
            e.printStackTrace();
        }
    }

    @Command
    public void exit() throws IOException {
        api.disconnect();
        System.exit(0);
    }

    private <T> Map<String, T> convertToMap(List<T> listToConvert, Type T) {
        Map<String, T> tmpMap = Maps.newHashMapWithExpectedSize(listToConvert.size());
        for (int i = 0; i < listToConvert.size(); i++) {
            tmpMap.put(i + "", listToConvert.get(i));
        }
        return tmpMap;
    }

    private void printItemList(Map<String, ?> map) {
        List<String> itemKeys = new ArrayList<String>(map.keySet());
        Collections.sort(itemKeys);
        for (String key : itemKeys) {
            System.out.println(String.format("Item %s = %s", key, map.get(key)));
        }
    }
}
