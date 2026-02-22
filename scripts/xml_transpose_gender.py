# -*- coding: utf-8 -*-
import sys
from xml_transpose_key import to_midi, from_midi
import xml.etree.ElementTree as ET

"""
MuTune — Gender-based Transpose (최종 개선판)

사용법:
    python xml_transpose_gender.py <input_xml> <output_xml> <genderMode>
"""

if len(sys.argv) != 4:
    print("사용법: python xml_transpose_gender.py <input_xml> <output_xml> <genderMode>")
    sys.exit(1)

input_xml = sys.argv[1]
output_xml = sys.argv[2]
genderMode = sys.argv[3].strip().lower()

def gender_shift(mode):
    if mode in ["female", "woman", "여자"]:
        return -3
    if mode in ["male", "man", "남자"]:
        return +3
    if mode in ["female_to_male", "여→남"]:
        return -3
    if mode in ["male_to_female", "남→여"]:
        return +3
    return 0

shift = gender_shift(genderMode)

print(f"[GENDER] 실행 시작: input={input_xml}, output={output_xml}, mode={genderMode}, shift={shift}")

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

print(f"[GENDER] 적용된 음표 수: {count}")

tree.write(output_xml, encoding="UTF-8", xml_declaration=True)
print(f"[GENDER] 저장 완료 → {output_xml}")
