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

import java.util.Date;
import java.util.Formatter;

import org.slf4j.Logger;

final class ShutDownHook extends Thread {

    private final long start = System.currentTimeMillis();

    private final Logger log;

    public ShutDownHook(Logger log) {
        super("shutdown-hook");
        this.log = log;
    }

    public void run() {
        // format the uptime string

        Formatter uptimeFormatter = new Formatter();
        uptimeFormatter.format( "Total uptime:" );

        long uptime = System.currentTimeMillis() - start;
        if (uptime < 1000) {
            uptimeFormatter.format( " %s millisecond%s", uptime, ( uptime > 1 ? "s" : "" ) );
        } else {
            long uptimeInSeconds = ( uptime ) / 1000;
            final long hours = uptimeInSeconds / 3600;

            if ( hours > 0 )
            {
                uptimeFormatter.format( " %s hour%s", hours, ( hours > 1 ? "s" : "" ) );
            }

            uptimeInSeconds = uptimeInSeconds - ( hours * 3600 );
            final long minutes = uptimeInSeconds / 60;

            if ( minutes > 0 )
            {
                uptimeFormatter.format( " %s minute%s", minutes, ( minutes > 1 ? "s" : "" ) );
            }

            uptimeInSeconds = uptimeInSeconds - ( minutes * 60 );

            if ( uptimeInSeconds > 0 )
            {
                uptimeFormatter.format( " %s second%s", uptimeInSeconds, ( uptimeInSeconds > 1 ? "s" : "" ) );
            }
        }

        log.info( uptimeFormatter.toString() );
        uptimeFormatter.close();

        log.info( "Finished at: {}", new Date() );

        final Runtime runtime = Runtime.getRuntime();
        final int megaUnit = 1024 * 1024;
        log.info( "Final Memory: {}M/{}M",
                     ( runtime.totalMemory() - runtime.freeMemory() ) / megaUnit,
                     runtime.totalMemory() / megaUnit );

        log.info( "                     .oooO                            " );
        log.info( "                     (   )   Oooo.                    " );
        log.info( "+---------------------\\ (----(   )--------------------+" );
        log.info( "                       \\_)    ) /" );
        log.info( "                             (_/" );
    }

}
