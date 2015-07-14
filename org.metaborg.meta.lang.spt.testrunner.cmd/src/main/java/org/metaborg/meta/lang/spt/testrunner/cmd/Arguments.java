package org.metaborg.meta.lang.spt.testrunner.cmd;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(separators = "=")
public class Arguments {
    @Parameter(names = { "--help", "-h" }, description = "Shows usage help", required = false, help = true) public boolean help;

    @Parameter(names = { "--lang", "-l" }, description = "Location of language under test.", required = true) public String targetLanguageLocation;

    @Parameter(names = { "--tests", "-t" },
        description = "Location of test files. Defaults to current directory.", required = false) public String testsLocation;
}
