#!/usr/bin/env python3
"""
Self-tests for V-01, V-05, V-06 verifiers.
Proves each verifier CAN FAIL by using fixtures.

Run with: python -m unittest discover -s .swdev/verifiers -p test_verifiers.py
"""

import unittest
import sys
import os
import subprocess
import tempfile
import shutil
import struct
from pathlib import Path


class TestV01ReleaseManifest(unittest.TestCase):
    """Tests for V-01 verifier."""

    @classmethod
    def setUpClass(cls):
        """Find the fixtures directory."""
        fixtures_dir = Path(__file__).parent / "fixtures"
        cls.fixtures_dir = fixtures_dir

    def test_v01_fails_with_uses_permission(self):
        """V-01 should fail when uses-permission is present."""
        manifest = self.fixtures_dir / "manifest_uses_permission.xml"

        with tempfile.TemporaryDirectory() as tmpdir:
            res_dir = Path(tmpdir) / "res"
            xml_dir = res_dir / "xml"
            xml_dir.mkdir(parents=True)

            from v01_release_manifest import check_manifest
            result = check_manifest(str(manifest), str(res_dir))
            self.assertFalse(result, "V-01 should fail with uses-permission")

    def test_v01_fails_with_permission(self):
        """V-01 should fail when permission element is present."""
        manifest = self.fixtures_dir / "manifest_permission.xml"

        with tempfile.TemporaryDirectory() as tmpdir:
            res_dir = Path(tmpdir) / "res"
            xml_dir = res_dir / "xml"
            xml_dir.mkdir(parents=True)

            rules_file = xml_dir / "data_extraction_rules.xml"
            rules_file.write_text("""<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="root" path="." />
        <exclude domain="file" path="." />
        <exclude domain="database" path="." />
        <exclude domain="sharedpref" path="." />
        <exclude domain="external" path="." />
        <exclude domain="device_root" path="." />
        <exclude domain="device_file" path="." />
        <exclude domain="device_database" path="." />
        <exclude domain="device_sharedpref" path="." />
    </cloud-backup>
    <device-transfer>
        <exclude domain="root" path="." />
        <exclude domain="file" path="." />
        <exclude domain="database" path="." />
        <exclude domain="sharedpref" path="." />
        <exclude domain="external" path="." />
        <exclude domain="device_root" path="." />
        <exclude domain="device_file" path="." />
        <exclude domain="device_database" path="." />
        <exclude domain="device_sharedpref" path="." />
    </device-transfer>
</data-extraction-rules>""")

            from v01_release_manifest import check_manifest
            result = check_manifest(str(manifest), str(res_dir))
            self.assertFalse(result, "V-01 should fail with permission element")

    def test_v01_fails_with_allowbackup_true(self):
        """V-01 should fail when allowBackup is true."""
        manifest = self.fixtures_dir / "manifest_allowbackup_true.xml"

        with tempfile.TemporaryDirectory() as tmpdir:
            res_dir = Path(tmpdir) / "res"
            xml_dir = res_dir / "xml"
            xml_dir.mkdir(parents=True)

            rules_file = xml_dir / "data_extraction_rules.xml"
            rules_file.write_text("""<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="root" path="." />
        <exclude domain="file" path="." />
        <exclude domain="database" path="." />
        <exclude domain="sharedpref" path="." />
        <exclude domain="external" path="." />
        <exclude domain="device_root" path="." />
        <exclude domain="device_file" path="." />
        <exclude domain="device_database" path="." />
        <exclude domain="device_sharedpref" path="." />
    </cloud-backup>
    <device-transfer>
        <exclude domain="root" path="." />
        <exclude domain="file" path="." />
        <exclude domain="database" path="." />
        <exclude domain="sharedpref" path="." />
        <exclude domain="external" path="." />
        <exclude domain="device_root" path="." />
        <exclude domain="device_file" path="." />
        <exclude domain="device_database" path="." />
        <exclude domain="device_sharedpref" path="." />
    </device-transfer>
</data-extraction-rules>""")

            from v01_release_manifest import check_manifest
            result = check_manifest(str(manifest), str(res_dir))
            self.assertFalse(result, "V-01 should fail with allowBackup=true")

    def test_v01_fails_without_extraction_rules(self):
        """V-01 should fail when dataExtractionRules is missing."""
        manifest = self.fixtures_dir / "manifest_no_extraction_rules.xml"

        with tempfile.TemporaryDirectory() as tmpdir:
            res_dir = Path(tmpdir) / "res"

            from v01_release_manifest import check_manifest
            result = check_manifest(str(manifest), str(res_dir))
            self.assertFalse(result, "V-01 should fail without extraction rules")

    def test_v01_fails_with_incomplete_extraction_rules(self):
        """V-01 should fail when extraction rules don't exclude all nine domains."""
        manifest = self.fixtures_dir / "manifest_with_emoji.xml"

        with tempfile.TemporaryDirectory() as tmpdir:
            res_dir = Path(tmpdir) / "res"
            xml_dir = res_dir / "xml"
            xml_dir.mkdir(parents=True)

            incomplete_rules = self.fixtures_dir / "data_extraction_rules_incomplete.xml"
            shutil.copy(incomplete_rules, xml_dir / "data_extraction_rules.xml")

            from v01_release_manifest import check_manifest
            result = check_manifest(str(manifest), str(res_dir))
            self.assertFalse(result, "V-01 should fail with incomplete extraction rules")

    def test_v01_fails_with_emoji_initializer(self):
        """V-01 should fail when EmojiCompatInitializer is present."""
        manifest = self.fixtures_dir / "manifest_with_emoji.xml"

        with tempfile.TemporaryDirectory() as tmpdir:
            res_dir = Path(tmpdir) / "res"
            xml_dir = res_dir / "xml"
            xml_dir.mkdir(parents=True)

            rules_file = xml_dir / "data_extraction_rules.xml"
            rules_file.write_text("""<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="root" path="." />
        <exclude domain="file" path="." />
        <exclude domain="database" path="." />
        <exclude domain="sharedpref" path="." />
        <exclude domain="external" path="." />
        <exclude domain="device_root" path="." />
        <exclude domain="device_file" path="." />
        <exclude domain="device_database" path="." />
        <exclude domain="device_sharedpref" path="." />
    </cloud-backup>
    <device-transfer>
        <exclude domain="root" path="." />
        <exclude domain="file" path="." />
        <exclude domain="database" path="." />
        <exclude domain="sharedpref" path="." />
        <exclude domain="external" path="." />
        <exclude domain="device_root" path="." />
        <exclude domain="device_file" path="." />
        <exclude domain="device_database" path="." />
        <exclude domain="device_sharedpref" path="." />
    </device-transfer>
</data-extraction-rules>""")

            from v01_release_manifest import check_manifest
            result = check_manifest(str(manifest), str(res_dir))
            self.assertFalse(result, "V-01 should fail with EmojiCompatInitializer")

    def test_v01_passes_with_clean_manifest(self):
        """V-01 should pass with a clean manifest."""
        manifest = self.fixtures_dir / "manifest_clean.xml"

        with tempfile.TemporaryDirectory() as tmpdir:
            res_dir = Path(tmpdir) / "res"
            xml_dir = res_dir / "xml"
            xml_dir.mkdir(parents=True)

            rules_file = xml_dir / "data_extraction_rules.xml"
            rules_file.write_text("""<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="root" path="." />
        <exclude domain="file" path="." />
        <exclude domain="database" path="." />
        <exclude domain="sharedpref" path="." />
        <exclude domain="external" path="." />
        <exclude domain="device_root" path="." />
        <exclude domain="device_file" path="." />
        <exclude domain="device_database" path="." />
        <exclude domain="device_sharedpref" path="." />
    </cloud-backup>
    <device-transfer>
        <exclude domain="root" path="." />
        <exclude domain="file" path="." />
        <exclude domain="database" path="." />
        <exclude domain="sharedpref" path="." />
        <exclude domain="external" path="." />
        <exclude domain="device_root" path="." />
        <exclude domain="device_file" path="." />
        <exclude domain="device_database" path="." />
        <exclude domain="device_sharedpref" path="." />
    </device-transfer>
</data-extraction-rules>""")

            from v01_release_manifest import check_manifest
            result = check_manifest(str(manifest), str(res_dir))
            self.assertTrue(result, "V-01 should pass with clean manifest")


