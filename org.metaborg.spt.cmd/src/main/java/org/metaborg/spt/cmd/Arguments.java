package org.metaborg.spt.cmd;

import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(separators = "=")
public class Arguments {
    @Parameter(names = { "--help", "-h" }, description = "Shows usage help", required = false,
        help = true) public boolean help;


    @Parameter(names = { "--lut", "-l" }, description = "Location of the language under test",
        required = true) public String lutLocation;

    @Parameter(names = { "--spt", "-s" }, description = "Location of the SPT language",
        required = true) public String sptLocation;

    @Parameter(names = { "--tests", "-t" }, description = "Location of test files",
        required = true) public String testsLocation;


    @Parameter(names = { "--start-symbol", "-start" }, description = "Start Symbol for these tests",
        required = false) public String startSymbol;

    @Parameter(names = { "--lang", "-ol" }, description = "Location of any other language that should be loaded",
        required = false) public List<String> targetLanguageLocation = new ArrayList<>();

    @Parameter(names = { "--reporter", "-C" }, description = "Class name of custom reporter to use",
            required = false) public String customReporter;


    @Parameter(names = { "--exit" }, description = "Immediately exit, used for testing purposes",
        hidden = true) public boolean exit;
}
