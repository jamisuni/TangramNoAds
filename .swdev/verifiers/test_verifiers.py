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


if __name__ == "__main__":
    unittest.main()