class TestV05StringParity(unittest.TestCase):
    """Tests for V-05 verifier."""

    @classmethod
    def setUpClass(cls):
        """Find the fixtures directory."""
        fixtures_dir = Path(__file__).parent / "fixtures"
        cls.fixtures_dir = fixtures_dir
        cls.test_module = fixtures_dir / "test_module"

    def test_v05_fails_with_missing_fi_key(self):
        """V-05 should fail when a key is missing in values-fi."""
        from v05_string_parity import check_module

        result = check_module(self.test_module)
        self.assertFalse(result, "V-05 should fail with missing fi key")

    def test_v05_passes_with_no_strings(self):
        """V-05 should pass when a module has no strings."""
        with tempfile.TemporaryDirectory() as tmpdir:
            module_path = Path(tmpdir)

            from v05_string_parity import check_module
            result = check_module(module_path)
            self.assertTrue(result, "V-05 should pass with no strings")

    def test_v05_respects_translatable_false(self):
        """V-05 should ignore strings with translatable='false'."""
        with tempfile.TemporaryDirectory() as tmpdir:
            module_path = Path(tmpdir)
            res_path = module_path / "src" / "main" / "res"

            for lang_dir in ["values", "values-fi"]:
                (res_path / lang_dir).mkdir(parents=True)
                strings_file = res_path / lang_dir / "strings.xml"
                strings_file.write_text(f"""<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="test">Test</string>
    <string name="untranslatable" translatable="false">Untranslatable</string>
</resources>""")

            from v05_string_parity import check_module
            result = check_module(module_path)
            self.assertTrue(result, "V-05 should ignore translatable=false strings")

    def test_v05_fails_with_malformed_xml(self):
        """V-05 should fail when an XML file is malformed."""
        from v05_string_parity import check_module

        result = check_module(self.test_module)
        self.assertFalse(result, "V-05 should fail with malformed XML")


