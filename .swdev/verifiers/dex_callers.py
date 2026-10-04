#!/usr/bin/env python3
"""
dex_callers: every call site (caller class -> caller method) of the platform sound and haptic APIs in an APK's dex.

Derived from the WO-007 design reviewer's scratch script dex_callers.py (reviews/WO-007-design-review.md, E3;
design WO-007 section 3.7 and the "Suggested cut" V-1 bullet). Used by v08_promise_apk.py (V-08, DA-123/DA-125).
Stdlib only.

Mechanism: class_defs -> class_data_item -> code_item -> instruction stream; every invoke-* (35c, 3rc and
invoke-polymorphic) whose method_id is a target is a hit.

Targets (is_target):
  * by method name in any class: performHapticFeedback (View, ViewCompat, Compose HapticFeedback), playSoundEffect
    (View, AudioManager), playClickSound (Compose SoundEffect); inline-class mangled names ("name-hash") count;
  * any method of android.os.Vibrator*, VibratorManager and VibrationEffect (the "Vibrat" prefix);
  * AudioTrack.play, and any method of SoundPool, MediaPlayer, ToneGenerator, MediaActionSound, Ringtone* and
    android.speech.tts.* (TextToSpeech);
  * AudioManager adjust* and setStreamVolume (they can play the volume beep), View.performClick, performLongClick
    and callOnClick on android.view / android.widget / android.webkit classes (the framework clicks or vibrates).
Other android.media classes play nothing and are NOT targets (NON_PLAYING_MEDIA, named exclusions).

Fail closed (DexError, which the verifier maps to exit 2; never a partial answer): every table offset and size,
string, type, method and class index, class_data uleb128 (max 5 bytes, bounds checked), code_item offset, the
insns_size against the file, an invoke or payload that runs past the end of its code_item, and a method index in
an invoke that is outside method_ids.
"""
import struct
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from v04_release_apk import DexError, parse_dex_strings  # noqa: E402

TARGET_NAMES = {"performHapticFeedback", "playSoundEffect", "playClickSound"}
TARGET_CLASS_PREFIXES = (
    "Landroid/os/Vibrat",          # Vibrator, VibratorManager, VibrationEffect, ...
    "Landroid/media/SoundPool",
    "Landroid/media/MediaPlayer",
    "Landroid/media/ToneGenerator",
    "Landroid/media/Ringtone",     # Ringtone, RingtoneManager
    "Landroid/media/MediaActionSound",
    "Landroid/speech/tts/",        # TextToSpeech speak, playEarcon, synthesizeToFile, ...
)
AUDIO_TRACK = "Landroid/media/AudioTrack;"
AUDIO_MANAGER = "Landroid/media/AudioManager;"
# Framework click and long-click entry points: View.performClick plays the click sound, performLongClick vibrates
# (long-press haptic) and callOnClick runs the listener. Matched on framework classes only (a View subclass such as
# CompoundButton is named as the callee class), so an app-level method that merely shares the name is not a target.
CLICK_NAMES = {"performClick", "performLongClick", "callOnClick"}
# AudioManager methods that can play: playSoundEffect (by name), adjust* and setStreamVolume (FLAG_PLAY_SOUND plays
# the volume beep). Other AudioManager methods (getStreamVolume, requestAudioFocus, ...) play nothing.
# android.media classes that exist in today's APK and play nothing (design V-1 bullet); documented, never targets.
NON_PLAYING_MEDIA = (
    "Landroid/media/ImageReader",
    "Landroid/media/MediaDrm",
    "Landroid/media/ApplicationMediaCapabilities",
    "Landroid/media/AudioAttributes",
    "Landroid/media/AudioFormat",
)


def is_target(cls, name):
    """True when a call of cls->name is a sound or haptic request."""
    base = name.split("-")[0]
    if base in TARGET_NAMES:
        return True
    if cls.startswith(TARGET_CLASS_PREFIXES):
        return True
    if cls == AUDIO_TRACK and base == "play":
        return True
    if cls == AUDIO_MANAGER and (base.startswith("adjust") or base == "setStreamVolume"):
        return True
    return base in CLICK_NAMES and cls.startswith(("Landroid/view/", "Landroid/widget/", "Landroid/webkit/"))


