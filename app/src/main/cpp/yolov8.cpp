#include "yolov8.h"
#include <android/asset_manager_jni.h>
#include <android/log.h>
#include <algorithm>

// NMS辅助函数声明
static float intersection_area(const Object& a, const Object& b);
static void nms_sorted_bboxes(const std::vector<Object>& faceobjects, std::vector<int>& picked, float nms_threshold);

#define TAG "Yolov8"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

const char* Yolov8::class_names[] = {
    "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat",
    "traffic light", "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat",
    "dog", "horse", "sheep", "cow", "elephant", "bear", "zebra", "giraffe", "backpack",
    "umbrella", "handbag", "tie", "suitcase", "frisbee", "skis", "snowboard", "sports ball",
    "kite", "baseball bat", "baseball glove", "skateboard", "surfboard", "tennis racket",
    "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple",
    "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair",
    "couch", "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse", "remote",
    "keyboard", "cell phone", "microwave", "oven", "toaster", "sink", "refrigerator", "book",
    "clock", "vase", "scissors", "teddy bear", "hair drier", "toothbrush"
};

Yolov8::Yolov8() {
}

Yolov8::~Yolov8() {
}

int Yolov8::load(AAssetManager* mgr, const char* param_path, const char* bin_path) {
    // 如果使用vulkan版本，可以启用GPU加速（需要设备支持Vulkan）
    // 如果使用普通版本，保持为false
    yolov8.opt.use_vulkan_compute = true;  // 如果下载的是vulkan版本，设为true；否则设为false
    yolov8.opt.use_winograd_convolution = true;
    yolov8.opt.use_sgemm_convolution = true;
    yolov8.opt.use_fp16_packed = true;
    yolov8.opt.use_fp16_storage = true;
    yolov8.opt.use_fp16_arithmetic = true;
    yolov8.opt.use_packing_layout = true;
    yolov8.opt.use_shader_pack8 = false;
    yolov8.opt.use_image_storage = false;

    int ret = yolov8.load_param(mgr, param_path);
    if (ret != 0) {
        LOGE("load_param failed");
        return -1;
    }

    ret = yolov8.load_model(mgr, bin_path);
    if (ret != 0) {
        LOGE("load_model failed");
        return -1;
    }

    LOGD("model loaded successfully");
    return 0;
}

int Yolov8::detect(const ncnn::Mat& rgb, std::vector<Object>& objects, float prob_threshold) {
    objects.clear();

    const int target_size = 640;
    int img_w = rgb.w;
    int img_h = rgb.h;

    // letterbox pad to multiple of 32
    int w = img_w;
    int h = img_h;
    float scale = 1.f;
    if (w > h) {
        scale = (float)target_size / w;
        w = target_size;
        h = h * scale;
    } else {
        scale = (float)target_size / h;
        h = target_size;
        w = w * scale;
    }

    ncnn::Mat in = ncnn::Mat::from_pixels_resize(rgb.data, ncnn::Mat::PIXEL_RGB, rgb.w, rgb.h, w, h);

    // pad to target_size rectangle
    int wpad = (w + 31) / 32 * 32 - w;
    int hpad = (h + 31) / 32 * 32 - h;
    ncnn::Mat in_pad;
    ncnn::copy_make_border(in, in_pad, hpad / 2, hpad - hpad / 2, wpad / 2, wpad - wpad / 2, ncnn::BORDER_CONSTANT, 114.f);

    const float norm_vals[3] = {1 / 255.f, 1 / 255.f, 1 / 255.f};
    in_pad.substract_mean_normalize(0, norm_vals);

    ncnn::Extractor ex = yolov8.create_extractor();

    ex.input("in0", in_pad);

    ncnn::Mat out;
    ex.extract("out0", out);

    // decode box
    std::vector<Object> proposals;
    const int num_grid = out.h;

    for (int i = 0; i < num_grid; i++) {
        const float* ptr = out.row(i);
        float x_center = ptr[0];
        float y_center = ptr[1];
        float w = ptr[2];
        float h = ptr[3];

        float x0 = x_center - w * 0.5f;
        float y0 = y_center - h * 0.5f;
        float x1 = x_center + w * 0.5f;
        float y1 = y_center + h * 0.5f;

        x0 = (x0 - (wpad / 2)) / scale;
        y0 = (y0 - (hpad / 2)) / scale;
        x1 = (x1 - (wpad / 2)) / scale;
        y1 = (y1 - (hpad / 2)) / scale;

        for (int j = 0; j < num_classes; j++) {
            float score = ptr[4 + j];
            if (score >= prob_threshold) {
                Object obj;
                obj.rect.x = x0;
                obj.rect.y = y0;
                obj.rect.width = x1 - x0;
                obj.rect.height = y1 - y0;
                obj.label = j;
                obj.prob = score;
                proposals.push_back(obj);
            }
        }
    }

    // nms
    std::vector<int> picked;
    nms_sorted_bboxes(proposals, picked, 0.45f);

    int count = picked.size();
    objects.resize(count);
    for (int i = 0; i < count; i++) {
        objects[i] = proposals[picked[i]];
    }

    return 0;
}

std::string Yolov8::get_class_name(int class_id) {
    if (class_id >= 0 && class_id < num_classes) {
        return std::string(class_names[class_id]);
    }
    return "unknown";
}

// NMS implementation
static inline float intersection_area(const Object& a, const Object& b) {
    float inter_left = std::max(a.rect.x, b.rect.x);
    float inter_top = std::max(a.rect.y, b.rect.y);
    float inter_right = std::min(a.rect.x + a.rect.width, b.rect.x + b.rect.width);
    float inter_bottom = std::min(a.rect.y + a.rect.height, b.rect.y + b.rect.height);
    
    if (inter_right < inter_left || inter_bottom < inter_top) {
        return 0.0f;
    }
    
    return (inter_right - inter_left) * (inter_bottom - inter_top);
}

static void nms_sorted_bboxes(const std::vector<Object>& faceobjects, std::vector<int>& picked, float nms_threshold) {
    picked.clear();

    const int n = faceobjects.size();
    if (n == 0) return;

    std::vector<float> areas(n);
    for (int i = 0; i < n; i++) {
        areas[i] = faceobjects[i].rect.width * faceobjects[i].rect.height;
    }

    for (int i = 0; i < n; i++) {
        const Object& a = faceobjects[i];

        int keep = 1;
        for (int j = 0; j < (int)picked.size(); j++) {
            const Object& b = faceobjects[picked[j]];

            float inter_area = intersection_area(a, b);
            float union_area = areas[i] + areas[picked[j]] - inter_area;
            if (union_area <= 0) continue;
            
            float iou = inter_area / union_area;

            if (iou > nms_threshold && a.label == b.label)
                keep = 0;
        }

        if (keep)
            picked.push_back(i);
    }
}

