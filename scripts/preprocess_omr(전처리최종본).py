# -*- coding: utf-8 -*-
import cv2
import numpy as np
import os
import glob


# =====================================================
# 1) 기울기 보정(deskew)
# =====================================================
def deskew(img):
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    blur = cv2.GaussianBlur(gray, (5, 5), 0)
    edges = cv2.Canny(blur, 50, 150)

    lines = cv2.HoughLines(edges, 1, np.pi/180, 200)
    if lines is None:
        return img  # 보정 불가 → 원본 유지

    angles = []
    for rho, theta in lines[:, 0]:
        # 수평선 근처만 사용
        if abs(theta - np.pi/2) < np.deg2rad(15):
            angles.append(theta - np.pi/2)

    if len(angles) == 0:
        return img

    avg_angle = np.mean(angles)
    angle_deg = np.rad2deg(avg_angle)

    (h, w) = img.shape[:2]
    M = cv2.getRotationMatrix2D((w // 2, h // 2), angle_deg, 1.0)
    rotated = cv2.warpAffine(
        img, M, (w, h),
        flags=cv2.INTER_LINEAR,
        borderMode=cv2.BORDER_REPLICATE
    )
    return rotated


# =====================================================
# 2) 약한 노이즈 제거 (Gaussian + Median)
# =====================================================
def light_denoise(img):
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

    # ▶ 1차: 아주 약한 Gaussian blur (형태 손상 없음)
    blur = cv2.GaussianBlur(gray, (3, 3), 0)

    # ▶ 2차: 작은 점 노이즈 제거
    median = cv2.medianBlur(blur, 3)

    clean = cv2.cvtColor(median, cv2.COLOR_GRAY2BGR)
    return clean


# =====================================================
# 3) 초미세 오선 강화 (Tiny Staff Enhance)
# =====================================================
def tiny_staff_enhance(img, alpha=0.03):  # 3% 강화
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

    # 수평 성분 추출 (오선 추출)
    sobel = cv2.Sobel(gray, cv2.CV_64F, 0, 1, ksize=3)
    sobel = np.clip(np.abs(sobel), 0, 255).astype(np.uint8)

    # 너무 강해지는 것 방지 (부드럽게)
    sobel = cv2.GaussianBlur(sobel, (5, 5), 0)

    # 원본과 3% 이내로만 합성 → 음표·가사 손상 X
    enhanced = cv2.addWeighted(gray, 1.0, sobel, alpha, 0)

    enhanced = cv2.cvtColor(enhanced, cv2.COLOR_GRAY2BGR)
    return enhanced


# =====================================================
# MAIN
# =====================================================
if __name__ == "__main__":

    print("\n전처리할 세션 폴더 경로 입력 : ", end="")
    target = input().strip().strip('"')

    if not os.path.isdir(target):
        print(f"[ERROR] 폴더가 존재하지 않습니다 → {target}")
        exit()

    print("\n===== MuTune OMR 전처리 시작 =====")

    img_files = sorted(
        glob.glob(os.path.join(target, "*.png")) +
        glob.glob(os.path.join(target, "*.jpg")) +
        glob.glob(os.path.join(target, "*.jpeg"))
    )

    if not img_files:
        print("[ERROR] 처리할 이미지가 없습니다.")
        exit()

    for path in img_files:
        file_name = os.path.basename(path)
        print(f"\n[PROCESS] {file_name}")

        img = cv2.imread(path)
        if img is None:
            print("[ERROR] 로드 실패")
            continue

        # 1) 기울기 보정
        img = deskew(img)

        # 2) 약한 노이즈 제거
        img = light_denoise(img)

        # 3) 초미세 오선 강화
        img = tiny_staff_enhance(img, alpha=0.03)

        # 저장
        out_path = os.path.join(target, f"clean_{file_name}")
        cv2.imwrite(out_path, img)

        print(f"[OK] 저장됨 → {out_path}")

    print("\n===== MuTune OMR 전처리 완료 =====\n")
