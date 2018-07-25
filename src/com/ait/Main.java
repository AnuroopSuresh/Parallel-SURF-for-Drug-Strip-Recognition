package com.ait;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Stream;

import static com.ait.Constants.*;
import static org.opencv.highgui.Highgui.imread;

public class Main {

    public static void main(String[] args) {
        // write your code here

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        //knowledgeBaseCreation();

        simulationDrugStripRecognition();

        /*try(Stream<Path> paths = Files.walk(Paths.get(IMAGES_DIR_FOR_SIMULATION))) {
            paths.forEach(filePath -> {
                //System.out.println("Directory... "+filePath.getFileName());
                if (Files.isRegularFile(filePath)) {
                    //Converting files
                    //System.out.println("Loading... "+filePath.getFileName());

                }else if(Files.isDirectory(filePath)){
                    System.out.println("This is directory... "+filePath.);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }*/


    }

    private static void knowledgeBaseCreation(){
        ExecutorService executor = Executors.newSingleThreadExecutor();

        //for 10 percent of descriptors hence 9 i.e 1/9 = 10
        //DataSetCreator worker1 = new DataSetCreator(SOURCE_FOLDER_OF_IMAGES,"D:\\Java_Projects\\Knowledge Base\\IEEE_Exp",9);

        double i;
        int folderName;
        for(i=0.2;i<=0.2;i=i+0.1){
            //for getting the folder names as 1 2 3 4 .. for 10% 20% 30%
            folderName = (int)(i*10);

            executor.submit(new DataSetCreator(SOURCE_FOLDER_OF_IMAGES, KNOWLEDGE_BASE_DIR, i, folderName));

        }

        System.out.println("ALL DESCRIPTORS AND FILE NAMES LOADED SUCCESSFULLY ");
    }

    private static void simulationDrugStripRecognition(){
        File[] knowledgeBaseDirs = new File(KNOWLEDGE_BASE_DIR).listFiles(File::isDirectory);
        int numberOfSimulationDir = knowledgeBaseDirs.length;
        int i,j;
        ExecutorService executor = Executors.newFixedThreadPool(2);
        //ExecutorService executor = Executors.newSingleThreadExecutor();
        List<Callable<String>> callables = new ArrayList<Callable<String>>();
        String kdAbsolutePath,kdNameorPercentage;

        for(i=0;i<numberOfSimulationDir;i++){
            kdAbsolutePath = knowledgeBaseDirs[i].getAbsolutePath();
            kdNameorPercentage = knowledgeBaseDirs[i].getName();
            System.out.println("i "+kdAbsolutePath);
            System.out.println("i "+kdNameorPercentage);
            callables.add(new SimulationDrugStripWorker(kdAbsolutePath,kdNameorPercentage,kdNameorPercentage));
        }

        List<Future<String>> result = null;

        try {
            result = executor.invokeAll(callables);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Performing some shutdown cleanup...");
                executor.shutdown();
                while (true) {
                    try {
                        System.out.println("Waiting for the service to terminate...");
                        if (executor.awaitTermination(5, TimeUnit.SECONDS)) {
                            break;
                        }
                    } catch (InterruptedException e) {
                    }
                }
                System.out.println("Done cleaning");
            }
        }));

        System.out.println("<-----------FINISHED------------->");
    }
}