class TestV06ModuleDeps(unittest.TestCase):
    """Tests for V-06 verifier."""

    def test_v06_fails_with_invalid_dependency(self):
        """V-06 should fail when a module depends on an unapproved module."""
        with tempfile.TemporaryDirectory() as tmpdir:
            project_root = Path(tmpdir)

            play_dir = project_root / "play"
            play_dir.mkdir()
            (play_dir / "build.gradle.kts").write_text("""plugins {
    id("com.android.library")
}
dependencies {
    implementation(project(":kernel"))
    implementation(project(":contracts"))
    implementation(project(":browse"))
}
""")

            from v06_module_deps import check_module_dependencies, ALLOWED_DEPS

            build_file = project_root / "play" / "build.gradle.kts"
            result = check_module_dependencies("play", build_file, ALLOWED_DEPS)

            self.assertFalse(result, "V-06 should fail with invalid dependency")

    def test_v06_allows_valid_dependencies(self):
        """V-06 should pass with valid main-scope dependencies."""
        with tempfile.TemporaryDirectory() as tmpdir:
            project_root = Path(tmpdir)

            play_dir = project_root / "play"
            play_dir.mkdir()
            (play_dir / "build.gradle.kts").write_text("""plugins {
    id("com.android.library")
}
dependencies {
    implementation(project(":kernel"))
    implementation(project(":contracts"))
}
""")

            from v06_module_deps import check_module_dependencies, ALLOWED_DEPS

            build_file = project_root / "play" / "build.gradle.kts"
            result = check_module_dependencies("play", build_file, ALLOWED_DEPS)

            self.assertTrue(result, "V-06 should pass with valid dependencies")

    def test_v06_allows_test_scope_dependencies(self):
        """V-06 should allow testImplementation(project(':content')) for testing."""
        with tempfile.TemporaryDirectory() as tmpdir:
            project_root = Path(tmpdir)

            play_dir = project_root / "play"
            play_dir.mkdir()
            (play_dir / "build.gradle.kts").write_text("""plugins {
    id("com.android.library")
}
dependencies {
    implementation(project(":kernel"))
    implementation(project(":contracts"))
    testImplementation(project(":content"))
}
""")

            from v06_module_deps import check_module_dependencies, ALLOWED_DEPS

            build_file = project_root / "play" / "build.gradle.kts"
            result = check_module_dependencies("play", build_file, ALLOWED_DEPS)

            self.assertTrue(result, "V-06 should pass allowing test-scope content")

    def test_v06_fails_with_projects_accessor(self):
        """V-06 should fail on projects.x accessor in main scope."""
        with tempfile.TemporaryDirectory() as tmpdir:
            project_root = Path(tmpdir)

            play_dir = project_root / "play"
            play_dir.mkdir()
            (play_dir / "build.gradle.kts").write_text("""plugins {
    id("com.android.library")
}
dependencies {
    implementation(project(":kernel"))
    implementation(project(":contracts"))
    implementation(projects.browse)
}
""")

            from v06_module_deps import check_module_dependencies, ALLOWED_DEPS

            build_file = project_root / "play" / "build.gradle.kts"
            result = check_module_dependencies("play", build_file, ALLOWED_DEPS)

            self.assertFalse(result, "V-06 should fail with projects.x accessor")

    def test_v06_fails_with_project_path_format(self):
        """V-06 should fail on project(path = ':x') format in main scope."""
        with tempfile.TemporaryDirectory() as tmpdir:
            project_root = Path(tmpdir)

            play_dir = project_root / "play"
            play_dir.mkdir()
            (play_dir / "build.gradle.kts").write_text("""plugins {
    id("com.android.library")
}
dependencies {
    implementation(project(":kernel"))
    implementation(project(":contracts"))
    api(project(path = ":browse"))
}
""")

            from v06_module_deps import check_module_dependencies, ALLOWED_DEPS

            build_file = project_root / "play" / "build.gradle.kts"
            result = check_module_dependencies("play", build_file, ALLOWED_DEPS)

            self.assertFalse(result, "V-06 should fail with project(path = ':x') format")

    def test_v06_allows_test_scope_projects_accessor(self):
        """V-06 should allow testImplementation(projects.content) in test scope."""
        with tempfile.TemporaryDirectory() as tmpdir:
            project_root = Path(tmpdir)

            play_dir = project_root / "play"
            play_dir.mkdir()
            (play_dir / "build.gradle.kts").write_text("""plugins {
    id("com.android.library")
}
dependencies {
    implementation(project(":kernel"))
    implementation(project(":contracts"))
    testImplementation(projects.content)
}
""")

            from v06_module_deps import check_module_dependencies, ALLOWED_DEPS

            build_file = project_root / "play" / "build.gradle.kts"
            result = check_module_dependencies("play", build_file, ALLOWED_DEPS)

            self.assertTrue(result, "V-06 should allow testImplementation(projects.x) in test scope")


