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


class TestV08PromiseApk(unittest.TestCase):
    """Tests for V-08. Crafted dex fixtures (no Gradle, no real APK, no real aapt2: a fake aapt2 script).
    Every detector fixture = clean base B plus exactly ONE change."""

    PKG = "Lio/github/jamisuni/tangram/"
    MAIN = PKG + "MainActivity;"
    PLAY = PKG + "play/PlayArea;"
    LOCALE = PKG + "LocaleOverrideActivity;"
    TESTCFG = PKG + "TestConfig;"
    PROBE = PKG + "FeedbackProbe;"
    SOCKET = "Ljava/net/Socket;"
    WEBVIEW = "Landroid/webkit/WebView;"
    ARSC = b"app_name\x00Restart\x00Other\x00"
    CLEAN_TREE = (
        "E: manifest (line=2)\n"
        "  A: package=\"io.github.jamisuni.tangram\" (Raw: \"io.github.jamisuni.tangram\")\n"
        "  E: uses-sdk (line=7)\n"
        "    A: http://schemas.android.com/apk/res/android:minSdkVersion(0x0101020c)=26\n"
        "  E: application (line=12)\n"
        "    A: http://schemas.android.com/apk/res/android:name(0x01010003)=\"x.App\" (Raw: \"x.App\")\n"
    )

    def setUp(self):
        import contextlib
        import io
        import zipfile
        self.contextlib, self.io, self.zipfile = contextlib, io, zipfile
        self.tmp = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.tmp, True)
        sys.path.insert(0, str(Path(__file__).parent))
        import v08_promise_apk
        self.v08 = v08_promise_apk
        self.n = 0
        # a project tree whose only input is an hour old, so every fresh fixture APK is newer
        self.root = self.tmp / "proj"
        src = self.root / "app" / "src" / "main"
        src.mkdir(parents=True)
        self.input_file = src / "A.kt"
        self.input_file.write_text("x", encoding="utf-8")
        old = os.path.getmtime(self.input_file) - 3600
        os.utime(self.input_file, (old, old))
        self.aapt2 = self.fake_aapt2(self.CLEAN_TREE)

    def fake_aapt2(self, tree):
        self.n += 1
        txt = self.tmp / f"tree{self.n}.txt"
        txt.write_text(tree, encoding="utf-8", newline="\n")
        if os.name == "nt":
            path = self.tmp / f"aapt2_{self.n}.bat"
            path.write_text(f'@echo off\r\ntype "{txt}"\r\n', encoding="ascii", newline="")
        else:
            path = self.tmp / f"aapt2_{self.n}.sh"
            path.write_text(f'#!/bin/sh\ncat "{txt}"\n', encoding="utf-8", newline="\n")
            path.chmod(0o755)
        return path

    @staticmethod
    def _uleb(n):
        out = bytearray()
        while True:
            b = n & 0x7F
            n >>= 7
            if n:
                out.append(b | 0x80)
            else:
                out.append(b)
                return bytes(out)

    @classmethod
    def make_dex(cls, defined, referenced=(), extra_strings=(), version="038", truncate=0, class_idx_override=None,
                 descriptor_idx_override=None, defs_size_override=None, bad_magic=False, pad=0,
                 callers=(), code_fault=None):
        """A dex with string_ids, type_ids, method_ids, class_defs, class_data and code_items.
        defined = class descriptors with a class_def; referenced = descriptors in type_ids only.
        callers = [(class, method, [(callee class, callee name), ...])]: a defined class whose method is a real
        code_item of invoke-virtual instructions plus return-void. code_fault (one change) breaks the code_item:
        "short" (insns_size cuts an invoke), "size" (insns_size beyond the file), "method" (an invoke names
        method index 999), "code_off" (code_off beyond the file)."""
        defined = list(defined)
        for c in dict.fromkeys(c for c, _, _ in callers):
            if c not in defined:
                defined.append(c)
        meths = []   # (class, name), deduped, caller methods first
        for c, m, callees in callers:
            if (c, m) not in meths:
                meths.append((c, m))
        for _, _, callees in callers:
            for ce in callees:
                if tuple(ce) not in meths:
                    meths.append(tuple(ce))
        descs = list(defined)
        for d in list(referenced) + [c for c, _ in meths]:
            if d not in descs:
                descs.append(d)
        strings = list(descs)
        for s in [n for _, n in meths] + list(extra_strings):
            if s not in strings:
                strings.append(s)
        ns, nt, nd, nm = len(strings), len(descs), len(defined), len(meths)
        sid_off = 0x70
        tid_off = sid_off + 4 * ns
        mid_off = tid_off + 4 * nt
        data_off = mid_off + 8 * nm
        blobs, offs, cur = [], [], data_off
        for st in strings:
            item = bytes([len(st)]) + st.encode("utf-8") + b"\x00"
            offs.append(cur)
            blobs.append(item)
            cur += len(item)
        def_off = cur
        tail_off = def_off + 32 * nd
        tail = bytearray()
        class_data_off = {}
        for c in defined:
            mine = [(meths.index((cc, m)), cc, m, callees) for cc, m, callees in callers if cc == c]
            if not mine:
                continue
            entries = []
            for midx, _, _, callees in sorted(mine, key=lambda t: t[0]):
                units = []
                for ce in callees:
                    units += [0x6E, 999 if code_fault == "method" else meths.index(tuple(ce)), 0]
                units.append(0x000E)
                size = {"short": 2, "size": 10 ** 6}.get(code_fault, len(units))
                code_off = tail_off + len(tail)
                tail += struct.pack("<HHHHII", 1, 0, 0, 0, 0, size) + b"".join(struct.pack("<H", u) for u in units)
                entries.append((midx, 10 ** 8 if code_fault == "code_off" else code_off))
            class_data_off[c] = tail_off + len(tail)
            tail += cls._uleb(0) + cls._uleb(0) + cls._uleb(len(entries)) + cls._uleb(0)
            prev = 0
            for midx, code_off in entries:
                tail += cls._uleb(midx - prev) + cls._uleb(1) + cls._uleb(code_off)
                prev = midx
        body = bytearray()
        for o in offs:
            body += struct.pack("<I", o)
        for i in range(nt):
            body += struct.pack("<I", i if descriptor_idx_override is None else descriptor_idx_override)
        for c, m in meths:
            body += struct.pack("<HHI", descs.index(c), 0, strings.index(m))
        body += b"".join(blobs)
        for i, c in enumerate(defined):
            idx = i if class_idx_override is None else class_idx_override
            body += struct.pack("<IIIIIIII", idx, 0, 0, 0, 0, 0, class_data_off.get(c, 0), 0)
        body += tail
        full_len = 0x70 + len(body)   # the header file_size keeps the ORIGINAL length (truncate/pad leave it stale)
        if truncate:
            body = body[:-truncate]
        body += bytes(pad)
        head = bytearray(0x70)
        head[0:8] = (b"xxx\n" if bad_magic else b"dex\n") + version.encode() + b"\x00"
        struct.pack_into("<I", head, 0x24, 0x70)
        struct.pack_into("<II", head, 0x38, ns, sid_off)
        struct.pack_into("<II", head, 0x40, nt, tid_off)
        struct.pack_into("<II", head, 0x58, nm, mid_off)
        struct.pack_into("<II", head, 0x60, nd if defs_size_override is None else defs_size_override, def_off)
        struct.pack_into("<I", head, 0x20, full_len)
        return bytes(head) + bytes(body)

    OK_CALLER = ("Landroidx/compose/ui/platform/AndroidSoundEffect;", "playClickSound",
                 [("Landroid/view/View;", "playSoundEffect")])

    def base(self, **kw):
        d = dict(defined=[self.MAIN, self.PLAY], referenced=[self.SOCKET, self.WEBVIEW], callers=[self.OK_CALLER])
        d.update(kw)
        return d

    def make_apk(self, dex=None, arsc=None, dexes=None, **dexkw):
        self.n += 1
        path = self.tmp / f"f{self.n}.apk"
        if dexes is None:
            dexes = {"classes.dex": dex if dex is not None else self.make_dex(**self.base(**dexkw))}
        with self.zipfile.ZipFile(path, "w") as z:
            for name, data in dexes.items():
                z.writestr(name, data)
            z.writestr("resources.arsc", self.ARSC if arsc is None else arsc)
            z.writestr("AndroidManifest.xml", b"<manifest/>")
        return path

    def run_v08(self, apk, *flags, aapt2=None, root=None):
        buf = self.io.StringIO()
        with self.contextlib.redirect_stdout(buf):
            code = self.v08.main(["--apk", str(apk), "--project-root", str(root or self.root),
                                  "--aapt2", str(aapt2 or self.aapt2), *flags])
        return code, buf.getvalue().splitlines()

    def assert_pass(self, apk, *flags):
        code, lines = self.run_v08(apk, *flags)
        self.assertEqual(code, 0, lines)
        self.assertTrue(any(l.startswith("V-08 PASS") for l in lines), lines)

    def assert_fail(self, apk, needle, *flags, **kw):
        code, lines = self.run_v08(apk, *flags, **kw)
        self.assertEqual(code, 1, lines)
        self.assertTrue(any(needle in l for l in lines), lines)
        self.assertFalse(any("scanner blind" in l for l in lines), lines)

    def test_v08_clean_base_passes_with_framework_references(self):
        self.assert_pass(self.make_apk())

    def test_v08_dex_versions_035_to_039(self):
        for v in ("035", "036", "037", "038", "039"):
            self.assert_pass(self.make_apk(version=v))

    def test_v08_same_type_referenced_passes_then_defined_fails(self):
        sdk = "Lcom/google/firebase/analytics/FirebaseAnalytics;"
        self.assert_pass(self.make_apk(referenced=[self.SOCKET, self.WEBVIEW, sdk]))
        self.assert_fail(self.make_apk(defined=[self.MAIN, self.PLAY, sdk]), "sdk-class")

    def test_v08_each_denied_sdk_kind_defined_fails_with_its_kind(self):
        for prefix, kind in self.v08.DENIED_SDK:
            desc = f"L{prefix}Probe;"
            apk = self.make_apk(defined=[self.MAIN, self.PLAY, desc])
            code, lines = self.run_v08(apk)
            self.assertEqual(code, 1, (prefix, lines))
            self.assertTrue(any(l.startswith("V-08 FAIL sdk-class") and kind in l and desc in l for l in lines),
                            (prefix, lines))
            self.assertFalse(any("scanner blind" in l for l in lines), lines)

    def test_v08_denied_kinds_cover_the_promise(self):
        joined = " ".join(k for _, k in self.v08.DENIED_SDK)
        for word in ("ads", "billing", "review", "analytics", "crash", "network"):
            self.assertIn(word, joined)

    def test_v08_sdk_in_second_dex_fails(self):
        dexes = {"classes.dex": self.make_dex(**self.base()),
                 "classes2.dex": self.make_dex(defined=["Lokhttp3/OkHttpClient;"])}
        self.assert_fail(self.make_apk(dexes=dexes), "okhttp3")

    def test_v08_locale_activity_defined_in_release_fails(self):
        self.assert_fail(self.make_apk(defined=[self.MAIN, self.PLAY, self.LOCALE]), "debug-class-in-release")

    def test_v08_testconfig_defined_in_release_fails(self):
        self.assert_fail(self.make_apk(defined=[self.MAIN, self.PLAY, self.TESTCFG]), "debug-class-in-release")

    def test_v08_expect_debug_absent_fails_and_present_passes(self):
        self.assert_fail(self.make_apk(), "debug-class-missing", "--expect-debug")
        self.assert_fail(self.make_apk(defined=[self.MAIN, self.PLAY, self.LOCALE, self.PROBE]), self.TESTCFG,
                         "--expect-debug")
        self.assert_pass(self.make_apk(defined=[self.MAIN, self.PLAY, self.LOCALE, self.TESTCFG, self.PROBE]),
                         "--expect-debug")

    def test_v08_feedback_probe_defined_in_release_fails(self):
        self.assert_fail(self.make_apk(defined=[self.MAIN, self.PLAY, self.PROBE]), "debug-class-in-release " + self.PROBE)

    def test_v08_feedback_probe_absent_with_expect_debug_fails(self):
        self.assert_fail(self.make_apk(defined=[self.MAIN, self.PLAY, self.LOCALE, self.TESTCFG]), self.PROBE,
                         "--expect-debug")

    def test_v08_debug_classes_only_referenced_do_not_count_as_defined(self):
        apk = self.make_apk(referenced=[self.LOCALE, self.TESTCFG, self.PROBE])
        self.assert_pass(apk)
        self.assert_fail(apk, "debug-class-missing", "--expect-debug")

    def test_v08_canary_minus_one_each_reports_exactly_one_blind_line(self):
        cases = {
            "Lio/github/jamisuni/tangram/play/": dict(defined=[self.MAIN]),
            "Lio/github/jamisuni/tangram/MainActivity;": dict(defined=[self.PLAY]),
            "app_name": dict(arsc=b"Restart\x00"),
            "Restart": dict(arsc=b"app_name\x00"),
        }
        for label, kw in cases.items():
            code, lines = self.run_v08(self.make_apk(**kw))
            self.assertEqual(code, 1, lines)
            blind = [l for l in lines if "scanner blind" in l]
            self.assertEqual(len(blind), 1, lines)
            self.assertIn(label, blind[0])
            self.assertEqual([l for l in lines if not l.startswith("V-08 note")], blind, lines)

    def test_v08_truncated_class_table_is_exit_2(self):
        self.assertEqual(self.run_v08(self.make_apk(dex=self.make_dex(**self.base(), truncate=10)))[0], 2)

    def test_v08_inconsistent_tables_are_exit_2(self):
        bad = [
            dict(class_idx_override=99),
            dict(descriptor_idx_override=999),
            dict(defs_size_override=1000),
            dict(bad_magic=True),
        ]
        for kw in bad:
            code, lines = self.run_v08(self.make_apk(dex=self.make_dex(**self.base(), **kw)))
            self.assertEqual(code, 2, (kw, lines))

    def test_v08_file_size_check_alone_rejects_a_padded_dex(self):
        # tables intact and in bounds; only the header file_size no longer matches the bytes
        self.v08.parse_dex_classes(self.make_dex(**self.base()))
        with self.assertRaisesRegex(self.v08.DexError, "file_size"):
            self.v08.parse_dex_classes(self.make_dex(**self.base(), pad=4))
        with self.assertRaisesRegex(self.v08.DexError, "file_size"):
            self.v08.parse_dex_classes(self.make_dex(**self.base(), truncate=10))
        self.assertEqual(self.run_v08(self.make_apk(dex=self.make_dex(**self.base(), pad=4)))[0], 2)

    def test_v08_one_bad_dex_among_good_is_exit_2(self):
        dexes = {"classes.dex": self.make_dex(**self.base()),
                 "classes2.dex": self.make_dex(**self.base(), truncate=5)}
        self.assertEqual(self.run_v08(self.make_apk(dexes=dexes))[0], 2)

    def test_v08_no_dex_is_exit_2(self):
        self.assertEqual(self.run_v08(self.make_apk(dexes={"x.bin": b"1"}))[0], 2)

    def test_v08_parse_dex_classes_raises_on_truncation(self):
        with self.assertRaises(self.v08.DexError):
            self.v08.parse_dex_classes(self.make_dex(**self.base(), truncate=10))
        self.assertEqual(self.v08.parse_dex_classes(self.make_dex(**self.base())),
                         [self.MAIN, self.PLAY, self.OK_CALLER[0]])

    def test_v08_missing_apk_is_exit_2(self):
        self.assertEqual(self.run_v08(self.tmp / "nope.apk")[0], 2)

    def test_v08_stale_apk_is_exit_2_and_fresh_apk_passes(self):
        apk = self.make_apk()
        newer = os.path.getmtime(apk) + 100
        os.utime(self.input_file, (newer, newer))
        code, lines = self.run_v08(apk)
        self.assertEqual(code, 2, lines)
        self.assertTrue(any("stale" in l for l in lines), lines)
        older = os.path.getmtime(apk) - 100
        os.utime(self.input_file, (older, older))
        self.assert_pass(apk)

    def test_v08_stale_guard_input_set(self):
        apk = self.make_apk()
        future = os.path.getmtime(apk) + 100
        # inputs that count
        for rel in ("app/src/release/R.kt", "app/src/debug/res/values-fi/strings.xml", "gradle/libs.versions.toml",
                    "app/build.gradle.kts", "settings.gradle.kts", "Tangrams/p.json",
                    "app/src/debug/AndroidManifest.xml", "app/src/debug/java/io/x/FeedbackProbe.kt"):
            f = self.root / rel
            f.parent.mkdir(parents=True, exist_ok=True)
            f.write_text("x", encoding="utf-8")
            os.utime(f, (future, future))
            self.assertEqual(self.run_v08(apk)[0], 2, rel)
            os.utime(f, (future - 1000, future - 1000))
        # outputs and caches do not count
        for rel in ("app/build/gen/X.kt", ".gradle/c.kts", "app/src/main/build/y.txt"):
            f = self.root / rel
            f.parent.mkdir(parents=True, exist_ok=True)
            f.write_text("x", encoding="utf-8")
            os.utime(f, (future, future))
        self.assertEqual(self.run_v08(apk)[0], 0)

    def test_v08_empty_input_tree_is_exit_2(self):
        empty = self.tmp / "empty"
        empty.mkdir()
        self.assertEqual(self.run_v08(self.make_apk(), root=empty)[0], 2)

    def test_v08_missing_aapt2_is_exit_2(self):
        self.assertEqual(self.run_v08(self.make_apk(), aapt2=self.tmp / "no-aapt2.exe")[0], 2)

    def test_v08_permission_findings(self):
        def tree(*elems):
            t = self.CLEAN_TREE
            for el, name in elems:
                t += f'  E: {el} (line=3)\n    A: http://schemas.android.com/apk/res/android:name(0x01010003)="{name}" (Raw: "{name}")\n'
            return t
        apk = self.make_apk()
        bad = [("uses-permission", "android.permission.VIBRATE"),
               ("permission", "io.github.jamisuni.tangram.SOME_PERMISSION"),
               ("uses-permission", "android.permission.INTERNET"),
               ("uses-permission-sdk-23", "android.permission.ACCESS_NETWORK_STATE"),
               ("uses-permission", "com.android.vending.BILLING"),
               ("uses-feature", "android.hardware.wifi")]
        for el, name in bad:
            self.assert_fail(apk, f"permission {el} {name}", aapt2=self.fake_aapt2(tree((el, name))))
        self.assert_pass(apk, "--aapt2", str(self.fake_aapt2(tree(("uses-feature", "android.hardware.touchscreen")))))

    def test_v08_debug_mode_reports_androidx_permissions_as_notes(self):
        name = "io.github.jamisuni.tangram.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
        t = self.CLEAN_TREE
        for el in ("permission", "uses-permission"):
            t += f'  E: {el} (line=3)\n    A: android:name(0x01010003)="{name}" (Raw: "{name}")\n'
        dbg = self.make_apk(defined=[self.MAIN, self.PLAY, self.LOCALE, self.TESTCFG, self.PROBE])
        aapt = self.fake_aapt2(t)
        code, lines = self.run_v08(dbg, "--expect-debug", aapt2=aapt)
        self.assertEqual(code, 0, lines)
        notes = [l for l in lines if l.startswith("V-08 note permission")]
        self.assertEqual(len(notes), 2, lines)
        self.assertIn(name, notes[0])
        # the same manifest in release mode is a finding; a network name stays a finding in debug
        self.assert_fail(self.make_apk(), f"permission permission {name}", aapt2=aapt)
        net = self.fake_aapt2(self.CLEAN_TREE + '  E: uses-permission (line=3)\n'
                              '    A: android:name(0x01010003)="android.permission.INTERNET"\n')
        self.assert_fail(dbg, "permission uses-permission android.permission.INTERNET", "--expect-debug", aapt2=net)

    def test_v08_manifest_tree_without_uses_sdk_or_application_is_exit_2(self):
        apk = self.make_apk()
        for tree in ("E: manifest (line=2)\n  E: application (line=3)\n",
                     "E: manifest (line=2)\n  E: uses-sdk (line=3)\n",
                     "E: manifest (line=2)\n"):
            self.assertEqual(self.run_v08(apk, aapt2=self.fake_aapt2(tree))[0], 2, tree)

    def test_v08_aapt2_without_manifest_output_is_exit_2(self):
        self.assertEqual(self.run_v08(self.make_apk(), aapt2=self.fake_aapt2("garbage\n"))[0], 2)

    # ---- feedback callers (WO-007 V-1): crafted dex with real code_items and invoke-virtual ----

    ROGUE = "Lcom/example/Rogue;"

    # (kind, callee class, callee name): every target kind the design names
    FEEDBACK_TARGETS = [
        ("View.playSoundEffect", "Landroid/view/View;", "playSoundEffect"),
        ("View.performHapticFeedback", "Landroid/view/View;", "performHapticFeedback"),
        ("ViewCompat.performHapticFeedback", "Landroidx/core/view/ViewCompat;", "performHapticFeedback"),
        ("AudioManager.playSoundEffect", "Landroid/media/AudioManager;", "playSoundEffect"),
        ("Vibrator.vibrate", "Landroid/os/Vibrator;", "vibrate"),
        ("VibratorManager", "Landroid/os/VibratorManager;", "getDefaultVibrator"),
        ("VibrationEffect", "Landroid/os/VibrationEffect;", "createOneShot"),
        ("AudioTrack.play", "Landroid/media/AudioTrack;", "play"),
        ("SoundPool", "Landroid/media/SoundPool;", "play"),
        ("MediaPlayer", "Landroid/media/MediaPlayer;", "start"),
        ("ToneGenerator", "Landroid/media/ToneGenerator;", "startTone"),
        ("Ringtone", "Landroid/media/Ringtone;", "play"),
        ("HapticFeedback.performHapticFeedback", "Landroidx/compose/ui/hapticfeedback/HapticFeedback;",
         "performHapticFeedback"),
        ("SoundEffect.playClickSound", "Landroidx/compose/ui/platform/SoundEffect;", "playClickSound"),
        # CR-6 S3: the framework clicks or vibrates from these, and these media and speech players
        ("View.performClick", "Landroid/view/View;", "performClick"),
        ("View.performLongClick", "Landroid/view/View;", "performLongClick"),
        ("View.callOnClick", "Landroid/view/View;", "callOnClick"),
        ("CompoundButton.performClick", "Landroid/widget/CompoundButton;", "performClick"),
        ("AudioManager.adjustVolume", "Landroid/media/AudioManager;", "adjustVolume"),
        ("AudioManager.adjustStreamVolume", "Landroid/media/AudioManager;", "adjustStreamVolume"),
        ("AudioManager.adjustSuggestedStreamVolume", "Landroid/media/AudioManager;", "adjustSuggestedStreamVolume"),
        ("AudioManager.setStreamVolume", "Landroid/media/AudioManager;", "setStreamVolume"),
        ("MediaActionSound", "Landroid/media/MediaActionSound;", "play"),
        ("TextToSpeech.speak", "Landroid/speech/tts/TextToSpeech;", "speak"),
        ("TextToSpeech.playEarcon", "Landroid/speech/tts/TextToSpeech;", "playEarcon"),
    ]

    def with_callers(self, *extra, **kw):
        return self.make_apk(callers=[self.OK_CALLER, *extra], **kw)

    def allow_list_callers(self):
        out = []
        for cls, method, ecls, ename, _status, _reason in self.v08.FEEDBACK_ALLOW:
            out.append((cls, method, [(ecls, ename)]))
        return out

    def test_v08_feedback_allow_list_alone_passes(self):
        self.assert_pass(self.make_apk(callers=self.allow_list_callers()))

    def test_v08_feedback_allow_list_rows_are_exact_and_have_no_planned_entries(self):
        allow = self.v08.FEEDBACK_ALLOW
        self.assertEqual({e[4] for e in allow}, {"inventory", "WO-007"})
        self.assertTrue(all(len(e) == 6 and e[5] for e in allow))     # every row has a reason
        self.assertFalse(any("Lever" in e[0] or e[0].endswith("SilentHaptic;") or e[0].endswith("SilentSoundEffect;")
                             for e in allow))
        self.assertEqual(len({e[:4] for e in allow}), len(allow))      # no duplicate row
        self.assertEqual({(e[0], e[1], e[2], e[3]) for e in allow if e[4] == "WO-007"}, {
            ("Lio/github/jamisuni/tangram/settings/ViewHapticOut;", "tick", "Landroid/view/View;", "performHapticFeedback"),
            ("Lio/github/jamisuni/tangram/settings/AudioTrackSoundOut;", "play", "Landroid/media/AudioTrack;", "play")})
        da127 = [e for e in allow if e[0].endswith("HapticDefaults;")]
        self.assertEqual([e[:4] for e in da127], [(
            "Landroidx/compose/ui/platform/HapticDefaults;", "isPremiumVibratorEnabled",
            "Landroid/os/Vibrator;", "areAllPrimitivesSupported")])

    def test_v08_feedback_allowed_method_calling_its_pinned_callee_passes(self):
        cls = "Lio/github/jamisuni/tangram/settings/ViewHapticOut;"
        self.assert_pass(self.with_callers((cls, "tick", [("Landroid/view/View;", "performHapticFeedback")])))
        # mangled Kotlin method names compare on the part before the dash
        self.assertIsNotNone(self.v08.allow_entry(
            "Landroidx/compose/foundation/text/selection/SelectionManager;", "onRelease-abc123",
            "Landroidx/compose/ui/hapticfeedback/HapticFeedback;", "performHapticFeedback-CdsT49E"))

    def test_v08_feedback_allowed_class_calling_a_different_target_fails(self):
        cls = "Lio/github/jamisuni/tangram/settings/ViewHapticOut;"
        for method, ecls, ename in (("tick", "Landroid/os/Vibrator;", "vibrate"),                      # other callee
                                    ("buzz", "Landroid/view/View;", "performHapticFeedback"),          # other caller method
                                    ("tick", "Landroid/view/View;", "playSoundEffect")):               # other callee method
            apk = self.with_callers((cls, method, [(ecls, ename)]))
            self.assert_fail(apk, f"feedback-caller {cls}->{method}  calls {ecls}->{ename}")
        # the same holds for an inventory row: AudioTrackSoundOut may play, not vibrate
        cls = "Lio/github/jamisuni/tangram/settings/AudioTrackSoundOut;"
        self.assert_fail(self.with_callers((cls, "play", [("Landroid/os/Vibrator;", "vibrate")])),
                         f"feedback-caller {cls}->play")

    def test_v08_feedback_one_extra_caller_fails_for_each_target_kind(self):
        for kind, ccls, cname in self.FEEDBACK_TARGETS:
            apk = self.with_callers((self.ROGUE, "buzz", [(ccls, cname)]))
            code, lines = self.run_v08(apk)
            self.assertEqual(code, 1, (kind, lines))
            hits = [l for l in lines if l.startswith(f"V-08 FAIL feedback-caller {self.ROGUE}->buzz")]
            self.assertEqual(len(hits), 1, (kind, lines))
            self.assertIn(f"{ccls}->{cname}", hits[0])
            self.assertFalse(any("scanner blind" in l for l in lines), (kind, lines))

    def test_v08_feedback_s3_non_targets_do_not_fire(self):
        # AudioManager methods that play nothing, and an app-level method that only shares the name performClick
        calls = [("Landroid/media/AudioManager;", "getStreamVolume"),
                 ("Landroid/media/AudioManager;", "requestAudioFocus"),
                 ("Lcom/example/Own;", "performClick")]
        self.assert_pass(self.with_callers((self.ROGUE, "quiet", calls)))

    def test_v08_feedback_extra_caller_in_second_dex_fails(self):
        dexes = {"classes.dex": self.make_dex(**self.base()),
                 "classes2.dex": self.make_dex(defined=[], callers=[
                     (self.ROGUE, "buzz", [("Landroid/os/Vibrator;", "vibrate")])])}
        self.assert_fail(self.make_apk(dexes=dexes), f"feedback-caller {self.ROGUE}->buzz")

    def test_v08_feedback_non_playing_media_calls_are_named_exclusions(self):
        calls = [(c + ";", "x") for c in self.v08.dex_callers.NON_PLAYING_MEDIA]
        calls += [("Landroid/media/AudioManager;", "getStreamVolume"), ("Landroid/media/AudioTrack;", "<init>")]
        self.assertGreaterEqual(len(self.v08.dex_callers.NON_PLAYING_MEDIA), 4)
        self.assert_pass(self.with_callers((self.ROGUE, "quiet", calls)))

    def test_v08_feedback_blind_when_no_pinned_inventory_caller_is_found(self):
        planned = ("Lio/github/jamisuni/tangram/settings/AudioTrackSoundOut;", "play",
                   [("Landroid/media/AudioTrack;", "play")])
        for callers in ([], [planned], [(self.ROGUE, "quiet", [("Landroid/os/Handler;", "post")])]):
            code, lines = self.run_v08(self.make_apk(callers=callers))
            self.assertEqual(code, 1, lines)
            blind = [l for l in lines if "scanner blind feedback-caller" in l]
            self.assertEqual(len(blind), 1, lines)
            self.assertEqual([l for l in lines if not l.startswith("V-08 note")], blind, lines)

    def test_v08_feedback_malformed_code_item_is_exit_2(self):
        for fault in ("short", "size", "method", "code_off"):
            dex = self.make_dex(**self.base(), code_fault=fault)
            code, lines = self.run_v08(self.make_apk(dex=dex))
            self.assertEqual(code, 2, (fault, lines))
            with self.assertRaises(self.v08.DexError):
                self.v08.dex_callers.find_callers(dex)

    def test_v08_feedback_find_callers_returns_exact_hits(self):
        dex = self.make_dex(**self.base(callers=[
            self.OK_CALLER, (self.ROGUE, "m", [("Landroid/os/Vibrator;", "vibrate"), ("Landroid/os/Handler;", "post")])]))
        self.assertEqual(self.v08.dex_callers.find_callers(dex), {
            (self.OK_CALLER[0], "playClickSound", "Landroid/view/View;", "playSoundEffect"),
            (self.ROGUE, "m", "Landroid/os/Vibrator;", "vibrate")})

    def test_v08_feedback_dex_callers_fails_closed_on_bad_tables(self):
        good = self.make_dex(**self.base())
        self.v08.dex_callers.find_callers(good)
        with self.assertRaises(self.v08.DexError):
            self.v08.dex_callers.find_callers(self.make_dex(**self.base(), bad_magic=True))
        with self.assertRaises(self.v08.DexError):
            self.v08.dex_callers.find_callers(self.make_dex(**self.base(), truncate=10))
        for off, size in ((0x58, 5000), (0x60, 5000), (0x40, 5000)):   # method_ids, class_defs, type_ids beyond the file
            cut = bytearray(good)
            struct.pack_into("<I", cut, off, size)
            with self.assertRaises(self.v08.DexError, msg=hex(off)):
                self.v08.dex_callers.find_callers(bytes(cut))

    def test_v08_files_are_utf8_lf_no_nul(self):
        for name in ("v08_promise_apk.py", "dex_callers.py", "test_verifiers.py"):
            raw = (Path(__file__).parent / name).read_bytes()
            raw.decode("utf-8")
            self.assertNotIn(b"\r", raw, name)
            self.assertNotIn(b"\x00", raw, name)


