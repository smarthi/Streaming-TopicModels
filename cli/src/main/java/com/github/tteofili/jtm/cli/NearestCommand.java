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

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import com.github.tteofili.jtm.AnalysisTool;
import com.github.tteofili.jtm.EndToEndAnalysisTool;
import com.github.tteofili.jtm.StaticAnalysisTool;
import com.github.tteofili.jtm.aggregation.Topics;
import com.github.tteofili.jtm.aggregation.TopicsWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "nearest", sortOptions = true)
public class NearestCommand implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(NearestCommand.class);

    @Option(names = { "-h", "--help" }, usageHelp = true, description = "Display the usage message.")
    private boolean helpRequested = false;

    @Option(names = {"-mf", "--model-file"}, description = "Vectors model file.", required = true)
    private String pvFile = "";

    @Option(names = { "-t", "--top-n" }, description = "Top.")
    private int topN = 5;

    @Option( names = { "-o", "--output" }, description = "The directory where writing the extracted topics.")
    protected File outputDir = new File(System.getProperty("user.dir"));

    @Parameters(index = "0", description = "Texts to analyse.", arity = "*", type = String.class)
    private String[] texts;

    private final TopicsWriter topicsWriter = new TopicsWriter();

    @Override
    public void run() {
        /*
         * exit statuses:
         * -1: error
         *  0: info
         *  1: success
         */

        Runtime.getRuntime().addShutdownHook( new ShutDownHook(log) );

        // setup the logging stuff

        System.setProperty( "logging.level", "INFO" );

        // assume SLF4J is bound to logback in the current environment
        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

        try
        {
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext( lc );
            // the context was probably already configured by default configuration
            // rules
            lc.reset();
            configurator.doConfigure( EndToEndAnalysisTool.class.getClassLoader().getResourceAsStream( "logback-config.xml" ) );
        }
        catch ( JoranException je )
        {
            // StatusPrinter should handle this
        }

        // GO!!!

        log.info( "                         ''~``" );
        log.info( "                        ( o o )" );
        log.info( "+------------------.oooO--(_)--Oooo.------------------+" );
        log.info( "{} v{}", new Object[]{ System.getProperty( "app.name" ), System.getProperty( "project.version" ) } );
        log.info( "+-----------------------------------------------------+" );
        log.info( "" );

        // setup the output directory
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        int status = 1;
        Throwable error = null;

        try {
            setUp();

            AnalysisTool analysisTool = getAnalisysTool();

            int i = 1;
            for (String text : texts) {
                Topics topics = analysisTool.analyze(text);
                if (topics != null) {
                    // write each file per topic
                    String fileName = "topics_"+i;
                    writeTopics(topics, fileName.substring(0, fileName.lastIndexOf( '.')));
                }
                i++;
            }

            tearDown();

        } catch (Throwable t) {
            status = -1;
            error = t;
        }

        log.info( "+-----------------------------------------------------+" );
        log.info( "{} {}", System.getProperty( "app.name" ).toUpperCase(), ( status < 0 ) ? "FAILURE" : "SUCCESS" );
        log.info( "+-----------------------------------------------------+" );

        if ( status < 0 )
        {
            error.printStackTrace();
            log.error( "Execution terminated with errors: {}", error.getMessage() );

            log.info( "+-----------------------------------------------------+" );
        }
    }

    protected void setUp() throws Exception {
        // do nothing by default
    }

    protected void tearDown() throws Exception {
        // do nothing by default
    }

    private void writeTopics(Topics topics, String fileName) throws Exception {
        File topicsJsonFile = new File(outputDir, fileName + ".json");
        FileOutputStream outputStream = new FileOutputStream(topicsJsonFile);
        topicsWriter.write( topics, outputStream);
        closeQuietly(outputStream);
    }

    private static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                // do nothing
            }
        }
    }

    private AnalysisTool getAnalisysTool() {
        File paragraphVectorsModelFile = new File(pvFile);
        if (!paragraphVectorsModelFile.exists()) {
            throw new RuntimeException("file "+pvFile+" doesn't exist");
        }
        return new StaticAnalysisTool(paragraphVectorsModelFile, topN);
    }

}
