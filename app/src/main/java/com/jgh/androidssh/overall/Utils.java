package com.jgh.androidssh.overall;

import android.support.annotation.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class Utils {
    private Utils(){

    }

    public static File[] getAllFiles(@NonNull File dir){
        List<File> bucket = new ArrayList<>();
        File[] rootFiles = dir.listFiles();
        for (File f : rootFiles){
            if (f.isDirectory()){
                getAllFiles(f);
            }else {
                bucket.add(f);
            }
        }
        File[] result = new File[bucket.size()];
        return bucket.toArray(result);
    }

    public static HashMap<String,File[]> getFilesFromRoots(@NonNull File[] dirs){
        HashMap<String,File[]> result = new HashMap<>();
        for (File f : dirs){
            if (f.isDirectory()){
                result.put(f.getName(),getAllFiles(f));
            }
        }
        return result;
    }



}
