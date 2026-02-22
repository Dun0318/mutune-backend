import sys
import json
import zipfile
import tempfile
import os
import xml.etree.ElementTree as ET


def extract_score_xml_from_mxl(mxl_path: str) -> str:
    """
    MXL 내부에서 score-partwise XML 추출
    """
    tmp_dir = tempfile.mkdtemp(prefix="mxl_")

    with zipfile.ZipFile(mxl_path, "r") as z:
        candidates = [f for f in z.namelist() if f.endswith(".xml")]

        if not candidates:
            raise RuntimeError("MXL 내부에 XML 파일이 없음")

        for name in candidates:
            extracted = z.extract(name, tmp_dir)
            try:
                tree = ET.parse(extracted)
                root = tree.getroot()
                if "score-partwise" in root.tag:
                    return extracted
            except Exception:
                continue

    raise RuntimeError("score-partwise XML을 찾지 못함")


def parse_musicxml(xml_path: str) -> dict:
    tree = ET.parse(xml_path)
    root = tree.getroot()

    ns = {}
    if root.tag.startswith("{"):
        ns["m"] = root.tag.split("}")[0].strip("{")
        p = "m:"
    else:
        p = ""

    model = {
        "meta": {
            "source": "Audiveris",
            "version": "5.7.1"
        },
        "parts": []
    }

    for part in root.findall(f"{p}part", ns):
        part_data = {
            "id": part.attrib.get("id"),
            "measures": []
        }

        for measure in part.findall(f"{p}measure", ns):
            measure_data = {
                "number": measure.attrib.get("number"),
                "attributes": {
                    "divisions": None,
                    "key": None,
                    "time": None,
                    "clef": None
                },
                "harmonies": [],
                "notes": []
            }

            # attributes (보존만 한다)
            attr = measure.find(f"{p}attributes", ns)
            if attr is not None:
                div = attr.find(f"{p}divisions", ns)
                if div is not None:
                    measure_data["attributes"]["divisions"] = int(div.text)

                key = attr.find(f"{p}key/{p}fifths", ns)
                if key is not None:
                    measure_data["attributes"]["key"] = int(key.text)

                time = attr.find(f"{p}time", ns)
                if time is not None:
                    beats = time.find(f"{p}beats", ns)
                    beat_type = time.find(f"{p}beat-type", ns)
                    if beats is not None and beat_type is not None:
                        measure_data["attributes"]["time"] = {
                            "beats": beats.text,
                            "beat_type": beat_type.text
                        }

                clef = attr.find(f"{p}clef/{p}sign", ns)
                if clef is not None:
                    measure_data["attributes"]["clef"] = clef.text

            # harmony (코드)
            for harmony in measure.findall(f"{p}harmony", ns):
                root_step = harmony.findtext(f"{p}root/{p}root-step", default="", namespaces=ns)
                kind = harmony.findtext(f"{p}kind", default="", namespaces=ns)

                measure_data["harmonies"].append({
                    "root": root_step,
                    "kind": kind,
                    "bass": None
                })

            # notes
            for note in measure.findall(f"{p}note", ns):
                note_data = {
                    "isRest": note.find(f"{p}rest", ns) is not None,
                    "pitch": None,
                    "duration": int(note.findtext(f"{p}duration", default="0", namespaces=ns)),
                    "voice": int(note.findtext(f"{p}voice", default="1", namespaces=ns)),
                    "staff": int(note.findtext(f"{p}staff", default="1", namespaces=ns)),
                    "lyrics": []
                }

                pitch = note.find(f"{p}pitch", ns)
                if pitch is not None:
                    note_data["pitch"] = {
                        "step": pitch.findtext(f"{p}step", default="", namespaces=ns),
                        "alter": int(pitch.findtext(f"{p}alter", default="0", namespaces=ns)),
                        "octave": int(pitch.findtext(f"{p}octave", default="0", namespaces=ns))
                    }

                # lyrics (다국어 대응)
                for lyric in note.findall(f"{p}lyric", ns):
                    text = lyric.findtext(f"{p}text", default="", namespaces=ns)
                    lang = lyric.attrib.get("{http://www.w3.org/XML/1998/namespace}lang", "unknown")

                    note_data["lyrics"].append({
                        "text": text,
                        "lang": lang
                    })

                measure_data["notes"].append(note_data)

            part_data["measures"].append(measure_data)

        model["parts"].append(part_data)

    return model


def main():
    if len(sys.argv) != 2:
        print("Usage: python xml_to_json.py <Preprocessed_Score.mxl>")
        sys.exit(1)

    mxl_path = sys.argv[1]
    if not os.path.exists(mxl_path):
        print("파일이 존재하지 않음")
        sys.exit(1)

    base_dir = os.path.dirname(mxl_path)
    output_path = os.path.join(base_dir, "music_model.json")

    xml_path = extract_score_xml_from_mxl(mxl_path)
    model = parse_musicxml(xml_path)

    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(model, f, indent=2, ensure_ascii=False)

    print("music_model.json 생성 완료:", output_path)


if __name__ == "__main__":
    main()
