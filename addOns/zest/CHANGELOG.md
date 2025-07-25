# Changelog
All notable changes to this add-on will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased


## [48.8.0] - 2025-07-03

### Added
- New extension level method for recording client scripts.

### Changed
- Fail fast on client errors.
- Update Zest library to 0.31.0:
  - Update Selenium to version 4.34.0.

## [48.7.0] - 2025-06-10
### Changed
- Update Zest library to 0.30.0:
  - Update Selenium to version 4.33.0.
  - Send RETURN key if submit fails for input elements not in a form.

## [48.6.0] - 2025-05-20
### Added
- Client element waitForMsec parameter.

### Changed
- Update Zest library to 0.29.0:
  - Allow to access the `WebElement` referenced by a `ZestClientElement`.
  - Add new `waitForMsec` parameter to all client elements.
  - Change `ZestClientElementClick`, `ZestClientElementSendKeys`, and `ZestClientElementSubmit` to wait for the element to also be enabled when using `waitForMsec`.
  - Update Selenium to version 4.32.0.
  - Change `ZestClientElementClick` to click on the position of the element instead of the element itself when obscured, to better reproduce a manual click.
  - Change `ZestClientElementScrollTo` to only scroll to the element when not already in view and to scroll with `nearest` vertical alignment, to ensure the element is kept visible.
- Allow to copy the script's file system path from the Edit Zest Script dialogue.

### Fixed
- Print statements should print to the relevant script Output tab.
- Recording client Zest scripts.

## [48.5.0] - 2025-03-25
### Changed
- Use TOTP data defined under user credentials during authentication when available.
- Update Zest library to 0.25.0:
  - Update Selenium to version 4.30.0.
- Depend on newer version of Common Library add-on.

## [48.4.0] - 2025-02-27
### Changed
- Update Zest library to 0.24.0:
  - Update Selenium to version 4.29.0.
  - Remove workaround that was now causing exceptions.

## [48.3.0] - 2025-02-07
### Changed
- Enable ZAP API in the authentication runner so ZAP browser extension callbacks work.

## [48.2.0] - 2025-02-04
### Added
- Firefox to recorder.
- Added support for custom script authentication.

### Changed
- Record script order to be alphabetical.

### Fixed
- Record Client Submit statement.

## [48.1.0] - 2025-01-23
### Changed
- Use ZAP for launching Firefox and Chrome.
- Update Zest library to 0.23.0:
  - Update Selenium to version 4.28.0.
  - Update minimum Java version to 17.

## [48.0.0] - 2025-01-10
### Added
- Allow other add-ons to create a Zest script from a list of messages.

### Changed
- Update minimum ZAP version to 2.16.0.
- Use Semantic Version.
- Maintenance changes.
- Depend on Passive Scanner add-on (Issue 7959).

## [47] - 2024-09-24
### Fixed
- Correctly handle added scripts with no engine name.

## [46] - 2024-06-28
### Added
- Document the engine name in the help page.

### Changed
- Use script engines from ZAP when executing scripts.
- Update Zest library to 0.22.0:
  - Update Selenium to version 4.22.0.

## [45] - 2024-05-07
### Changed
- Update minimum ZAP version to 2.15.0.
- Update Zest library to 0.21.0:
  - Update Selenium to version 4.20.0.
  - Update HtmlUnit to major version 3.

## [44] - 2024-04-11
### Added
- Support for menu weights (Issue 8369)

### Changed
- Update minimum `scripts` add-on version to 45.1.0.
- Maintenance changes.

## [43] - 2023-12-19
### Changed
- Move "Zest" under "Scripts > Engine" in the Options panel list.

## [42] - 2023-10-12
### Changed
- Update minimum ZAP version to 2.14.0.
- Update Zest library to 0.20.0:
  - Update Selenium to version 4.14.0.

## [41] - 2023-09-26
### Added
- Allow rendering Zest scripts in YAML. The format (JSON/YAML) may be changed via the Zest Options screen.

### Changed
- Depend on Common Library add-on.
- Maintenance changes.

## [40] - 2023-09-11
### Added 
- Client support to record Zest scripts.

### Changed
- Maintenance changes.

### Fixed
- Resolved the browser window focus loss problem during the recording of Zest scripts.

