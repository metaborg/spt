# SPT
[![Build][github-badge:build]][github:build]
[![License][license-badge]][license]
[![GitHub Release][github-badge:release]][github:release]

Meta-languages for testing your DSLs.

Note that on this branch we are doing an overhaul of SPT, so many features won't work yet.

## SPT language specification
SPT is a Spoofax language just like any other.
At the moment it's still only a very limited version of what it should be and it doesn't have an Eclipse project yet.
The language specification can be found in [org.metaborg.meta.lang.spt](org.metaborg.meta.lang.spt).

## SPT Core
SPT Core is our attempt at extracting the core functionality of SPT into a Java API.
The project is located at [org.metaborg.spt.core](org.metaborg.spt.core).

The idea is that you can extract tests from a testsuite into an `ITestCase`.
This is done by the `ITestExtractor`.
These test cases can then be run using the `ITestRunner`.
Both the extractor and the runner can be registered and obtained using Google Guice.
See the `Module` class in [org.metaborg.spt.cmd](org.metaborg.spt.cmd) to see how that is done.
See the `Runner` class in [org.metaborg.spt.cmd](org.metaborg.spt.cmd) to see how they are used.

To keep the test expectations of SPT modular,
they too can be injected using Guice.
See the `ParseExpectationTest` class in [org.metaborg.spt.cmd](org.metaborg.spt.cmd) to see an example for the `parse succeeds` expectation.

## SPT command line
The command line interface can currently run test suites.
It won't print any results yet, but they can be checked using the logs.
See `Main` and `Arguments` in [org.metaborg.spt.cmd](org.metaborg.spt.cmd) to learn about the usage of this tool.


## License
Copyright 2016-2024 [Programming Languages Group](https://pl.ewi.tudelft.nl/), [Delft University of Technology](https://www.tudelft.nl/)

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at <https://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an **"as is" basis, without warranties or conditions of any kind**, either express or implied. See the License for the specific language governing permissions and limitations under the License.


[github-badge:build]: https://img.shields.io/github/actions/workflow/status/metaborg/spt/build.yaml
[github:build]: https://github.com/metaborg/spt/actions
[license-badge]: https://img.shields.io/github/license/metaborg/spt
[license]: https://github.com/metaborg/spt/blob/main/LICENSE
[github-badge:release]: https://img.shields.io/github/v/release/metaborg/spt?display_name=release
[github:release]: https://github.com/metaborg/spt/releases
