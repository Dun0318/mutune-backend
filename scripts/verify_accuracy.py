# -*- coding: utf-8 -*-
import os
import traceback
from music21 import converter, note, stream, meter, key

OUTPUT_DIR = r"D:\Mutune\backend\uploads\output\test"

def analyze_musicxml(file_path):
    print(f"\n[DEBUG] Analyzing file: {file_path}")
    try:
        score = converter.parse(file_path)
        total_notes = len(list(score.recurse().getElementsByClass(note.Note)))
        measures = len(list(score.parts[0].getElementsByClass(stream.Measure)))

        time_signatures = list(score.recurse().getElementsByClass(meter.TimeSignature))
        key_signatures = list(score.recurse().getElementsByClass(key.KeySignature))

        print(f"  ➤ 감지된 박자표: {[str(ts.ratioString) for ts in time_signatures]}")
        print(f"  ➤ 감지된 조표: {[str(ks.sharps) for ks in key_signatures]}")

        return {
            "file": os.path.basename(file_path),
            "notes": total_notes,
            "measures": measures,
            "time_sigs": len(time_signatures),
            "key_sigs": len(key_signatures),
            "time_list": [ts.ratioString for ts in time_signatures],
            "key_list": [ks.sharps for ks in key_signatures],
        }
    except Exception as e:
        print(f"  ⚠️ 예외 발생: {e}")
        traceback.print_exc()
        return {"file": os.path.basename(file_path), "error": str(e)}

def calculate_accuracy(stats):
    if "error" in stats:
        return 0
    score = 0
    if stats["notes"] > 150:
        score += 45
    elif stats["notes"] > 100:
        score += 40
    elif stats["notes"] > 50:
        score += 30

    if stats["measures"] > 20:
        score += 30
    elif stats["measures"] > 10:
        score += 25

    if stats["time_sigs"] >= 1:
        score += 15
    if stats["key_sigs"] >= 1:
        score += 10
    return min(score, 100)

def run_verification():
    print("🎼 Audiveris 변환 결과 디버깅 모드 시작\n")
    results = []

    for file in os.listdir(OUTPUT_DIR):
        if file.endswith(".mxl"):
            full_path = os.path.join(OUTPUT_DIR, file)
            stats = analyze_musicxml(full_path)
            accuracy = calculate_accuracy(stats)
            results.append((file, accuracy, stats))

    print("\n===== RESULT =====")
    for file, acc, s in results:
        if "error" in s:
            print(f" ❌ {file}: 오류 ({s['error']})")
        else:
            status = (
                "✅ 통과 (정확도 97% 이상)"
                if acc >= 97
                else "⚠️ 보통 (90% 이상)"
                if acc >= 90
                else "❌ 정확도 부족"
            )
            print(f"{status}")
            print(f"  파일명: {file}")
            print(f"  정확도: {acc}%")
            print(f"  음표 수: {s['notes']}")
            print(f"  마디 수: {s['measures']}")
            print(f"  박자표 감지: {s['time_sigs']}개 → {s.get('time_list')}")
            print(f"  조표 감지: {s['key_sigs']}개 → {s.get('key_list')}")
            print("-------------------------")

if __name__ == "__main__":
    run_verification()