# Instruction length in 16-bit units per opcode (Dalvik format table); every opcode 0x00-0xff is covered.
_LEN = {}


def _fill(a, b, n):
    for op in range(a, b + 1):
        _LEN[op] = n


_fill(0x00, 0x01, 1); _LEN[0x02] = 2; _LEN[0x03] = 3; _LEN[0x04] = 1; _LEN[0x05] = 2; _LEN[0x06] = 3
_LEN[0x07] = 1; _LEN[0x08] = 2; _LEN[0x09] = 3
_fill(0x0A, 0x12, 1); _LEN[0x13] = 2; _LEN[0x14] = 3; _LEN[0x15] = 2; _LEN[0x16] = 2; _LEN[0x17] = 3
_LEN[0x18] = 5; _LEN[0x19] = 2; _LEN[0x1A] = 2; _LEN[0x1B] = 3
_LEN[0x1C] = 2; _fill(0x1D, 0x1E, 1); _fill(0x1F, 0x20, 2); _LEN[0x21] = 1; _fill(0x22, 0x23, 2)
_fill(0x24, 0x26, 3); _LEN[0x27] = 1; _LEN[0x28] = 1; _LEN[0x29] = 2; _LEN[0x2A] = 3
_fill(0x2B, 0x2C, 3); _fill(0x2D, 0x3D, 2); _fill(0x3E, 0x43, 1); _fill(0x44, 0x6D, 2); _fill(0x6E, 0x72, 3)
_LEN[0x73] = 1; _fill(0x74, 0x78, 3); _fill(0x79, 0x7A, 1); _fill(0x7B, 0x8F, 1)
_fill(0x90, 0xAF, 2); _fill(0xB0, 0xCF, 1); _fill(0xD0, 0xE2, 2); _fill(0xE3, 0xF9, 1)
_fill(0xFA, 0xFB, 4); _fill(0xFC, 0xFD, 3); _fill(0xFE, 0xFF, 2)
assert len(_LEN) == 256
INVOKE = set(range(0x6E, 0x73)) | set(range(0x74, 0x79)) | {0xFA, 0xFB}


def _u16(d, o):
    if o < 0 or o + 2 > len(d):
        raise DexError(f"read of 2 bytes at {o:#x} beyond end of file")
    return struct.unpack_from("<H", d, o)[0]


def _u32(d, o):
    if o < 0 or o + 4 > len(d):
        raise DexError(f"read of 4 bytes at {o:#x} beyond end of file")
    return struct.unpack_from("<I", d, o)[0]


def _uleb(d, o):
    r = s = 0
    for _ in range(5):
        if o >= len(d):
            raise DexError("uleb128 runs off the file")
        b = d[o]
        o += 1
        r |= (b & 0x7F) << s
        s += 7
        if b < 0x80:
            return r, o
    raise DexError("uleb128 longer than 5 bytes")


def _table(d, size_off, entry, what):
    n = _u32(d, size_off)
    off = _u32(d, size_off + 4)
    if n and off < 0x70:
        raise DexError(f"{what} table offset {off:#x} inside the header")
    if off + n * entry > len(d):
        raise DexError(f"{what} table beyond end of file")
    return n, off