class TestV07NoJunkPaths(unittest.TestCase):
    """Tests for V-07 verifier. Bad names are created at test time in a temp dir, never committed."""

    def make_tree(self, tmpdir, *relpaths):
        for rel in relpaths:
            p = Path(tmpdir) / rel
            p.parent.mkdir(parents=True, exist_ok=True)
            p.write_text("x")

    def test_v07_passes_on_clean_tree(self):
        from v07_no_junk_paths import find_junk
        with tempfile.TemporaryDirectory() as tmpdir:
            self.make_tree(tmpdir, "app/src/Main.kt", "Tangrams/a.json", "notes copy.md")
            self.assertEqual(find_junk(tmpdir), [])

    def _fails_for(self, name):
        from v07_no_junk_paths import find_junk
        with tempfile.TemporaryDirectory() as tmpdir:
            try:
                self.make_tree(tmpdir, "ok/" + name)
            except OSError:
                self.skipTest(f"this filesystem cannot hold the name {name!r}")
            found = find_junk(tmpdir)
            self.assertEqual(len(found), 1, f"V-07 should fail on {name!r}: {found}")

    def test_v07_fails_on_msys_colon_substitute(self):
        self._fails_for("CUsersfile.txt")

    def test_v07_fails_on_private_use_quote(self):
        self._fails_for("name.txt")

    def test_v07_fails_on_cp_prefix(self):
        self._fails_for("cp junk")

    def test_v07_name_rules_for_names_windows_cannot_hold(self):
        from v07_no_junk_paths import bad_reason
        for name in ("a:b", 'say"hi"', "C:Users", "xy", "xy", "cp thing"):
            self.assertIsNotNone(bad_reason(name), repr(name))
        for name in ("plain.txt", "cpu.md", "my copy.txt", "café"):
            self.assertIsNone(bad_reason(name), repr(name))

    def test_v07_fails_on_junk_folder(self):
        from v07_no_junk_paths import find_junk
        with tempfile.TemporaryDirectory() as tmpdir:
            self.make_tree(tmpdir, "cp x/inner.txt")
            self.assertEqual([r for r, _ in find_junk(tmpdir)], ["cp x"])

    def test_v07_skips_git_build_and_gradle(self):
        from v07_no_junk_paths import find_junk
        with tempfile.TemporaryDirectory() as tmpdir:
            self.make_tree(tmpdir, ".git/cp junk", "app/build/cp junk", ".gradle/cp junk")
            self.assertEqual(find_junk(tmpdir), [])

    def test_v07_cli_exit_codes(self):
        script = Path(__file__).parent / "v07_no_junk_paths.py"
        with tempfile.TemporaryDirectory() as tmpdir:
            self.make_tree(tmpdir, "a.txt")
            ok = subprocess.run([sys.executable, str(script), tmpdir], capture_output=True, text=True)
            self.assertEqual(ok.returncode, 0)
            self.assertIn("V-07 PASS", ok.stdout)
            self.make_tree(tmpdir, "cp bad")
            bad = subprocess.run([sys.executable, str(script), tmpdir], capture_output=True, text=True)
            self.assertEqual(bad.returncode, 1)
            self.assertIn("V-07 FAIL", bad.stdout)


