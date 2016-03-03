package org.metaborg.spt.cmd;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(separators = "=")
public class Arguments {
    @Parameter(names = { "--help", "-h" }, description = "Shows usage help", required = false,
        help = true) public boolean help;

    @Parameter(names = { "--lang", "-l" }, description = "Location of language under test",
        required = true) public String targetLanguageLocation;

    @Parameter(names = { "--spt", "-s" }, description = "Location of the SPT language",
        required = true) public String sptLocation;

    @Parameter(names = { "--tests", "-t" }, description = "Location of test files",
        required = true) public String testsLocation;

    @Parameter(names = { "--exit" }, description = "Immediately exit, used for testing purposes",
        hidden = true) public boolean exit;
}