class TestV09ReleaseLibrary(unittest.TestCase):
    """Fixture tests of V-09 (TASK-093). Scaffolding: the acceptance tests are written elsewhere."""

    THEMES = ["shapes", "animals", "things", "nature", "vehicles"]

    @classmethod
    def setUpClass(cls):
        import json as _json
        import v09_release_library as v09
        cls.json = _json
        cls.v09 = v09
        cls.review_hash = staticmethod(v09.review_hash)

    def puzzle(self, pid, theme, flag=False, inline=False):
        if inline:   # the minis' form: the flag inside an inline provenance object
            prov = '"provenance": {"source": "x", "reviewedByHuman": %s}' % ("true" if flag else "false")
        else:
            prov = '"provenance": {\n    "source": "x",\n    "reviewedByHuman": %s\n  }' % ("true" if flag else "false")
        return ('{\n  "id": "%s",\n  "category": "%s",\n  %s\n}\n' % (pid, theme, prov)).encode("utf-8")

    def library(self, n=20, reviewed=True, with_square=True, themes=None, inline_every=5):
        """(files, ledger) of n puzzles; all flagged and ledgered when reviewed."""
        themes = themes or self.THEMES
        files, ledger = {}, {}
        for i in range(n):
            pid = "shapes-square" if (i == 0 and with_square) else "p%02d" % i
            theme = "shapes" if pid == "shapes-square" else themes[i % len(themes)]
            raw = self.puzzle(pid, theme, flag=reviewed, inline=(i % inline_every == 0))
            files[pid + ".json"] = raw
            if reviewed:
                ledger[pid] = {"sha256": self.review_hash(raw.decode("utf-8")), "by": "t", "date": "2026-10-05"}
        return files, ledger

    def test_v09_full_pass(self):
        files, ledger = self.library(22)
        self.assertEqual(self.v09.check(files, ledger, 20, False), [])
        self.assertEqual(self.v09.summary(files, ledger), (22, 22, 5))

    def test_v09_uses_the_review_hash_of_puzzle_review(self):
        import puzzle_review
        self.assertIs(self.v09.review_hash, puzzle_review.review_hash)

    def test_v09_unreviewed_flag_false(self):
        files, ledger = self.library(22)
        raw = self.puzzle("p03", "things", flag=False)
        files["p03.json"] = raw
        f = self.v09.check(files, ledger, 20, False)
        self.assertIn("unreviewed p03", f)

    def test_v09_unreviewed_inline_form(self):
        files, ledger = self.library(22)
        files["p05.json"] = self.puzzle("p05", "shapes", flag=False, inline=True)
        self.assertIn("unreviewed p05", self.v09.check(files, ledger, 20, False))

    def test_v09_both_flag_forms_pass_when_reviewed(self):
        files, ledger = self.library(20, inline_every=2)
        self.assertTrue(any(b"reviewedByHuman\": true}" in r for r in files.values()))
        self.assertTrue(any(b"\n    \"reviewedByHuman\": true\n" in r for r in files.values()))
        self.assertEqual(self.v09.check(files, ledger, 20, False), [])

    def test_v09_no_ledger_entry_is_unledgered(self):
        files, ledger = self.library(22)
        del ledger["p04"]
        self.assertIn("unledgered p04", self.v09.check(files, ledger, 20, False))

    def test_v09_stale_entry(self):
        files, ledger = self.library(22)
        files["p04.json"] = files["p04.json"].replace(b'"source": "x"', b'"source": "y"')
        f = self.v09.check(files, ledger, 20, False)
        self.assertIn("changed after approval p04", f)

    def test_v09_flag_flip_alone_is_not_stale_but_crlf_is_equal(self):
        files, ledger = self.library(22)
        files["p04.json"] = files["p04.json"].replace(b"\n", b"\r\n")   # the hash normalises CRLF
        self.assertEqual(self.v09.check(files, ledger, 20, False), [])

    def test_v09_too_few_puzzles(self):
        files, ledger = self.library(19)
        f = self.v09.check(files, ledger, 20, False)
        self.assertIn("count 19 of 19 reviewed (< 20)", f)
        self.assertIn("count 19 puzzles (< 20)", self.v09.check(files, ledger, 20, True))

    def test_v09_too_few_themes(self):
        files, ledger = self.library(22, themes=["shapes", "animals", "things"])
        self.assertIn("themes 3 (< 4)", self.v09.check(files, ledger, 20, False))

    def test_v09_missing_square(self):
        files, ledger = self.library(22, with_square=False)
        self.assertIn("missing shapes-square", self.v09.check(files, ledger, 20, False))

    def test_v09_unreadable_file_is_a_finding(self):
        files, ledger = self.library(22)
        files["bad.json"] = b"{not json"
        self.assertIn("unreadable bad.json", self.v09.check(files, ledger, 20, False))

    def test_v09_schema_and_non_json_are_ignored(self):
        files, ledger = self.library(22)
        files["puzzle.schema.json"] = b"{}"
        files["index.txt"] = b"x\n"
        self.assertEqual(self.v09.check(files, ledger, 20, False), [])

    def test_v09_pre_review_passes_with_zero_reviewed(self):
        files, ledger = self.library(25, reviewed=False)
        self.assertEqual(self.v09.check(files, {}, 20, True), [])
        strict = self.v09.check(files, {}, 20, False)
        self.assertIn("count 0 of 25 reviewed (< 20)", strict)
        self.assertEqual(sum(1 for x in strict if x.startswith("unreviewed")), 25)

    def test_v09_pre_review_still_fails_stale_unledgered_themes_square(self):
        files, ledger = self.library(22)
        files["p04.json"] = files["p04.json"].replace(b'"source": "x"', b'"source": "y"')
        del ledger["p05"]
        f = self.v09.check(files, ledger, 20, True)
        self.assertIn("changed after approval p04", f)
        self.assertIn("unledgered p05", f)
        files2, ledger2 = self.library(22, with_square=False, themes=["shapes", "animals"])
        f2 = self.v09.check(files2, ledger2, 20, True)
        self.assertIn("missing shapes-square", f2)
        self.assertIn("themes 2 (< 4)", f2)

    def write_dir(self, root, files):
        d = Path(root) / "Tangrams"
        d.mkdir()
        for n, raw in files.items():
            (d / n).write_bytes(raw)
        return d

    def run_main(self, argv):
        import io
        import contextlib
        buf = io.StringIO()
        with contextlib.redirect_stdout(buf):
            code = self.v09.main(argv)
        return code, buf.getvalue().splitlines()

    def test_v09_main_dir_pass_and_fail_lines(self):
        files, ledger = self.library(21)
        with tempfile.TemporaryDirectory() as tmp:
            d = self.write_dir(tmp, files)
            lp = Path(tmp) / "ledger.json"
            lp.write_text(self.json.dumps(ledger), encoding="utf-8")
            code, lines = self.run_main(["--dir", str(d), "--ledger", str(lp)])
            self.assertEqual(code, 0, lines)
            self.assertEqual(lines[-1], "V-09 PASS")
            self.assertIn("V-09 library: 21 of 21 reviewed, 5 themes", lines)
            code, lines = self.run_main(["--dir", str(d), "--ledger", str(Path(tmp) / "none.json")])
            self.assertEqual(code, 1, lines)   # a missing ledger is empty: every flagged puzzle is unledgered
            self.assertEqual(lines[-1], "V-09 FAIL")
            self.assertIn("V-09 FAIL unledgered p03", lines)
            code, lines = self.run_main(["--dir", str(d), "--ledger", str(Path(tmp) / "none.json"), "--pre-review"])
            self.assertEqual(code, 1, lines)

    def test_v09_main_pre_review_zero_reviewed_exit_0_and_ledger_warning(self):
        files, _ = self.library(25, reviewed=False)
        with tempfile.TemporaryDirectory() as tmp:
            d = self.write_dir(tmp, files)
            lp = Path(tmp) / "ledger.json"
            lp.write_text(self.json.dumps({"ghost": {"sha256": "0", "by": "t", "date": "d"}}), encoding="utf-8")
            code, lines = self.run_main(["--dir", str(d), "--ledger", str(lp), "--pre-review"])
            self.assertEqual(code, 0, lines)
            self.assertIn("V-09 warn ledger entry without file ghost", lines)
            self.assertTrue(any("0 of 25 reviewed" in l for l in lines), lines)
            code, lines = self.run_main(["--dir", str(d), "--ledger", str(lp)])
            self.assertEqual(code, 1)

    def test_v09_main_apk_input(self):
        files, ledger = self.library(20)
        with tempfile.TemporaryDirectory() as tmp:
            apk = Path(tmp) / "tiny.apk"
            import zipfile as _zf
            with _zf.ZipFile(apk, "w") as z:
                z.writestr("AndroidManifest.xml", b"x")
                for n, raw in files.items():
                    z.writestr("tangrams/" + n, raw)
                z.writestr("tangrams/index.txt", "\n".join(sorted(files)) + "\n")
            lp = Path(tmp) / "ledger.json"
            lp.write_text(self.json.dumps(ledger), encoding="utf-8")
            code, lines = self.run_main(["--apk", str(apk), "--ledger", str(lp)])
            self.assertEqual(code, 0, lines)
            lp.write_text("{}", encoding="utf-8")
            code, lines = self.run_main(["--apk", str(apk), "--ledger", str(lp)])
            self.assertEqual(code, 1, lines)
            self.assertIn("V-09 FAIL unledgered p03", lines)

    def test_v09_main_usage_errors_exit_2(self):
        self.assertEqual(self.run_main(["--bogus"])[0], 2)
        self.assertEqual(self.run_main(["--dir", "a", "--apk", "b"])[0], 2)
        self.assertEqual(self.run_main(["--apk", "does-not-exist.apk"])[0], 2)
        self.assertEqual(self.run_main(["--dir", "does-not-exist"])[0], 2)

    def test_v09_main_is_read_only(self):
        files, ledger = self.library(21)
        with tempfile.TemporaryDirectory() as tmp:
            d = self.write_dir(tmp, files)
            lp = Path(tmp) / "ledger.json"
            lp.write_text(self.json.dumps(ledger), encoding="utf-8")
            before = sorted((str(p), p.read_bytes()) for p in Path(tmp).rglob("*") if p.is_file())
            self.run_main(["--dir", str(d), "--ledger", str(lp)])
            after = sorted((str(p), p.read_bytes()) for p in Path(tmp).rglob("*") if p.is_file())
            self.assertEqual(before, after)

    def test_v09_files_are_utf8_lf_no_nul(self):
        for name in ("v09_release_library.py",):
            raw = (Path(__file__).parent / name).read_bytes()
            raw.decode("utf-8")
            self.assertNotIn(b"\r", raw, name)
            self.assertNotIn(b"\x00", raw, name)


if __name__ == "__main__":
    unittest.main()
