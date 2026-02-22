import cv2
import numpy as np
import os
import uuid
from datetime import datetime
import json
from natsort import natsorted
import glob
import time


# =====================================================
# LOG 함수
# =====================================================
def log(msg):
    print(f"[LOG] {msg}")

def warn(msg):
    print(f"[WARN] {msg}")

def info(msg):
    print(f"[INFO] {msg}")

def step(msg):
    print(f"\n===== [STEP] {msg} =====")


# =====================================================
# 0) 저장 경로 생성
# =====================================================
def create_output_folders(num_pages):

    base_dir = r"D:\Mutune\backend\uploads\staff_detect"

    session_id = f"staffDetect_{uuid.uuid4().hex[:8]}_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
    session_path = os.path.join(base_dir, session_id)
    os.makedirs(session_path, exist_ok=True)

    page_dirs = {}
    for i in range(1, num_pages + 1):
        p = os.path.join(session_path, f"page{i}")
        os.makedirs(p, exist_ok=True)
        page_dirs[i] = p

    info(f"세션 생성됨 → {session_path}")
    info(f"총 페이지 폴더 생성: {num_pages}")

    return session_path, page_dirs


# =====================================================
# 1-A) 워터마크 마스크 생성
# =====================================================
def build_watermark_mask(binary_img):
    """
    binary_img : 0/255 이진 이미지 (THRESH_BINARY_INV 기준, 흰색 = 오선/텍스트)

    전략:
      1) closing 커널을 크게 해서 워터마크 글자들을 한 덩어리로 묶는다.
      2) connectedComponentsWithStats로 각 덩어리의 크기/비율/위치를 보고
         '워터마크 패턴'에 해당하는 것만 마스크로 선정한다.
      3) staff 라인(매우 얇고 긴 선)은 세로 두께가 너무 얇기 때문에 필터에서 제외한다.
    """
    step("워터마크 마스크 생성 시작 (강화 버전)")

    h, w = binary_img.shape

    # 1) 글자들을 더 강하게 붙이기 위한 closing
    #    - 가로 65, 세로 11 정도로 넓게 잡아서 akbobada.com 같은 문자열을 하나의 큰 blob으로 만듦
    kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (65, 11))
    closed = cv2.morphologyEx(binary_img, cv2.MORPH_CLOSE, kernel, iterations=3)

    # 2) 연결 성분 분석
    num_labels, labels, stats, centroids = cv2.connectedComponentsWithStats(
        closed, connectivity=8
    )

    info(f"[WM] Connected Components 개수: {num_labels}")

    wm_mask = np.zeros_like(binary_img)
    wm_count = 0

    # 화면 전체 대비 기준 값
    img_area = h * w

    for i in range(1, num_labels):  # 0은 배경
        x, y, w_i, h_i, area = stats[i]

        aspect = w_i / max(h_i, 1)

        # ───────── 조건 튜닝 포인트 ─────────
        # 1) 가로 길이: 화면 가로의 25% 이상 (너무 짧은 건 워터마크로 보지 않음)
        cond_width = w_i > int(0.25 * w)

        # 2) 세로 두께: staff 묶음보다 두껍고, 페이지 전체에 비해 너무 크지 않게
        #    - staff 라인은 보통 3~8px 수준, 워터마크 글자는 20~80px 정도.
        cond_height = 18 <= h_i <= int(h * 0.4)

        # 3) 면적: 이미지 전체의 일정 비율 이상일 때만 워터마크 후보
        #    (너무 작은 것은 코드/가사 조각일 가능성 큼)
        cond_area = area > img_area * 0.003  # 0.3% 이상

        # 4) 가로/세로 비율: 너무 세로로 긴 것(수직선)이나 정사각형 계열 제외
        cond_aspect = 1.2 <= aspect <= 50.0

        # 5) 위치: 보통 워터마크는 페이지 중앙~하단에 있는 경우가 많음
        #    (상단 작은 텍스트는 제목/코드일 가능성이 크므로 우선 제외)
        cond_ypos = y > int(h * 0.25)

        if cond_width and cond_height and cond_area and cond_aspect and cond_ypos:
            wm_mask[labels == i] = 255
            wm_count += 1

    info(f"[WM] 최종 워터마크 후보 블록 수: {wm_count}")

    if wm_count == 0:
        warn("[WM] 워터마크 영역이 감지되지 않음 (조건이 너무 강할 수 있음)")

    return wm_mask
