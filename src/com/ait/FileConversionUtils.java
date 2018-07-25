package com.ait;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.FeatureDetector;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Created by Anuroop on 07-Mar-17.
 */
public class FileConversionUtils {

    //public static final String sourceFolderPath="D:\\Cropped project pics\\";
    //public static final String destFolderPath = "D:\\Java_Projects\\Knowledge Base\\Sorted Descriptors\\";
    //public final String sourceFolderPath;
    //public final String destFolderPath;

   /* public FileConversionUtils(String allImageFolderPathSrc,String storagePath){
        this.sourceFolderPath = allImageFolderPathSrc;
        this.destFolderPath=storagePath;
    }*/


    public void knowledgeBaseCreation(String sourceFolderPath,String destFolderPath,double percentageOfDiscriptors){

        //read from folder all files

        try(Stream<Path> paths = Files.walk(Paths.get(sourceFolderPath))) {
            paths.forEach(filePath -> {
                if (Files.isRegularFile(filePath)) {
                    //Converting files
                    System.out.println("KD Conversion "+filePath.getFileName());
                    System.out.println("File name = "+destFolderPath+filePath.getFileName().toString());

                    if(filePath.getFileName().toString().contains("png")) {
                        try {
                            //read from the path
                            Mat image = Highgui.imread(filePath.toString());
                            //debug
                            //Convert to grey scale image
                            Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2GRAY);

                            //Extract features and assign them to temp
                            MatOfKeyPoint temp = imageDescriptions(image);

                            //write the mat of key points to file
                            String actualPath = destFolderPath + filePath.getFileName().toString().replace(".png", ".dat");
                            System.out.println("File Path = " + actualPath);

                            //For Sorted Descriptors
                            MatOfKeyPoint sortedTemp = sortingMat(temp,percentageOfDiscriptors);
                            saveMat(actualPath, sortedTemp);

                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<FileNameAndDescriptor> loadKnowledgeBaseFromFolder(String folderPath){
        ArrayList<FileNameAndDescriptor> fileNameAndDescriptorArrayList = new ArrayList<>();
        ArrayList<MatOfKeyPoint> descriptorsList = new ArrayList<MatOfKeyPoint>();
        ArrayList<String> fileNameList = new ArrayList<String>();

        try(Stream<Path> paths = Files.walk(Paths.get(folderPath))) {
            paths.forEach(filePath -> {
                if (Files.isRegularFile(filePath)) {
                    //Converting files
                    System.out.println("Loading.... "+filePath.getFileName());
                    System.out.println("File name = "+folderPath+filePath.getFileName().toString());

                    try{
                        Mat tempMat = loadMat(filePath.toString());
                        MatOfKeyPoint tempMatofKey = new MatOfKeyPoint();

                        tempMatofKey.create(tempMat.rows(),tempMat.cols(),tempMat.type());
                        for(int i=0;i<tempMat.rows();i++){
                            for(int j=0;j<tempMat.cols();j++){
                                double[] data = tempMat.get(i,j);
                                tempMatofKey.put(i,j,data);
                            }
                        }
                        fileNameAndDescriptorArrayList.add(new FileNameAndDescriptor(tempMatofKey,filePath.getFileName().toString()));


                    }catch(Exception ex){
                        ex.printStackTrace();
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fileNameAndDescriptorArrayList;
    }


    public MatOfKeyPoint imageDescriptions(Mat image){
        //Image detector
        MatOfKeyPoint imageKeyPoints = new MatOfKeyPoint();
        FeatureDetector detector = FeatureDetector.create(FeatureDetector.SURF);
        detector.detect(image,imageKeyPoints);
        //Image Description
        MatOfKeyPoint imageDescription = new MatOfKeyPoint();
        DescriptorExtractor extractor = DescriptorExtractor.create(DescriptorExtractor.SURF);
        extractor.compute(image,imageKeyPoints,imageDescription);
        return imageDescription;
    }

    public final void saveMat(String path, Mat mat) {
        File file = new File(path).getAbsoluteFile();
        file.getParentFile().mkdirs();
        try {
            int cols = mat.cols();
            float[] data = new float[(int) mat.total() * mat.channels()];
            mat.get(0, 0, data);
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path))) {
                oos.writeObject(cols);
                oos.writeObject(data);
                oos.close();
            }
        } catch (IOException | ClassCastException ex) {
            System.err.println("ERROR: Could not save mat to file: " + path);
            //Logger.getLogger(this.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public final Mat loadMat(String path) {
        try {
            int cols;
            float[] data;
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path))) {
                cols = (int) ois.readObject();
                data = (float[]) ois.readObject();
            }
            Mat mat = new Mat(data.length / cols, cols, CvType.CV_32F);
            mat.put(0, 0, data);
            return mat;
        } catch (IOException | ClassNotFoundException | ClassCastException ex) {
            System.err.println("ERROR: Could not load mat from file: " + path);
            //Logger.getLogger(this.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public ArrayList<ArrayList<MatOfKeyPoint>> chop(ArrayList<MatOfKeyPoint> list,int l)
    {
        ArrayList<ArrayList<MatOfKeyPoint>> parts = new ArrayList<>();
        int n = list.size();
        for(int i=0;i<n;i+=l){
            parts.add(new ArrayList<MatOfKeyPoint>(list.subList(i,Math.min(n,i+l))));
        }
        return parts;
    }

    public ArrayList<String> readNamesFile(String folderPath) {
        ArrayList<String> fileNameList = new ArrayList<String>();

        try(Stream<Path> paths = Files.walk(Paths.get(folderPath))) {
            paths.forEach(filePath -> {
                if (Files.isRegularFile(filePath)) {
                    //Converting files
                    System.out.println("Loading... "+filePath.getFileName());
                    System.out.println("File name = "+folderPath+filePath.getFileName().toString());

                    String fileName = null;

                    try{
                        fileName = filePath.getFileName().toString();
                        fileNameList.add(fileName);

                    }catch(Exception ex){
                        ex.printStackTrace();
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fileNameList;

    }

    public ArrayList<ArrayList<String>> chopFileNameList(ArrayList<String> list,int l){
        ArrayList<ArrayList<String>> parts = new ArrayList<ArrayList<String>>();
        int n = list.size();
        for(int i=0;i<n;i+=l){
            parts.add(new ArrayList<String>(list.subList(i,Math.min(n,i+l))));
        }
        return parts;
    }

    public MatOfKeyPoint sortingMat(MatOfKeyPoint mat,double percentageOfDesc){
        MatOfKeyPoint temp = mat;
        double[] sum = new double[mat.rows()];

        for(int i=0;i<temp.rows();i++){
            for(int j=0;j<temp.cols();j++){
                double[] data = temp.get(i,j);
                for(int k=0;k<data.length;k++){
                    sum[i]+=data[k];
                }
            }
        }

        ArrayIndexComparator comparator = new ArrayIndexComparator(sum);
        Integer[] indexes = comparator.createIndexArray();
        Arrays.sort(indexes, comparator);

        MatOfKeyPoint sortedMatofKey = new MatOfKeyPoint();
        int sortedRows = (int)(temp.rows()*percentageOfDesc);
        int sortedCols = temp.cols();

        //TO put the sorted matrix in the proper place
        int rowCount=0;

        sortedMatofKey.create(sortedRows,sortedCols,temp.type());
        for(int i=temp.rows()-1;i>sortedRows;i--){

            //Debug Statments
            for(int j=0;j<sortedCols;j++){

                double[] data = temp.get(indexes[i],j);
                sortedMatofKey.put(rowCount,j,data);
            }
            rowCount++;
        }
        System.out.println("AFTER SORTING ----> Rows = "+sortedMatofKey.rows()+" Cols = "+sortedMatofKey.cols()+" Percentage: "+percentageOfDesc);
        return sortedMatofKey;
    }
}