class TestV04ReleaseApk(unittest.TestCase):
    """Tests for V-04. Fixtures are crafted at test time (no Gradle, no real APK).
    Every detector fixture = canary-complete base B plus exactly ONE change (rev 2, E1)."""

    PLAY = "Lio/github/jamisuni/tangram/play/PlayArea;"
    MAIN = "Lio/github/jamisuni/tangram/MainActivity;"
    DEV = "Lio/github/jamisuni/tangram/devtools/DevToolsState;"
    ARSC = b"app_name\x00Restart\x00Other\x00"
    DEBUG_ARSC = ARSC + b"devtools_dev_button\x00Wrong passcode.\x00"

    def setUp(self):
        import contextlib
        import io
        import zipfile
        self.contextlib, self.io, self.zipfile = contextlib, io, zipfile
        self.tmp = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.tmp, True)
        sys.path.insert(0, str(Path(__file__).parent))
        import v04_release_apk
        self.v04 = v04_release_apk
        self.n = 0

    @staticmethod
    def make_dex(strings, version="038", header_size=0x70, table_beyond=False, bad_magic=False):
        n = len(strings)
        data_start = 0x70 + 4 * n
        blobs, offs, cur = [], [], data_start
        for st in strings:
            item = bytes([len(st)]) + st.encode("utf-8") + b"\x00"   # fixture strings are short: one ULEB byte
            offs.append(cur)
            blobs.append(item)
            cur += len(item)
        head = bytearray(0x70)
        head[0:8] = (b"xxx\n" if bad_magic else b"dex\n") + version.encode() + b"\x00"
        struct.pack_into("<I", head, 0x24, header_size)
        struct.pack_into("<II", head, 0x38, n, 0x70 + 100000 if table_beyond else 0x70)
        return bytes(head) + b"".join(struct.pack("<I", o) for o in offs) + b"".join(blobs)

    def base_dex_strings(self):
        return [self.MAIN, self.PLAY, "DebugAids", "Lio/github/jamisuni/tangram/devtool/Near;"]

    def make_apk(self, dex_strings=None, arsc=None, extra=None, dexes=None):
        """dexes: {name: bytes} replaces the single base dex; extra: {entry: bytes} added."""
        self.n += 1
        path = self.tmp / f"f{self.n}.apk"
        if dexes is None:
            dexes = {"classes.dex": self.make_dex(self.base_dex_strings() if dex_strings is None else dex_strings)}
        with self.zipfile.ZipFile(path, "w") as z:
            for name, data in dexes.items():
                z.writestr(name, data)
            z.writestr("resources.arsc", self.ARSC if arsc is None else arsc)
            z.writestr("AndroidManifest.xml", b"<manifest/>")
            for name, data in (extra or {}).items():
                z.writestr(name, data)
        return path

    def run_v04(self, apk, *flags):
        buf = self.io.StringIO()
        with self.contextlib.redirect_stdout(buf):
            code = self.v04.main(["--apk", str(apk), "--no-puzzles", *flags])
        return code, buf.getvalue().splitlines()

    def assert_fail(self, apk, kind):
        code, lines = self.run_v04(apk)
        self.assertEqual(code, 1, lines)
        self.assertTrue(any(l.startswith(f"V-04 FAIL {kind} ") for l in lines), lines)
        self.assertFalse(any("scanner blind" in l for l in lines), lines)

    def assert_pass(self, apk):
        code, lines = self.run_v04(apk)
        self.assertEqual(code, 0, lines)
        self.assertTrue(any(l.startswith("V-04 PASS") for l in lines), lines)

    def test_v04_base_passes(self):
        self.assert_pass(self.make_apk())

    def test_v04_devtools_descriptor_slash_form(self):
        self.assert_fail(self.make_apk(self.base_dex_strings() + [self.DEV]), "devtools-class")

    def test_v04_devtools_dotted_form(self):
        self.assert_fail(self.make_apk(self.base_dex_strings() + ["io.github.jamisuni.tangram.devtools.X"]),
                         "devtools-class")

    def test_v04_devtools_in_second_dex(self):
        dexes = {"classes.dex": self.make_dex(self.base_dex_strings()),
                 "classes2.dex": self.make_dex([self.DEV])}
        self.assert_fail(self.make_apk(dexes=dexes), "devtools-class")

    def test_v04_passcode_exact_string(self):
        self.assert_fail(self.make_apk(self.base_dex_strings() + ["0417"]), "passcode")

    def test_v04_passcode_substrings_pass(self):
        self.assert_pass(self.make_apk(self.base_dex_strings() + ["10417", "0.04170"]))

    def test_v04_wrong_passcode_text_utf8_and_utf16(self):
        for blob in ("Wrong passcode.".encode("utf-8"), "Wrong passcode.".encode("utf-16-le")):
            self.assert_fail(self.make_apk(arsc=self.ARSC + blob), "devtools-resource")

    def test_v04_deny_list_entries(self):
        needles = (b"devtools_x", b"io/github/jamisuni/tangram/devtools", b"io.github.jamisuni.tangram.devtools")
        for entry in ("res/a.xml", "META-INF/x.kotlin_module", "root.txt", "kotlin/k.kotlin_builtins", "lib/x86/a.so"):
            self.assert_fail(self.make_apk(extra={entry: b"..." + needles[self.n % 3] + b"..."}), "devtools-resource")
        self.assert_fail(self.make_apk(arsc=self.ARSC + b"devtools_x"), "devtools-resource")

    def test_v04_finnish_devtools_string_in_arsc(self):
        self.assert_fail(self.make_apk(arsc=self.ARSC + "Näytä ratkaisu".encode("utf-8")), "devtools-resource")
        self.assert_fail(self.make_apk(arsc=self.ARSC + "Näytä ratkaisu".encode("utf-16-le")), "devtools-resource")

    def test_v04_long_english_devtools_hint_in_arsc(self):
        hint = "Unlocked until the app is restarted. The solution overlay stays on while you browse puzzles."
        self.assert_fail(self.make_apk(arsc=self.ARSC + hint.encode("utf-8")), "devtools-resource")
        self.assert_fail(self.make_apk(arsc=self.ARSC + "Solve this puzzle now".encode("utf-16-le")),
                         "devtools-resource")

    def test_v04_short_and_common_words_do_not_fire(self):
        self.assert_pass(self.make_apk(arsc=self.ARSC + b"OK\x00DEV\x00Done\x00Settings\x00Puzzle\x00"))

    def test_v04_devtools_values_threshold(self):
        vals = self.v04.devtools_values()
        self.assertIn("Show the solution", vals)
        self.assertIn("N\u00e4yt\u00e4 ratkaisu", vals)
        self.assertIn("The game engine could not place this puzzle's stored solution.", vals)
        for short in ("OK", "DEV", "Done"):
            self.assertNotIn(short, vals)
        self.assertNotIn("Valmis", vals)   # also a browse string (shared word, excluded)
        self.assertTrue(all(len(v) >= 6 for v in vals))

    def test_v04_png_entry_is_excluded(self):
        self.assert_pass(self.make_apk(extra={"res/a.png": b"devtools_x"}))

    def test_v04_arsc_with_digits_0417_passes(self):
        self.assert_pass(self.make_apk(arsc=self.ARSC + b"0417"))

    def test_v04_canary_minus_one_each_reports_exactly_one_blind_line(self):
        cases = {
            "Lio/github/jamisuni/tangram/play/": dict(dex_strings=[self.MAIN, "DebugAids"]),
            "Lio/github/jamisuni/tangram/MainActivity;": dict(dex_strings=[self.PLAY, "DebugAids"]),
            "app_name": dict(arsc=b"Restart\x00"),
            "Restart": dict(arsc=b"app_name\x00"),
        }
        for label, kw in cases.items():
            code, lines = self.run_v04(self.make_apk(**kw))
            self.assertEqual(code, 1, label)
            self.assertEqual(len(lines), 1, lines)
            self.assertTrue(lines[0].startswith(f"V-04 FAIL scanner blind {label};"), lines)
            self.assertIn("update CANARIES in v04_release_apk.py", lines[0])

    def test_v04_fail_closed_dex_cases(self):
        code, lines = self.run_v04(self.make_apk(dexes={}))
        self.assertEqual(code, 1)
        self.assertTrue(any(l.startswith("V-04 FAIL no-dex") for l in lines), lines)
        bad = {
            "magic": dict(bad_magic=True),
            "version 040": dict(version="040"),
            "header size": dict(header_size=0x71),
            "table offset": dict(table_beyond=True),
        }
        for label, kw in bad.items():
            apk = self.make_apk(dexes={"classes.dex": self.make_dex(self.base_dex_strings(), **kw)})
            code, lines = self.run_v04(apk)
            self.assertEqual(code, 1, label)
            self.assertTrue(any(l.startswith("V-04 FAIL dex-unparseable classes.dex") for l in lines), (label, lines))

    def test_v04_unparseable_dex_is_raw_scanned(self):
        raw = self.make_dex(self.base_dex_strings() + [self.DEV], bad_magic=True)
        code, lines = self.run_v04(self.make_apk(dexes={"classes.dex": raw}))
        self.assertEqual(code, 1)
        self.assertTrue(any(l.startswith("V-04 FAIL devtools-raw classes.dex") for l in lines), lines)

    def test_v04_dex_version_035_and_039_accepted(self):
        for v in ("035", "039"):
            self.assert_pass(self.make_apk(dexes={"classes.dex": self.make_dex(self.base_dex_strings(), version=v)}))

    @staticmethod
    def _write_strings(root, dev_en=("devtools_hint", "Show the solution"), dev_fi=("devtools_hint", "Valmis"),
                       app=(("done", "Valmis"),), skip_dev=False):
        def res(path, rows):
            f = Path(root) / path
            f.parent.mkdir(parents=True, exist_ok=True)
            body = "".join(f'<string name="{k}">{v}</string>' for k, v in rows)
            f.write_text(f"<resources>{body}</resources>", encoding="utf-8", newline="\n")
        if not skip_dev:
            res("devtools/src/main/res/values/strings.xml", [dev_en])
            res("devtools/src/main/res/values-fi/strings.xml", [dev_fi])
        res("browse/src/main/res/values-fi/strings.xml", list(app))

    def test_v04_shared_value_exclusion_synthetic_root(self):
        root = self.tmp / "synth"
        self._write_strings(root)
        denied, excluded = self.v04.devtools_value_sets(root)
        self.assertEqual(denied, {"Show the solution"})
        self.assertEqual(excluded, {"Valmis"})
        # a leaked non-shared devtools value fails; the shared one alone does not
        leaked = self.make_apk(arsc=self.ARSC + b"Show the solution")
        shared = self.make_apk(arsc=self.ARSC + b"Valmis")
        buf = self.io.StringIO()
        with self.contextlib.redirect_stdout(buf), self.contextlib.redirect_stderr(self.io.StringIO()) as err:
            self.assertEqual(self.v04.main(["--apk", str(leaked), "--no-puzzles", "--project-root", str(root)]), 1)
            self.assertIn("V-04 FAIL devtools-resource resources.arsc contains Show the solution", buf.getvalue())
            buf.truncate(0)
            self.assertEqual(self.v04.main(["--apk", str(shared), "--no-puzzles", "--project-root", str(root)]), 0)
        self.assertIn("V-04 note: 1 devtools values denied, 1 shared value(s) excluded (Valmis)", err.getvalue())

    def test_v04_missing_devtools_strings_is_exit_2_no_fallback(self):
        root = self.tmp / "nodev"
        self._write_strings(root, skip_dev=True)
        buf = self.io.StringIO()
        with self.contextlib.redirect_stdout(buf):
            code = self.v04.main(["--apk", str(self.make_apk()), "--no-puzzles", "--project-root", str(root)])
        self.assertEqual(code, 2, buf.getvalue())
        self.assertIn("devtools strings file not found", buf.getvalue())

    def _puzzle_root(self, apk_extra):
        root = self.tmp / f"root{self.n}"
        (root / "Tangrams").mkdir(parents=True)
        (root / "Tangrams" / "a.json").write_bytes(b'{"id":"a"}\n')
        self._write_strings(root)
        return root, self.make_apk(extra=apk_extra)

    def _run_with_puzzles(self, root, apk):
        buf = self.io.StringIO()
        with self.contextlib.redirect_stdout(buf):
            code = self.v04.main(["--apk", str(apk), "--project-root", str(root)])
        return code, buf.getvalue().splitlines()

    def test_v04_folded_puzzle_check(self):
        good = {"tangrams/a.json": b'{"id":"a"}\n', "tangrams/index.txt": b"a.json\n"}
        code, lines = self._run_with_puzzles(*self._puzzle_root(good))
        self.assertEqual(code, 0, lines)
        code, lines = self._run_with_puzzles(*self._puzzle_root({"tangrams/index.txt": b"a.json\n"}))
        self.assertEqual(code, 1)
        self.assertTrue(any(l.startswith("V-04 FAIL puzzles") and "MISSING" in l for l in lines), lines)
        code, lines = self._run_with_puzzles(*self._puzzle_root({**good, "tangrams/a.json": b'{"id":"X"}\n'}))
        self.assertEqual(code, 1)
        self.assertTrue(any("CHANGED" in l for l in lines), lines)

    def test_v04_exit_2_missing_and_non_zip(self):
        code, lines = self.run_v04(self.tmp / "nope.apk")
        self.assertEqual(code, 2)
        junk = self.tmp / "junk.apk"
        junk.write_bytes(b"not a zip at all")
        code, lines = self.run_v04(junk)
        self.assertEqual(code, 2, lines)
        self.assertTrue(lines[0].startswith("V-04 ERROR"), lines)

    def test_v04_exit_2_when_folded_check_raises(self):
        import unittest.mock
        root, apk = self._puzzle_root({})
        sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "tools"))
        import check_apk_puzzles
        with unittest.mock.patch.object(check_apk_puzzles, "check", side_effect=OSError("boom")):
            code, lines = self._run_with_puzzles(root, apk)
        self.assertEqual(code, 2, lines)
        self.assertTrue(lines[-1].startswith("V-04 ERROR OSError"), lines)

    def test_v04_usage_error_is_exit_2(self):
        buf = self.io.StringIO()
        with self.contextlib.redirect_stdout(buf):
            self.assertEqual(self.v04.main(["--bogus"]), 2)

    def debug_strings(self):
        return self.base_dex_strings() + [self.DEV, "0417"]

    def test_v04_positive_control_all_kinds_found(self):
        apk = self.make_apk(self.debug_strings(), arsc=self.DEBUG_ARSC)
        self.assertEqual(self.v04.positive_control(apk), [])
        code, lines = self.run_v04(apk, "--positive-control")
        self.assertEqual(code, 0, lines)

    def test_v04_positive_control_names_each_missing_kind(self):
        cases = {
            "devtools package in dex": dict(dex_strings=self.base_dex_strings() + ["0417"], arsc=self.DEBUG_ARSC),
            "dex string 0417": dict(dex_strings=self.base_dex_strings() + [self.DEV], arsc=self.DEBUG_ARSC),
            "devtools_ key in resources.arsc": dict(dex_strings=self.debug_strings(),
                                                    arsc=self.ARSC + b"Wrong passcode.\x00"),
            "Wrong passcode. in resources.arsc": dict(dex_strings=self.debug_strings(),
                                                      arsc=self.ARSC + b"devtools_dev_button\x00"),
        }
        for kind, kw in cases.items():
            apk = self.make_apk(**kw)
            self.assertEqual(self.v04.positive_control(apk), [kind])
            code, lines = self.run_v04(apk, "--positive-control")
            self.assertEqual(code, 1)
            self.assertEqual(lines, [f"V-04 FAIL scanner cannot see {kind}"])

    def test_v04_positive_control_fails_on_a_release_like_apk(self):
        self.assertEqual(len(self.v04.positive_control(self.make_apk())), 4)


if __name__ == "__main__":
    unittest.main()