## [39] - 2023-07-11
### Added 
- Dialogs for scroll, scrollTo, window resize and mouse over.

### Changed
- Update minimum ZAP version to 2.13.0.
- Update Zest library to 0.18.0:
  - Update Selenium to version 4.
  - jBrowserDriver (JBD), Opera, and PhantomJS are no longer supported (no longer being actively maintained).
  - Add client statements for Scroll, MouseOver, and Window Resize events.
- Maintenance changes.

## [38] - 2023-01-03
### Changed
- Maintenance changes.

### Fixed
- Prevent exception if no display (Issue 3978).

## [37] - 2022-10-27
### Changed
- Update minimum ZAP version to 2.12.0.
- Update Zest library to 0.16.0:
  - Search script engines also by extension not just by name when invoking scripts otherwise it could miss some engines (e.g. Jython).
- Maintenance changes.

## [36] - 2022-09-23
### Changed
- Update minimum ZAP version to 2.11.1.
- Use Network add-on to proxy client authentication requests.
- Maintenance changes.

## [35] - 2021-10-06
### Changed
- Maintenance changes.
- Update minimum ZAP version to 2.11.0.

## [34] - 2021-04-22
### Changed
- Depend on Script Console add-on, it's required to work with Zest scripts (Issue 2656).
- Clear Zest Results Panel when new script is added.
- Update minimum ZAP version to 2.10.0.

### Fixed
- Track modifications done to the scripts to refresh the cached ones (Issue 6558).

## [33] - 2020-11-27
### Added
- Allow to create a screenshot from the browser, using the context menu `Add Zest Client` > `Screenshot`.

### Changed
- Update minimum ZAP version to 2.9.0.
- Update Zest library to 0.15.0:
  - Do not follow redirects when disabled;
  - Reduce the changes done to the requests sent.
- Maintenance changes.

### Fixed
- Make sure the header fields are separated with CRLF when edited in the UI.
- Handle client requests when authenticating (Issue 5940).

## [32] - 2020-01-24
### Changed
- Update Zest library to 0.14.2, to correctly ignore cert checks.

## [31] - 2020-01-17
### Added
- Add info and repo URLs.

### Changed
- Update Zest library to 0.14.1 to restore proxying capability, in the previous version the proxy settings were ignored.

## [30] - 2019-12-06

### Added
- Allow to set, remove, and get global variables (Issue 3512), using the context menus:
  - `Add Zest Action` > `Action - Global Variable - Set`
  - `Add Zest Action` > `Action - Global Variable - Remove`
  - `Add Zest Assignment` > `Assign variable to Global Variable`
- Allow to start browsers (e.g. Chrome, Firefox) headless, enabled by default (Related to Issue 3866).
- Add new assignment which can filter the parsed DOM by element or attributes and select the content
of an element or the value of an attribute.

