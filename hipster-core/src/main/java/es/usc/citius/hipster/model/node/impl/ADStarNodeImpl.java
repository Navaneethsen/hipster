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
package es.usc.citius.hipster.model.node.impl;

import es.usc.citius.hipster.model.node.ADStarNode;
import es.usc.citius.hipster.model.node.AbstractNode;
import es.usc.citius.hipster.model.Transition;
import es.usc.citius.hipster.model.node.Node;

/**
 * Interface defining the basic operations for {@link Node} to be used with
 * {@link es.usc.citius.hipster.algorithm.ADStarForward}. Contains the declaration of the methods to retrieve
 * te cost elements of the node (G and V) and the definition of the {@link ADStarNodeImpl.Key}
 * to compare {@link ADStarNodeImpl} elements.
 *
 * @param <A> type of the actions ({@code Void} if actions are not explicit).
 * @param <S> type of the states.
 * @param <C> type of the cost
 *
 * @author Adrián González Sieira <adrian.gonzalez@usc.es>
 */
public class ADStarNodeImpl<A, S, C extends Comparable<C>>
        extends AbstractNode<A, S, ADStarNodeImpl<A, S, C>>
        implements ADStarNode<A, S, C, ADStarNodeImpl<A, S, C>> {

    protected C g;
    protected C v;
    protected ADStarNode.Key<C> key;
    protected boolean doUpdate;

    /**
     * Default constructor for ADStarNode. Requires the transition used
     * to reach the new one and the previous node. The current cost (G),
     * score (V) and key to compare between ADStarNode instances are also required.
     *
     * @param transition incoming transition
     * @param previousNode parent node
     * @param g accumulated cost from begin
     * @param v score to goal
     * @param k key value evaluated over G and V
     */
    public ADStarNodeImpl(Transition<A, S> transition, ADStarNodeImpl<A, S, C> previousNode, C g, C v, ADStarNode.Key<C> k) {
        super(previousNode, transition.getState(), transition.getAction());
        this.g = g;
        this.v = v;
        this.key = k;
    }

    /**
     * Cost from beginning state.
     *
     * @return object representing the current cost
     */
    @Override
    public C getG() {
        return g;
    }

    /**
     * Score to goal given as heuristic.
     *
     * @return object representing the estimated cost to goal
     */
    @Override
    public C getV() {
        return v;
    }

    @Override
    public boolean isDoUpdate() {
        return doUpdate;
    }

    @Override
    public void setG(C g) {
        this.g = g;
    }

    @Override
    public void setV(C v) {
        this.v = v;
    }

    @Override
    public void setKey(ADStarNode.Key<C> key) {
        this.key = key;
    }

    @Override
    public Key<C> getKey() {
        return key;
    }

    @Override
    public void setPreviousNode(ADStarNodeImpl<A, S, C> parent){
        this.previousNode = parent;
    }

    @Override
    public void setState(S state){
        this.state = state;
    }

    @Override
    public void setAction(A action){
        this.action = action;
    }

    @Override
    public void setDoUpdate(boolean doUpdate) {
        this.doUpdate = doUpdate;
    }

    /**
     * Returns if the node is in a consistent state or not.
     * @return v > g
     */
    public boolean isConsistent(){ return v.compareTo(g) > 0; }

    @Override
    public C getEstimation() {
        return v;
    }

    @Override
    public C getScore() {
        return key.getFirst();
    }

    @Override
    public C getCost() {
        return g;
    }

    /**
     * Method to retrieve the parent ADStarNode, the same
     * retrieved by {@link es.usc.citius.hipster.model.Transition#getFromState()}.
     *
     * @return parent {@link ADStarNodeImpl},
     */
    @SuppressWarnings("unchecked") //suppress warnings to return an ADStarNode instead of Node, which is the inherited return type from parent
    @Override
    public ADStarNodeImpl<A, S, C> previousNode() {
        return (ADStarNodeImpl<A, S, C>) previousNode;
    }

    /**
     * Compares ADSTarNode instances attending to their {@link ADStarNode.Key}
     * values.
     *
     * @param o ADStarNode instance
     * @return usual comparison value
     */
    @Override
    public int compareTo(ADStarNodeImpl<A, S, C> o) {
        return this.key.compareTo(o.key);
    }

}
