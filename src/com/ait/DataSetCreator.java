package com.ait;

import java.io.File;

/**
 * Created by Anuroop on 7/22/2018.
 */
public class DataSetCreator implements Runnable {

    private final String sourceFolderPath;
    private final String destFolderPath;
    private final double descriptionPercentage;

    public DataSetCreator(String absoluteSourceImagesFolderPath,String absoluteDestFolderPath,double descriptorPercentageLength,int folderName){
        this.sourceFolderPath = absoluteSourceImagesFolderPath;
        this.descriptionPercentage = descriptorPercentageLength;
        this.destFolderPath = absoluteDestFolderPath+folderName+File.separator;
        File directoryStructure = new File(destFolderPath);
        if(directoryStructure.mkdirs()){
            System.out.println("DataSetCreatorThread: Constructor: Directory Structure Created: "+destFolderPath);
        }else{
            System.out.println("DataSetCreatorThread: Constructor: Directory Structure Already exists ");
        }
    }

    @Override
    public void run() {
        FileConversionUtils descriptorUtils = new FileConversionUtils();
        descriptorUtils.knowledgeBaseCreation(sourceFolderPath,destFolderPath,descriptionPercentage);

        System.out.println("<-----COMPLETED: Thread Name: "+ descriptionPercentage +"------>");
    }
}
