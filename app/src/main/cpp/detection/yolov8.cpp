#include "yolov8.h"
#include <android/asset_manager_jni.h>
#include <android/log.h>
#include <algorithm>
#include <math.h>

static float intersection_area(const Object& a, const Object& b);
static void nms_sorted_bboxes(const std::vector<Object>& faceobjects, std::vector<int>& picked, float nms_threshold);

#define TAG "Yolov8"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// 修改为中文名称，以匹配你的搜索逻辑
const char* Yolov8::class_names[] = {
    "人", "自行车", "汽车", "摩托车", "飞机", "公交车", "火车", "卡车", "船",
    "红绿灯", "消防栓", "停止标志", "收费表", "长凳", "鸟", "猫",
    "狗", "马", "羊", "牛", "大象", "熊", "斑马", "长颈鹿", "背包",
    "雨伞", "手提包", "领带", "手提箱", "飞盘", "滑雪板", "单板滑雪", "运动球",
    "风筝", "棒球棒", "棒球手套", "滑板", "冲浪板", "网球拍",
    "瓶子", "红酒杯", "杯子", "叉子", "刀", "勺子", "碗", "香蕉", "苹果",
    "三明治", "橙子", "西兰花", "胡萝卜", "热狗", "比萨", "甜推圈", "蛋糕", "椅子",
    "沙发", "盆栽", "床", "餐桌", "厕所", "电视", "笔记本电脑", "鼠标", "遥控器",
    "键盘", "手机", "微波炉", "烤箱", "烤面包机", "水槽", "冰箱", "书",
    "时钟", "花瓶", "剪刀", "泰迪熊", "吹风机", "牙刷"
};

Yolov8::Yolov8() {}
Yolov8::~Yolov8() {}

int Yolov8::load(AAssetManager* mgr, const char* param_path, const char* bin_path) {
    // 强制关闭 Vulkan 测试，解决华为设备驱动兼容性导致的识别异常
    yolov8.opt.use_vulkan_compute = false; 
    yolov8.opt.use_fp16_packed = true;
    yolov8.opt.use_fp16_storage = true;
    yolov8.opt.use_fp16_arithmetic = true;
    yolov8.opt.use_packing_layout = true;

    if (yolov8.load_param(mgr, param_path) != 0 || yolov8.load_model(mgr, bin_path) != 0) {
        LOGE("load_model failed");
        return -1;
    }
    LOGD("model loaded successfully");
    return 0;
}

int Yolov8::detect(const ncnn::Mat& rgb, std::vector<Object>& objects, float prob_threshold) {
    objects.clear();
    int img_w = rgb.w;
    int img_h = rgb.h;

    const int target_size = 640;
    float scale = 1.f;
    int w = img_w;
    int h = img_h;
    if (w > h) {
        scale = (float)target_size / w;
        w = target_size;
        h = h * scale;
    } else {
        scale = (float)target_size / h;
        h = target_size;
        w = w * scale;
    }

    ncnn::Mat in = ncnn::Mat::from_pixels_resize((const unsigned char*)rgb.data, ncnn::Mat::PIXEL_RGBA2RGB, img_w, img_h, img_w * 4, w, h);

    // Letterbox 居中填充
    int wpad = (target_size - w) / 2;
    int hpad = (target_size - h) / 2;
    ncnn::Mat in_pad;
    ncnn::copy_make_border(in, in_pad, hpad, target_size - h - hpad, wpad, target_size - w - wpad, ncnn::BORDER_CONSTANT, 114.f);

    const float norm_vals[3] = {1 / 255.f, 1 / 255.f, 1 / 255.f};
    in_pad.substract_mean_normalize(0, norm_vals);

    ncnn::Extractor ex = yolov8.create_extractor();
    ex.input("in0", in_pad);
    ncnn::Mat out;
    ex.extract("out0", out);

    if (out.empty()) return -1;

    std::vector<Object> proposals;
    const int num_grid = out.w; // 8400
    const int num_class = out.h - 4; // 80

    for (int i = 0; i < num_grid; i++) {
        float x_center = out.row(0)[i];
        float y_center = out.row(1)[i];
        float box_w = out.row(2)[i];
        float box_h = out.row(3)[i];

        int label = -1;
        float score = -1.f;
        for (int j = 0; j < num_class; j++) {
            float class_score = out.row(4 + j)[i];
            if (class_score > score) {
                label = j;
                score = class_score;
            }
        }

        if (score >= prob_threshold) {
            // 将 640x640 空间内的坐标还原到原图
            float x0 = (x_center - box_w * 0.5f - wpad) / scale;
            float y0 = (y_center - box_h * 0.5f - hpad) / scale;
            float x1 = (x_center + box_w * 0.5f - wpad) / scale;
            float y1 = (y_center + box_h * 0.5f - hpad) / scale;

            Object obj;
            obj.rect.x = std::max(0.f, x0);
            obj.rect.y = std::max(0.f, y0);
            obj.rect.width = std::max(0.f, x1 - x0);
            obj.rect.height = std::max(0.f, y1 - y0);
            obj.label = label;
            obj.prob = score;
            proposals.push_back(obj);
        }
    }

    std::vector<int> picked;
    nms_sorted_bboxes(proposals, picked, 0.45f);

    int count = picked.size();
    objects.resize(count);
    for (int i = 0; i < count; i++) {
        objects[i] = proposals[picked[i]];
    }

    if (count > 0) {
        LOGD("Found %d objects. First box: x=%.1f, y=%.1f, w=%.1f, h=%.1f, label=%s, score=%.2f", 
             count, objects[0].rect.x, objects[0].rect.y, objects[0].rect.width, objects[0].rect.height, 
             class_names[objects[0].label], objects[0].prob);
    }

    return 0;
}

std::string Yolov8::get_class_name(int class_id) {
    if (class_id >= 0 && class_id < num_classes) return std::string(class_names[class_id]);
    return "unknown";
}

static inline float intersection_area(const Object& a, const Object& b) {
    float inter_left = std::max(a.rect.x, b.rect.x);
    float inter_top = std::max(a.rect.y, b.rect.y);
    float inter_right = std::min(a.rect.x + a.rect.width, b.rect.x + b.rect.width);
    float inter_bottom = std::min(a.rect.y + a.rect.height, b.rect.y + b.rect.height);
    if (inter_right < inter_left || inter_bottom < inter_top) return 0.0f;
    return (inter_right - inter_left) * (inter_bottom - inter_top);
}

static void nms_sorted_bboxes(const std::vector<Object>& faceobjects, std::vector<int>& picked, float nms_threshold) {
    picked.clear();
    const int n = faceobjects.size();
    if (n == 0) return;
    std::vector<float> areas(n);
    for (int i = 0; i < n; i++) areas[i] = faceobjects[i].rect.width * faceobjects[i].rect.height;
    for (int i = 0; i < n; i++) {
        const Object& a = faceobjects[i];
        int keep = 1;
        for (int j = 0; j < (int)picked.size(); j++) {
            const Object& b = faceobjects[picked[j]];
            float inter_area = intersection_area(a, b);
            float union_area = areas[i] + areas[picked[j]] - inter_area;
            if (union_area <= 0) continue;
            if (inter_area / union_area > nms_threshold && a.label == b.label) keep = 0;
        }
        if (keep) picked.push_back(i);
    }
}

