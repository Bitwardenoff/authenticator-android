[![Github Workflow build on main](https://github.com/bitwarden/authenticator-android/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/bitwarden/authenticator-android/actions/workflows/build.yml?query=branch:main)
[![Join the chat at https://gitter.im/bitwarden/Lobby](https://badges.gitter.im/bitwarden/Lobby.svg)](https://gitter.im/bitwarden/Lobby)

# Bitwarden Authenticator Android App

<a href="https://play.google.com/store/apps/details?id=com.bitwarden.authenticator" target="_blank"><img alt="Get it on Google Play" src="https://imgur.com/YQzmZi9.png" width="153" height="46"></a>

Bitwarden Authenticator allows you easily store and generate two-factor authentication codes on your device. The Bitwarden Authenticator Android application is written in Kotlin.

<img src="https://raw.githubusercontent.com/bitwarden/brand/master/screenshots/authenticator-android-codes.png" alt="" width="325" height="650" />

## Compatibility

- **Minimum SDK**: 28
- **Target SDK**: 34
- **Device Types Supported**: Phone and Tablet
- **Orientations Supported**: Portrait and Landscape

## Setup


1. Clone the repository:

    ```sh
    $ git clone https://github.com/bitwarden/authenticator-android
    ```

2. Create a `user.properties` file in the root directory of the project and add the following properties:

    - `gitHubToken`: A "classic" Github Personal Access Token (PAT) with the `read:packages` scope (ex: `gitHubToken=gph_xx...xx`). These can be generated by going to the [Github tokens page](https://github.com/settings/tokens). See [the Github Packages user documentation concerning authentication](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry#authenticating-to-github-packages) for more details.

3. Setup the code style formatter:

    All code must follow the guidelines described in the [Code Style Guidelines document](docs/STYLE_AND_BEST_PRACTICES.md). To aid in adhering to these rules, all contributors should apply `docs/bitwarden-style.xml` as their code style scheme. In IntelliJ / Android Studio:

    - Navigate to `Preferences > Editor > Code Style`.
    - Hit the `Manage` button next to `Scheme`.
    - Select `Import`.
    - Find the `bitwarden-style.xml` file in the project's `docs/` directory.
    - Import "from" `BitwardenStyle` "to" `BitwardenStyle`.
    - Hit `Apply` and `OK` to save the changes and exit Preferences.

    Note that in some cases you may need to restart Android Studio for the changes to take effect.

    All code should be formatted before submitting a pull request. This can be done manually but it can also be helpful to create a macro with a custom keyboard binding to auto-format when saving. In Android Studio on OS X:

    - Select `Edit > Macros > Start Macro Recording`
    - Select `Code > Optimize Imports`
    - Select `Code > Reformat Code`
    - Select `File > Save All`
    - Select `Edit > Macros > Stop Macro Recording`

    This can then be mapped to a set of keys by navigating to `Android Studio > Preferences` and editing the macro under `Keymap` (ex : shift + command + s).

    Please avoid mixing formatting and logical changes in the same commit/PR. When possible, fix any large formatting issues in a separate PR before opening one to make logical changes to the same code. This helps others focus on the meaningful code changes when reviewing the code.

## Contribute

Code contributions are welcome! Please commit any pull requests against the `main` branch. Learn more about how to contribute by reading the [Contributing Guidelines](https://contributing.bitwarden.com/contributing/). Check out the [Contributing Documentation](https://contributing.bitwarden.com/) for how to get started with your first contribution.

Security audits and feedback are welcome. Please open an issue or email us privately if the report is sensitive in nature. You can read our security policy in the [`SECURITY.md`](SECURITY.md) file.

 

