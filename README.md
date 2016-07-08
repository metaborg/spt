# SPT

The Spoofax Testing Language (SPT) allows you to test your DSLs.
See [http://www.metaborg.org/spt](http://www.metaborg.org/spt/) for documentation on SPT.

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
