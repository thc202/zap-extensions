# Changelog
All notable changes to this add-on will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## Unreleased
### Changed
- The scan rule now has the "TEST_TIMING" alert tag.

## [16] - 2025-04-30
### Changed
- Update minimum ZAP version to 2.16.0.
- Maintenance changes.
- The included active scan rule has been tagged of interest to Penetration Testers.

## [15] - 2021-10-20
### Fixed
- Re-ordered variable initialization to prevent an NPE.

## [14] - 2021-10-07
### Added
- Add help and link to the code.
- Add info and repo URLs.
- OWASP Top Ten 2021/2017 mappings.

### Fixed
 - Terminology

### Changed
- Update minimum ZAP version to 2.11.0.
- Maintenance changes.
- Updated owasp.org references (Issue 5962).

## [13] - 2019-06-07

- Update minimum ZAP version to 2.5.0.
- Bundle JDOM library instead of relying on core.

## 12 - 2017-11-27

- Minor code changes.

## 11 - 2016-07-07

- Check all DB techs when evaluating if the scanner should be run.

## 10 - 2016-06-02

- Prevent XXE vulnerability.
- Log level adjustments.
- Internationalisation of scanner and alert's data.
- Check for skip/stop more often.
- Added EXP error based payloads
- Preserved the space in commented suffixes

## 9 - 2015-07-30

- Split boundary and plugin definition in two different files
- Updated all plugin and boundary files to the newest SQLMap database
- Changed to target DB technology (Issue 1618).

## 8 - 2015-04-20

- Improved execution time when a WAF or Reverse Proxy is in place disabling keep-alive for all request
- Enforced the time based statistical model taking in account also the wait time threshold
- Solved some wrong "escape" that happened on some time based SQLi
- Users can specify the technology of interest, enabling or disabling them using the Advanced Active Scan tab

## 7 - 2015-04-13

- Updated for ZAP 2.4

## 6 - 2014-04-10

- Updated add-on dir structure (Issue 1113).

## 5 - 2014-02-15

- Resolved some race conditions related to slow and not responding sites

## 4 - 2013-11-25

- Solved some bugs and updated the SQLi payload configuration file

## 3 - 2013-09-11

- Added support for HyperSQL DB

[16]: https://github.com/zaproxy/zap-extensions/releases/sqliplugin-v16
[15]: https://github.com/zaproxy/zap-extensions/releases/sqliplugin-v15
[14]: https://github.com/zaproxy/zap-extensions/releases/sqliplugin-v14
[13]: https://github.com/zaproxy/zap-extensions/releases/sqliplugin-v13
