#!/bin/bash

# Version bump script for lift-simulator
# Usage: ./scripts/bump-version.sh <new-version>
# Example: ./scripts/bump-version.sh 0.41.0

set -e  # Exit on error

NEW_VERSION=$1

if [ -z "$NEW_VERSION" ]; then
    echo "Error: Version number required"
    echo "Usage: ./scripts/bump-version.sh <version>"
    echo "Example: ./scripts/bump-version.sh 0.41.0"
    exit 1
fi

# Validate version format (basic semver check)
if ! [[ "$NEW_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "Error: Invalid version format. Use semantic versioning (e.g., 0.41.0)"
    exit 1
fi

echo "=========================================="
echo "Bumping version to $NEW_VERSION"
echo "=========================================="

# 1. Update Maven version
echo "Updating Maven version in pom.xml..."
mvn versions:set -DnewVersion=$NEW_VERSION -q
mvn versions:commit -q
echo "✓ pom.xml updated"

# 2. Update frontend version
echo "Updating frontend version..."
cd frontend
npm version $NEW_VERSION --no-git-tag-version --allow-same-version
cd ..
echo "✓ frontend/package.json and package-lock.json updated"

# 3. Update README files with version placeholders
echo "Updating README files..."

# Main README.md
sed -i.bak "s/Current version: \*\*[0-9.]*\*\*/Current version: **$NEW_VERSION**/" README.md
sed -i.bak "s/lift-simulator-[0-9.]*.jar/lift-simulator-$NEW_VERSION.jar/g" README.md
sed -i.bak "s/(v[0-9.]*)/v$NEW_VERSION/g" README.md
rm -f README.md.bak
echo "✓ README.md updated"

# Frontend README.md
sed -i.bak "s/lift-simulator-[0-9.]*.jar/lift-simulator-$NEW_VERSION.jar/g" frontend/README.md
rm -f frontend/README.md.bak
echo "✓ frontend/README.md updated"

echo ""
echo "=========================================="
echo "Version bump complete!"
echo "=========================================="
echo ""
echo "Updated files:"
echo "  - pom.xml"
echo "  - frontend/package.json"
echo "  - frontend/package-lock.json"
echo "  - README.md"
echo "  - frontend/README.md"
echo ""
echo "Next steps:"
echo "  1. Update CHANGELOG.md with release notes"
echo "  2. Review changes: git diff"
echo "  3. Commit: git add -A && git commit -m 'chore: bump version to $NEW_VERSION'"
echo "  4. Tag: git tag -a v$NEW_VERSION -m 'Release v$NEW_VERSION'"
echo "  5. Push: git push && git push --tags"
echo ""
