import onnxruntime as ort
import numpy as np
import json
from pathlib import Path
from transformers import CLIPTokenizer  # 需要 pip install transformers

# 1. 定义你想要支持的意图（提示词 Prompt）
# 这里的 Key 是你的业务 ID，Value 是给模型看的描述
actions = {
    "TEXT": "a photo of a document, book, paper, text, receipt, sign, handwriting",
    "SCENERY": "a photo of a landscape, mountain, sky, nature, sunset, park, beach",
    "PERSON": "a photo of a person, face, man, woman, selfie, crowd, portrait",
    "FOOD": "a photo of food, dish, meal, restaurant, fruit, vegetable, meat",
    "OBJECT": "a photo of a product, bottle, box, item, tool, gadget, toy"
}

# 2. 加载文本模型 (使用脚本所在目录的文件，避免当前工作目录不一致)
BASE_DIR = Path(__file__).resolve().parent
model_path = BASE_DIR / "text_model_uint8.onnx"
session = ort.InferenceSession(str(model_path))

# 注意：Xenova 的模型通常自带 Tokenizer 逻辑或需要特定的 Tokenizer
# 为了简化，这里假设你使用 transformers 库的标准 CLIP Tokenizer
# 如果这步报错，说明你需要下载对应的 tokenizer.json
# 简单替代方案：去 HuggingFace 在线 API 跑一下文本获取向量，或者下载完整 repo
tokenizer = CLIPTokenizer.from_pretrained("openai/clip-vit-base-patch32")

output_data = {}

print("正在生成意图向量...")
for key, prompt in actions.items():
    inputs = tokenizer(prompt, padding="max_length", max_length=77, return_tensors="np")
    # ONNX 模型要求 int64 输入
    input_ids = inputs["input_ids"].astype(np.int64)

    # 运行推理
    # 注意：不同导出版本的输入名可能不同，通常是 input_ids
    outputs = session.run(None, {"input_ids": input_ids})
    embedding = outputs[0][0] # 拿到向量

    # 归一化 (Normalization) - 非常重要！用于余弦相似度计算
    norm = np.linalg.norm(embedding)
    embedding = embedding / norm

    output_data[key] = embedding.tolist()

# 3. 保存为 JSON（同目录下）
with open(BASE_DIR / "action_embeddings.json", "w") as f:
    json.dump(output_data, f)

print("完成！请把 action_embeddings.json 放入 Android 的 assets 文件夹。")