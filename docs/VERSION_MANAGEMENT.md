# Version Management Guide

This document describes the version management workflow for the lift-simulator project.

## Overview

The project uses automated tools to simplify version bumping across multiple files:

1. **Maven Versions Plugin** - Updates `pom.xml`
2. **NPM Version Command** - Updates `frontend/package.json` and `package-lock.json`
3. **Custom Bump Script** - Coordinates all tools and updates README files
4. **Manual CHANGELOG** - High-quality, detailed release documentation

## Philosophy

This project maintains **high-quality, detailed CHANGELOG entries** with:
- Comprehensive feature descriptions
- Technical implementation details
- Benefits and rationale sections
- Code references and examples

While automated CHANGELOG generation tools exist, they produce generic entries that don't match the quality and detail of manual documentation. Therefore, **CHANGELOG updates are manual**.

## Quick Start

### Standard Release Workflow

```bash
# 1. Make your changes and commit
git add .
git commit -m "Add comprehensive logging to GlobalExceptionHandler"

# 2. Update CHANGELOG.md manually with detailed entry
# (See CHANGELOG format section below)

# 3. Bump version across all files
./scripts/bump-version.sh 0.41.0

# 4. Review changes
git diff

# 5. Commit and tag
git add -A
git commit -m "chore(release): bump version to 0.41.0"
git tag -a v0.41.0 -m "Release v0.41.0"

# 6. Push
git push && git push --tags
```

## Version Bump Script

The `bump-version.sh` script is the primary tool for version updates.

### Usage

```bash
./scripts/bump-version.sh <new-version>
```

**Example:**
```bash
./scripts/bump-version.sh 0.41.0
```

### What It Does

1. **Validates version format** (semantic versioning: X.Y.Z)
2. **Updates pom.xml** using Maven Versions Plugin
3. **Updates frontend/package.json** using NPM version command
4. **Updates frontend/package-lock.json** automatically via NPM
5. **Updates README.md** - replaces all version references and JAR filenames
6. **Updates frontend/README.md** - replaces JAR filename references

### What It Does NOT Do

- ❌ Does NOT update CHANGELOG.md (you do this manually)
- ❌ Does NOT create git commits (you commit manually)
- ❌ Does NOT create git tags (you tag manually)

This gives you full control over the commit message and allows you to review all changes before committing.

### Output

The script provides helpful next-step instructions:

```
==========================================
Version bump complete!
==========================================

Updated files:
  - pom.xml
  - frontend/package.json
  - frontend/package-lock.json
  - README.md
  - frontend/README.md

Next steps:
  1. Update CHANGELOG.md with release notes
  2. Review changes: git diff
  3. Commit: git add -A && git commit -m 'chore: bump version to 0.41.0'
  4. Tag: git tag -a v0.41.0 -m 'Release v0.41.0'
  5. Push: git push && git push --tags
```

## CHANGELOG Format

Follow the existing format in `CHANGELOG.md`:

### Structure

```markdown
## [X.Y.Z] - YYYY-MM-DD

### Added
- **Feature Name**: Brief description
  - Detailed bullet point 1
  - Detailed bullet point 2
  - Sub-bullets with implementation details
  - **Benefits**:
    - Benefit 1
    - Benefit 2

### Changed
- Version bumped from X.Y.Z to X.Y.Z
- Other changes

### Fixed
- Bug fix descriptions

### Technical Details
- Implementation details
- Dependencies
- Configuration changes
- Performance characteristics
```

### Example Entry

```markdown
## [0.40.0] - 2026-01-17

### Added
- **Comprehensive Logging to GlobalExceptionHandler**: Added SLF4J-based logging for all exception handlers
  - **SLF4J Logger Integration**: Added `org.slf4j.Logger` and `org.slf4j.LoggerFactory` imports
  - **ERROR-level Logging**: Generic exception handler (`handleGenericException`) logs unexpected errors with full stack traces
    - Logs exception message and complete stack trace for debugging production issues
    - Critical for troubleshooting unexpected 500 errors
  - **WARN-level Logging**: Malformed JSON request handler (`handleHttpMessageNotReadable`)
    - Logs malformed JSON requests with specific details for unknown properties
  - **INFO-level Logging**: Business exception handlers log expected operational errors
    - `handleResourceNotFound`: Logs 404 resource not found errors
    - `handleIllegalArgument`: Logs 400 bad request errors
  - **Benefits**:
    - Enables debugging of production issues with full stack traces
    - Provides audit trail of all exceptions
    - Improves system observability for operations teams

### Changed
- Version bumped from 0.39.1 to 0.40.0
- Frontend package version updated to 0.40.0
- Enhanced JavaDoc for `GlobalExceptionHandler` class to document logging capabilities

### Technical Details
- Uses SLF4J API with Logback implementation (included in `spring-boot-starter-web`)
- No additional dependencies required
- Logging levels follow industry best practices:
  - ERROR: Unexpected errors requiring immediate investigation
  - WARN: Malformed requests that may indicate client issues
  - INFO: Expected business errors and operational events
  - DEBUG: Verbose validation details for development/debugging
- Stack traces included only for ERROR-level logs to balance detail with log volume
- All log messages use parameterized logging (SLF4J `{}` placeholders) for performance
```