# =====================================================
# 1-B) 워터마크 / 텍스트 제거
# =====================================================
def remove_watermark_and_text(gray):
    """
    1) Adaptive Threshold로 이진화
    2) 워터마크 마스크 생성 후 제거
    3) 수직 성분 제거 (코드/가사/텍스트)
    4) 수평 성분 강조 (오선)
    """
    step("워터마크 제거 + 텍스트 제거 시작")

    # 1) Adaptive Threshold
    th = cv2.adaptiveThreshold(
        gray,
        255,
        cv2.ADAPTIVE_THRESH_MEAN_C,
        cv2.THRESH_BINARY_INV,
        51,
        7
    )
    info("Adaptive Threshold 완료")

    # 2) 워터마크 마스크 생성
    wm_mask = build_watermark_mask(th)

    # 제거된 픽셀 수 (디버그용)
    removed_area = int(np.sum(wm_mask > 0))
    info(f"[WM] 워터마크 제거 픽셀 수: {removed_area}")

    if removed_area == 0:
        warn("[WM] 워터마크 제거 픽셀 수가 0입니다. 조건이 너무 빡셀 수 있음.")

    # 3) 워터마크 부분을 0(검정)으로 날려버리기
    th_no_wm = th.copy()
    th_no_wm[wm_mask == 255] = 0

    # 4) 수직 성분 제거 (코드/가사/텍스트 억제)
    vertical_kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (3, 25))
    vertical_removed = cv2.morphologyEx(th_no_wm, cv2.MORPH_OPEN, vertical_kernel)
    info("수직 성분 제거 완료")

    # 5) 수평 성분 강조 (오선 강조)
    horizontal_kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (50, 1))
    staff_emphasized = cv2.morphologyEx(th_no_wm, cv2.MORPH_OPEN, horizontal_kernel)
    info("오선 강조 처리 완료")

    # 6) 오선만 남기기 (현재는 staff_emphasized 그대로 사용)
    staff_only = cv2.bitwise_and(staff_emphasized, staff_emphasized)

    return th_no_wm, wm_mask, vertical_removed, staff_emphasized, staff_only



# =====================================================
# 2) Projection Profile 기반 오선 검출
# =====================================================
def detect_staff_lines_projection(staff_only):

    step("오선 Projection 검출 시작")

    projection = np.sum(staff_only, axis=1)
    threshold = np.mean(projection) * 1.2

    peaks = [i for i, v in enumerate(projection) if v > threshold]

    info(f"Peaks 감지 수: {len(peaks)}")

    grouped = []
    temp = []

    for y in peaks:
        if not temp:
            temp.append(y)
            continue

        if abs(y - temp[-1]) <= 5:
            temp.append(y)
        else:
            if len(temp) >= 2:
                grouped.append(int(np.mean(temp)))
            temp = [y]

    if len(temp) >= 2:
        grouped.append(int(np.mean(temp)))

    info(f"Group 묶음 수: {len(grouped)}")

    staff_sets = []
    for i in range(0, len(grouped), 5):
        block = grouped[i:i + 5]
        if len(block) == 5:
            staff_sets.append(block)

    info(f"최종 검출된 오선 세트 수: {len(staff_sets)}")

    if len(staff_sets) == 0:
        warn("⚠ 오선 검출 실패 가능 — staff_sets가 비어 있음")

    return staff_sets, projection


# =====================================================
# 3) main 실행부
# =====================================================
def process_images(pattern):

    step("이미지 목록 확인")
    files = natsorted([f for f in glob.glob(pattern)])

    if not files:
        warn("입력 이미지 없음")
        return

    info(f"총 입력 이미지 수: {len(files)}")

    session_path, page_dirs = create_output_folders(len(files))

    result_json = {"pages": []}

    total_start = time.time()

    for idx, file_path in enumerate(files, start=1):

        step(f"PAGE {idx} 처리 시작 — {os.path.basename(file_path)}")

        page_start = time.time()
        gray = cv2.imread(file_path, cv2.IMREAD_GRAYSCALE)

        if gray is None:
            warn(f"{file_path} 로딩 실패")
            continue

        th_no_wm, wm_mask, vertical_removed, staff_emphasized, staff_only = \
            remove_watermark_and_text(gray)

        staff_sets, proj = detect_staff_lines_projection(staff_only)

        page_dir = page_dirs[idx]

        cv2.imwrite(os.path.join(page_dir, "threshold_no_watermark.png"), th_no_wm)
        cv2.imwrite(os.path.join(page_dir, "watermark_mask.png"), wm_mask)
        cv2.imwrite(os.path.join(page_dir, "vertical_removed.png"), vertical_removed)
        cv2.imwrite(os.path.join(page_dir, "staff_emphasized.png"), staff_emphasized)
        cv2.imwrite(os.path.join(page_dir, "staff_only.png"), staff_only)

        info(f"DEBUG 이미지 저장 완료 → {page_dir}")

        result_json["pages"].append({
            "page": idx,
            "file": os.path.basename(file_path),
            "num_staff_sets": len(staff_sets),
            "staff_sets": staff_sets
        })

        info(f"PAGE {idx} 처리 완료 — 소요시간: {time.time() - page_start:.2f}초")

    json_path = os.path.join(session_path, "staff.json")
    with open(json_path, "w", encoding="utf-8") as f:
        json.dump(result_json, f, indent=4)

    info(f"최종 staff.json 저장 → {json_path}")
    info(f"전체 처리 시간: {time.time() - total_start:.2f}초")

    step("=== 전체 프로세스 완료 ===")
    return session_path


# =====================================================
# 실제 실행
# =====================================================
if __name__ == "__main__":
    import sys

    if len(sys.argv) < 2:
        print("Usage: python staff_detect.py \"path/to/*.png\"")
        exit()

    pattern = sys.argv[1]
    process_images(pattern)
