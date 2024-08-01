<!--
!! THIS FILE WAS GENERATED USING repoman !!
Modify `repo.yaml` instead and use `repoman` to update this file
See: https://github.com/metaborg/metaborg-gradle/
-->

# SPT
[![Build][github-badge:build]][github:build]
[![License][license-badge]][license]
[![GitHub Release][github-badge:release]][github:release]

Meta-languages for testing your DSLs.


## Spoofax 3 Artifacts

| Spoofax Language | Latest Release | Latest Snapshot |
|----------|----------------|-----------------|
| `org.metaborg.devenv:org.metaborg.meta.lang.spt` | [![Release][mvn-rel-badge:org.metaborg.devenv:org.metaborg.meta.lang.spt]][mvn:org.metaborg.devenv:org.metaborg.meta.lang.spt] | [![Snapshot][mvn-snap-badge:org.metaborg.devenv:org.metaborg.meta.lang.spt]][mvn:org.metaborg.devenv:org.metaborg.meta.lang.spt] |

| Maven Artifact | Latest Release | Latest Snapshot |
|----------|----------------|-----------------|
| `org.metaborg.devenv:org.metaborg.mbt.core` | [![Release][mvn-rel-badge:org.metaborg.devenv:org.metaborg.mbt.core]][mvn:org.metaborg.devenv:org.metaborg.mbt.core] | [![Snapshot][mvn-snap-badge:org.metaborg.devenv:org.metaborg.mbt.core]][mvn:org.metaborg.devenv:org.metaborg.mbt.core] |
| `org.metaborg.devenv:org.metaborg.spt.core` | [![Release][mvn-rel-badge:org.metaborg.devenv:org.metaborg.spt.core]][mvn:org.metaborg.devenv:org.metaborg.spt.core] | [![Snapshot][mvn-snap-badge:org.metaborg.devenv:org.metaborg.spt.core]][mvn:org.metaborg.devenv:org.metaborg.spt.core] |


## Spoofax 2 Artifacts

| Spoofax Language | Latest Release | Latest Snapshot |
|----------|----------------|-----------------|
| `org.metaborg:org.metaborg.lang.minisql` | [![Release][mvn-rel-badge:org.metaborg:org.metaborg.lang.minisql]][mvn:org.metaborg:org.metaborg.lang.minisql] | [![Snapshot][mvn-snap-badge:org.metaborg:org.metaborg.lang.minisql]][mvn:org.metaborg:org.metaborg.lang.minisql] |
| `org.metaborg:org.metaborg.meta.lang.spt` | [![Release][mvn-rel-badge:org.metaborg:org.metaborg.meta.lang.spt]][mvn:org.metaborg:org.metaborg.meta.lang.spt] | [![Snapshot][mvn-snap-badge:org.metaborg:org.metaborg.meta.lang.spt]][mvn:org.metaborg:org.metaborg.meta.lang.spt] |
| `org.metaborg:org.metaborg.meta.lang.spt.interactive` | [![Release][mvn-rel-badge:org.metaborg:org.metaborg.meta.lang.spt.interactive]][mvn:org.metaborg:org.metaborg.meta.lang.spt.interactive] | [![Snapshot][mvn-snap-badge:org.metaborg:org.metaborg.meta.lang.spt.interactive]][mvn:org.metaborg:org.metaborg.meta.lang.spt.interactive] |