def find_callers(data):
    """Set of (caller class, caller method, callee class, callee method) for every target call in one dex.
    Raises DexError for anything it cannot trust."""
    strings = parse_dex_strings(data)  # magic, header size, string table bounds
    nstr = len(strings)
    tn, toff = _table(data, 0x40, 4, "type_ids")
    types = []
    for i in range(tn):
        idx = _u32(data, toff + 4 * i)
        if idx >= nstr:
            raise DexError(f"type {i} descriptor index {idx} out of range")
        types.append(strings[idx])
    mn, moff = _table(data, 0x58, 8, "method_ids")
    meths = []
    for i in range(mn):
        c = _u16(data, moff + 8 * i)
        n = _u32(data, moff + 8 * i + 4)
        if c >= tn or n >= nstr:
            raise DexError(f"method {i} class or name index out of range")
        meths.append((types[c], strings[n]))
    targets = {i for i, (c, n) in enumerate(meths) if is_target(c, n)}
    cn, coff = _table(data, 0x60, 32, "class_defs")
    hits = set()
    for ci in range(cn):
        cls_idx = _u32(data, coff + 32 * ci)
        cdata = _u32(data, coff + 32 * ci + 24)
        if cls_idx >= tn:
            raise DexError(f"class_def {ci} class_idx out of range")
        if not cdata:
            continue
        if cdata >= len(data):
            raise DexError(f"class_def {ci} class_data_off beyond end of file")
        o = cdata
        sf, o = _uleb(data, o)
        inf, o = _uleb(data, o)
        dm, o = _uleb(data, o)
        vm, o = _uleb(data, o)
        if sf + inf + dm + vm > len(data):
            raise DexError(f"class_data of class {ci} claims more members than bytes in the file")
        for _ in range(sf + inf):
            _, o = _uleb(data, o)
            _, o = _uleb(data, o)
        for count in (dm, vm):
            midx = 0
            for _ in range(count):
                diff, o = _uleb(data, o)
                midx += diff
                _, o = _uleb(data, o)
                code, o = _uleb(data, o)
                if midx >= mn:
                    raise DexError(f"class {ci} method index {midx} out of range")
                if code:
                    _scan_code(data, code, types[cls_idx], meths[midx][1], meths, targets, hits)
    return hits


def _scan_code(d, code, caller_cls, caller_name, meths, targets, hits):
    ins_size = _u32(d, code + 12)
    base = code + 16
    if base + 2 * ins_size > len(d):
        raise DexError(f"code_item of {caller_cls}->{caller_name}: insns_size {ins_size} beyond end of file")
    pc = 0
    while pc < ins_size:
        u = _u16(d, base + 2 * pc)
        op = u & 0xFF
        if op == 0 and u in (0x0100, 0x0200, 0x0300):  # packed-switch, sparse-switch, fill-array payloads
            if pc + 2 > ins_size:
                raise DexError(f"payload header runs past code_item in {caller_cls}->{caller_name}")
            if u == 0x0100:
                step = _u16(d, base + 2 * pc + 2) * 2 + 4
            elif u == 0x0200:
                step = _u16(d, base + 2 * pc + 2) * 4 + 2
            else:
                if pc + 4 > ins_size:
                    raise DexError(f"payload header runs past code_item in {caller_cls}->{caller_name}")
                width = _u16(d, base + 2 * pc + 2)
                size = _u32(d, base + 2 * pc + 4)
                step = (size * width + 1) // 2 + 4
        else:
            step = _LEN[op]
        if pc + step > ins_size:
            raise DexError(f"instruction at unit {pc} runs past the end of {caller_cls}->{caller_name}")
        if op in INVOKE:
            t = _u16(d, base + 2 * pc + 2)
            if t >= len(meths):
                raise DexError(f"invoke in {caller_cls}->{caller_name} names method index {t} out of range")
            if t in targets:
                hits.add((caller_cls, caller_name, meths[t][0], meths[t][1]))
        pc += step


def scan_apk(apk):
    """Union of find_callers over every classes*.dex of an APK (DexError on any bad dex)."""
    import re
    import zipfile
    hits = set()
    with zipfile.ZipFile(apk) as z:
        for name in sorted(n for n in z.namelist() if re.match(r"^classes\d*\.dex$", n)):
            hits |= find_callers(z.read(name))
    return hits


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print(__doc__)
        sys.exit(2)
    try:
        found = sorted(scan_apk(sys.argv[1]))
    except Exception as e:
        print(f"dex_callers ERROR {type(e).__name__}: {e}")
        sys.exit(2)
    for c, m, ec, en in found:
        print(f"{c} {m}  ->  {ec}->{en}")
    print(len(found), "distinct call sites")
