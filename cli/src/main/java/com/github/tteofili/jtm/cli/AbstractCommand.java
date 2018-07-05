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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.tteofili.jtm.AnalysisTool;
import com.github.tteofili.jtm.EndToEndAnalysisTool;
import com.github.tteofili.jtm.aggregation.Topics;
import com.github.tteofili.jtm.aggregation.TopicsIndexer;
import com.github.tteofili.jtm.aggregation.TopicsWriter;
import com.github.tteofili.jtm.feed.Feed;
import com.github.tteofili.jtm.feed.reader.FeedReader;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

abstract class AbstractCommand implements Runnable {

    protected static final Logger log = LoggerFactory.getLogger(AbstractCommand.class);

    @Option(names = { "-h", "--help" }, usageHelp = true, description = "Display the usage message.")
    private boolean helpRequested = false;

    @Option(names = { "-V", "--version" }, versionHelp = true, description = "Display version info.")
    private boolean versionInfoRequested = false;

    @Option(names = { "-X", "--verbose" }, description = "Produce execution debug output.")
    private boolean verbose = false;

    @Option( names = { "-q", "--quiet" }, description = "Log errors only." )
    private boolean quiet = false;

    @Option( names = { "-r", "--reader" }, description = "The reader type name.", required = true )
    private String readerType;

    @Option(names = { "-i", "--index" }, description = "The index system to be used.")
    private String index = null;

    @Option( names = { "-o", "--output" }, description = "The directory where writing the extracted topics.")
    protected File outputDir = new File(System.getProperty("user.dir"));

    @Parameters(index = "0", description = "Exported JIRA XML feed file(s).", arity = "*")
    private File[] exportedJiraFeeds;

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

        if ( quiet )
        {
            System.setProperty( "logging.level", "ERROR" );
        }
        else if ( verbose )
        {
            System.setProperty( "logging.level", "DEBUG" );
        }
        else
        {
            System.setProperty( "logging.level", "INFO" );
        }

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

        // load all the available readers in the classpath
        Map<String, FeedReader> readers = new HashMap<>();
        ServiceLoader.load(FeedReader.class)
                     .forEach(feedReader -> readers.put(feedReader.getSourceType(), feedReader));
        FeedReader feedReader = readers.get(readerType);

        // load all the available indexers in the classpath
        Map<String, TopicsIndexer> indexers = new HashMap<>();
        ServiceLoader.load(TopicsIndexer.class)
                     .forEach(topicsIndexer -> indexers.put(topicsIndexer.getIndexerName(), topicsIndexer));
        TopicsIndexer topicsIndexer = null;
        if (index != null) {
            topicsIndexer = indexers.get(index);
        }

        int status = 1;
        Throwable error = null;
        InputStream input = null;
        Feed feed = null;

        try {
            if (feedReader == null) {
                throw new Exception("Feed reader '"
                                    + readerType
                                    + "' does not exists, availables are: "
                                    + readers.keySet());
            }

            if (index != null && topicsIndexer == null) {
                throw new Exception("Topics Indexer '"
                                    + index
                                    + "' does not exists, availables are: "
                                    + indexers.keySet());
            }

            setUp();
            if (topicsIndexer != null) {
                topicsIndexer.setUp();
            }

            AnalysisTool analysisTool = getAnalisysTool();

            for (File exportedJiraFeed : exportedJiraFeeds) {
                input = new FileInputStream(exportedJiraFeed);
                feed = feedReader.read(input);
                closeQuietly(input);

                Topics topics = analysisTool.analyze(feed);
                if (topics != null) {
                    // write each file per topic
                    String fileName = exportedJiraFeed.getName();
                    writeTopics(topics, fileName.substring(0, fileName.lastIndexOf( '.')));

                    if (topicsIndexer != null) {
                        topicsIndexer.index(feed, topics);
                    }
                }
            }

            tearDown();
            if (topicsIndexer != null) {
                topicsIndexer.tearDown();
            }
        } catch (Throwable t) {
            status = -1;
            error = t;
        } finally {
            closeQuietly(input);
        }

        log.info( "+-----------------------------------------------------+" );
        log.info( "{} {}", System.getProperty( "app.name" ).toUpperCase(), ( status < 0 ) ? "FAILURE" : "SUCCESS" );
        log.info( "+-----------------------------------------------------+" );

        if ( status < 0 )
        {
            log.error( "Execution terminated with errors", error );
            log.info( "+-----------------------------------------------------+" );
        }
    }

    protected abstract AnalysisTool getAnalisysTool();

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

}
