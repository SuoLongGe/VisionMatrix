#ifndef YOLOV8_H
#define YOLOV8_H

#include <string>
#include <vector>
#include <ncnn/net.h>
#include <android/asset_manager.h>

struct Object {
    struct Rect {
        float x;
        float y;
        float width;
        float height;
    } rect;
    int label;
    float prob;
};

class Yolov8 {
public:
    Yolov8();
    ~Yolov8();

    int load(AAssetManager* mgr, const char* param_path, const char* bin_path);
    int detect(const ncnn::Mat& rgb, std::vector<Object>& objects, float prob_threshold = 0.25f);
    std::string get_class_name(int class_id);

private:
    ncnn::Net yolov8;
    static const char* class_names[];
    static const int num_classes = 80;
};

#endif // YOLOV8_H

