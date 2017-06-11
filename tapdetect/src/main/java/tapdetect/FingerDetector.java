/*
* @Author: zhouben
* @Date:   2017-05-10 09:14:53
* @Last Modified by:   zhouben
* @Last Modified time: 2017-05-30 21:08:38
*/

package tapdetect;

import java.util.List;
import java.util.ArrayList;
// import java.util.stream.Collectors;

import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

public class FingerDetector {
    public List<Point> getFingers(Mat im, Mat hand) {
        assert im.size().height == Config.IM_HEIGHT;

        List<MatOfPoint> contours = Util.largeContours(hand, Config.HAND_AREA_MIN);
        if (contours.isEmpty()) {
            return new ArrayList<>();
        }

        Mat hand_with_contour = Util.drawContours(im, contours, new Scalar(0, 0, 255));
        ImgLogger.info("11_contour.jpg", hand_with_contour);

        ArrayList<Point> finger_tips = new ArrayList<>();
        for (MatOfPoint cnt : contours) {
            finger_tips.addAll(this.findFingerTips(cnt.toList(), hand));
        }

        Mat hand_with_finger_tips = Util.drawPoints(hand_with_contour, finger_tips, new Scalar(255, 0, 0));

        ImgLogger.info("12_finger_tips.jpg", hand_with_finger_tips);
        hand_with_finger_tips.assignTo(im);  // so that the caller can have the `im` with contour and fingertip painted

        return finger_tips;
    }

    private List<Point> findFingerTips(List<Point> contour, Mat hand) {
        int len = contour.size();

        List<Point> ret = new ArrayList<>();
        List<Point> cache = new ArrayList<>();
        double cacheY = 0.0;
        double lastY = contour.get(0).y;

        for (int i = 2; i < len; ++i) {
            Point pt = contour.get(i);

            if (cache.isEmpty()) { cacheY = pt.y; }

            if (pt.y == cacheY) {
                cache.add(pt);
            } else {

                if (cacheY >= lastY && cacheY >= pt.y                // 1. is local lowest point
                        && cache.size() < Config.FINGER_TIP_WIDTH           // 2. not too long
                        ) {

                    Point aver = Util.averPoint(cache);
                    if (hand.get((int) aver.y - 1, (int) aver.x)[0] > 0){ // 3. is lower bound
                        ret.add(aver);
                    }
                }
                cache.clear();
                lastY = cacheY;
                i -= 1;
            }
        }

        return ret;
    }

    private List<Point> findFingerTipsOld(MatOfPoint contour, Mat hand) {
        /*
         * @deprecated
         */
        List<Point> contour_pt = contour.toList();
        int step = Config.FINGER_TIP_STEP;
        int len = contour_pt.size();

        ArrayList<Integer> finger_tips_ind = new ArrayList<>();

        for (int i = 0; i < len; ++i) {
            Point pt = contour_pt.get(i);
            Point ahead = contour_pt.get((i + step) % len);
            Point behind = contour_pt.get((i - step) % len < 0 ? (i - step) % len + len : (i - step) % len);

            int center_x = (int) (ahead.x + behind.x + pt.x) / 3;
            int center_y = (int) (ahead.y + behind.y + pt.y) / 3;

            if (center_y > pt.y) {
                continue;
            }

            if ((int) hand.get(center_y, center_x)[0] == 0) {
                continue;
            }

            double cos = Util.intersectCos(contour_pt.get(i), ahead, behind);

            if (cos > 0.7) {
                continue;
            }

            finger_tips_ind.add(i);
        }

        List<Integer> finger_tips_ind_separate = this.mergeNeighbors(finger_tips_ind, Config.FINGER_TIP_WIDTH);

        // FIXME: can't use stream until android sdk 24
        // return finger_tips_ind_separate.stream().map(contour_pt::get).collect(Collectors.toList());
        List<Point> ret = new ArrayList<>();
        for (Integer ind: finger_tips_ind_separate) { ret.add(contour_pt.get(ind)); }
        return ret;
    }

    private List<Integer> mergeNeighbors(List<Integer> inds, int tolerance) {
        List<Integer> ret = new ArrayList<>();
        List<Integer> series = new ArrayList<>();

        for (int ind : inds) {
            if (series.isEmpty() || Math.abs(ind - series.get(series.size() - 1)) < tolerance) {
                series.add(ind);
            } else {
                // ret.add(series.stream().reduce(0, (x, y) -> x + y) / series.size());
                ret.add((int) aver(series));
                series.clear();
            }
        }
        if (!series.isEmpty()) {
            // ret.add(series.stream().reduce(0, (x, y) -> x + y) / series.size());
            ret.add((int) aver(series));
        }
        return ret;
    }
    
    private double aver(List<Integer> lst) {
         double sum = 0.0;
         for (int val: lst) { sum += val; }
         return sum / lst.size();
    }
}