/**
 * Copyright (C) 2013-2018 Centro de Investigación en Tecnoloxías da Información (CITIUS) (http://citius.usc.es)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
* Copyright 2013 Centro de Investigación en Tecnoloxías da Información (CITIUS), University of Santiago de Compostela (USC).
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package es.usc.citius.hipster.algorithm;

import es.usc.citius.hipster.model.Transition;
import es.usc.citius.hipster.model.node.ADStarNode;
import es.usc.citius.hipster.model.node.impl.ADStarNodeExpander;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * <p>Iterative implementation of the forward Anytime Dynamic A* (AD*-f) search algorithm.</p>
 *
 * <p>AD* is an anytime, dynamic search algorithm. It is able to obtain suboptimal-bounded solutions,
 * tuning the quality of the solution based on the available search time (this is done by adjusting
 * the heuristic inflation parameter, epsilon). This algorithm is executed
 * iteratively improving the quality of the solution and reusing previous search efforts. The algorithm
 * also takes into account the changes produced over the graph arc costs to incrementally repair
 * the previous solution. AD* provides anytime results and an efficient
 * way to solve dynamic search problems.</p>
 *
 * <p>This is the forward implementation of AD*, the algorithm starts exploring the state space
 * from the beginning state and trying to reach a goal state (or multiple ones).</p>
 *
 * <p><u>Reference</u>:
 * </br>Maxim Likhachev, David Ferguson, Geoffrey Gordon, Anthony (Tony) Stentz, and Sebastian Thrun,
 * <b><a href="http://www-cgi.cs.cmu.edu/afs/cs.cmu.edu/Web/People/maxim/files/ad_icaps05.pdf">
 * "Anytime Dynamic A*: An Anytime, Replanning Algorithm"</a></b>
 * <i>Proceedings of the International Conference on Automated Planning and Scheduling (ICAPS), June, 2005.</i></p>
 *
 * @param <A> class defining the action
 * @param <S> class defining the state
 * @param <C> class defining the cost, must implement {@link java.lang.Comparable}
 * @param <N> type of the nodes
 *
 * @author Adrián González Sieira <<a href="adrian.gonzalez@usc.es">adrian.gonzalez@usc.es</a>>
 */
public class ADStarForward<A,S,C extends Comparable<C>, N extends ADStarNode<A, S, C, N>> extends Algorithm<A, S, N> {

    protected S begin;
    protected Collection<S> goals;
    protected ADStarNodeExpander<A, S, C, N> expander;

    /**
     * Create an instance of the algorithm with a begin, a goal and a component to
     * expand new nodes from the current one.
     *
     * @param begin begin state
     * @param goal goal state
     * @param expander component which generates new nodes from the current
     */
    public ADStarForward(S begin, S goal, ADStarNodeExpander<A, S, C, N> expander) {
        this(begin, Collections.singleton(goal), expander);
    }

    /**
     * Create an instance of the algorithm with a begin, multiple goals and a component to
     * expand new nodes from the current one.
     *
     * @param begin begin state
     * @param goals collection of goal states
     * @param expander component which generates new nodes from the current
     */
    public ADStarForward(S begin, Collection<S> goals, ADStarNodeExpander<A, S, C, N> expander) {
        this.begin = begin;
        this.goals = goals;
        this.expander = expander;
    }

    @Override
    public Iterator iterator() {
        return new Iterator();
    }

    /**
     * Internal iterator that implements all the logic of the A* search
     */
    public class Iterator implements java.util.Iterator<N> {
        //queues used by the algorithm
        protected Map<S, N> open;
        protected Map<S, N> closed;
        protected Map<S, N> incons;
        protected Collection<Transition<A, S>> transitionsChanged;
        protected Queue<N> queue;
        protected boolean replan;
        protected final N beginNode;
        protected final Collection<N> goalNodes;

        protected Iterator() {
            this.replan = false;
            //initialize nodes
            this.beginNode = expander.makeNode(null, new Transition<A, S>(null, begin));
            //initialize goal node collection
            this.goalNodes = new ArrayList<N>(goals.size());
            //iterate over the set of goals
            for(S current : goals){
                //create new node for current goal
                this.goalNodes.add(expander.makeNode(beginNode, new Transition<A, S>(null, current)));
            }
            //initialize queues of the algorithm
            this.open = new HashMap<S, N>();
            this.closed = new HashMap<S, N>();
            this.incons = new HashMap<S, N>();
            this.queue = new PriorityQueue<N>();
            //initialize list of visited nodes
            expander.clearVisited();
            //initialize set of changed transitions
            this.transitionsChanged = new HashSet<Transition<A, S>>();
            //mark begin node as visited by the algorithm
            expander.getVisited().put(beginNode.state(), beginNode);
            //mark goal nodes as visited
            for(N current : goalNodes){
                //mark current current as visited by the algorithm
                expander.getVisited().put(current.state(), current);
            }
            //insert beginning node at OPEN
            insertOpen(beginNode);
        }

        /**
         * Inserts a node in the open queue.
         *
         * @param node instance of node to add
         */
        protected void insertOpen(N node) {
            this.open.put(node.state(), node);
            this.queue.offer(node);
        }

