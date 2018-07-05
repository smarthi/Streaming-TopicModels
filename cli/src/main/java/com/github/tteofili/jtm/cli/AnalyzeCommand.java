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

import com.github.tteofili.jtm.AnalysisTool;
import com.github.tteofili.jtm.EndToEndAnalysisTool;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "analyze", sortOptions = true)
public class AnalyzeCommand extends AbstractCommand {

    @Option(names = { "-e", "--epochs" }, description = "Epochs.")
    private int epochs = 5;

    @Option(names = { "-l", "--layer-size" }, description = "Layers.")
    private int layerSize = 200;

    @Option(names = { "-t", "--top-n" }, description = "Top.")
    private int topN = 5;

    @Option(names = { "-v", "--hierarchical-vectors" }, description = "Hierarchical vectors.")
    private boolean hierarchicalVectors = false;

    @Option(names = { "-c", "--include-comments" }, description = "Include comments.")
    private boolean includeComments = true;

    @Option(names = { "-g", "--generate-clusters" }, description = "Generate clusters.")
    private boolean generateClusters = false;

    @Option(names = {"-a", "--analyzer"}, description = "Analyzer.")
    private String analyzerType = "shingle-simple";

    @Option(names = {"-vo", "--vectors-output"}, description = "Vectors output file.")
    private String vectorsOutputFile = null;

    @Override
    protected AnalysisTool getAnalisysTool() {
        return new EndToEndAnalysisTool(epochs,
                                    layerSize,
                                    topN,
                                    hierarchicalVectors,
                                    includeComments,
                                    generateClusters,
                                    analyzerType,
                                    vectorsOutputFile);
    }

}
