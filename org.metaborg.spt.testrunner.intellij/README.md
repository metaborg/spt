# SPT Test Runner for IntelliJ
This plugin adds a test runner for SPT tests to IntelliJ IDEA. (It however does not add syntax highlighting. Use the [Spoofax for IntelliJ][1] plugin for that.)

![Screenshot of the SPT Test Runner for IntelliJ](https://raw.githubusercontent.com/metaborg/spt/master/org.metaborg.spt.testrunner.intellij/docs/intellij-testing.png)



## Installation
To install the SPT test runner in your IntelliJ installation:

1. In IntelliJ, go to _File_ → _Settings_ menu.
2. Go to the _Plugins_ tab, and click the _Browse repositories..._ button.
3. Search for `SPT` and install the _SPT_ plugin by _Metaborg_.
4. Restart IntelliJ to load the plugin.

This plugin has no dependencies on other plugins, i.e. it can be used without the [Spoofax for IntelliJ][1] plugin, although you won't have syntax highlighting.



## Usage
To use the SPT test runner on your Spoofax project in IntelliJ:

1. Create a new _Spoofax SPT_ run configuration.
2. Set the _Test folder_ to the folder that contains the SPT test files.
3. Set the _Language module_ to the module that contains the language used in the tests.
4. Save the new configuration.
5. Run the new configuration.



## Contributing
If you want to [report bugs][2], contribute fixes, add features, [submit pull requests][3], please do so. If you have a question, please [submit an issue][2] as well.



## Build and Run
To build this project, invoke this command from the command line:

    ./gradlew build

To start a sandbox IntelliJ instance with the plugin loaded into it, run this command:

    ./gradlew runIde


## Publishing
To publish the plugin in the JetBrains plugin repository, use the following command where you replace the username and password by your own:

    ./gradlew publishPlugin \
        -PpublishUsername=<your username> \
        -PpublishPassword=<your password> \
        -PpublishChannel=Nightly




## Overview
This plugin contains just the parts that are necessary to add a run configuration to IntelliJ. It has three main dependencies:

- [Metaborg Core][4] - The core library used by all Spoofax languages. This plugin mostly uses the test reporter functionality.
- [SPT Command Line][5] - The SPT command-line test runner, which is invoked by the run configuration.
- [SPT Language][6] - The SPT language, used by the command-line utility to parse the test suites.

The main resources useful for working on this plugin are:

- [IntelliJ Platform SDK DevGuide: Run Configuration Management](http://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_management.html)
- [IntelliJ Platform SDK DevGuide: Execution](http://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_execution.html)
- [Build Script Interaction with TeamCity](https://confluence.jetbrains.com/display/TCD10/Build+Script+Interaction+with+TeamCity)
- [Graphical integration of running tests in plugin](https://intellij-support.jetbrains.com/hc/en-us/community/posts/206103879-Graphical-integration-of-running-tests-in-plugin)




## License
Copyright 2017-2018 [Programming Languages Group](https://pl.ewi.tudelft.nl/), [Delft University of Technology](https://www.tudelft.nl/)

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-2.0>.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an **"as is" basis, without warranties or conditions of any kind**, either express or implied. See the License for the specific language governing permissions and limitations under the License.


[1]: https://github.com/metaborg/spoofax-intellij
[2]: http://yellowgrass.org/
[3]: https://github.com/metaborg/spt/pulls
[4]: https://github.com/metaborg/spoofax
[5]: https://github.com/metaborg/spt/tree/master/org.metaborg.spt.cmd
[6]: https://github.com/metaborg/spt/tree/master/org.metaborg.meta.lang.spt
