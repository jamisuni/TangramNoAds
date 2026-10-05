"""SCAFFOLDING: Self-test for tools/zip_same_content.py (TASK-096): compares two zip files."""
import shutil
import sys
import tempfile
import unittest
import warnings
import zipfile
from pathlib import Path

TOOLS = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(TOOLS))
import zip_same_content as zsc  # noqa: E402


class ZipSameContent(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.tmp, True)

    def create_zip(self, entries):
        """Create a zip file with given entries.

        Args:
            entries: Dict of {path: content_bytes}

        Returns:
            Path to the created zip file
        """
        p = self.tmp / f"test_{id(entries)}.zip"
        with zipfile.ZipFile(p, "w") as z:
            for path, content in entries.items():
                z.writestr(path, content)
        return p

    def create_zip_with_duplicates(self, entries_list):
        """Create a zip file with duplicate entries (in order).

        Args:
            entries_list: List of (path, content_bytes) tuples, allowing duplicates

        Returns:
            Path to the created zip file
        """
        p = self.tmp / f"test_{id(entries_list)}.zip"
        with zipfile.ZipFile(p, "w") as z:
            with warnings.catch_warnings():
                warnings.simplefilter("ignore")
                for path, content in entries_list:
                    z.writestr(path, content)
        return p

    def test_equal_zips_return_0(self):
        """Equal zips should return 0."""
        entries = {"file.txt": b"content", "dir/file.txt": b"data"}
        zip_a = self.create_zip(entries)
        zip_b = self.create_zip(entries)

        equal, diff = zsc.compare_zips(zip_a, zip_b)
        self.assertTrue(equal)
        self.assertIsNone(diff)
        self.assertEqual(zsc.main([str(zip_a), str(zip_b)]), 0)

    def test_extra_entry_returns_1(self):
        """Extra entry in one zip should return 1."""
        entries_a = {"file.txt": b"content"}
        entries_b = {"file.txt": b"content", "extra.txt": b"extra"}
        zip_a = self.create_zip(entries_a)
        zip_b = self.create_zip(entries_b)

        equal, diff = zsc.compare_zips(zip_a, zip_b)
        self.assertFalse(equal)
        self.assertEqual(diff, "extra.txt")
        self.assertEqual(zsc.main([str(zip_a), str(zip_b)]), 1)

    def test_missing_entry_returns_1(self):
        """Missing entry in one zip should return 1."""
        entries_a = {"file.txt": b"content", "other.txt": b"data"}
        entries_b = {"file.txt": b"content"}
        zip_a = self.create_zip(entries_a)
        zip_b = self.create_zip(entries_b)

        equal, diff = zsc.compare_zips(zip_a, zip_b)
        self.assertFalse(equal)
        self.assertEqual(diff, "other.txt")
        self.assertEqual(zsc.main([str(zip_a), str(zip_b)]), 1)

    def test_changed_byte_returns_1(self):
        """Changed byte in file should return 1."""
        entries_a = {"file.txt": b"content"}
        entries_b = {"file.txt": b"contend"}
        zip_a = self.create_zip(entries_a)
        zip_b = self.create_zip(entries_b)

        equal, diff = zsc.compare_zips(zip_a, zip_b)
        self.assertFalse(equal)
        self.assertEqual(diff, "file.txt")
        self.assertEqual(zsc.main([str(zip_a), str(zip_b)]), 1)

    def test_changed_top_level_meta_inf_returns_0(self):
        """Changed top-level META-INF/MANIFEST.MF should return 0."""
        entries_a = {
            "file.txt": b"content",
            "META-INF/MANIFEST.MF": b"version 1",
        }
        entries_b = {
            "file.txt": b"content",
            "META-INF/MANIFEST.MF": b"version 2",
        }
        zip_a = self.create_zip(entries_a)
        zip_b = self.create_zip(entries_b)

        equal, diff = zsc.compare_zips(zip_a, zip_b)
        self.assertTrue(equal)
        self.assertIsNone(diff)
        self.assertEqual(zsc.main([str(zip_a), str(zip_b)]), 0)

    def test_changed_nested_meta_inf_returns_1(self):
        """Changed nested base/root/META-INF/x should return 1."""
        entries_a = {
            "file.txt": b"content",
            "base/root/META-INF/x": b"original",
        }
        entries_b = {
            "file.txt": b"content",
            "base/root/META-INF/x": b"modified",
        }
        zip_a = self.create_zip(entries_a)
        zip_b = self.create_zip(entries_b)

        equal, diff = zsc.compare_zips(zip_a, zip_b)
        self.assertFalse(equal)
        self.assertEqual(diff, "base/root/META-INF/x")
        self.assertEqual(zsc.main([str(zip_a), str(zip_b)]), 1)

    def test_bad_path_returns_2(self):
        """Bad path (non-existent file) should return 2."""
        bad_path = self.tmp / "nonexistent.zip"
        good_zip = self.create_zip({"file.txt": b"content"})

        equal, diff = zsc.compare_zips(str(bad_path), str(good_zip))
        self.assertIsNone(equal)
        self.assertIsNotNone(diff)
        self.assertEqual(zsc.main([str(bad_path), str(good_zip)]), 2)

    def test_usage_error_wrong_arg_count_returns_2(self):
        """Wrong number of arguments should return 2."""
        self.assertEqual(zsc.main([]), 2)
        self.assertEqual(zsc.main(["only_one"]), 2)
        self.assertEqual(zsc.main(["one", "two", "three"]), 2)

    def test_multiple_top_level_meta_inf_entries(self):
        """Multiple top-level META-INF entries should be ignored."""
        entries_a = {
            "file.txt": b"content",
            "META-INF/MANIFEST.MF": b"v1",
            "META-INF/CERT.SF": b"cert1",
            "META-INF/CERT.RSA": b"rsa1",
        }
        entries_b = {
            "file.txt": b"content",
            "META-INF/MANIFEST.MF": b"v2",
            "META-INF/CERT.SF": b"cert2",
            "META-INF/CERT.RSA": b"rsa2",
        }
        zip_a = self.create_zip(entries_a)
        zip_b = self.create_zip(entries_b)

        equal, diff = zsc.compare_zips(zip_a, zip_b)
        self.assertTrue(equal)
        self.assertIsNone(diff)
        self.assertEqual(zsc.main([str(zip_a), str(zip_b)]), 0)

    def test_nested_meta_inf_at_different_level(self):
        """Nested META-INF at any depth level should be compared."""
        entries_a = {"META-INF/sub/file.txt": b"content"}
        entries_b = {"META-INF/sub/file.txt": b"different"}
        zip_a = self.create_zip(entries_a)
        zip_b = self.create_zip(entries_b)

        equal, diff = zsc.compare_zips(zip_a, zip_b)
        self.assertFalse(equal)
        self.assertEqual(diff, "META-INF/sub/file.txt")

    def test_empty_zips(self):
        """Empty zips should be equal."""
        zip_a = self.create_zip({})
        zip_b = self.create_zip({})

        equal, diff = zsc.compare_zips(zip_a, zip_b)
        self.assertTrue(equal)
        self.assertIsNone(diff)
        self.assertEqual(zsc.main([str(zip_a), str(zip_b)]), 0)

    def test_only_meta_inf_difference(self):
        """Zips with only top-level META-INF differences should be equal."""
        entries_a = {"META-INF/MANIFEST.MF": b"original"}
        entries_b = {"META-INF/MANIFEST.MF": b"changed"}
        zip_a = self.create_zip(entries_a)
        zip_b = self.create_zip(entries_b)

        equal, diff = zsc.compare_zips(zip_a, zip_b)
        self.assertTrue(equal)
        self.assertEqual(zsc.main([str(zip_a), str(zip_b)]), 0)

    def test_corrupt_zip_returns_2(self):
        """Corrupt zip file should return 2."""
        bad_zip = self.tmp / "corrupt.zip"
        bad_zip.write_bytes(b"not a zip file")
        good_zip = self.create_zip({"file.txt": b"content"})

        equal, diff = zsc.compare_zips(str(bad_zip), str(good_zip))
        self.assertIsNone(equal)
        self.assertIsNotNone(diff)
        self.assertEqual(zsc.main([str(bad_zip), str(good_zip)]), 2)

    def test_duplicate_entry_in_a_returns_1(self):
        """Duplicate entry in zip A should return 1."""
        zip_a = self.create_zip_with_duplicates([
            ("file.txt", b"content"),
            ("file.txt", b"duplicate"),
        ])
        zip_b = self.create_zip({"file.txt": b"content"})

        equal, diff = zsc.compare_zips(str(zip_a), str(zip_b))
        self.assertFalse(equal)
        self.assertEqual(diff, "duplicate entry: file.txt")
        self.assertEqual(zsc.main([str(zip_a), str(zip_b)]), 1)

    def test_duplicate_entry_in_b_returns_1(self):
        """Duplicate entry in zip B should return 1."""
        zip_a = self.create_zip({"file.txt": b"content"})
        zip_b = self.create_zip_with_duplicates([
            ("file.txt", b"content"),
            ("file.txt", b"duplicate"),
        ])

        equal, diff = zsc.compare_zips(str(zip_a), str(zip_b))
        self.assertFalse(equal)
        self.assertEqual(diff, "duplicate entry: file.txt")
        self.assertEqual(zsc.main([str(zip_a), str(zip_b)]), 1)

    def test_duplicate_in_top_level_meta_inf_returns_0(self):
        """Duplicate entry under top-level META-INF/ should return 0."""
        zip_a = self.create_zip_with_duplicates([
            ("file.txt", b"content"),
            ("META-INF/MANIFEST.MF", b"version1"),
            ("META-INF/MANIFEST.MF", b"version2"),
        ])
        zip_b = self.create_zip({"file.txt": b"content"})

        equal, diff = zsc.compare_zips(str(zip_a), str(zip_b))
        self.assertTrue(equal)
        self.assertIsNone(diff)
        self.assertEqual(zsc.main([str(zip_a), str(zip_b)]), 0)

    def test_duplicate_in_nested_meta_inf_returns_1(self):
        """Duplicate entry in nested META-INF should return 1."""
        zip_a = self.create_zip_with_duplicates([
            ("file.txt", b"content"),
            ("base/META-INF/x", b"orig1"),
            ("base/META-INF/x", b"orig2"),
        ])
        zip_b = self.create_zip({"file.txt": b"content"})

        equal, diff = zsc.compare_zips(str(zip_a), str(zip_b))
        self.assertFalse(equal)
        self.assertEqual(diff, "duplicate entry: base/META-INF/x")
        self.assertEqual(zsc.main([str(zip_a), str(zip_b)]), 1)


if __name__ == "__main__":
    unittest.main()
