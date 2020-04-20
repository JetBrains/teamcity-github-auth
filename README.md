[![official JetBrains project](https://jb.gg/badges/official-plastic.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0) 
# teamcity-github-auth

Allows users to authenticate in TeamCity using the GitHub.com account.
The plugin is compatible with TeamCity 11.

How to use:
* Donwload latest plugin build [here](https://teamcity.jetbrains.com/viewType.html?buildTypeId=TeamCityPluginsByJetBrains_Build_2&guest=1)
* [Install the plugin](https://confluence.jetbrains.com/display/TCDL/Installing+Additional+Plugins)
* Add a GitHub.com [Connection](https://confluence.jetbrains.com/display/TCDL/Integrating+TeamCity+with+VCS+Hosting+Services) to the Root Project
* Configure the GitHub OAuth authentication module on the Administration -> Authentication tab
* Now users will see the 'Login using GitHub' link on the login page.
