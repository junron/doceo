package com.example.attendance.util.android.ocr;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.example.attendance.MainActivity;

import org.opencv.android.Utils;
import org.opencv.aruco.Aruco;
import org.opencv.aruco.Dictionary;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyImageProcessing {
    final static int SIZE_X = 2000;
    final static int UNIT = SIZE_X/17;
    final static Dictionary dict = Aruco.getPredefinedDictionary(Aruco.DICT_4X4_50);
    final static Map<Integer, Point> markerPositions = new HashMap<>();

    static {
        markerPositions.put(0, new Point(UNIT, UNIT));
        markerPositions.put(1, new Point(UNIT*21, UNIT));
        markerPositions.put(2, new Point(UNIT*21, UNIT*14));
        markerPositions.put(3, new Point(UNIT, UNIT*14));
        markerPositions.put(4, new Point(UNIT, UNIT*15/2d));
        markerPositions.put(5, new Point(UNIT*11, UNIT*15/2d));
        markerPositions.put(6, new Point(UNIT*21, UNIT*15/2d));
    }

    public static String[] readOASPage(Mat image) throws NoOASFoundException {
        Mat OAS = warpToOASPage(image);
        ArrayList<Integer> bubbleX = new ArrayList<>();
        ArrayList<Integer> bubbleY= new ArrayList<>();
        for (int i = 0; i < 4; i++) for (int j = 0; j < 2; j++) {
            bubbleX.add(UNIT*(3+i) + j*UNIT*19/2);
        }
        Collections.sort(bubbleX);
        for (int i = 0; i<10; i++) {
            bubbleY.add(UNIT*(27+10*i)/6);
        }
        Collections.sort(bubbleY);

        Mat kernel = new Mat(new Size(5,5), CvType.CV_8UC(1), new Scalar(255));
        Imgproc.erode(OAS, OAS, kernel, new Point(-1, -1), 3);

        String[] ret = new String[21];
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.activity.getBaseContext());

        int points = 0;
        int total = 0;
        for (int i = 0; i < bubbleY.size(); i++) {
            String line = "";
            for (int j = 0; j < bubbleX.size(); j++) {
                int x = bubbleX.get(j);
                int y = bubbleY.get(i);
                Rect roi = new Rect(x-UNIT/4, y-UNIT/4, UNIT/2, UNIT/2);
                Mat crop = new Mat(OAS, roi);
                Scalar avg = Core.mean(crop);
                Log.d("OAS", ""+i+" "+j+" "+(i+(j>3?10:0)+1)+(avg.val[0] < 120?" X":"  "));
                if (avg.val[0] < 120) {
                    switch(j) {
                        case 0: case 4: line += "A"; break;
                        case 1: case 5: line += "B"; break;
                        case 2: case 6: line += "C"; break;
                        case 3: case 7: line += "D"; break;
                    }
                }
                if (j%4 == 3) {
                    int qnum = i+(j>3?10:0) + 1;
                    String ans = preferences.getString("q"+qnum, "Blank");
                    if (ans.equals("Blank")) {
                        ret[qnum-1] = line;
                        line = "";
                        continue;
                    }
                    total++;
                    if (ans.equals(line)) {
                        line = "✔" + line;
                        points++;
                    } else {
                        if (line.isEmpty()) line = "";
                        line = "✖" + line;
                    }
                    ret[qnum-1] = line;
                    line = "";
                }
            }
        }
        ret[20] = ""+points+"/"+total;
        return ret;
    }

    public static Mat warpToOASPage(Mat image) throws NoOASFoundException {

        Mat proc = new Mat();
        Imgproc.cvtColor(image, proc, Imgproc.COLOR_BGR2GRAY);
        Imgproc.threshold(proc, proc, 127, 255, Imgproc.THRESH_OTSU| Imgproc.THRESH_BINARY);

        List<Mat> corners = new ArrayList<>(); Mat ids = new Mat();
        Aruco.detectMarkers(proc, dict, corners, ids);
        ArrayList<Point> srcPoints = new ArrayList<>();
        ArrayList<Point> desPoints = new ArrayList<>();

        if (corners.size() == 0) throw new NoOASFoundException("No OAS found");
        for (int pos = 0; pos < ids.size().height; pos++) {
            for (int i = 0; i < 4; i++) {
                int offsetX = 2 * UNIT * (i==1||i==2?1:0); int offsetY = 2 * UNIT * (i==2||i==3?1:0);
                Point dest = markerPositions.get((int) ids.get(pos, 0)[0]);
                if (dest == null) {
                    Log.d("OAS", ""+pos);
                    continue;
                }
                dest = new Point(dest.y+offsetX, dest.x+offsetY);
                desPoints.add(dest);
                srcPoints.add(new Point(corners.get(pos).get(0, i)));
            }
        }

        if (srcPoints.size() < 4) throw new NoOASFoundException("No OAS found");

        Mat M = Calib3d.findHomography(new MatOfPoint2f(srcPoints.toArray(new Point[0])), new MatOfPoint2f(desPoints.toArray(new Point[0])));
        Mat warped = new Mat();
        Imgproc.warpPerspective(image, warped, M, new Size(SIZE_X, SIZE_X*Math.sqrt(2)), Imgproc.INTER_LINEAR, Core.BORDER_CONSTANT, new Scalar(255, 255, 255));
        Imgproc.cvtColor(warped, warped, Imgproc.COLOR_BGR2GRAY);
        Imgproc.threshold(warped, warped, 127, 255, Imgproc.THRESH_OTSU| Imgproc.THRESH_BINARY);

        return warped;
    }

    public static Mat warpImageToPage(Mat image) {
        return warpImageToPage(image, 4000, Math.sqrt(2), true);
    }

    public static Mat warpImageToPageFromOtherImage(Mat imageSource, Mat imageApplyTo, int shortSidePixels, double whRatio, boolean forceUpright) {
        Point[] pageCorners;
        double scaleFactor = imageApplyTo.size().height /imageSource.size().height;
        try {
            pageCorners = detectPageCornersFromRaw(imageSource).toArray();
        } catch (NoPageFoundException e) {
            try {
                pageCorners = detectPageCornersFromRaw(imageApplyTo).toArray();
                scaleFactor = 1;
            } catch (NoPageFoundException e2) {
                return imageApplyTo;
            }
        }

        pageCorners = multMat(pageCorners, scaleFactor);

        double d1 = pointDistance(pageCorners[0], pageCorners[1]) + pointDistance(pageCorners[2], pageCorners[3]);
        double d2 = pointDistance(pageCorners[0], pageCorners[3]) + pointDistance(pageCorners[1], pageCorners[2]);

        int imageWidth, imageHeight;
        boolean isUpright = d1 > d2;
        if (isUpright) {
            imageWidth = shortSidePixels;
            imageHeight = (int) (shortSidePixels * whRatio);
        } else {
            imageWidth = (int) (shortSidePixels * whRatio);
            imageHeight = shortSidePixels;
        }

        MatOfPoint2f destinationCoordinates = new MatOfPoint2f(new Point(0, 0), new Point(0, imageHeight), new Point(imageWidth, imageHeight), new Point(imageWidth, 0));
        MatOfPoint2f sourceCoordinates = new MatOfPoint2f(pageCorners);
        Mat perspectiveMatrix = Imgproc.getPerspectiveTransform(sourceCoordinates, destinationCoordinates);
        Mat warped = new Mat();
        Imgproc.warpPerspective(imageApplyTo, warped, perspectiveMatrix, new Size(imageWidth, imageHeight));

        if (forceUpright && !isUpright) {
            int rotateCode = 0;
            if (pageCorners[1].y > pageCorners[2].y) rotateCode = -1;
            Core.rotate(warped, warped, rotateCode);
        }
        return warped;
    }

    public static Mat warpImageToPage(Mat image, int shortSidePixels, double whRatio, boolean forceUpright) {
        return warpImageToPageFromOtherImage(image, image, shortSidePixels, whRatio, forceUpright);
    }

    public static Mat drawPagePolygonTransparent(Mat image) {
        Mat transparent = new Mat(image.size(), CvType.CV_8UC4, new Scalar(0, 0, 0, 0));

        Point[] pageCorners;
        try {
            pageCorners = detectPageCornersFromRaw(image).toArray();
        } catch (NoPageFoundException e) {
            return transparent;
        }
        for (int i = 0; i < 4; i++)
            Imgproc.line(transparent, pageCorners[i], pageCorners[(i+1)%4], new Scalar(0x0b, 0x69, 0x39, 200), 4);
        for (int i = 0; i < 2; i++)
            Imgproc.line(transparent, pageCorners[i], pageCorners[i+2], new Scalar(0x0b, 0x69, 0x39, 200), 4);
        return transparent;
    }

    public static MatOfPoint2f detectPageCornersFromRaw(Mat image) throws NoPageFoundException {
        Mat im = preprocessImage(image);
        return detectPagePolygon(im);
    }

    public static Mat preprocessImage(Mat image) {
        Mat im = new Mat();
        Mat canny = new Mat();
        Mat dilate = new Mat();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.activity.getBaseContext());
        int dilationSize = preferences.getInt("dilationKernelSize", 2);
        int blurSize = preferences.getInt("blurKernelSize", 4);
        int cannyParam1 = preferences.getInt("cannyParam1", 50);
        int cannyParam2 = preferences.getInt("cannyParam2", 50);
        int dilationIterations = preferences.getInt("dilationIterations", 3);

        Mat dilationKernel = new Mat(new Size(dilationSize, dilationSize), CvType.CV_8UC(1), new Scalar(255));
        Imgproc.blur(image, im, new Size(blurSize, blurSize));
        Imgproc.Canny(im, canny, cannyParam1, cannyParam2);
        Imgproc.dilate(canny, dilate, dilationKernel, new Point(-1, -1), dilationIterations);
        return dilate;
    }

    public static MatOfPoint2f detectPagePolygon(Mat image) throws NoPageFoundException {
        List<MatOfPoint> contours = new ArrayList<>();
        Mat im = new Mat();
        image.copyTo(im);
        double bestArea = -1;
        MatOfPoint2f bestPagePolygon = null;
        Imgproc.findContours(im, contours, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        for (MatOfPoint contour : contours) {
            double area = Imgproc.contourArea(contour);
            if (area < 10000) continue;
            if (area < bestArea) continue;
            MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
            double contourPerimeter = Imgproc.arcLength(contour2f, true);
            MatOfPoint2f approximatePolygon = new MatOfPoint2f();
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.activity.getBaseContext());
            double tolerance =  preferences.getInt("tolerance", 5)/100d;
            Imgproc.approxPolyDP(contour2f, approximatePolygon, contourPerimeter * tolerance, true);
            Imgproc.approxPolyDP(approximatePolygon, approximatePolygon, 0, true);
            if (!Imgproc.isContourConvex(new MatOfPoint(approximatePolygon.toArray()))) continue;

            Point[] pointsArr = approximatePolygon.toArray();
            int numSides = pointsArr.length;
            if (numSides == 4) {
                bestPagePolygon = approximatePolygon;
                bestArea = Imgproc.contourArea(approximatePolygon);
            }
        }
        if (bestPagePolygon == null) throw new NoPageFoundException("No page found");
        return sortPagePolygonCorners(bestPagePolygon);
    }

    public static MatOfPoint2f sortPagePolygonCorners(MatOfPoint2f points) {
        Point topLeft = null, bottomLeft = null, bottomRight = null, topRight = null;
        Point centerPoint = sumPoint(points);
        centerPoint = multPoint(centerPoint, 0.25);

        for (Point point : points.toArray()) {
            Point shift = subPoint(point, centerPoint);
            if (shift.x > 0 && shift.y > 0) bottomRight = point;
            if (shift.x < 0 && shift.y > 0) bottomLeft = point;
            if (shift.x < 0 && shift.y < 0) topLeft = point;
            if (shift.x > 0 && shift.y < 0) topRight = point;
        }

        MatOfPoint2f ret;
        try {
            ret = new MatOfPoint2f(topLeft, bottomLeft, bottomRight, topRight);
        } catch (NullPointerException e) {
            return points;
        }
        return ret;
    }

    private static Point sumPoint(MatOfPoint2f pts) {
        Point sum = new Point(0, 0);
        for (Point p : pts.toArray()) {
            sum = addPoint(sum, p);
        }
        return sum;
    }

    private static Point addPoint(Point p1, Point p2) {
        return new Point(p1.x+p2.x, p1.y+p2.y);
    }

    private static Point subPoint(Point p1, Point p2) {
        return new Point(p1.x-p2.x, p1.y-p2.y);
    }

    private static Point multPoint(Point p1, double scale) {
        return new Point(p1.x*scale, p1.y*scale);
    }

    private static Point[] multMat(Point[] mat, double scale) {
        Point[] points = new Point[4];
        int pos = 0;
        for (Point p : mat) {
            points[pos] = multPoint(p, scale);
            pos++;
        }
        return points;
    }


    public static double pointDistance(Point p1, Point p2) {
        return Math.sqrt(Math.pow(p1.x-p2.x, 2) + Math.pow(p1.y-p2.y, 2));
    }

    public static Bitmap matToBitmap(Mat image) {
        Bitmap bitmap = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(image, bitmap);
        return bitmap;
    }

    static class NoPageFoundException extends Exception {
        public NoPageFoundException(String error) {
            super(error);
        }
    }

    public static class NoOASFoundException extends Exception {
        public NoOASFoundException(String error) {
            super(error);
        }
    }
}
