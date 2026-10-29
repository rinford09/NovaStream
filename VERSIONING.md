# Versioning & Release Protocol

This repository follows [Semantic Versioning (SemVer)](https://semver.org/) and maintains immutable Git tags for every release and significant milestone.

---

## 🏷️ Version Stamp Format

Versions are formatted as:
```
v4.8.0-jam-<MAJOR>.<MINOR>.<PATCH>
```
* **MAJOR**: Architectural changes or major UI rewrites (e.g., `1.0.0` -> `2.0.0`).
* **MINOR**: New features (e.g., new filter categories, new player controls, new download managers) (e.g., `1.0.0` -> `1.1.0`).
* **PATCH**: Bug fixes, styling adjustments, or performance enhancements (e.g., `1.0.0` -> `1.0.1`).

---

## 📋 Steps for Any Future Update

Whenever making an update:

1. **Update Project Version**:
   In `gradle/libs.versions.toml`:
   - Increment `versionCode` (e.g., `69` -> `70`).
   - Update `versionName` (e.g., `"4.8.0-jam-1.1.0"`).

2. **Update `CHANGELOG.md`**:
   Add a new entry at the top of `CHANGELOG.md`:
   ```markdown
   ## [4.8.0-jam-X.Y.Z] - YYYY-MM-DD
   ### Milestone: <Title of Update>
   * **Git Tag**: `v4.8.0-jam-X.Y.Z`
   
   ### Added
   - Description of added features...
   
   ### Fixed
   - Description of bugs fixed...
   
   ### Changed
   - Description of modifications...
   ```

3. **Commit and Tag**:
   ```powershell
   git add .
   git commit -m "feat/fix: <summary of changes> (v4.8.0-jam-X.Y.Z)"
   git tag -a v4.8.0-jam-X.Y.Z -m "Release v4.8.0-jam-X.Y.Z: <summary>"
   ```

4. **Push with Tags**:
   ```powershell
   git push origin main --tags
   ```

---

## ⏪ Rollback Instructions

You never need manual backup folders. To revert or test any historical state:

* **View all version checkpoints**:
  ```powershell
  git tag -n
  ```

* **Roll back / inspect any version**:
  ```powershell
  git checkout v4.8.0-jam-1.0.0
  ```

* **Create a new branch from an old version**:
  ```powershell
  git checkout -b fix-from-v1.0.0 v4.8.0-jam-1.0.0
  ```

* **Return to latest code**:
  ```powershell
  git checkout main
  ```