### Changed
- Update Zest library to 0.14.0 (Issue 4797). Refer to its [CHANGELOG](https://github.com/zaproxy/zest/blob/0.14.0/CHANGELOG.md#changelog) for full set of changes.
- Send sequence messages with ZAP so that they make use of ZAP features e.g. authentication, HTTP
Sender scripts. (Issue 5590)
- Set timestamp from/to Zest requests.

### Fixed
- Send PUT request with its body (Issue 4337).
- Launch browsers with capability `acceptInsecureCerts` set to true (Issue 4870).
- Proxy localhost with Chrome 72+ and Firefox 67+.

## [29] - 2019-06-07

- Rely on script context writer for script output.
- Correct message handling in HTTP Sender scripts.
- Remove Scripts tree selection listener when add-on is uninstalled.
- Depend on newer version of Selenium add-on.

## 28 - 2018-11-07

- Display HTTP message also when request statement is selected with keyboard.
- Update Content-Length of proxied responses (Issue 4613).
- Added input for Variable Name in Client Element Assign dialog.
- Allow to clear the Zest panel.
- Allow to access the options through Zest panel.
- Title caps adjustments (Issue 2000).
- Use selected text when adding assignments from the request/response.
- Show expression's inverse state in more tree nodes.
- Correct dialogue titles of client statements.
- Allow to invoke the context menu in text fields also with keyboard.
- Correct fields' state in Switch To Frame dialogue.
- Correct request conversion that dropped the topmost header (Issue 5100).

## 27 - 2018-01-19

- Fix exception when editing Action - Script with unsaved scripts.
- Allow to select more HTTP methods in Zest Request dialogue.

## 26 - 2017-11-27

- Use custom plugin ID for fail actions.
- Updated for 2.7.0.

## 25 - 2017-10-17

- Address exception when adding calc assign statement.
- Validate cookie name not empty.
- Code changes for Java 9 (Issue 2602).
- Default 'load on start' to true in all cases.
- Re-enabled parameterize option.
- Cope with parameterizing strings in the URL.
- Correct drag-and-drop in loop statements.

## 24 - 2017-08-18

- Update Zest library to version 0.13.
- Update to support Selenium version 3.4.0 (Issue 3509).
- Replace variables when running Action Invoke (Issue 3511).
- Execute scripts before programs when running Action Invoke (Issue 3488).
- Fix exceptions when running scripts (Issue 2859 and 2871).
- Bugfix in ZestScript Ui: When more than one 'Assign variable to a form field' node is below a RequestNode then the RequestNode is now correctly determined.
- Correct operation set in calc assigns.
- Allow to loop files even if fuzzers.jbrf does not exist (Issue 3400).
- Properly remove Zest scripts (Issue 3401).
- Allow to select the case on assign replacements.
- Show/select the correct script in the Edit Zest Action dialogue (Issue 3489).
- Ensure recorded Sequence scripts can be scanned through context menu (Issue 3536).
- Updated to support latest selenium addon.

## 23 - 2017-04-03

- Always show the expected URL in request statements (Issue 2854).
- Add HTTP requests to Sequence scripts when recording (Issue 3044).
- Execute nodes' mouse click action just once (Issue 3099).
- Clear Zest Results panel on session changes.

## 22 - 2016-08-05

- Change Sequence scripts to not use Sites tree nodes directly.
- Correct assertion of response body length when using charset (Issue 2669).
- Require just the parameters defined in the Authentication script (Issue 2734).

## 21 - 2016-06-02

- Fix (UI) exceptions related to Zest Results tab.

## 20 - 2016-03-07

- Add missing error messages for 'Assign variable via string delimiters'.
- Add missing field (operand B) in 'Assign variable to a calculation' dialogue.
- Send authentication requests with ZAP's configurations (Issue 2114).
- Fix "Active scan sequence" (Issue 2120).
- Fix exception while opening a dialogue.
- Cannot use auth script in daemon mode (Issue 2294).
- Support httpsender scripts (Issue 2293).
- Can't paste variable into new request dialog (Issue 2007).
- Script context menu has duplicated items (Issue 2106).
- Exception while adding Zest Condition (Issue 2296).
- Should not be able to delete THEN statement (Issue 2295).
- Statement lost after drag and drop (Issue 2299).
- It's possible to drag the script node (Issue 2302).

## 19 - 2015-08-23

- Updated add-on's info URL.
- Correct the message shown in failed asserts (Issue 1647).
- Other code changes.

## 18 - 2015-04-13

- ZAP Issue 1411: Missing authentication handling for Zest scripts called from scanner
- ZAP Issue 1501: Add "Inverse" option in all the expression  dialog boxes.
- ZAP Issue 1507: Unload all components when uninstalling.
- ZAP Issue 1536: Change "Zest" add-on to depend on "Selenium" add-on.
- Zest Issue 66: Zest scripts cant be invoked from Zest scripts on Windows
- Zest Issue 67: Multipart/form-data request failed
- Zest Issue 68: ZestExpressionURL fails to initialise regex patterns
- Zest Issue 69: Added support for AssignCalc and ExpressionIsInteger
- Updated for ZAP 2.4
- Removed fuzzing code - will need to be re-implemented for new adv fuzzing

## 17 - 2014-09-10

- ZAP Issue 600: Add option to include response details or not
- ZAP Issue 658: define which headers to be included by default
- ZAP Issue 1248: ZestRequest always follow redirects, fails to match redirect responses
- ZAP Issue 1327: Support drag and drop
- ZAP Issue 1329: support commenting in/out statements
- ZAP Issue 1331: Update selenium jar to fix issue working with firefox 32.0
- ZAP Issue 1278: Safe menu items not available in protected and safe modes.

## 16 - 2014-07-09

- ZAP Issue 1259: Zest exception when adding request to Zest script

## 15 - 2014-07-02

- ZAP issue 1235: Support client side scripting
- ZAP issue 1250: Zest proxy scripts can break binary content
- ZAP Issue 1254:	Allow adding and pasting statements after other statements

## 14 - 2014-06-02

- ZAP issue 1218: ZEST Record button broken in Toolbar
- ZAP issue 1230: Changes to Zest scripts lost after the top level script node is changed

## 13 - 2014-05-21



## 12 - 2014-04-10

- Moved templates from core
- Updated to use the latest core changes, other minor code changes (Issues 416, 609,  503, 1085, 1104 and 1105).
- Changed help file structure to support internationalisation (Issue 981).
- Added content-type to help pages (Issue 1080).
- Updated add-on dir structure (Issue 1113).

## 11 - 2013-12-20

- Changed to depend on core ExtensionScript to avoid NullPointerException (Issue 848)
- Fixed to allow Loops to use Custom fuzzers (Issue 916)
- Changed to show the correct selected message when the results are sorted in "Zest Results" tab (Issue 942)
- Added a 'Record new Zest script' toolbar button (Issue 953)

## 10 - 2013-11-17

- Misc bug fixes

## 9 - 2013-09-27

- Misc bug fixes

## 8 - 2013-09-11

- Promoted to beta (provisionally) and plug into script console ** Not ready for release yet!! ***

## 7 - 2013-05-03

- Added support for cut and paste in the scripts tree

## 6 - 2013-05-02

- Load and save passive scripts, added fail priorities, fixed some related bugs

## 5 - 2013-05-02

- Load and save passive scripts and added fail priorities

## 4 - 2013-04-24

- Added support for passive scripts and URL conditionals

## 3 - 2013-04-18

- Updated for 2.1.0

[48.8.0]: https://github.com/zaproxy/zap-extensions/releases/zest-v48.8.0
[48.7.0]: https://github.com/zaproxy/zap-extensions/releases/zest-v48.7.0
[48.6.0]: https://github.com/zaproxy/zap-extensions/releases/zest-v48.6.0
[48.5.0]: https://github.com/zaproxy/zap-extensions/releases/zest-v48.5.0
[48.4.0]: https://github.com/zaproxy/zap-extensions/releases/zest-v48.4.0
[48.3.0]: https://github.com/zaproxy/zap-extensions/releases/zest-v48.3.0
[48.2.0]: https://github.com/zaproxy/zap-extensions/releases/zest-v48.2.0
[48.1.0]: https://github.com/zaproxy/zap-extensions/releases/zest-v48.1.0
[48.0.0]: https://github.com/zaproxy/zap-extensions/releases/zest-v48.0.0
[47]: https://github.com/zaproxy/zap-extensions/releases/zest-v47
[46]: https://github.com/zaproxy/zap-extensions/releases/zest-v46
[45]: https://github.com/zaproxy/zap-extensions/releases/zest-v45
[44]: https://github.com/zaproxy/zap-extensions/releases/zest-v44
[43]: https://github.com/zaproxy/zap-extensions/releases/zest-v43
[42]: https://github.com/zaproxy/zap-extensions/releases/zest-v42
[41]: https://github.com/zaproxy/zap-extensions/releases/zest-v41
[40]: https://github.com/zaproxy/zap-extensions/releases/zest-v40
[39]: https://github.com/zaproxy/zap-extensions/releases/zest-v39
[38]: https://github.com/zaproxy/zap-extensions/releases/zest-v38
[37]: https://github.com/zaproxy/zap-extensions/releases/zest-v37
[36]: https://github.com/zaproxy/zap-extensions/releases/zest-v36
[35]: https://github.com/zaproxy/zap-extensions/releases/zest-v35
[34]: https://github.com/zaproxy/zap-extensions/releases/zest-v34
[33]: https://github.com/zaproxy/zap-extensions/releases/zest-v33
[32]: https://github.com/zaproxy/zap-extensions/releases/zest-v32
[31]: https://github.com/zaproxy/zap-extensions/releases/zest-v31
[30]: https://github.com/zaproxy/zap-extensions/releases/zest-v30
[29]: https://github.com/zaproxy/zap-extensions/releases/zest-v29
