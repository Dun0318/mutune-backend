import os
import uuid
from datetime import datetime
from pdf2image import convert_from_path


# ============================================================
#  PDF → PNG 변환 (Mutune 공식 파이프라인 v2) — 경로 확정 버전
# ============================================================
def pdf_to_png(pdf_path,
               output_base="D:/Mutune/backend/uploads/input",
               dpi=300):

    # 0) UUID & 타임스탬프 생성
    uid = uuid.uuid4().hex[:8]
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")

    # 1) 최종 저장 폴더 생성
    folder_name = f"Input_{uid}_{timestamp}"
    save_dir = os.path.join(output_base, folder_name)
    os.makedirs(save_dir, exist_ok=True)

    print(f"[PDF→PNG] 변환 시작: {pdf_path}")
    print(f"[저장 경로] {save_dir}")

    # 2) PDF → 이미지 변환
    try:
        pages = convert_from_path(pdf_path, dpi=dpi)
    except Exception as e:
        print(f"[ERROR] PDF 변환 실패: {e}")
        return None, []
    
    total_pages = len(pages)
    result_paths = []

    # 3) 페이지별 PNG 저장
    for idx, page in enumerate(pages, start=1):
        filename = f"{uid}_{timestamp}_{idx}_{total_pages}.png"
        out_path = os.path.join(save_dir, filename)

        page.save(out_path, "PNG")
        result_paths.append(out_path)

    print(f"[PDF→PNG] 변환 완료 ({total_pages} pages)")
    return save_dir, result_paths


# ============================================================
#  독립 실행
# ============================================================
if __name__ == "__main__":
    import sys
    if len(sys.argv) < 2:
        print("Usage: python pdf_to_png.py C:\\path\\to\\file.pdf")
        exit()

    folder, paths = pdf_to_png(sys.argv[1])
    print("\n[OUTPUT] 생성된 폴더:", folder)
    for p in paths:
        print(" -", p)
