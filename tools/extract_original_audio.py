#!/usr/bin/env python3
"""
Extract original PushPush MP3 sounds from original/game.swf.

Usage from the project root on Windows:
    py tools\\extract_original_audio.py

The script writes:
    app/src/main/res/raw/success.mp3
    app/src/main/res/raw/start.mp3
    app/src/main/res/raw/move.mp3
    app/src/main/res/raw/clear.mp3
    app/src/main/res/raw/button.mp3
"""

from __future__ import annotations

import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
INPUT = ROOT / "original" / "game.swf"
OUTPUT = ROOT / "app" / "src" / "main" / "res" / "raw"

SOUND_NAMES = {
    1: "success",
    2: "start",
    3: "move",
    4: "clear",
    5: "button",
}


class BitReader:
    def __init__(self, data: bytes, pos: int = 0) -> None:
        self.data = data
        self.pos = pos
        self.bit = 0

    def read_bits(self, count: int) -> int:
        value = 0
        for _ in range(count):
            value = (value << 1) | (
                (self.data[self.pos] >> (7 - self.bit)) & 1
            )
            self.bit += 1
            if self.bit == 8:
                self.bit = 0
                self.pos += 1
        return value

    def align(self) -> None:
        if self.bit:
            self.bit = 0
            self.pos += 1

    def u16(self) -> int:
        self.align()
        value = struct.unpack_from("<H", self.data, self.pos)[0]
        self.pos += 2
        return value


def unpack_swf(data: bytes) -> bytes:
    signature = data[:3]

    if signature == b"FWS":
        return data

    if signature == b"CWS":
        body = zlib.decompress(data[8:])
        return b"FWS" + data[3:8] + body

    raise RuntimeError(
        f"Unsupported SWF signature: {signature!r}"
    )


def tag_start(swf: bytes) -> int:
    reader = BitReader(swf, 8)

    nbits = reader.read_bits(5)
    reader.read_bits(nbits)
    reader.read_bits(nbits)
    reader.read_bits(nbits)
    reader.read_bits(nbits)
    reader.align()

    # FrameRate UI16 + FrameCount UI16
    return reader.pos + 4


def iter_tags(swf: bytes, pos: int):
    while pos + 2 <= len(swf):
        record = struct.unpack_from("<H", swf, pos)[0]
        pos += 2

        code = record >> 6
        length = record & 0x3F

        if length == 0x3F:
            length = struct.unpack_from("<I", swf, pos)[0]
            pos += 4

        payload = swf[pos : pos + length]
        pos += length

        yield code, payload

        if code == 0:
            break


def extract() -> None:
    if not INPUT.exists():
        raise FileNotFoundError(
            "original/game.swf not found. "
            "Place the original SWF in the original folder first."
        )

    swf = unpack_swf(INPUT.read_bytes())
    OUTPUT.mkdir(parents=True, exist_ok=True)

    found = set()

    for code, payload in iter_tags(swf, tag_start(swf)):
        # DefineSound
        if code != 14 or len(payload) < 9:
            continue

        sound_id = struct.unpack_from("<H", payload, 0)[0]

        if sound_id not in SOUND_NAMES:
            continue

        packed = payload[2]
        sound_format = (packed >> 4) & 0x0F

        if sound_format != 2:
            raise RuntimeError(
                f"Sound {sound_id} is not MP3 (format={sound_format})."
            )

        # DefineSound:
        # UI16 id
        # packed format/rate/size/type
        # UI32 sample count
        # SI16 MP3 seek samples
        # MP3 frames...
        mp3_data = payload[9:]

        name = SOUND_NAMES[sound_id]
        output_path = OUTPUT / f"{name}.mp3"
        output_path.write_bytes(mp3_data)
        found.add(sound_id)

        print(
            f"[OK] {name}.mp3 "
            f"({len(mp3_data):,} bytes)"
        )

    missing = set(SOUND_NAMES) - found

    if missing:
        names = ", ".join(SOUND_NAMES[i] for i in sorted(missing))
        raise RuntimeError(
            f"Could not find expected sounds: {names}"
        )

    print()
    print("Original PushPush audio extraction complete.")
    print(f"Output: {OUTPUT}")


if __name__ == "__main__":
    extract()
