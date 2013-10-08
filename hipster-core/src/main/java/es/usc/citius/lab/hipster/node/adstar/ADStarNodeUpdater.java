/*
 * Copyright 2013 Centro de Investigación en Tecnoloxías da Información (CITIUS).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package es.usc.citius.lab.hipster.node.adstar;

import es.usc.citius.lab.hipster.function.CostFunction;
import es.usc.citius.lab.hipster.function.HeuristicFunction;
import es.usc.citius.lab.hipster.function.ScalarFunction;
import es.usc.citius.lab.hipster.function.impl.CostOperator;
import es.usc.citius.lab.hipster.node.Transition;

import java.util.Map;
import java.util.Map.Entry;

/**
 * Implements the default updater for {@link ADStarNode} instances using
 * {@link Double} values.
 *
 * @author Adrián González Sieira <adrian.gonzalez@usc.es>
 * @version 1.0
 * @since 01-04-2013
 */
public class ADStarNodeUpdater<S, T extends Comparable<T>> {

    private final CostFunction<S, T> costFunction;
    private final HeuristicFunction<S, T> heuristicFunction;
    private final CostOperator<T> add;
    private final ScalarFunction<T> scale;
    private double epsilon;

    public ADStarNodeUpdater(CostFunction<S, T> costFunction,
                             HeuristicFunction<S, T> heuristicFunction, CostOperator<T> add,
                             ScalarFunction<T> scale, double epsilon) {
        this.costFunction = costFunction;
        this.heuristicFunction = heuristicFunction;
        this.epsilon = epsilon;
        this.add = add;
        this.scale = scale;
    }

    public boolean updateConsistent(ADStarNode<S, T> node,
                                    ADStarNode<S, T> parent, Transition<S> transition) {
        T accumulatedCost = this.add.apply(parent.getG(),
                this.costFunction.evaluate(transition)); // parent.getG().add(this.costFunction.evaluate(transition));
        if (node.g.compareTo(accumulatedCost) > 0) {
            node.setPreviousNode(parent);
            // node.previousNode = parent;
            node.g = accumulatedCost;
            node.setState(transition);
            // node.state = transition;
            node.key = new ADStarNode.Key<T>(node.g, node.v,
                    this.heuristicFunction.estimate(transition.to()),
                    this.epsilon, this.add, this.scale);
            return true;
        }
        return false;
    }

    public boolean updateInconsistent(ADStarNode<S, T> node,
                                      Map<Transition<S>, ADStarNode<S, T>> predecessorMap) {
        T minValue = this.add.getIdentityElem();
        ADStarNode<S, T> minParent = null;
        Transition<S> minTransition = null;
        for (Entry<Transition<S>, ADStarNode<S, T>> current : predecessorMap
                .entrySet()) {
            T value = this.add.apply(current.getValue().v, this.costFunction.evaluate(current.getKey()));
            //T value = current.getValue().v.add(this.costFunction.evaluate(current.getKey()));
            if (value.compareTo(minValue) < 0) {
                minValue = value;
                minParent = current.getValue();
                minTransition = current.getKey();
            }
        }
        node.setPreviousNode(minParent);
        // node.previousNode = minParent;
        node.g = minValue;
        node.setState(minTransition);
        // node.state = minTransition;
        node.key = new ADStarNode.Key<T>(node.g, node.v,
                this.heuristicFunction.estimate(minTransition.to()),
                this.epsilon, this.add, this.scale);
        return true;
    }

    public void setMaxV(ADStarNode<S, T> node) {
        node.setV(this.add.getMaxElem());
    }

    public void setEpsilon(double epsilon) {
        this.epsilon = epsilon;
    }
}
