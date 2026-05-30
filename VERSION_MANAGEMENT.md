# Version Management

## Overview

This project uses a centralized version management system via the `version.properties` file. The version is displayed in the main screen title of the application.

## Current Version

**v1.0.0**

## Version File Location

```
/version.properties
```

## Version Format

The project follows [Semantic Versioning](https://semver.org/):

```
MAJOR.MINOR.PATCH
```

- **MAJOR**: Incompatible API changes or major feature overhauls
- **MINOR**: Add functionality in a backwards compatible manner
- **PATCH**: Backwards compatible bug fixes and minor improvements

## How to Update Version

### Manual Update

Edit the `version.properties` file:

```properties
version=1.0.1
```

### Version Increment Guidelines

**PATCH** (1.0.0 → 1.0.1):
- Bug fixes
- Minor UI improvements
- Performance optimizations
- Documentation updates
- Default increment for most changes

**MINOR** (1.0.0 → 1.1.0):
- New features
- Significant UI changes
- New OAuth flow implementations
- New configuration options

**MAJOR** (1.0.0 → 2.0.0):
- Breaking changes to API
- Complete redesign
- Major architectural changes
- Removal of deprecated features

## Implementation Details

### Build Configuration

The version is loaded from `version.properties` in `app/build.gradle`:

```groovy
def versionPropsFile = file('../version.properties')
def versionProps = new Properties()
if (versionPropsFile.exists()) {
    versionProps.load(new FileInputStream(versionPropsFile))
}
def appVersion = versionProps['version'] ?: '1.0.0'
```

### BuildConfig Integration

The version is made available via `BuildConfig`:

```groovy
buildConfigField "String", "APP_VERSION", "\"${appVersion}\""
```

### UI Display

The version is displayed in `MainActivity.kt`:

```kotlin
import com.masstack.authn.BuildConfig

TopAppBar(
    title = { Text("OAuth2 Flow Tester v${BuildConfig.APP_VERSION}") }
)
```

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0   | 2024-11-07 | Initial release with Authorization Code, WebAuthn, and CIBA flows |

## After Updating Version

1. Edit `version.properties` with new version
2. Update this file's "Version History" table
3. Rebuild the project:
   ```bash
   export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
   ./gradlew clean assembleDebug
   ```
4. The new version will appear in the app title automatically

## Notes

- The version in `version.properties` is the single source of truth
- Both `versionName` in the APK manifest and `BuildConfig.APP_VERSION` read from this file
- No need to manually update version in multiple places
- Version is visible to users in the main screen title
