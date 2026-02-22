import cv2
import numpy as np
import os
import json
from datetime import datetime
from natsort import natsorted
import glob
import math


# ------------------------------------------------------
# LOGGING
# ------------------------------------------------------
def step(msg):
    print(f"\n===== [STEP] {msg} =====")

def info(msg):
    print(f"[INFO] {msg}")

def warn(msg):
    print(f"[WARN] {msg}")


# ------------------------------------------------------
# 기울기(skew) 검출
# ------------------------------------------------------
def compute_skew_angle(gray):
    edges = cv2.Canny(gray, 50, 150, apertureSize=3)
    lines = cv2.HoughLines(edges, 1, np.pi/180, 150)

    if lines is None:
        warn("HoughLines에서 직선 검출 실패 → 기울기 0으로 가정")
        return 0.0

    angles = []
    for rho, theta in lines[:,0]:
        angle_deg = (theta * 180 / np.pi)
        # 수평선 부근만 사용
        if 80 < angle_deg < 100:
            angles.append(angle_deg - 90)

    if len(angles) == 0:
        warn("수평선 기반 기울기 검출 실패 → 0°로 가정")
        return 0.0

    median_angle = np.median(angles)
    info(f"기울기 추정값: {median_angle:.3f}°")
    return median_angle


# ------------------------------------------------------
# skew 보정
# ------------------------------------------------------
def deskew_image(gray, angle):
    if abs(angle) < 0.05:
        info("기울기 매우 작음 → deskew 생략")
        return gray

    h, w = gray.shape
    M = cv2.getRotationMatrix2D((w//2, h//2), angle, 1.0)
    rotated = cv2.warpAffine(gray, M, (w, h), flags=cv2.INTER_LINEAR)
    info("deskew 완료")
    return rotated


# ------------------------------------------------------
# 오선 감지
# ------------------------------------------------------
def detect_staff_lines(gray):
    step("오선 감지 시작")

    # 이미지 대비 강화
    blur = cv2.GaussianBlur(gray, (3,3), 0)
    edges = cv2.Canny(blur, 30, 120)

    # Hough Line 검출
    lines = cv2.HoughLinesP(
        edges,
        rho=1,
        theta=np.pi/180,
        threshold=50,
        minLineLength=gray.shape[1] * 0.6,
        maxLineGap=10
    )

    if lines is None:
        warn("오선 직선 검출 실패")
        return []

    candidate_lines = []
    for (x1, y1, x2, y2) in lines[:,0]:
        # 거의 수평선만
        if abs(y1 - y2) < 3:
            candidate_lines.append(int((y1 + y2) / 2))

    candidate_lines = sorted(candidate_lines)
    info(f"검출된 선 후보 수: {len(candidate_lines)}")

    # --------------------------------------------------
    # 가까운 선끼리 묶기
    # --------------------------------------------------
    grouped = []
    temp = []

    for y in candidate_lines:
        if not temp:
            temp.append(y)
            continue
        if abs(y - temp[-1]) <= 5:
            temp.append(y)
        else:
            grouped.append(int(np.mean(temp)))
            temp = [y]

    if temp:
        grouped.append(int(np.mean(temp)))

    info(f"Y 그룹 수(정제 후): {len(grouped)}")

    # --------------------------------------------------
    # 5줄씩 묶어서 staff block 생성
    # --------------------------------------------------
    staff_blocks = []
    block = []

    for y in grouped:
        block.append(y)
        if len(block) == 5:
            staff_blocks.append(sorted(block))
            block = []

    info(f"최종 검출된 오선 블록 수: {len(staff_blocks)}")

    return staff_blocks


# ------------------------------------------------------
# staff mask 이미지 생성
# ------------------------------------------------------
def build_staff_mask(gray, staff_blocks, thickness=3):
    mask = np.zeros_like(gray)

    for block in staff_blocks:
        for y in block:
            cv2.line(mask, (0,y), (gray.shape[1], y), 255, thickness)

    return mask


# ------------------------------------------------------
# staff_only 이미지 생성
# ------------------------------------------------------
def make_staff_only_image(gray, staff_mask):
    return cv2.bitwise_and(gray, staff_mask)


# ------------------------------------------------------
# JSON 저장
# ------------------------------------------------------
def save_staff_json(save_path, staff_blocks):
    data = {
        "staff_blocks": staff_blocks,
        "generated_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    }

    with open(save_path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=4)

    info(f"staff_lines.json 저장 완료 → {save_path}")


# ------------------------------------------------------
# 메인 처리 함수
# ------------------------------------------------------
def process_image_file(file_path, output_dir):

    step(f"이미지 처리 시작 → {os.path.basename(file_path)}")

    gray = cv2.imread(file_path, cv2.IMREAD_GRAYSCALE)

    # 1) Skew angle 계산
    angle = compute_skew_angle(gray)

    # 2) Skew 보정
    deskewed = deskew_image(gray, angle)

    # 3) 오선 검출
    staff_blocks = detect_staff_lines(deskewed)

    if len(staff_blocks) == 0:
        warn("오선 블록 검출 실패 → 결과 저장 중단")
        return

    # 4) 오선 마스크 생성
    staff_mask = build_staff_mask(deskewed, staff_blocks, thickness=3)

    # 5) staff_only 이미지 생성
    staff_only = make_staff_only_image(deskewed, staff_mask)

    # 6) 파일 저장
    cv2.imwrite(os.path.join(output_dir, "original.png"), gray)
    cv2.imwrite(os.path.join(output_dir, "deskewed.png"), deskewed)
    cv2.imwrite(os.path.join(output_dir, "staff_mask.png"), staff_mask)
    cv2.imwrite(os.path.join(output_dir, "staff_only.png"), staff_only)

    # 7) JSON 저장
    save_staff_json(os.path.join(output_dir, "staff_lines.json"), staff_blocks)


# ------------------------------------------------------
# Batch 실행
# ------------------------------------------------------
def process_folder(pattern):
    files = natsorted(glob.glob(pattern))
    if not files:
        warn("입력 이미지 없음")
        return

    for file_path in files:
        base = os.path.splitext(os.path.basename(file_path))[0]
        output_dir = os.path.join("D:/Mutune/backend/uploads/staff_detect", base)
        os.makedirs(output_dir, exist_ok=True)

        process_image_file(file_path, output_dir)


if __name__ == "__main__":
    import sys

    if len(sys.argv) < 2:
        print("Usage: python staff_engine.py \"path/to/*.png\"")
        exit()

    pattern = sys.argv[1]
    process_folder(pattern)
