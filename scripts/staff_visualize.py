# -*- coding: utf-8 -*-
import cv2
import json
import sys
import os

"""
MuTune — Staff Visualization Tool (오선 검증 이미지 생성)
"""

if len(sys.argv) != 3:
    print("Usage: python staff_visualize.py <image_path> <staff_json_path>")
    sys.exit(1)

img_path = sys.argv[1]
json_path = sys.argv[2]

# ========== 이미지 로드 ==========
img = cv2.imread(img_path)
if img is None:
    print("Error: image load failed")
    sys.exit(1)

# ========== JSON 로드 ==========
if not os.path.exists(json_path):
    print("Error: JSON file not found")
    sys.exit(1)

with open(json_path, "r", encoding="utf-8") as f:
    data = json.load(f)

staffs = data.get("staffs", [])

preview = img.copy()
h, w = img.shape[:2]

line_color = (0, 0, 255)

# ========== 오선 그리기 ==========
for staff in staffs:
    for y in staff["lines"]:
        cv2.line(preview, (0, y), (w, y), line_color, 2)

# ========== 저장 ==========
out_path = os.path.join(os.path.dirname(json_path), "preview_staff.png")
cv2.imwrite(out_path, preview)

print(f"[OK] Preview saved → {out_path}")