### Tips for Writing CHANGELOG Entries

1. **Be detailed** - Include what, why, and how
2. **Include code references** - File paths, line numbers, class names
3. **Document benefits** - Why does this matter to users/developers?
4. **Add technical details** - Implementation notes, dependencies, configuration
5. **Use consistent formatting** - Follow the established pattern
6. **Cross-reference** - Link to ADRs, issues, or PRs when relevant

## Semantic Versioning

Follow [Semantic Versioning 2.0.0](https://semver.org/):

- **MAJOR** (X.0.0) - Breaking changes, incompatible API changes
- **MINOR** (0.X.0) - New features, backward-compatible additions
- **PATCH** (0.0.X) - Bug fixes, backward-compatible fixes

### Examples

| Change Type | Example | Version Bump |
|------------|---------|--------------|
| New feature | Add logging infrastructure | 0.39.1 → 0.40.0 (MINOR) |
| Bug fix | Fix validation error | 0.40.0 → 0.40.1 (PATCH) |
| Breaking change | Redesign API endpoints | 0.40.0 → 1.0.0 (MAJOR) |
| Documentation | Update README | 0.40.0 → 0.40.1 (PATCH) |

## Detailed Workflow

### Step-by-Step Release Process

#### 1. Make Changes

```bash
# Create feature branch (optional)
git checkout -b feature/add-logging

# Make your changes
# ... edit files ...

# Commit changes
git add .
git commit -m "Add comprehensive logging to GlobalExceptionHandler"
```

#### 2. Update CHANGELOG.md

Open `CHANGELOG.md` and add a new entry at the top:

```markdown
## [0.41.0] - 2026-01-17

### Added
- **Feature Name**: Description
  - Implementation details
  - Benefits

### Changed
- Version bumped from 0.40.0 to 0.41.0

### Technical Details
- Technical implementation notes
```

Make sure to:
- Set correct version number
- Set correct date (today's date)
- Use detailed descriptions
- Follow existing format

#### 3. Run Version Bump Script

```bash
./scripts/bump-version.sh 0.41.0
```

This updates all version references across the repository.

#### 4. Review Changes

```bash
# See all modified files
git status

# Review specific changes
git diff pom.xml
git diff frontend/package.json
git diff README.md
git diff CHANGELOG.md
```

Verify:
- ✅ pom.xml has correct version
- ✅ package.json has correct version
- ✅ README files have correct JAR filenames
- ✅ CHANGELOG has detailed entry

#### 5. Commit Release

```bash
# Stage all changes
git add -A

# Create release commit
git commit -m "chore(release): bump version to 0.41.0"

# Or more detailed message:
git commit -m "chore(release): bump version to 0.41.0

- Update version across all files
- Add comprehensive CHANGELOG entry
- Update README with new version references"
```

#### 6. Create Git Tag

```bash
# Create annotated tag
git tag -a v0.41.0 -m "Release v0.41.0"

# Or with more detail:
git tag -a v0.41.0 -m "Release v0.41.0

Add comprehensive logging to GlobalExceptionHandler
- SLF4J-based logging with appropriate log levels
- ERROR, WARN, INFO, DEBUG levels
- Full stack traces for production debugging"
```

#### 7. Push to Remote

```bash
# Push commits
git push

# Push tags
git push --tags

# Or push both in one command
git push && git push --tags
```

## Individual Tool Usage

### Maven Versions Plugin

Update Maven version only:

```bash
# Set new version
mvn versions:set -DnewVersion=0.41.0

# Commit changes (removes backup files)
mvn versions:commit

# Or revert if needed
mvn versions:revert
```

### NPM Version Command

Update NPM version only:

```bash
cd frontend

# Set specific version
npm version 0.41.0 --no-git-tag-version

# Or use semver bump commands
npm version patch --no-git-tag-version  # 0.40.0 → 0.40.1
npm version minor --no-git-tag-version  # 0.40.0 → 0.41.0
npm version major --no-git-tag-version  # 0.40.0 → 1.0.0
```

The `--no-git-tag-version` flag prevents NPM from creating automatic git commits/tags (we handle this manually).

## Version References in Code

### Current Locations

Version numbers appear in these files:

1. **pom.xml** (line 17)
   ```xml
   <version>0.40.0</version>
   ```

2. **frontend/package.json** (line 4)
   ```json
   "version": "0.40.0"
   ```

3. **frontend/package-lock.json** (auto-updated by NPM)

4. **README.md** (multiple locations)
   - Current version badge (line 7)
   - JAR filenames in command examples (15+ occurrences)

5. **frontend/README.md**
   - JAR filename in deployment example

6. **CHANGELOG.md**
   - New entry header
   - "Version bumped from X to Y" line

### Future: Maven Resource Filtering

**Optional improvement:** Use Maven placeholders in README instead of hardcoded versions.

**In README.md:**
```markdown
Current version: **${project.version}**

java -jar target/lift-simulator-${project.version}.jar
```

**In pom.xml:**
```xml
<build>
    <resources>
        <resource>
            <directory>${project.basedir}</directory>
            <includes>
                <include>README.md</include>
            </includes>
            <filtering>true</filtering>
            <targetPath>${project.build.directory}</targetPath>
        </resource>
    </resources>
</build>
```

This is currently **not implemented** because:
- It adds build complexity
- The bump script handles it reliably
- We prefer source files to be readable without build processing

## Troubleshooting

### Script fails with "Invalid version format"

**Problem:** Version doesn't match X.Y.Z pattern

**Solution:** Use semantic versioning format:
```bash
# Correct
./scripts/bump-version.sh 0.41.0

# Incorrect
./scripts/bump-version.sh v0.41.0  # Remove 'v' prefix
./scripts/bump-version.sh 0.41     # Must have patch version
```

### Maven versions:set fails

**Problem:** Maven Versions Plugin not working

**Solution:** Ensure you're in the project root:
```bash
cd /home/user/lift-simulator
./scripts/bump-version.sh 0.41.0
```

### NPM version fails

**Problem:** NPM can't update package.json

**Solution:** Check you have write permissions:
```bash
ls -la frontend/package.json
chmod u+w frontend/package.json
```

### README not updating

**Problem:** sed commands not working (macOS/Linux differences)

**Solution:** The script handles both platforms, but if issues occur:
```bash
# Manually find and replace
vim README.md
# Search: /0\.40\.0
# Replace: 0.41.0
```

### Forgot to update CHANGELOG

**Problem:** Committed version bump without CHANGELOG

**Solution:** Amend the commit:
```bash
# Edit CHANGELOG.md
vim CHANGELOG.md

# Amend previous commit
git add CHANGELOG.md
git commit --amend

# Update tag
git tag -d v0.41.0
git tag -a v0.41.0 -m "Release v0.41.0"

# Force push (if already pushed)
git push --force
git push --tags --force
```

## Best Practices

1. **Always update CHANGELOG first** - Before running bump script
2. **Review all changes** - Use `git diff` before committing
3. **Write detailed CHANGELOG entries** - Include why, not just what
4. **Use semantic versioning correctly** - Major/Minor/Patch meanings
5. **Test before releasing** - Run tests, build, verify functionality
6. **Create annotated tags** - Use `git tag -a` with descriptions
7. **Keep README current** - Version bump script handles this automatically
8. **Document breaking changes** - Highlight in CHANGELOG
9. **Reference issues/PRs** - Link to GitHub issues when relevant
10. **Consistent commit messages** - Use "chore(release):" prefix

## Release Checklist

Before running `./scripts/bump-version.sh`:

- [ ] All changes committed
- [ ] Tests passing
- [ ] Build successful
- [ ] CHANGELOG.md updated with detailed entry
- [ ] Date in CHANGELOG is correct
- [ ] Version number determined (major/minor/patch)

After running `./scripts/bump-version.sh`:

- [ ] Review pom.xml version
- [ ] Review package.json version
- [ ] Review README.md version references
- [ ] Review frontend/README.md version references
- [ ] All changes look correct

Before pushing:

- [ ] Created git commit
- [ ] Created annotated git tag
- [ ] Tag message is descriptive
- [ ] Final review of `git log` and `git diff`

After pushing:

- [ ] Verify on GitHub that tag appears
- [ ] Verify on GitHub that version is correct
- [ ] Consider creating GitHub Release with CHANGELOG content

## Migration Notes

This version management system was introduced in version 0.40.0 and includes:

- ✅ Maven Versions Plugin - for pom.xml updates
- ✅ NPM Version Command - for package.json updates
- ✅ Automated bump script - for coordinated updates
- ✅ Manual CHANGELOG - for high-quality documentation
- ❌ No automated CHANGELOG generation - maintains documentation quality

## References

- [Semantic Versioning 2.0.0](https://semver.org/)
- [Keep a Changelog](https://keepachangelog.com/)
- [Maven Versions Plugin](https://www.mojohaus.org/versions-maven-plugin/)
- Project CHANGELOG: `CHANGELOG.md`
- Bump script: `scripts/bump-version.sh`
