/*
 * Copyright 2018 Tommaso Teofili and Simone Tripodi
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.github.tteofili.jtm.cli;

import java.util.List;

import com.google.common.collect.Iterables;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "jtm",
    descriptionHeading = "Tool for analyzing Atlassian Jira issues exported to the XML feed format\n",
    description = "This tool does topic modelling based on word2vec and paragraph vectors",
    versionProvider = JtmVersionProvider.class,
    subcommands = { AnalyzeCommand.class, MergeCommand.class, PipelineCommand.class, NearestCommand.class },
    separator = " ",
    sortOptions = true
)
public class JtmCommand {

    @Option(names = { "-h", "--help" }, usageHelp = true, description = "Display the usage message.")
    private boolean helpRequested = false;

    public static void main(String[] args) {
        JtmCommand jtmCommand = new JtmCommand();
        CommandLine mainCommandLine = new CommandLine(jtmCommand);

        List<CommandLine> parsed = null;
        try {
            parsed = mainCommandLine.parse(args);
        } catch (Throwable t) {
            System.err.println( t.getMessage() );
            System.exit( -1 );
        }

        CommandLine commandLine = Iterables.getLast(parsed);

        if (parsed.size() == 1 || commandLine.isUsageHelpRequested()) {
           commandLine.usage(System.out);
           System.exit( 0 );
        } else if (commandLine.isVersionHelpRequested()) {
           commandLine.printVersionHelp(System.out);
           System.exit( 0 );
        }

        Runnable runnable = commandLine.getCommand();
        runnable.run();
    }

}
