# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.6.0] - 2024-06-26

### Fixed
- Sending WalletConnect transaction with 0 energy if its payload is too large

### Removed
- Shielding – you can still enable and see your shielded balance and history,
but to unshield the funds CryptoX Concordium wallet must be used

## [1.5.1] - 2024-03-18

### Fixed
- Inability to update the validator's rewards restake preference if their stake is below
- Inability to add CIS-2 tokens with corrupted metadata or missing balance
- Writing incorrect `environment` value to the key export file
- Inability to edit validator pool commission rates in locales with comma decimal separator
- Inability to search for CIS-2 token by ID on contracts with lots of tokens
- When managing CIS-2 tokens, removing all of them when only unselecting the visible ones 

## [1.5.0]

### Changed

- Changed application colours to align with new designs.
- Renamed baker/baking to validator/validation.

### Fixed

- WalletConnect: Fix parsing of "broken" schema format for contract update transactions.
- Fix incorrect text in the identity name dialog
- Fix not renaming account from the settings.
- incorrect CCD token balance if some amount is shielded
- keyboard not being shown when the auth dialog is opened

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

[Unreleased]: https://github.com/Concordium/concordium-reference-wallet-android/compare/1.6.0...HEAD
[1.6.0]: https://github.com/Concordium/concordium-reference-wallet-android/compare/1.5.1...1.6.0
[1.5.1]: https://github.com/Concordium/concordium-reference-wallet-android/compare/1.5.0...1.5.1
[1.5.0]: https://github.com/Concordium/concordium-reference-wallet-android/compare/1.3.0...1.5.0
[1.3.0]: https://github.com/Concordium/concordium-reference-wallet-android/compare/1.2.1...1.3.0
[1.2.1]: https://github.com/Concordium/concordium-reference-wallet-android/compare/1.2.0...1.2.1
[1.2.0]: https://github.com/Concordium/concordium-reference-wallet-android/compare/1.1.8...1.2.0
[1.1.8]: https://github.com/Concordium/concordium-reference-wallet-android/tree/1.1.8
