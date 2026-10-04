"""Self-test for tools/device_reset.py (TASK-042, H-1, DA-108). SCAFFOLDING: disposable, never calls a real adb.

A FakeDevice stands in for adb: it answers the commands the script issues from a small state dict, with canned
`wm` output, and records every command so the per-API differences can be asserted.
"""
import io
import re
import sys
import unittest
from pathlib import Path

TOOLS = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(TOOLS))
import device_reset as dr  # noqa: E402

# N8 (CR-5): the five-row DisplaySpec table is parsed out of the design itself, never typed here.
DESIGN = Path(__file__).resolve().parents[2] / "designs" / "WO-006-design.md"
_ROW = re.compile(r"^\|\s*`((?:PHONE|TABLET)_\w+)`[^|]*\|\s*(\d+) × (\d+)\s*\|\s*(\d+)\s*\|\s*(\d+)x(\d+)\s*\|", re.M)


def parse_design_table():
    """name -> dict(widthDp, heightDp, densityDpi, widthPx, heightPx), from section 1 of the design. Fails loudly."""
    if not DESIGN.is_file():
        raise AssertionError(f"design file not found: {DESIGN}")
    rows = _ROW.findall(DESIGN.read_text(encoding="utf-8"))
    if len(rows) != 5:
        raise AssertionError(f"expected 5 DisplaySpec rows in {DESIGN}, parsed {len(rows)}")
    return {n: dict(widthDp=int(a), heightDp=int(b), densityDpi=int(c), widthPx=int(d), heightPx=int(e))
            for n, a, b, c, d, e in rows}


DESIGN_DISPLAY_SPEC_TABLE = parse_design_table()


class FakeDevice:
    """Simulates adb for one AVD. `state` holds the device globals; `log` the shell commands run."""

    def __init__(self, avd, sdk, nav=None, reachable=True, refuse_overlay=False):
        self.avd, self.sdk, self.reachable, self.refuse_overlay = avd, sdk, reachable, refuse_overlay
        self.size_override = None
        self.density_override = None
        self.s = {"font_scale": "1.0", "accelerometer_rotation": "1", "user_rotation": "0",
                  "airplane_mode_on": "0", "wifi_on": "1", "mobile_data": "1", "navigation_mode": nav}
        self.log = []
        self.fail_commands = set()   # joined shell commands that return exit 1 (a refused command)

    def __call__(self, args):
        if not self.reachable:
            return 1, "error: device offline"
        if args[:3] == ["emu", "avd", "name"]:
            return 0, f"{self.avd}\r\nOK\r\n"
        assert args[0] == "shell", args
        cmd = args[1:]
        joined = " ".join(cmd)
        self.log.append(joined)
        if joined in self.fail_commands:
            return 1, "denied"
        s = self.s
        if cmd[:2] == ["getprop", "ro.build.version.sdk"]:
            return 0, f"{self.sdk}\n"
        if cmd[:2] == ["wm", "size"]:
            if len(cmd) == 2:
                o = "Physical size: 1080x2400\n"
                return 0, o + (f"Override size: {self.size_override}\n" if self.size_override else "")
            self.size_override = None if cmd[2] == "reset" else cmd[2]
            return 0, ""
        if cmd[:2] == ["wm", "density"]:
            if len(cmd) == 2:
                o = "Physical density: 420\n"
                return 0, o + (f"Override density: {self.density_override}\n" if self.density_override else "")
            self.density_override = None if cmd[2] == "reset" else cmd[2]
            return 0, ""
        if cmd[:2] == ["settings", "get"]:
            v = s.get(cmd[3])
            return 0, ("null" if v is None else v) + "\n"
        if cmd[:2] == ["settings", "put"]:
            s[cmd[3]] = cmd[4]
            return 0, ""
        if cmd[:2] == ["cmd", "overlay"]:
            if self.refuse_overlay:
                return 1, "denied"
            cat = cmd[-1]
            s["navigation_mode"] = str({v: k for k, v in dr.NAV_CATEGORY.items()}[cat])
            return 0, ""
        if joined == "cmd connectivity airplane-mode disable":
            s["airplane_mode_on"] = "0"
            return 0, ""
        if joined == "svc wifi enable":
            s["wifi_on"] = "1"
            return 0, ""
        if joined == "svc data enable":
            s["mobile_data"] = "1"
            return 0, ""
        if cmd[0] == "am":
            return 0, "Broadcast completed: result=0\n"
        raise AssertionError(f"unexpected adb command: {joined}")


