# -*- coding: utf-8 -*-

import sys
sys.stdout.reconfigure(encoding='utf-8')

import cv2
import numpy as np
from pathlib import Path
from pdf2image import convert_from_path

#  PyMuPDF(텍스트 검사)
import fitz


def safe_print(s):
    try:
        print(s)
    except UnicodeEncodeError:
        print(s.encode('utf-8', errors='ignore').decode('utf-8'))


# ---------------------------
# PDF 텍스트 검사 함수
# ---------------------------
def is_text_pdf(pdf_path):
    try:
        doc = fitz.open(pdf_path)
        for page in doc:
            text = page.get_text().strip()
            if text:         # 한 글자라도 있으면 원본 PDF
                return True
        return False          # 텍스트 0 → 이미지 기반 PDF
    except Exception as e:
        safe_print("[PDF 검사 오류] " + str(e))
        return False


# --------------------------------------------------------
# 인자 처리 (4개 고정)
# --------------------------------------------------------
if len(sys.argv) != 5:
    safe_print("Usage: python image_preprocess.py <input> <omrA_base> <poppler_path> <overlay_base>")
    sys.exit(1)

input_path = Path(sys.argv[1])
omrA_base = Path(sys.argv[2])
poppler_path = sys.argv[3]
overlay_base = Path(sys.argv[4])

safe_print(f"입력 파일: {input_path}")
safe_print(f"Poppler 경로: {poppler_path}")
safe_print("--------------------------------------------------------")


# --------------------------------------------------------
# PDF 텍스트 검사 먼저 실행
# --------------------------------------------------------
if input_path.suffix.lower() == ".pdf":
    safe_print("[검사] PDF 텍스트 여부 확인 중...")

    if not is_text_pdf(str(input_path)):
        safe_print("ERROR_NOT_ORIGINAL_PDF: 텍스트 없는 이미지 PDF입니다.")
        sys.exit(9)   # ← 백엔드에서 이 코드를 감지해서 차단 처리
    else:
        safe_print("[검사] 텍스트 PDF 확인됨 (원본 PDF)")


# --------------------------------------------------------
# PDF 또는 이미지 읽기
# 기존 코드 유지
# --------------------------------------------------------
ext = input_path.suffix.lower()

try:
    if ext == ".pdf":
        pages = convert_from_path(str(input_path), dpi=300, poppler_path=poppler_path)
        safe_print(f"PDF 페이지 수: {len(pages)}")

        page_images = []
        for p in pages:
            img = cv2.cvtColor(np.array(p), cv2.COLOR_RGB2BGR)
            page_images.append(img)
    else:
        img = cv2.imread(str(input_path))
        if img is None:
            safe_print("이미지 로드 실패")
            sys.exit(2)
        page_images = [img]

except Exception as e:
    safe_print("입력 파일 읽기 오류: " + str(e))
    sys.exit(3)


# --------------------------------------------------------
# 페이지 반복 처리 (기존 유지)
# --------------------------------------------------------
page_count = len(page_images)

for idx, page in enumerate(page_images, start=1):

    safe_print(f"====== 페이지 {idx}/{page_count} 처리 시작 ======")

    # --------------------------------------------------------
    # 1) Deskew
    # --------------------------------------------------------
    try:
        gray = cv2.cvtColor(page, cv2.COLOR_BGR2GRAY)
        inv = cv2.bitwise_not(gray)

        thresh = cv2.threshold(inv, 0, 255, cv2.THRESH_BINARY | cv2.THRESH_OTSU)[1]
        coords = np.column_stack(np.where(thresh > 0))
        angle = cv2.minAreaRect(coords)[-1]

        if abs(angle) > 45:
            safe_print(f"[p{idx}] 각도 무시 (원본 유지): {angle:.4f}°")
            angle = 0
        else:
            angle = -angle

        safe_print(f"[p{idx}] 최종 적용 각도: {angle:.4f}°")

        h, w = page.shape[:2]
        center = (w // 2, h // 2)

        M = cv2.getRotationMatrix2D(center, angle, 1.0)
        deskewed = cv2.warpAffine(page, M, (w, h),
                                  flags=cv2.INTER_LINEAR,
                                  borderMode=cv2.BORDER_REPLICATE)

    except Exception as e:
        safe_print("[Deskew 오류] " + str(e))
        sys.exit(4)

    # --------------------------------------------------------
    # 2) overlay 저장
    # --------------------------------------------------------
    overlay_path = overlay_base.parent / f"{overlay_base.stem}_{idx}.png"

    try:
        overlay_path.parent.mkdir(parents=True, exist_ok=True)
        cv2.imwrite(str(overlay_path), deskewed)
        safe_print(f"[p{idx}] overlay 저장: {overlay_path}")
    except Exception as e:
        safe_print("[overlay 저장 오류] " + str(e))
        sys.exit(5)

    # --------------------------------------------------------
    # 3) OMR-A 생성 (Audiveris)
    # --------------------------------------------------------
    try:
        gray = cv2.cvtColor(deskewed, cv2.COLOR_BGR2GRAY)

        clahe = cv2.createCLAHE(clipLimit=3.5, tileGridSize=(8, 8))
        gray = clahe.apply(gray)

        denoise = cv2.fastNlMeansDenoising(gray, h=10,
                                           templateWindowSize=7,
                                           searchWindowSize=21)

        sharpen_kernel = np.array(
            [
                [-1, -1, -1],
                [-1,  9, -1],
                [-1, -1, -1]
            ],
            dtype=np.float32
        )
        sharp = cv2.filter2D(denoise, -1, sharpen_kernel)

        binary = cv2.adaptiveThreshold(
            sharp,
            255,
            cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
            cv2.THRESH_BINARY,
            35,
            7
        )

        omrA_path = omrA_base.parent / f"{omrA_base.stem}_{idx}.png"
        omrA_path.parent.mkdir(parents=True, exist_ok=True)
        cv2.imwrite(str(omrA_path), binary)

        safe_print(f"[p{idx}] omrA 저장: {omrA_path}")

    except Exception as e:
        safe_print("[omrA 처리 오류] " + str(e))
        sys.exit(6)

    safe_print(f"====== 페이지 {idx} 처리 완료 ======")

safe_print("전처리 전체 완료")
sys.exit(0)
