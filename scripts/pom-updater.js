/**
 * Custom updater for pom.xml version in standard-version
 * This allows standard-version to update both package.json and pom.xml
 */

const versionRegex = /<version>(\d+\.\d+\.\d+)<\/version>/;

module.exports.readVersion = function (contents) {
  const match = contents.match(versionRegex);
  return match ? match[1] : null;
};

module.exports.writeVersion = function (contents, version) {
  // Only replace the first <version> tag (the project version)
  let replaced = false;
  return contents.replace(versionRegex, function(match) {
    if (!replaced) {
      replaced = true;
      return `<version>${version}</version>`;
    }
    return match;
  });
};
