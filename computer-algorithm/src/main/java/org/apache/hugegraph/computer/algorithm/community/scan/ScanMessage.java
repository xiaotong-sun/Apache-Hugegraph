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

import java.io.IOException;

import org.apache.hugegraph.computer.core.common.ComputerContext;
import org.apache.hugegraph.computer.core.graph.GraphFactory;
import org.apache.hugegraph.computer.core.graph.id.Id;
import org.apache.hugegraph.computer.core.graph.id.IdFactory;
import org.apache.hugegraph.computer.core.graph.value.IdSet;
import org.apache.hugegraph.computer.core.graph.value.Value.CustomizeValue;
import org.apache.hugegraph.computer.core.io.RandomAccessInput;
import org.apache.hugegraph.computer.core.io.RandomAccessOutput;

public class ScanMessage implements CustomizeValue<ScanMessage> {

    private final GraphFactory graphFactory;

    private Id senderId;
    private IdSet neighbors;
    private int cluster;

    public ScanMessage() {
        this(IdFactory.createId(), new IdSet(), 0);
    }

    public ScanMessage(Id senderId, IdSet neighbors, int cluster) {
        this.graphFactory = ComputerContext.instance().graphFactory();
        this.senderId = senderId;
        this.neighbors = neighbors;
        this.cluster = cluster;
    }

    public Id senderId() {
        return this.senderId;
    }

    public IdSet neighbors() {
        return this.neighbors;
    }

    public int cluster() {
        return this.cluster;
    }

    @Override
    public void write(RandomAccessOutput out) throws IOException {
        this.senderId.write(out);
    }

    @Override
    public void read(RandomAccessInput in) throws IOException {
        this.senderId = this.graphFactory.createId();
        this.senderId.read(in);
    }

    @Override
    public ScanMessage value() {
        throw new UnsupportedOperationException();
    }
}
