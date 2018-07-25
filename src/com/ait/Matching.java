package com.ait;

import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorMatcher;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created by Anuroop on 20-Mar-17.
 */
public class Matching implements Callable<String> {
    private ArrayList<MatOfKeyPoint> subList;
    private MatOfKeyPoint imageMatch;
    private ArrayList<String> fileNameSubList;
    private int count=-1;
    private int largestPosition=-1;
    private int largestSize=0;

    BufferedWriter bw;

    public Matching(ArrayList<MatOfKeyPoint> list, ArrayList<String> nameList, MatOfKeyPoint imagedescript){
        subList = list;
        imageMatch = imagedescript;
        fileNameSubList = nameList;
    }

    @Override
    public String call() throws Exception {
        System.out.println("Starting Matching in Thread :"+Thread.currentThread().getName());
        DescriptorMatcher descriptorMatcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED);
        float nndrRatio = 0.7f;

        try{
            bw = new BufferedWriter(new FileWriter("Logs\\ThreadLog.txt",true));
        }catch(IOException e){
            e.printStackTrace();
        }

        for (MatOfKeyPoint desMatch:subList) {

            if (!Thread.interrupted()) {

                //Matching to individual descriptors
                count++;

                //Initialize the variables
                List<MatOfDMatch> matches = new LinkedList<MatOfDMatch>();
                try {
                    descriptorMatcher.knnMatch(imageMatch, desMatch, matches, 2);
                }catch (Exception e)
                {
                    e.printStackTrace();
                }
                LinkedList<DMatch> goodMatchesList = new LinkedList<DMatch>();


                //Performing actual match
                for (int i = 0; i < matches.size(); i++) {
                    try {
                        MatOfDMatch matofDMatch = matches.get(i);
                        DMatch[] dmatcharray = matofDMatch.toArray();
                        DMatch m1 = dmatcharray[0];
                        DMatch m2 = dmatcharray[1];

                        if (m1.distance <= m2.distance * nndrRatio) {
                            goodMatchesList.addLast(m1);

                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }

                System.out.println("Good Match List Size = " + goodMatchesList.size());

                if (goodMatchesList.size() >= 15) {
                    System.out.println("Object Found!!! = = " + fileNameSubList.get(count) + " Thread Name = " + Thread.currentThread().getName());
                    if(largestSize<goodMatchesList.size()){
                        largestPosition=count;
                        largestSize=goodMatchesList.size();
                        bw.write("Matching "+fileNameSubList.get(count)+" Size = "+goodMatchesList.size());
                        bw.newLine();
                        bw.flush();
                    }


                } else {
                    System.out.println("Object not found..");
                }

            }else{
                return null;
            }
        }
        bw.write("<----End Thread "+Thread.currentThread().getName()+"------>");
        bw.newLine();
        bw.flush();
        bw.close();

        if(largestPosition==-1){
            return "null,0";
        }else {
            return fileNameSubList.get(largestPosition)+","+largestSize;
        }
    }
}