def run(dev, **kw):
    buf = io.StringIO()
    code = dr.run("emulator-5554", runner=dev, out=lambda m: buf.write(m + "\n"), **kw)
    return code, buf.getvalue()


def api37(nav="0"):
    return FakeDevice("Medium_Phone_API_37.0", 37, nav=nav)


def api26():
    return FakeDevice("Phone_API_26", 26)


class DeviceResetTest(unittest.TestCase):
    def setUp(self):
        # the tests set the API 37 nav baseline to 0 for the leak cases and restore the real constant afterwards.
        self._saved = dict(dr.AVD_NAV_BASELINE)
        dr.AVD_NAV_BASELINE["Medium_Phone_API_37.0"] = 0
        self._settle = dr.NAV_SETTLE_SECONDS
        dr.NAV_SETTLE_SECONDS = 0
        self.addCleanup(lambda: setattr(dr, "NAV_SETTLE_SECONDS", self._settle))
        self.addCleanup(lambda: (dr.AVD_NAV_BASELINE.clear(), dr.AVD_NAV_BASELINE.update(self._saved)))

    # ---- clean device
    def test_clean_api37_check_exits_0(self):
        code, out = run(api37(), check_only=True)
        self.assertEqual(code, 0, out)
        self.assertIn("PASS", out)

    def test_clean_api26_check_exits_0(self):
        code, out = run(api26(), check_only=True)
        self.assertEqual(code, 0, out)

    # ---- each single leftover -> 1, listing exactly it
    def leftover_lines(self, out):
        return [l.strip() for l in out.splitlines() if l.startswith("  ")]

    def assert_single(self, dev, prefix):
        code, out = run(dev, check_only=True)
        self.assertEqual(code, 1, out)
        lines = self.leftover_lines(out)
        self.assertEqual(len(lines), 1, out)
        self.assertTrue(lines[0].startswith(prefix), lines)
        self.assertIn("1 leftover(s)", out)

    def test_leftover_display_size(self):
        d = api37(); d.size_override = "900x1440"
        self.assert_single(d, "display size override: 900x1440")

    def test_leftover_display_density(self):
        d = api37(); d.density_override = "240"
        self.assert_single(d, "display density override: 240")

    def test_leftover_font_scale(self):
        d = api37(); d.s["font_scale"] = "1.3"
        self.assert_single(d, "font_scale: 1.3")

    def test_leftover_accelerometer_rotation(self):
        d = api37(); d.s["accelerometer_rotation"] = "0"
        self.assert_single(d, "accelerometer_rotation: 0")

    def test_clean_api37_with_accelerometer_rotation_1(self):
        d = api37()
        self.assertEqual(d.s["accelerometer_rotation"], "1")
        code, out = run(d, check_only=True)
        self.assertEqual(code, 0, out)

    def test_null_means_baseline_where_platform_default(self):
        d = api26(); d.s["font_scale"] = None; d.s["user_rotation"] = None
        code, out = run(d, check_only=True)
        self.assertEqual(code, 0, out)

    def test_null_accelerometer_is_a_leftover(self):
        d = api26(); d.s["accelerometer_rotation"] = None
        self.assert_single(d, "accelerometer_rotation: null")

    def test_reset_writes_baseline_rotation_not_zero(self):
        d = api37(); d.s["accelerometer_rotation"] = "0"; run(d)
        self.assertEqual(d.s["accelerometer_rotation"], "1")
        self.assertIn("settings put system accelerometer_rotation 1", d.log)

    def test_leftover_user_rotation(self):
        d = api37(); d.s["user_rotation"] = "1"
        self.assert_single(d, "user_rotation: 1")

    def test_leftover_navigation_mode_api37(self):
        self.assert_single(api37(nav="2"), "navigation_mode: 2")

    def test_leftover_airplane_api37(self):
        d = api37(); d.s["airplane_mode_on"] = "1"
        self.assert_single(d, "airplane_mode_on: 1")

    def test_leftover_airplane_api26(self):
        d = api26(); d.s["airplane_mode_on"] = "1"
        self.assert_single(d, "airplane_mode_on: 1")

    def test_leftover_wifi_radio_api26(self):
        d = api26(); d.s["wifi_on"] = "0"
        self.assert_single(d, "wifi radio: off")

    def test_leftover_data_radio_api26(self):
        d = api26(); d.s["mobile_data"] = "0"
        self.assert_single(d, "mobile data radio: off")

    def test_radios_not_checked_on_api37(self):
        d = api37(); d.s["wifi_on"] = "0"; d.s["mobile_data"] = "0"
        code, out = run(d, check_only=True)
        self.assertEqual(code, 0, out)

    # ---- several leftovers -> all listed
    def test_several_leftovers_all_listed(self):
        d = api26()
        d.size_override = "900x1440"; d.density_override = "240"; d.s["font_scale"] = "1.3"
        d.s["airplane_mode_on"] = "1"; d.s["wifi_on"] = "0"; d.s["mobile_data"] = "0"
        code, out = run(d, check_only=True)
        self.assertEqual(code, 1)
        lines = self.leftover_lines(out)
        self.assertEqual(len(lines), 6, out)
        for want in ("display size override", "display density override", "font_scale", "airplane_mode_on",
                     "wifi radio", "mobile data radio"):
            self.assertTrue(any(l.startswith(want) for l in lines), (want, lines))
        self.assertIn("6 leftover(s)", out)

    def test_check_does_not_modify(self):
        d = api37(); d.size_override = "900x1440"
        run(d, check_only=True)
        self.assertEqual(d.size_override, "900x1440")
        self.assertFalse(any(c.startswith(("wm size reset", "settings put", "cmd ")) for c in d.log), d.log)

    # ---- reset then verify
    def test_reset_cleans_all_leaks_api37(self):
        d = api37(); d.size_override = "900x1440"; d.density_override = "240"; d.s["font_scale"] = "1.3"
        d.s["airplane_mode_on"] = "1"; d.s["navigation_mode"] = "2"; d.s["user_rotation"] = "3"
        d.s["accelerometer_rotation"] = "0"
        code, out = run(d)
        self.assertEqual(code, 0, out)
        self.assertIsNone(d.size_override); self.assertIsNone(d.density_override)
        self.assertEqual(d.s["navigation_mode"], "0")

    def test_reset_cleans_all_leaks_api26(self):
        d = api26(); d.size_override = "900x1440"; d.s["airplane_mode_on"] = "1"
        d.s["wifi_on"] = "0"; d.s["mobile_data"] = "0"; d.s["font_scale"] = "1.3"
        code, out = run(d)
        self.assertEqual(code, 0, out)

    def test_reset_that_cannot_fix_reports_leftover(self):
        d = api37(nav="2"); d.refuse_overlay = True
        code, out = run(d)
        self.assertEqual(code, 1, out)
        self.assertEqual(len(self.leftover_lines(out)), 1, out)
        self.assertIn("navigation_mode: 2", out)

    # ---- unreachable and configuration -> 2
    def test_unreachable_device_exits_2(self):
        d = api37(); d.reachable = False
        code, out = run(d, check_only=True)
        self.assertEqual(code, 2, out)
        self.assertIn("cannot reach", out)

    def test_runner_exception_exits_2(self):
        def boom(args):
            raise dr.DeviceUnreachable("adb missing")
        code, _ = run(boom)
        self.assertEqual(code, 2)

    def test_unknown_avd_exits_2_naming_the_constant(self):
        code, out = run(FakeDevice("Some_Other_AVD", 34, nav="0"), check_only=True)
        self.assertEqual(code, 2, out)
        self.assertIn("AVD_NAV_BASELINE", out)
        self.assertIn("Some_Other_AVD", out)

    def test_unfilled_api37_constant_exits_2_naming_it(self):
        dr.AVD_NAV_BASELINE["Medium_Phone_API_37.0"] = None
        code, out = run(api37(), check_only=True)
        self.assertEqual(code, 2, out)
        self.assertIn("NAV_MODE_BASELINE_API37", out)

    def test_script_constants_match_live_baselines(self):
        # decision DA-108: values read from the clean AVDs in the live round
        self.assertEqual(dr.NAV_MODE_BASELINE_API37, 2)
        self.assertEqual(dr.NAV_CATEGORY[1], "com.android.internal.systemui.navbar.twobutton")
        self.assertEqual(dr.AVD_ROTATION_BASELINE["Medium_Phone_API_37.0"], (1, 0))
        self.assertEqual(dr.AVD_ROTATION_BASELINE["Phone_API_26"], (1, 0))

    # ---- API 26 airplane path differs from API 37's
    def test_airplane_reset_commands_differ_per_api(self):
        d37 = api37(); run(d37)
        d26 = api26(); run(d26)
        self.assertIn("cmd connectivity airplane-mode disable", d37.log)
        self.assertNotIn("settings put global airplane_mode_on 0", d37.log)
        self.assertNotIn("svc wifi enable", d37.log)
        self.assertNotIn("svc data enable", d37.log)
        self.assertIn("settings put global airplane_mode_on 0", d26.log)
        self.assertIn("am broadcast -a android.intent.action.AIRPLANE_MODE", d26.log)
        self.assertIn("svc wifi enable", d26.log)
        self.assertIn("svc data enable", d26.log)
        self.assertNotIn("cmd connectivity airplane-mode disable", d26.log)

    def test_api26_has_no_overlay_handling(self):
        d = api26(); run(d)
        self.assertFalse(any(c.startswith("cmd overlay") for c in d.log), d.log)
        self.assertFalse(any("navigation_mode" in c for c in d.log), d.log)

    def test_api26_broadcast_refusal_is_tolerated(self):
        d = api26(); d.s["airplane_mode_on"] = "1"
        d.fail_commands.add("am broadcast -a android.intent.action.AIRPLANE_MODE")
        code, out = run(d)
        self.assertEqual(code, 0, out)

    # ---- nav-mode baseline comes from the constant, not from the device
    def test_nav_baseline_from_constant_not_device(self):
        dr.AVD_NAV_BASELINE["Medium_Phone_API_37.0"] = 0
        code, out = run(api37(nav="2"), check_only=True)   # device says 2 (leaked); constant says 0
        self.assertEqual(code, 1, out)
        self.assertIn("navigation_mode: 2 (baseline 0)", out)
        dr.AVD_NAV_BASELINE["Medium_Phone_API_37.0"] = 2
        code, out = run(api37(nav="2"), check_only=True)   # constant now 2: same device is clean
        self.assertEqual(code, 0, out)

    def test_wait_nav_mode_polls_until_settled(self):
        d = api37(nav="0")
        reads = []
        def lagging(args):
            if args[-1] == "navigation_mode":
                reads.append(1)
                return 0, ("2" if len(reads) >= 4 else "0")
            return d(args)
        dr.wait_nav_mode(dr.Device(lagging), 2, seconds=10, sleep=lambda s: None)
        self.assertEqual(len(reads), 4)

    def test_nav_reset_uses_constant_category(self):
        dr.AVD_NAV_BASELINE["Medium_Phone_API_37.0"] = 2
        d = api37(nav="0"); run(d)
        self.assertIn("cmd overlay enable-exclusive --category com.android.internal.systemui.navbar.gestural", d.log)
        self.assertEqual(d.s["navigation_mode"], "2")

    def test_overlay_skipped_when_already_at_baseline(self):
        dr.AVD_NAV_BASELINE["Medium_Phone_API_37.0"] = 2
        d = api37(nav="2"); d.s["font_scale"] = "1.3"
        code, out = run(d)
        self.assertEqual(code, 0, out)
        self.assertFalse(any(c.startswith("cmd overlay") for c in d.log), d.log)

    def test_overlay_issued_when_off_baseline(self):
        dr.AVD_NAV_BASELINE["Medium_Phone_API_37.0"] = 2
        d = api37(nav="0"); run(d)
        self.assertTrue(any(c.startswith("cmd overlay") for c in d.log), d.log)

    def test_airplane_null_is_clean(self):
        d = api37(); d.s["airplane_mode_on"] = None
        code, out = run(d, check_only=True)
        self.assertEqual(code, 0, out)

    # ---- N9: wifi leftover is unresettable on the API 26 image
    def test_wifi_leftover_says_cannot_be_reset_and_still_fails(self):
        d = api26(); d.s["wifi_on"] = "0"
        d.fail_commands.add("svc wifi enable")   # svc wifi is killed on this image
        code, out = run(d)   # reset tries, cannot fix
        self.assertEqual(code, 1, out)
        self.assertIn("wifi radio: off (baseline on) (cannot be reset on this image)", out)
        self.assertIn("svc wifi enable", d.log)   # reset keeps trying, harmlessly

    def test_docstring_documents_svc_wifi_limit(self):
        self.assertIn("svc wifi", dr.__doc__)
        self.assertIn("137", dr.__doc__)

    # ---- --display
    def test_display_applies_and_verifies(self):
        d = api37()
        code, out = run(d, display="TABLET_600x960")
        self.assertEqual(code, 0, out)
        self.assertEqual(d.size_override, "900x1440")
        self.assertEqual(d.density_override, "240")
        self.assertIn("wm size 900x1440", d.log)
        self.assertIn("wm density 240", d.log)

    def test_display_mismatch_exits_1(self):
        d = api37()
        d.fail_commands.add("wm density 400")
        code, out = run(d, display="PHONE_390x844")
        self.assertEqual(code, 1, out)
        self.assertIn("display density", out)

    # ---- N4: the five display specs equal the design's DisplaySpec table
    def test_display_specs_equal_design_table(self):
        # Source: designs/WO-006-design.md rev 2, section 1, the five-spec table (DisplaySpec, seam row).
        self.assertEqual(set(dr.DISPLAY_SPECS), set(DESIGN_DISPLAY_SPEC_TABLE))
        for name, want in DESIGN_DISPLAY_SPEC_TABLE.items():
            wdp, hdp, dens, wpx, hpx = dr.DISPLAY_SPECS[name]
            got = dict(widthDp=wdp, heightDp=hdp, densityDpi=dens, widthPx=wpx, heightPx=hpx)
            self.assertEqual(got, want, name)

    def test_design_table_parser_fails_loudly_when_rows_missing(self):
        global DESIGN
        old = DESIGN
        DESIGN = Path(__file__)   # this file has no spec table rows... except its own regex text, which is not a row
        try:
            with self.assertRaises(AssertionError):
                parse_design_table()
        finally:
            DESIGN = old
        self.assertEqual(len(parse_design_table()), 5)

    def test_design_table_is_self_consistent(self):
        # px = dp x density / 160 (design section 1)
        for name, v in DESIGN_DISPLAY_SPEC_TABLE.items():
            self.assertEqual(v["widthDp"] * v["densityDpi"] / 160, v["widthPx"], name)
            self.assertEqual(v["heightDp"] * v["densityDpi"] / 160, v["heightPx"], name)


if __name__ == "__main__":
    unittest.main()