        /**
         * Retrieves the most promising node from the open collection, or null if it
         * is empty.
         *
         * @return most promising node
         */
        protected N takePromising() {
            while (!queue.isEmpty()) {
                N head = queue.peek();
                if (!open.containsKey(head.state())) {
                    queue.poll();
                } else {
                    return head;
                }
            }
            return null;
        }

        /**
         * Updates the membership of the node to the algorithm queues.
         *
         * @param node instance of node
         */
        protected void updateQueues(N node) {
            S state = node.state();
            if (node.getV().compareTo(node.getG()) != 0) {
                if (!this.closed.containsKey(state)) {
                    insertOpen(node);
                } else {
                    this.incons.put(state, node);
                }
            } else {
                this.open.remove(state);
                //this.queue.remove(node);
                this.incons.remove(state);
            }
            //remove flag to update queues
            node.setDoUpdate(false);
        }

        /**
         * The iterator will have next() nodes when the stop condition of the algorithm is not reached, or if the
         * value of Epsilon has changed and a replanning is needed. It may happen that after chaning Epsilon the
         * solution does not improve, and therefore this method will return false. In that case, Epsilon should be
         * reduced again until the minimum value of 1 is reached. In that case, the solution will be optimal.
         *
         * @see #setEpsilon(double)
         *
         * @return true if a replan is pending or the solution has not been found
         */
        @Override
        public boolean hasNext() {
            N current = takePromising();
            N minGoal = Collections.min(goalNodes);
            return replan || this.open.containsKey(minGoal.state()) || minGoal.compareTo(current) >= 0 || minGoal.getV().compareTo(minGoal.getG()) < 0;
        }

        /**
         * Removing is not supported.
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public N next() {
            //First node in OPEN retrieved, not removed
            N current = takePromising();
            S state = current.state();
            N minGoal = Collections.min(goalNodes);
            if (minGoal.compareTo(current) >= 0 || minGoal.getV().compareTo(minGoal.getG()) < 0) {
                //s removed from OPEN
                open.remove(state);
                //this.queue.remove(current);
                //if v(s) > g(s)
                boolean consistent = current.isConsistent();
                if (consistent) {
                    //v(s) = g(s)
                    current.setV(current.getG());
                    //closed = closed U current
                    closed.put(state, current);
                } else {
                    //v(s) = Infinity
                    expander.setMaxV(current);
                    updateQueues(current);
                }
                expander.setNodeConsistent(consistent);
                //expand successors
                for (N successorNode : expander.expand(current)) {
                    if(successorNode.isDoUpdate()){
                        updateQueues(successorNode);
                    }
                }
            } else {
                this.replan = false;
                // for all directed edges (u, v) with changed edge costs
                for(N nodeTransitionsChanged : expander.expandTransitionsChanged(beginNode, transitionsChanged)){
                    updateQueues(nodeTransitionsChanged);
                }
                //empty the list of transitions
                transitionsChanged.clear();
                //move states from INCONS to OPEN
                open.putAll(incons);
                //empty INCONS queue
                incons.clear();
                //updateQueues the priorities for all s in OPEN according to key(s)
                queue.clear();
                for(N node : open.values()){
                    //key is recalculated according to the new value of Epsilon
                    expander.updateKey(node);
                    //insert into the priority queue
                    queue.offer(node);
                }
                //closed = empty
                closed.clear();
                current = takePromising();
            }
            return current;
        }

        /**
         * AD* uses the OPEN queue to order the most promising nodes to be expanded by the
         * algorithm. This method retrieves the original map (not a copy) that contains
         * the pairs of <State, Node>
         *
         * @return open map with the unexplored nodes and states.
         */
        public Map<S, N> getOpen() { return open; }

        /**
         * Get the internal map used by the algorithm to keep the relations between
         * explored states and nodes. Modifications to the map can alter the normal
         * function of the algorithm.
         *
         * @return closed map with the explored nodes and states
         */
        public Map<S, N> getClosed() { return closed; }

        public Map<S, N> getIncons() { return incons; }

        /**
         * Retrieves the list of goal nodes for its modification.
         *
         * @return list of goals
         */
        public Collection<N> getGoalNodes() {
            return goalNodes;
        }

        /**
         * Updates the value of epsilon to improve the cost of the solutions.
         * The update of Epsilon must be manually done, as this parameter higlhy depends on the heuristic used,
         * and the search problem. Use only values of epsilon above 1, as the opposite will lead to diminish the
         * estimate of the heuristic, which is supposed to be optimistic. Values below 1 result in underestimating
         * the cost to the goal, and therefore in a greater number of expansions to find the same solution than with
         * epsilon = 1.
         *
         * @param epsilon new value of epsilon (sub-optimal bound to obtain anytime solutions)
         */
        public void setEpsilon(double epsilon){
            this.replan = true;
            expander.setEpsilon(epsilon);
        }

        /**
         * Queries the current value of Epsilon (sub-optimal bound for anytime solutions).
         *
         * @return current value of Epsilon
         */
        public double getEpsilon(){
            return expander.getEpsilon();
        }

        /**
         * Marks transitions to be processed in the next replan event.
         *
         * @param transitions
         */
        public void addTransitionsChanged(Collection<Transition<A, S>> transitions){
            this.replan = true;
            transitionsChanged.addAll(transitions);
        }
    }

}
