# -*- coding: utf-8 -*-
import sys
import xml.etree.ElementTree as ET

"""
MuTune — XML Key Transpose (최종 개선판)

사용법:
    python xml_transpose_key.py <input_xml> <output_xml> <shift>
"""

if len(sys.argv) != 4:
    print("사용법: python xml_transpose_key.py <input_xml> <output_xml> <shift>")
    sys.exit(1)

input_xml = sys.argv[1]
output_xml = sys.argv[2]
shift = int(sys.argv[3])

print(f"[KEY] 실행 시작: input={input_xml}, output={output_xml}, shift={shift}")

STEP_TO_SEMITONE = {
    "C": 0, "D": 2, "E": 4,
    "F": 5, "G": 7, "A": 9, "B": 11
}

SEMITONE_TO_NOTE = [
    ("C", 0),
    ("C", 1),
    ("D", 0),
    ("D", 1),
    ("E", 0),
    ("F", 0),
    ("F", 1),
    ("G", 0),
    ("G", 1),
    ("A", 0),
    ("A", 1),
    ("B", 0),
]

def to_midi(step, alter, octave):
    semitone = STEP_TO_SEMITONE[step] + alter
    return (octave + 1) * 12 + semitone

def from_midi(midi_num):
    octave = midi_num // 12 - 1
    semitone = midi_num % 12
    step, alter = SEMITONE_TO_NOTE[semitone]
    return step, alter, octave

tree = ET.parse(input_xml)
root = tree.getroot()

count = 0

for pitch in root.iter("pitch"):
    step_el = pitch.find("step")
    alter_el = pitch.find("alter")
    octave_el = pitch.find("octave")

    if step_el is None or octave_el is None:
        continue

    step = step_el.text
    alter = int(alter_el.text) if alter_el is not None else 0
    octave = int(octave_el.text)

    midi = to_midi(step, alter, octave)
    new_midi = midi + shift
    new_step, new_alter, new_octave = from_midi(new_midi)

    step_el.text = new_step
    octave_el.text = str(new_octave)

    if new_alter != 0:
        if alter_el is None:
            alter_el = ET.SubElement(pitch, "alter")
        alter_el.text = str(new_alter)
    else:
        if alter_el is not None:
            pitch.remove(alter_el)

    count += 1

print(f"[KEY] 변환된 음표 수: {count}")

tree.write(output_xml, encoding="UTF-8", xml_declaration=True)
print(f"[KEY] 저장 완료 → {output_xml}")