| Maven Artifact | Latest Release | Latest Snapshot |
|----------|----------------|-----------------|
| `org.metaborg:org.metaborg.mbt.core` | [![Release][mvn-rel-badge:org.metaborg:org.metaborg.mbt.core]][mvn:org.metaborg:org.metaborg.mbt.core] | [![Snapshot][mvn-snap-badge:org.metaborg:org.metaborg.mbt.core]][mvn:org.metaborg:org.metaborg.mbt.core] |
| `org.metaborg:org.metaborg.meta.lang.spt.eclipse` | [![Release][mvn-rel-badge:org.metaborg:org.metaborg.meta.lang.spt.eclipse]][mvn:org.metaborg:org.metaborg.meta.lang.spt.eclipse] | [![Snapshot][mvn-snap-badge:org.metaborg:org.metaborg.meta.lang.spt.eclipse]][mvn:org.metaborg:org.metaborg.meta.lang.spt.eclipse] |
| `org.metaborg:org.metaborg.meta.lang.spt.interactive.eclipse` | [![Release][mvn-rel-badge:org.metaborg:org.metaborg.meta.lang.spt.interactive.eclipse]][mvn:org.metaborg:org.metaborg.meta.lang.spt.interactive.eclipse] | [![Snapshot][mvn-snap-badge:org.metaborg:org.metaborg.meta.lang.spt.interactive.eclipse]][mvn:org.metaborg:org.metaborg.meta.lang.spt.interactive.eclipse] |
| `org.metaborg:org.metaborg.spt.core` | [![Release][mvn-rel-badge:org.metaborg:org.metaborg.spt.core]][mvn:org.metaborg:org.metaborg.spt.core] | [![Snapshot][mvn-snap-badge:org.metaborg:org.metaborg.spt.core]][mvn:org.metaborg:org.metaborg.spt.core] |
| `org.metaborg:org.metaborg.spt.cmd` | [![Release][mvn-rel-badge:org.metaborg:org.metaborg.spt.cmd]][mvn:org.metaborg:org.metaborg.spt.cmd] | [![Snapshot][mvn-snap-badge:org.metaborg:org.metaborg.spt.cmd]][mvn:org.metaborg:org.metaborg.spt.cmd] |
| `org.metaborg:org.metaborg.spt.testrunner.eclipse` | [![Release][mvn-rel-badge:org.metaborg:org.metaborg.spt.testrunner.eclipse]][mvn:org.metaborg:org.metaborg.spt.testrunner.eclipse] | [![Snapshot][mvn-snap-badge:org.metaborg:org.metaborg.spt.testrunner.eclipse]][mvn:org.metaborg:org.metaborg.spt.testrunner.eclipse] |


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
Copyright 2010-2024 [Programming Languages Group](https://pl.ewi.tudelft.nl/), [Delft University of Technology](https://www.tudelft.nl/)

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at <https://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an **"as is" basis, without warranties or conditions of any kind**, either express or implied. See the License for the specific language governing permissions and limitations under the License.

[github-badge:build]: https://img.shields.io/github/actions/workflow/status/metaborg/spt/build.yaml
[github:build]: https://github.com/metaborg/spt/actions
[license-badge]: https://img.shields.io/github/license/metaborg/spt
[license]: https://github.com/metaborg/spt/blob/master/LICENSE.md
[github-badge:release]: https://img.shields.io/github/v/release/metaborg/spt?display_name=release
[github:release]: https://github.com/metaborg/spt/releases
[mvn:org.metaborg.devenv:org.metaborg.mbt.core]: https://artifacts.metaborg.org/#nexus-search;gav~org.metaborg.devenv~org.metaborg.mbt.core~~~
[mvn:org.metaborg.devenv:org.metaborg.meta.lang.spt]: https://artifacts.metaborg.org/#nexus-search;gav~org.metaborg.devenv~org.metaborg.meta.lang.spt~~~
[mvn:org.metaborg.devenv:org.metaborg.spt.core]: https://artifacts.metaborg.org/#nexus-search;gav~org.metaborg.devenv~org.metaborg.spt.core~~~
[mvn:org.metaborg:org.metaborg.lang.minisql]: https://artifacts.metaborg.org/#nexus-search;gav~org.metaborg~org.metaborg.lang.minisql~~~
[mvn:org.metaborg:org.metaborg.mbt.core]: https://artifacts.metaborg.org/#nexus-search;gav~org.metaborg~org.metaborg.mbt.core~~~
[mvn:org.metaborg:org.metaborg.meta.lang.spt]: https://artifacts.metaborg.org/#nexus-search;gav~org.metaborg~org.metaborg.meta.lang.spt~~~
[mvn:org.metaborg:org.metaborg.meta.lang.spt.eclipse]: https://artifacts.metaborg.org/#nexus-search;gav~org.metaborg~org.metaborg.meta.lang.spt.eclipse~~~
[mvn:org.metaborg:org.metaborg.meta.lang.spt.interactive]: https://artifacts.metaborg.org/#nexus-search;gav~org.metaborg~org.metaborg.meta.lang.spt.interactive~~~
[mvn:org.metaborg:org.metaborg.meta.lang.spt.interactive.eclipse]: https://artifacts.metaborg.org/#nexus-search;gav~org.metaborg~org.metaborg.meta.lang.spt.interactive.eclipse~~~
[mvn:org.metaborg:org.metaborg.spt.cmd]: https://artifacts.metaborg.org/#nexus-search;gav~org.metaborg~org.metaborg.spt.cmd~~~
[mvn:org.metaborg:org.metaborg.spt.core]: https://artifacts.metaborg.org/#nexus-search;gav~org.metaborg~org.metaborg.spt.core~~~
[mvn:org.metaborg:org.metaborg.spt.testrunner.eclipse]: https://artifacts.metaborg.org/#nexus-search;gav~org.metaborg~org.metaborg.spt.testrunner.eclipse~~~
[mvn-rel-badge:org.metaborg.devenv:org.metaborg.mbt.core]: https://img.shields.io/nexus/r/org.metaborg.devenv/org.metaborg.mbt.core?server=https%3A%2F%2Fartifacts.metaborg.org&label=%20
[mvn-rel-badge:org.metaborg.devenv:org.metaborg.meta.lang.spt]: https://img.shields.io/nexus/r/org.metaborg.devenv/org.metaborg.meta.lang.spt?server=https%3A%2F%2Fartifacts.metaborg.org&label=%20
[mvn-rel-badge:org.metaborg.devenv:org.metaborg.spt.core]: https://img.shields.io/nexus/r/org.metaborg.devenv/org.metaborg.spt.core?server=https%3A%2F%2Fartifacts.metaborg.org&label=%20
[mvn-rel-badge:org.metaborg:org.metaborg.lang.minisql]: https://img.shields.io/nexus/r/org.metaborg/org.metaborg.lang.minisql?server=https%3A%2F%2Fartifacts.metaborg.org&label=%20
[mvn-rel-badge:org.metaborg:org.metaborg.mbt.core]: https://img.shields.io/nexus/r/org.metaborg/org.metaborg.mbt.core?server=https%3A%2F%2Fartifacts.metaborg.org&label=%20
[mvn-rel-badge:org.metaborg:org.metaborg.meta.lang.spt]: https://img.shields.io/nexus/r/org.metaborg/org.metaborg.meta.lang.spt?server=https%3A%2F%2Fartifacts.metaborg.org&label=%20
[mvn-rel-badge:org.metaborg:org.metaborg.meta.lang.spt.eclipse]: https://img.shields.io/nexus/r/org.metaborg/org.metaborg.meta.lang.spt.eclipse?server=https%3A%2F%2Fartifacts.metaborg.org&label=%20
[mvn-rel-badge:org.metaborg:org.metaborg.meta.lang.spt.interactive]: https://img.shields.io/nexus/r/org.metaborg/org.metaborg.meta.lang.spt.interactive?server=https%3A%2F%2Fartifacts.metaborg.org&label=%20
[mvn-rel-badge:org.metaborg:org.metaborg.meta.lang.spt.interactive.eclipse]: https://img.shields.io/nexus/r/org.metaborg/org.metaborg.meta.lang.spt.interactive.eclipse?server=https%3A%2F%2Fartifacts.metaborg.org&label=%20
[mvn-rel-badge:org.metaborg:org.metaborg.spt.cmd]: https://img.shields.io/nexus/r/org.metaborg/org.metaborg.spt.cmd?server=https%3A%2F%2Fartifacts.metaborg.org&label=%20
[mvn-rel-badge:org.metaborg:org.metaborg.spt.core]: https://img.shields.io/nexus/r/org.metaborg/org.metaborg.spt.core?server=https%3A%2F%2Fartifacts.metaborg.org&label=%20
[mvn-rel-badge:org.metaborg:org.metaborg.spt.testrunner.eclipse]: https://img.shields.io/nexus/r/org.metaborg/org.metaborg.spt.testrunner.eclipse?server=https%3A%2F%2Fartifacts.metaborg.org&label=%20
[mvn-snap-badge:org.metaborg.devenv:org.metaborg.mbt.core]: https://img.shields.io/nexus/s/org.metaborg.devenv/org.metaborg.mbt.core?server=https%3A%2F%2Fartifacts.metaborg.org&label=%20
[mvn-snap-badge:org.metaborg.devenv:org.metaborg.meta.lang.spt]: https://img.shields.io/nexus/s/org.metaborg.devenv/org.metaborg.meta.lang.spt?server=https%3A%2F%2Fartifacts.metaborg.org&label=%20
[mvn-snap-badge:org.metaborg.devenv:org.metaborg.spt.core]: https://img.shields.io/nexus/s/org.metaborg.devenv/org.metaborg.spt.core?server=https%3A%2F%2Fartifacts.metaborg.org&label=%20
[mvn-snap-badge:org.metaborg:org.metaborg.lang.minisql]: https://img.shields.io/nexus/s/org.metaborg/org.metaborg.lang.minisql?server=https%3A%2F%2Fartifacts.metaborg.org&label=%20
[mvn-snap-badge:org.metaborg:org.metaborg.mbt.core]: https://img.shields.io/nexus/s/org.metaborg/org.metaborg.mbt.core?server=https%3A%2F%2Fartifacts.metaborg.org&label=%20
[mvn-snap-badge:org.metaborg:org.metaborg.meta.lang.spt]: https://img.shields.io/nexus/s/org.metaborg/org.metaborg.meta.lang.spt?server=https%3A%2F%2Fartifacts.metaborg.org&label=%20
[mvn-snap-badge:org.metaborg:org.metaborg.meta.lang.spt.eclipse]: https://img.shields.io/nexus/s/org.metaborg/org.metaborg.meta.lang.spt.eclipse?server=https%3A%2F%2Fartifacts.metaborg.org&label=%20
[mvn-snap-badge:org.metaborg:org.metaborg.meta.lang.spt.interactive]: https://img.shields.io/nexus/s/org.metaborg/org.metaborg.meta.lang.spt.interactive?server=https%3A%2F%2Fartifacts.metaborg.org&label=%20
[mvn-snap-badge:org.metaborg:org.metaborg.meta.lang.spt.interactive.eclipse]: https://img.shields.io/nexus/s/org.metaborg/org.metaborg.meta.lang.spt.interactive.eclipse?server=https%3A%2F%2Fartifacts.metaborg.org&label=%20
[mvn-snap-badge:org.metaborg:org.metaborg.spt.cmd]: https://img.shields.io/nexus/s/org.metaborg/org.metaborg.spt.cmd?server=https%3A%2F%2Fartifacts.metaborg.org&label=%20
[mvn-snap-badge:org.metaborg:org.metaborg.spt.core]: https://img.shields.io/nexus/s/org.metaborg/org.metaborg.spt.core?server=https%3A%2F%2Fartifacts.metaborg.org&label=%20
[mvn-snap-badge:org.metaborg:org.metaborg.spt.testrunner.eclipse]: https://img.shields.io/nexus/s/org.metaborg/org.metaborg.spt.testrunner.eclipse?server=https%3A%2F%2Fartifacts.metaborg.org&label=%20
