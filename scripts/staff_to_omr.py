import json
import os
from datetime import datetime

# ------------------------------------------------------
# MuTune staff_lines.json → Audiveris sheet.omr 변환기
# ------------------------------------------------------

TEMPLATE_HEAD = """<?xml version="1.0" encoding="UTF-8"?>
<omr xmlns="http://www.audiveris.org/omr">
  <creation>{timestamp}</creation>
  <staves>
"""

TEMPLATE_STAFF = """
    <staff>
      <lines>
{lines}
      </lines>
    </staff>
"""

TEMPLATE_LINE = """        <line y="{y}" />"""

TEMPLATE_FOOTER = """
  </staves>
</omr>
"""


def convert_staff_json_to_omr(json_path, omr_output_path):
    # Load json
    with open(json_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    staff_blocks = data["staff_blocks"]

    # Start assembling XML
    xml_parts = []
    xml_parts.append(TEMPLATE_HEAD.format(timestamp=datetime.now().strftime("%Y-%m-%d %H:%M:%S")))

    for block in staff_blocks:
        # block = [y1,y2,y3,y4,y5]
        lines_xml = "\n".join([TEMPLATE_LINE.format(y=y) for y in block])
        xml_parts.append(TEMPLATE_STAFF.format(lines=lines_xml))

    xml_parts.append(TEMPLATE_FOOTER)

    xml_string = "".join(xml_parts)

    # Save
    with open(omr_output_path, "w", encoding="utf-8") as f:
        f.write(xml_string)

    print(f"[INFO] sheet.omr 생성 완료 → {omr_output_path}")


if __name__ == "__main__":
    import sys
    if len(sys.argv) < 3:
        print("Usage: python staff_to_omr.py staff_lines.json output.omr")
        exit()

    convert_staff_json_to_omr(sys.argv[1], sys.argv[2])

