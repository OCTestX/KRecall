from flask import Flask, request, jsonify
import paddlex as pdx
import cv2
import numpy as np
import time
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

# 初始化 OCR 模型（检测+识别联合模型）
ocr_model = pdx.create_pipeline ('OCR')  # 需提前下载模型


@app.route('/ocr', methods=['POST'])
def ocr_image():
    if 'image' not in request.files:
        return jsonify({"error": "No image uploaded"}), 400

    file = request.files['image']
    if file.filename == '':
        return jsonify({"error": "Empty filename"}), 400

    try:
        print("DEBUG0: ", file.filename)
        # 读取图片字节流
        img_bytes = file.read()
        np_arr = np.frombuffer(img_bytes, np.uint8)
        img = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)

        if img is None:
            return jsonify({"error": "Invalid image data"}), 400

        # 执行 OCR
        start_time = time.time()
        print("DEBUG1")
        result = ocr_model.predict(img)
        print("DEBUG2: ", str(result))
        process_time = time.time() - start_time

        # 构建响应
        response_data = {
            "meta": {
                "image_size": {"width": img.shape[1], "height": img.shape[0]},
                "process_time": round(process_time, 3)
            },
            "results": []
        }

        for item in result:
            response_data["results"].append({
                "text": item["json"]["res"]['text'],
                "confidence": item['score'],
                "text_region": [
                    [int(item['bbox'][0]), int(item['bbox'][1])],
                    [int(item['bbox'][2]), int(item['bbox'][3])],
                    [int(item['bbox'][4]), int(item['bbox'][5])],
                    [int(item['bbox'][6]), int(item['bbox'][7])]
                ],
                "rotation": 0
            })

        return jsonify(response_data)

    except Exception as e:
        return jsonify({"error": str(e)}), 500


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=False)