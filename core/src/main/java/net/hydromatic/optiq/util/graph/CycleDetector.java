/*
// Licensed to Julian Hyde under one or more contributor license
// agreements. See the NOTICE file distributed with this work for
// additional information regarding copyright ownership.
//
// Julian Hyde licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except in
// compliance with the License. You may obtain a copy of the License at:
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
*/
package net.hydromatic.optiq.util.graph;

import java.util.Set;

/**
 * Detects cycles in directed graphs.
 */
public class CycleDetector<V, E extends DefaultEdge> {
  private final DirectedGraph<V, E> graph;

  public CycleDetector(DirectedGraph<V, E> graph) {
    this.graph = graph;
  }

  public Set<V> findCycles() {
    return new TopologicalOrderIterator<V, E>(graph).findCycles();
  }
}

// End CycleDetector.java

