# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Added CHANGELOG.md
- Identity view: Display raw "document type" when no localized string is matched

### Fixed

- Fix a crash when inputting a too large amount as the stake for delegation or baking.
- Fix not renaming account from the settings.
- Fix an issue where another tokens image was sometimes shown for tokens without an image.
- Fix when searching for a token, the "no tokens found" display did not appear.
- Fixed number of issues with 
  1. Remove “Name and icon” header text for token name (to keep it aligned with BW).
  2. Balance (for FT) section (header and values) is missing. Expected - For FT, “Balance” should be shown.
  3. Align the different sections order with BW (to keep behavior uniform across wallets).
  4. Metadata is missing for both FT and NFT. Expected - There’s should be a link “Show raw metadata” which should open token metadata in an overlay screen (check BW behavior).
  5. NFT token details screen should not have Decimal section.
  6. Token image size is very small, contents almost invisible.
- Fixed issue where owned tokens are not reflected in search result list and details screens
- When adding a new token the token details would in some cases show information from another token.

### Changed

- Changed Terms and Conditions screen to new UI.

## [1.1.7]

### Added

- Last release without changelog.
