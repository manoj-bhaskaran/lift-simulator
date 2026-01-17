# Version Management Guide

This document describes the automated version management workflow for the lift-simulator project.

## Overview

The project uses a combination of tools to automate version bumping:

1. **Maven Versions Plugin** - Updates `pom.xml`
2. **NPM Version Command** - Updates `frontend/package.json` and `package-lock.json`
3. **standard-version** - Automates CHANGELOG generation and version bumping
4. **Custom Scripts** - Coordinates all tools and updates README files

## Quick Start

### Option 1: Automated Release (Recommended)

Uses Conventional Commits to automatically determine version and generate CHANGELOG:

```bash
# Automatically bump version based on commits and update CHANGELOG
npm run release

# Or specify version type explicitly
npm run release:patch  # 0.40.0 -> 0.40.1
npm run release:minor  # 0.40.0 -> 0.41.0
npm run release:major  # 0.40.0 -> 1.0.0
```

**What this does:**
- Analyzes git commits since last release
- Determines next version (based on feat/fix/BREAKING CHANGE)
- Updates `CHANGELOG.md` automatically
- Updates version in `package.json`, `package-lock.json`, and `pom.xml`
- Creates a git commit and tag
- Does NOT update README files (see hybrid workflow below)

### Option 2: Manual Version Bump

For complete control, use the custom script:

```bash
# Bump to specific version
./scripts/bump-version.sh 0.41.0
```

**What this does:**
- Updates `pom.xml` using Maven Versions Plugin
- Updates `frontend/package.json` and `package-lock.json` using NPM
- Updates all version references in README files
- Does NOT update CHANGELOG (you must do this manually)
- Does NOT create git commit (you must do this manually)

## Conventional Commits

To use automated CHANGELOG generation, follow the Conventional Commits format:

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Commit Types

| Type | Description | CHANGELOG Section | Version Bump |
|------|-------------|-------------------|--------------|
| `feat` | New feature | Added | Minor (0.x.0) |
| `fix` | Bug fix | Fixed | Patch (0.0.x) |
| `chore` | Maintenance | Changed | None |
| `docs` | Documentation | Documentation | None |
| `refactor` | Code refactoring | Changed | None |
| `perf` | Performance improvement | Changed | None |
| `test` | Test changes | (hidden) | None |
| `BREAKING CHANGE` | Breaking change | (highlighted) | Major (x.0.0) |

### Examples

**Feature addition (minor bump):**
```bash
git commit -m "feat(logging): add comprehensive logging to GlobalExceptionHandler

- Add SLF4J logger with appropriate log levels
- ERROR level for unexpected errors with stack traces
- WARN level for malformed JSON requests
- INFO level for business exceptions"
```

**Bug fix (patch bump):**
```bash
git commit -m "fix(api): correct validation error handling

Fixed issue where validation errors were not properly logged"
```

**Breaking change (major bump):**
```bash
git commit -m "feat(api): redesign configuration API

BREAKING CHANGE: Configuration API now uses v2 endpoints.
Clients must update to use /api/v2/config instead of /api/config"
```

**Chore (no version bump):**
```bash
git commit -m "chore(deps): update dependencies to latest versions"
```

**Documentation (no version bump):**
```bash
git commit -m "docs(readme): update installation instructions"
```

## Workflows

### Workflow 1: Fully Automated Release

Best for: Routine releases with good commit messages

```bash
# 1. Make your changes and commit with Conventional Commits format
git add src/main/java/...
git commit -m "feat(api): add new endpoint for system health"

# 2. Run automated release
npm run release

# 3. Manually update README files if needed
./scripts/bump-version.sh $(node -p "require('./package.json').version")

# 4. Push changes and tags
git push --follow-tags origin main
```

### Workflow 2: Hybrid (Recommended)

Best for: Important releases that need detailed documentation

```bash
# 1. Make changes and commit with Conventional Commits
git add .
git commit -m "feat(logging): add comprehensive logging to GlobalExceptionHandler"

# 2. Run automated release to generate basic CHANGELOG
npm run release

# 3. Manually enhance CHANGELOG.md with detailed sections:
#    - Add "Technical Details" section
#    - Add "Benefits" section
#    - Expand descriptions with more context

# 4. Amend the commit
git add CHANGELOG.md
git commit --amend --no-edit

# 5. Update README files
./scripts/bump-version.sh $(node -p "require('./package.json').version")

# 6. Push
git push --follow-tags origin main
```

### Workflow 3: Manual Release

Best for: Complex releases or when you want full control

```bash
# 1. Make your changes and commit normally
git add .
git commit -m "Add comprehensive logging"

# 2. Manually update CHANGELOG.md with detailed entry

# 3. Bump version
./scripts/bump-version.sh 0.41.0

# 4. Review and commit
git add -A
git commit -m "chore(release): bump version to 0.41.0"
git tag -a v0.41.0 -m "Release v0.41.0"

# 5. Push
git push && git push --tags
```

## CHANGELOG Management

### Automated CHANGELOG

When you run `npm run release`, standard-version:
- Reads commits since last tag
- Groups by type (feat → Added, fix → Fixed, etc.)
- Generates markdown with commit links
- Prepends to `CHANGELOG.md`

**Example auto-generated entry:**
```markdown
## [0.41.0] - 2026-01-17

### Added
- **logging**: add comprehensive logging to GlobalExceptionHandler ([abc123](link))

### Fixed
- **api**: correct validation error handling ([def456](link))
```

