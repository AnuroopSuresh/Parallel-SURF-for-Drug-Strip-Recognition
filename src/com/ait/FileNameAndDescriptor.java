package com.ait;

import org.opencv.core.MatOfKeyPoint;

import java.util.ArrayList;

/**
 * Created by Anuroop on 7/23/2018.
 */
public class FileNameAndDescriptor {

    public final MatOfKeyPoint descriptor;
    public final String fileName;

    public FileNameAndDescriptor(MatOfKeyPoint descriptor, String fileName){
        this.descriptor = descriptor;
        this.fileName = fileName;

    }

    public String getFileName() {
        if(fileName!=null) {
            return fileName;
        }else{
            return "";
        }
    }

    public MatOfKeyPoint getDescriptor() {
        return descriptor;
    }

}
