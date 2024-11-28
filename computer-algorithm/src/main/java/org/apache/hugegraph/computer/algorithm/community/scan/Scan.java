/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.apache.hugegraph.computer.algorithm.community.scan;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.hugegraph.computer.core.config.Config;
import org.apache.hugegraph.computer.core.graph.edge.Edge;
import org.apache.hugegraph.computer.core.graph.edge.Edges;
import org.apache.hugegraph.computer.core.graph.id.Id;
import org.apache.hugegraph.computer.core.graph.value.IdSet;
import org.apache.hugegraph.computer.core.graph.vertex.Vertex;
import org.apache.hugegraph.computer.core.worker.Computation;
import org.apache.hugegraph.computer.core.worker.ComputationContext;

public class Scan implements Computation<ScanMessage> {
    public static final String OPTION_MU = "scan.mu";
    public static final String OPTION_EPS = "scan.eps";
    public static final int MU_DEFAULT_VALUE = 2;
    public static final double EPS_DEFAULT_VALUE = 0.6;

    private int mu = 0;
    private double eps = 0.0D;

    @Override
    public String name() {
        return "scan";
    }

    @Override
    public String category() {
        return "community";
    }

    @Override
    public void init(Config config) {
        this.mu = config.getInt(OPTION_MU, MU_DEFAULT_VALUE);
        this.eps = config.getDouble(OPTION_EPS, EPS_DEFAULT_VALUE);
    }

    @Override
    public void compute0(ComputationContext context, Vertex vertex) {
        IdSet selfSet = new IdSet();
        selfSet.add(vertex.id());

        ScanMessage scanMessage = new ScanMessage(vertex.id(), selfSet, 0);

        context.sendMessageToAllEdgesIf(vertex, scanMessage, (ids, targetId) -> {
            return !vertex.id().equals(targetId);
        });

        vertex.value(new ScanValue());
    }

    @Override
    public void compute(ComputationContext context, Vertex vertex, Iterator<ScanMessage> messages) {
        IdSet neighbors = ((ScanValue) vertex.value()).neighbors();
        Integer cluster = this.scanCluster(context, vertex, messages, neighbors);
        if (cluster != null) {
            ((ScanValue) vertex.value()).cluster(cluster);
            // vertex.inactivate();
        }
    }

    protected Integer scanCluster(ComputationContext context, Vertex vertex, Iterator<ScanMessage> messages,
            IdSet neighbors) {
        if (context.superstep() == 1) { // Get neighbors of each vertices.
            // Collect outgoing neighbors
            Set<Id> outNeighbors = getOutNeighbors(vertex);
            neighbors.addAll(outNeighbors);

            // Collect incoming neighbors
            while (messages.hasNext()) {
                ScanMessage scanMessage = messages.next();
                Id inId = scanMessage.senderId();
                IdSet inNb = scanMessage.neighbors();
                assert inNb.value().size() == 1;
                if (!outNeighbors.contains(inId)) {
                    neighbors.add(inId);
                }
            }

            // Set neighbors to value.neighbors
            ((ScanValue) vertex.value()).neighbors(neighbors);

            // Send all neighbors to neighbors
            for (Id targetId : neighbors.value()) {
                ScanMessage scanMessage = new ScanMessage(vertex.id(), neighbors, 0);
                context.sendMessage(targetId, scanMessage);
            }
        } else if (context.superstep() == 2) { // Get eps-neighbors of each vertices.
            IdSet selfNeighbor = ((ScanValue) vertex.value()).neighbors();
            selfNeighbor.add(vertex.id());

            while (messages.hasNext()) {
                ScanMessage scanMessage = messages.next();
                Id u = scanMessage.senderId();
                IdSet uNb = scanMessage.neighbors();
                uNb.add(u);
                if (this.getSimilarity(selfNeighbor, uNb) >= eps) {
                    ((ScanValue) vertex.value()).epsNbAdd(u);
                }
            }

            // if this vertex is a core vertex(i.e. |epsNb| >= mu), send clusterID to its epsNb.

            IdSet epsNeighbors = ((ScanValue) vertex.value()).epsNeighbors();
            epsNeighbors.add(vertex.id());

            if (epsNeighbors.value().size() >= mu) {
                for (Id targetId : epsNeighbors.value()) {
                    ScanMessage scanMessage = new ScanMessage(vertex.id(), null, 1);
                    context.sendMessage(targetId, scanMessage);
                }
                return 1;
            }
            return 0;
        } else { // superstep>2, Clustering
            IdSet epsNeighbors = ((ScanValue) vertex.value()).epsNeighbors();

            if (epsNeighbors.value().size() >= mu) {
                for 
            } else {
                vertex.inactivate();
            }
        }
        return null;
    }

    private double getSimilarity(IdSet vNb, IdSet uNb) {
        Set<Id> vNbSet = vNb.value();
        Set<Id> uNbSet = uNb.value();

        // get the intersection
        Set<Id> commonNB = new HashSet<>(vNbSet);
        commonNB.retainAll(uNbSet);
        double similarity = commonNB.size() / Math.sqrt(vNbSet.size() * uNbSet.size());

        return similarity;
    }

    private static Set<Id> getOutNeighbors(Vertex vertex) {
        Set<Id> outNeighbors = new HashSet<>();
        Edges edges = vertex.edges();
        for (Edge edge : edges) {
            Id targetId = edge.targetId();
            if (!vertex.id().equals(targetId)) {
                outNeighbors.add(targetId);
            }
        }
        return outNeighbors;
    }
}
