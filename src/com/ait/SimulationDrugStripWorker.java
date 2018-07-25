package com.ait;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.imgproc.Imgproc;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.ait.Constants.*;
import static org.opencv.highgui.Highgui.imread;

/**
 * Created by Anuroop on 7/24/2018.
 */
public class SimulationDrugStripWorker implements Callable<String> {

    private final String absoluteKnowledgeBasePath;
    private final String threadName;
    private final double percentage;

    public SimulationDrugStripWorker(String absoluteKnowledgeBasePath, String threadName, String percentage) {
        this.absoluteKnowledgeBasePath = absoluteKnowledgeBasePath;
        this.threadName = threadName;
        this.percentage = (Double.parseDouble(percentage)) / 10;
    }

    int successfulMatches, rowCounter = 2;

    @Override
    public String call() throws Exception {

        System.out.println("THREAD STARTED : " + threadName);

        DescriptorMatcher descriptorMatcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED);
        float nndrRatio = 0.7f;
        final FileNameAndDescriptor result;

        FileConversionUtils utils = new FileConversionUtils();
        int i, j;
        //load the files to main memory
        ArrayList<FileNameAndDescriptor> fileNameAndDescriptorArrayList = utils.loadKnowledgeBaseFromFolder(absoluteKnowledgeBasePath);
        System.out.println("Thread Name: " + threadName + ": FileNameAndDesListSize = " + fileNameAndDescriptorArrayList.size());
        try {
            if (fileNameAndDescriptorArrayList.size() > 0) {

                //get the simulation sample image directory path
                File[] directories = new File(IMAGES_DIR_FOR_SIMULATION).listFiles(File::isDirectory);
                int numberOfSamples = directories.length;

                File logDirStructure = new File(LOGS_DIR + threadName + File.separator);
                String absoluteLogDirStructure = LOGS_DIR;
                String logFile;
                if (logDirStructure.mkdirs()) {
                    absoluteLogDirStructure = logDirStructure.getAbsolutePath();
                }
                final HSSFWorkbook wb = new HSSFWorkbook();

                //Loop through all the directories like angles, normal etc
                for (j = 0; j < numberOfSamples; j++) {
                    rowCounter = 2;
                    File directory = directories[j];
                    String dirSampleName = directory.getName();
                    System.out.println("Main: simulationDrugStripRecognition: Directory Name: " + dirSampleName);
                    logFile = absoluteLogDirStructure + File.separator + "ResultsLog_" + dirSampleName + ".txt";
                    try {

                        final BufferedWriter logBW = new BufferedWriter(new FileWriter(logFile));
                        final BufferedWriter threadLogBW = new BufferedWriter(new FileWriter(LOGS_DIR + threadName + File.separator + "ThreadLog.txt"));
                        final Sheet sheet = wb.createSheet(dirSampleName);
                        successfulMatches = 0;
                        //sheet title
                        Row titleRow = sheet.createRow(0);
                        titleRow.createCell(0).setCellValue(dirSampleName);
                        //header row
                        Row headerRow = sheet.createRow(1);
                        headerRow.createCell(0).setCellValue("INPUT SAMPLE NAME");
                        headerRow.createCell(1).setCellValue("MATCHED RESULT NAME");
                        headerRow.createCell(2).setCellValue("MATCHED?");
                        headerRow.createCell(3).setCellValue("TIME TAKEN");


                        try (Stream<Path> paths = Files.walk(directory.toPath())) {
                            paths.forEach(filePath -> {
                                String fileNameString, filePathString;
                                if (Files.isRegularFile(filePath)) {
                                    //reading each file in the samples directory
                                    Row detailsRow = sheet.createRow(rowCounter++);
                                    int colCounter = 0;
                                    filePathString = filePath.toString();
                                    fileNameString = filePath.getFileName().toString();
                                    if (filePathString.contains(".jp") || filePathString.contains(".pn")) {
                                        //Converting files
                                        System.out.println("File name = " + filePathString);
                                        Mat image = imread(filePathString);
                                        Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2GRAY);

                                        try {
                                            //log and result
                                            logBW.append(fileNameString + ",");

                                            MatOfKeyPoint imageDescriptor = utils.imageDescriptions(image);
                                            //MatOfKeyPoint imageDescriptor = utils.sortingMat(tempDes, percentage);

                                            long startTime = System.nanoTime();
                                            MatOfKeyPoint desMatch;
                                            String descriptorFileName;

                                            int largestGoodSize = 0;
                                            FileNameAndDescriptor mostGoodMatchDescriptor = new FileNameAndDescriptor(null, "");

                                            for (FileNameAndDescriptor fileNameAndDescriptor : fileNameAndDescriptorArrayList) {
                                                desMatch = fileNameAndDescriptor.getDescriptor();
                                                descriptorFileName = fileNameAndDescriptor.getFileName();

                                                if (!Thread.interrupted()) {
                                                    //Initialize the variables
                                                    List<MatOfDMatch> matches = new LinkedList<MatOfDMatch>();
                                                    try {
                                                        descriptorMatcher.knnMatch(imageDescriptor, desMatch, matches, 2);
                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                    }
                                                    LinkedList<DMatch> goodMatchesList = new LinkedList<DMatch>();

                                                    //Performing actual match
                                                    for (int k = 0; k < matches.size(); k++) {
                                                        try {
                                                            MatOfDMatch matofDMatch = matches.get(k);
                                                            DMatch[] dmatcharray = matofDMatch.toArray();
                                                            DMatch m1 = dmatcharray[0];
                                                            DMatch m2 = dmatcharray[1];

                                                            if (m1.distance <= m2.distance * nndrRatio) {
                                                                goodMatchesList.addLast(m1);

                                                            }
                                                        } catch (Exception e) {
                                                            e.printStackTrace();
                                                        }
                                                    }
                                                    int goodMatchListSize = goodMatchesList.size();
                                                    if (goodMatchListSize >= 15) {
                                                        if (largestGoodSize < goodMatchListSize) {
                                                            System.out.println("Object Found!!! = = " + descriptorFileName + ": Size:" + goodMatchListSize + " Thread Name = " + threadName);
                                                            mostGoodMatchDescriptor = fileNameAndDescriptor;
                                                            largestGoodSize = goodMatchesList.size();
                                                            threadLogBW.write("Matching " + descriptorFileName + " Size = " + goodMatchListSize);
                                                            threadLogBW.newLine();
                                                            threadLogBW.flush();
                                                        }
                                                    }
                                                }
                                            }

                                            long endTIme = System.nanoTime();
                                            long timeTaken = (endTIme - startTime) / 1000000;
                                            System.out.println("Time Taken = " + timeTaken);
                                            logBW.write("Result =," + mostGoodMatchDescriptor.getFileName() + ",Time Taken =," + timeTaken);
                                            logBW.flush();
                                            logBW.newLine();

                                            String descriptOnlyName="";
                                            if(mostGoodMatchDescriptor.getFileName()!=null) {
                                                descriptOnlyName = mostGoodMatchDescriptor.getFileName().split("\\.",2)[0];
                                            }
                                            String fileNameOnly = fileNameString.split("\\.",2)[0];

                                            detailsRow.createCell(colCounter++).setCellValue(fileNameOnly);
                                            detailsRow.createCell(colCounter++).setCellValue(descriptOnlyName);

                                            if (Pattern.compile(Pattern.quote(descriptOnlyName), Pattern.CASE_INSENSITIVE).matcher(fileNameOnly).find()) {
                                                detailsRow.createCell(colCounter++).setCellValue("True");
                                                successfulMatches++;
                                            } else {
                                                detailsRow.createCell(colCounter++).setCellValue("False");
                                            }
                                            detailsRow.createCell(colCounter++).setCellValue(timeTaken);

                                            //free up for gc
                                            image = null;
                                            imageDescriptor = null;
                                            fileNameString=null;
                                            filePathString=null;
                                            fileNameOnly=null;
                                            descriptOnlyName=null;

                                            //tempDes = null;

                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }

                                }
                                System.out.println("<--------END---------->");
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        //footer row
                        Row footerRow = sheet.createRow(rowCounter);
                        footerRow.createCell(0).setCellValue("Total Matches");
                        footerRow.createCell(1).setCellValue(successfulMatches);

                    } catch (Exception e) {
                        System.err.println("SimulationDrugStripWorker: Call(): Exception : " + e.getMessage());
                        e.printStackTrace();
                    }

                    //output the workbook
                    FileOutputStream out = new FileOutputStream(new File(LOGS_DIR + threadName + File.separator + "Results.xls"));
                    wb.write(out);
                    out.close();
                }

            } else {
                System.err.println("SimulationDrugStripWorker: Worker Name-" + threadName + ": Unable to load fileNameAndDescriptorArrayList");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.gc();
        }

        System.out.println("THREAD ENDED: " + threadName);

        return null;
    }
}