### Manual CHANGELOG Enhancement

Your current CHANGELOG has excellent detail. To maintain this quality:

1. **Let standard-version generate the base entry**
2. **Manually enhance it** with:
   - Detailed bullet points under each item
   - "Technical Details" subsection
   - "Benefits" subsection
   - "Changed" subsection with version bumps
   - Cross-references to ADRs or issues

**Example enhanced entry:**
```markdown
## [0.40.0] - 2026-01-17

### Added
- **Comprehensive Logging to GlobalExceptionHandler**: Added SLF4J-based logging for all exception handlers
  - **SLF4J Logger Integration**: Added `org.slf4j.Logger` and `org.slf4j.LoggerFactory` imports
  - **ERROR-level Logging**: Generic exception handler logs unexpected errors with full stack traces
  - **WARN-level Logging**: Malformed JSON request handler logs with specific details
  - **INFO-level Logging**: Business exception handlers log expected operational errors
  - **Benefits**:
    - Enables debugging of production issues
    - Provides audit trail of all exceptions
    - Maintains security best practice

### Changed
- Version bumped from 0.39.1 to 0.40.0
- Frontend package version updated to 0.40.0

### Technical Details
- Uses SLF4J API with Logback implementation
- No additional dependencies required
- Logging levels follow industry best practices
```

## Tools Reference

### Maven Versions Plugin

Update Maven version only:
```bash
mvn versions:set -DnewVersion=0.41.0
mvn versions:commit
```

Revert if needed:
```bash
mvn versions:revert
```

### NPM Version Command

Update NPM version only:
```bash
cd frontend
npm version 0.41.0 --no-git-tag-version
```

Or with automatic semver bump:
```bash
npm version patch  # 0.40.0 -> 0.40.1
npm version minor  # 0.40.0 -> 0.41.0
npm version major  # 0.40.0 -> 1.0.0
```

### standard-version Commands

```bash
# Automatic bump based on commits
npm run release

# Explicit version bump
npm run release:patch
npm run release:minor
npm run release:major

# First release
npm run release -- --first-release

# Dry run (see what would happen)
npm run release -- --dry-run

# Custom version
npm run release -- --release-as 1.0.0

# Skip git operations (useful for testing)
npm run release -- --skip.commit --skip.tag
```

### Custom Bump Script

```bash
# Basic usage
./scripts/bump-version.sh 0.41.0

# The script:
# - Updates pom.xml via Maven Versions Plugin
# - Updates frontend package.json and package-lock.json via NPM
# - Updates README.md with new JAR filenames
# - Updates frontend/README.md with new JAR filenames
# - Does NOT commit (you commit manually)
```

## Configuration Files

### `.versionrc.json`

Configures standard-version behavior:
- Maps commit types to CHANGELOG sections
- Defines commit message format
- Specifies files to update (package.json, pom.xml)
- Sets GitHub URLs for links

### `scripts/pom-updater.js`

Custom updater that allows standard-version to update `pom.xml`:
- Reads current version from `<version>` tag
- Writes new version to `<version>` tag
- Only updates the project version (first occurrence)

### `scripts/bump-version.sh`

Shell script that coordinates all version updates:
- Validates version format
- Updates Maven via versions plugin
- Updates NPM versions
- Updates README files with sed
- Provides next-step instructions

## Best Practices

1. **Use Conventional Commits** - Makes automation possible
2. **Write detailed commit bodies** - These become CHANGELOG content
3. **Reference issues** - Use "Closes #123" in commit footer
4. **Enhance auto-generated CHANGELOG** - Add technical details for important releases
5. **Test with dry-run** - Use `--dry-run` flag to preview changes
6. **Review before pushing** - Always review generated CHANGELOG and version bumps
7. **Keep CHANGELOG readable** - Remove unnecessary noise, group related changes

## Troubleshooting

### standard-version fails with "No commits since last tag"

**Solution:** Either make new commits or use `--first-release`:
```bash
npm run release -- --first-release
```

### pom.xml not updating with standard-version

**Solution:** Ensure `scripts/pom-updater.js` exists and is referenced in `.versionrc.json`

### README versions not updating

**Solution:** Use the bump-version.sh script after running standard-version:
```bash
npm run release
./scripts/bump-version.sh $(node -p "require('./package.json').version")
```

### Want to undo a release

**Solution:**
```bash
# Delete local tag
git tag -d v0.41.0

# Delete remote tag (if pushed)
git push origin :refs/tags/v0.41.0

# Reset commit
git reset --hard HEAD~1
```

## Migration from Manual to Automated

Currently in progress:
- ✅ Maven Versions Plugin configured
- ✅ NPM version command available
- ✅ standard-version installed and configured
- ✅ Custom pom.xml updater created
- ✅ Bump script created for README updates
- ⏳ Transition to Conventional Commits (gradual)
- ⏳ Team adoption of new workflow

## References

- [Conventional Commits Specification](https://www.conventionalcommits.org/)
- [standard-version Documentation](https://github.com/conventional-changelog/standard-version)
- [Maven Versions Plugin](https://www.mojohaus.org/versions-maven-plugin/)
- [Keep a Changelog](https://keepachangelog.com/)
- [Semantic Versioning](https://semver.org/)
