# Changelog
All notable changes to this add-on will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## Unreleased
### Changed
- Update dependency.
- Use always the same newlines (LF) when exporting HAR files.

## [0.14.0] - 2025-03-25
### Changed
- Caps fix in Import menu (Issue 2000).

### Fixed
- Sites Tree export now correctly handles node names with newlines and special characters (Issue 8858).

## [0.13.0] - 2025-01-09
### Added
- Add Automation Framework job to export data (e.g. HAR, URLs).
- Support for Sites Tree export and prune.

### Changed
- Update minimum ZAP version to 2.16.0.
- Update dependency.
- Maintenance changes.

### Fixed
- Import HAR entry sent and elapsed time.
- Duplicate or missing "Save URLs..." entries in the Export menu.
- The "Save All URLs..." export option was saving only the selected URLs.
- Correct bundled dependencies to avoid conflicts with core logging libraries.

## [0.12.0] - 2024-10-07
### Changed
- Improved HTTP 1.1 traffic detection in PCAP files

### Fixed
- Count invalid messages as tasks done toward progress when importing HARs.

## [0.11.0] - 2024-09-24
### Changed
- Leverage Jackson library from the Common Library add-on.
- Depend on newer version of Common Library add-on.
- Leverage aboutsip's pkts.io library for parsing pcap files and organizing tcp streams
- PCAP importing for simple HTTP/1.1 traffic, works for traces with: No duplicates or missing TCP segments, with no missing HTTP responses to HTTP requests, and for HTTP 1.1 only

### Fixed
- Correctly load Automation Framework template plans.
- Base64 decode the response body when importing HARs.

## [0.10.0] - 2024-07-22
### Changed
- HAR importing now uses Sebastian Stöhr's har-reader library. It should be much more tolerant of 'weird' HAR things, and thus be able to import more samples. (If you come across HAR that won't import please open an issue and provide a sample so we can work on further improvements!)

## [0.9.0] - 2024-05-07
### Added
- Initial PCAP import support (Issue 4812).
- Support for menu weights (Issue 8369)

### Changed
- Update minimum ZAP version to 2.15.0.
- Maintenance changes.

## [0.8.0] - 2023-11-10
### Changed
- Keep the Export menu items sorted alphabetically.
- Dropped "to Clipboard" from ZAP copy menu items (Issue 8179).

## [0.7.0] - 2023-10-12
### Changed
- Update minimum ZAP version to 2.14.0.
- Depend on newer versions of Automation Framework and Common Library add-ons (Related to Issue 7961).

## [0.6.0] - 2023-07-11
### Changed
- Update minimum ZAP version to 2.13.0.

## [0.5.0] - 2023-04-04
### Changed
- Log cause of error when failed to import the HAR file.

### Fixed
- Ensure the 'ZAP messages' Export delimiters are consistent with the Import expectation.

## [0.4.0] - 2023-02-09
### Added
- Support for relative file paths and ones including vars in the Automation Framework job.

### Changed
- Maintenance changes.

### Fixed
- Show missing API endpoints' descriptions.

## [0.3.0] - 2022-10-27
### Changed
- Update minimum ZAP version to 2.12.0.
- Maintenance changes.
- When importing a file of URLs the output tab and log will now be more informative about failures.

### Added
- HAR related API endpoints being migrated from core (Issue 6579).

## [0.2.0] - 2022-07-20
### Fixed
- Tweaked import functionality to mark import progress components completed when an exception occurs during import (thus allowing them to be cleared properly).
- HAR imports will now use an indeterminate progress bar if the count of entries cannot be determined.
- Correct import of HAR responses to allow them to be passively scanned.

### Added
- Copy URLs, Export Context URLs, Export Selected URLs, Export Messages, and Export Responses functionality similar to what was previously offered via core functionality.
- Stats for migrated core components.

### Changed
- Save RAW functionality now includes an All option which saves the entire HTTP message.

## [0.1.0] - 2022-03-07
### Changed
- Reduce logging and display a warning dialog when unable to read files being imported (Issue 7081).
- Promoted to Beta.

### Added
- Importing a file of URLs or HAR is now displayed in the progress panel provided via commonlib.
- Automation Framework (Issue 7078).

## [0.0.1] - 2021-12-22

- First release.

[0.14.0]: https://github.com/zaproxy/zap-extensions/releases/exim-v0.14.0
[0.13.0]: https://github.com/zaproxy/zap-extensions/releases/exim-v0.13.0
[0.12.0]: https://github.com/zaproxy/zap-extensions/releases/exim-v0.12.0
[0.11.0]: https://github.com/zaproxy/zap-extensions/releases/exim-v0.11.0
[0.10.0]: https://github.com/zaproxy/zap-extensions/releases/exim-v0.10.0
[0.9.0]: https://github.com/zaproxy/zap-extensions/releases/exim-v0.9.0
[0.8.0]: https://github.com/zaproxy/zap-extensions/releases/exim-v0.8.0
[0.7.0]: https://github.com/zaproxy/zap-extensions/releases/exim-v0.7.0
[0.6.0]: https://github.com/zaproxy/zap-extensions/releases/exim-v0.6.0
[0.5.0]: https://github.com/zaproxy/zap-extensions/releases/exim-v0.5.0
[0.4.0]: https://github.com/zaproxy/zap-extensions/releases/exim-v0.4.0
[0.3.0]: https://github.com/zaproxy/zap-extensions/releases/exim-v0.3.0
[0.2.0]: https://github.com/zaproxy/zap-extensions/releases/exim-v0.2.0
[0.1.0]: https://github.com/zaproxy/zap-extensions/releases/exim-v0.1.0
[0.0.1]: https://github.com/zaproxy/zap-extensions/releases/exim-v0.0.1
