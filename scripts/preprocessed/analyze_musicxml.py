import sys
from music21 import converter

def analyze_key(xml_path):
    score = converter.parse(xml_path)
    key = score.analyze('key')
    return {
        "tonic": key.tonic.name,
        "mode": key.mode,
        "name": key.name,
        "fifths": key.sharps
    }

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python analyze_musicxml.py <xml_file>")
        sys.exit(1)

    xml_file = sys.argv[1]
    info = analyze_key(xml_file)
    print(info)
