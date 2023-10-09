# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed
- Changed application colours to align with new designs.

## [1.3.0]

### Fixed

- fixed button's text cut off on sign screen if longer text used
- fixed the app crashing if sending funds from the main screen
- fixed an when comparing 2 unsorted lists resulting in wrong message when managing tokens
- fixed scenario where after canceled authentication buttons would not enable in
  WalletConnectMessageFragment.kt
- Fixed wallet connect service crash caused by intent redeliver policy when restarting the
  WalletConnectService

### Changed

- improved WC pairing error handling
- Improved error message when transfer entrypoint fails

## [1.2.1]

### Fixed

- fixed end destination when exiting add/remove watched tokens.
- fixed item overlap issues with transaction details screen
- Removed all tokens with balance < 0 when selecting tokens for transfer
- Removed token thumbnail and added name in token details activity
- transfer token flow now ends in the proper place
- fixed issue where ID pub duplicated id error showed
- show raw metadata button now has green colour to indicate its clickable

### Changed

- Remove election difficulty from expected chain parameters. This does not
  affect wallet functionality.
- changed "You can add more tokens from the Manage menu." -> "To add more tokens, tap Manage." and
  added item decorator for divider in TokensFragment.kt
- changed max line size for token name. added separator for recycler
- Added new UI for raw metadata dialog
- Removed the ability to select other tokens when transferring from TokenDetailsActivity

## [1.2.0]

### Added

- Added CHANGELOG.md
- Identity view: Display raw "document type" when no localized string is matched

### Fixed

- Fix a crash when inputting a too large amount as the stake for delegation or baking.
- Fix validation of token metadata when looking for new tokens.
- Fix an issue where another tokens image was sometimes shown for tokens without an image.
- Fix when searching for a token, the "no tokens found" display did not appear.
- Fixed number of issues with
    1. Remove “Name and icon” header text for token name (to keep it aligned with BW).
    2. Balance (for FT) section (header and values) is missing. Expected - For FT, “Balance” should
       be shown.
    3. Align the different sections order with BW (to keep behavior uniform across wallets).
    4. Metadata is missing for both FT and NFT. Expected - There’s should be a link “Show raw
       metadata” which should open token metadata in an overlay screen (check BW behavior).
    5. NFT token details screen should not have Decimal section.
    6. Token image size is very small, contents almost invisible.
- Fixed issue where owned tokens are not reflected in search result list and details screens.
- When adding a new token the token details would in some cases show information from another token.
- Fixed UI bug that showed decimal places for NFT tokens.
- Fixed NFTs name missing on Collectibles and Send token screens.
- Fixed issue where the token details screen would not show the correct token image.
- Fixed issue with greyed out send funds button after reopening the screen.

### Changed

- Changed Terms and Conditions screen to new UI.
- Transferring CCD on Public balance now always uses the new sendToken activity.
- Removed the ability to search for tokens when selecting tokens to transfer.

## [1.1.8]

### Added

- Last release without changelog.
