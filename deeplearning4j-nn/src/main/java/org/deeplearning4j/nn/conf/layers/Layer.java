/*-
 *
 *  * Copyright 2015 Skymind,Inc.
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 */

package org.deeplearning4j.nn.conf.layers;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.deeplearning4j.nn.api.ParamInitializer;
import org.deeplearning4j.nn.api.layers.LayerConstraint;
import org.deeplearning4j.nn.conf.InputPreProcessor;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.misc.FrozenLayer;
import org.deeplearning4j.nn.conf.layers.objdetect.Yolo2OutputLayer;
import org.deeplearning4j.nn.conf.layers.variational.VariationalAutoencoder;
import org.deeplearning4j.nn.conf.memory.LayerMemoryReport;
import org.deeplearning4j.optimize.api.IterationListener;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.learning.config.IUpdater;
import org.nd4j.shade.jackson.annotation.JsonSubTypes;
import org.nd4j.shade.jackson.annotation.JsonTypeInfo;
import org.nd4j.shade.jackson.annotation.JsonTypeInfo.As;
import org.nd4j.shade.jackson.annotation.JsonTypeInfo.Id;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * A neural network layer.
 */
@JsonTypeInfo(use = Id.NAME, include = As.WRAPPER_OBJECT)
@JsonSubTypes(value = {@JsonSubTypes.Type(value = AutoEncoder.class, name = "autoEncoder"),
                @JsonSubTypes.Type(value = ConvolutionLayer.class, name = "convolution"),
                @JsonSubTypes.Type(value = Convolution1DLayer.class, name = "convolution1d"),
                @JsonSubTypes.Type(value = GravesLSTM.class, name = "gravesLSTM"),
                @JsonSubTypes.Type(value = LSTM.class, name = "LSTM"),
                @JsonSubTypes.Type(value = GravesBidirectionalLSTM.class, name = "gravesBidirectionalLSTM"),
                @JsonSubTypes.Type(value = OutputLayer.class, name = "output"),
                @JsonSubTypes.Type(value = RnnOutputLayer.class, name = "rnnoutput"),
                @JsonSubTypes.Type(value = LossLayer.class, name = "loss"),
                @JsonSubTypes.Type(value = RBM.class, name = "RBM"),
                @JsonSubTypes.Type(value = DenseLayer.class, name = "dense"),
                @JsonSubTypes.Type(value = SubsamplingLayer.class, name = "subsampling"),
                @JsonSubTypes.Type(value = Subsampling1DLayer.class, name = "subsampling1d"),
                @JsonSubTypes.Type(value = BatchNormalization.class, name = "batchNormalization"),
                @JsonSubTypes.Type(value = LocalResponseNormalization.class, name = "localResponseNormalization"),
                @JsonSubTypes.Type(value = EmbeddingLayer.class, name = "embedding"),
                @JsonSubTypes.Type(value = ActivationLayer.class, name = "activation"),
                @JsonSubTypes.Type(value = VariationalAutoencoder.class, name = "VariationalAutoencoder"),
                @JsonSubTypes.Type(value = DropoutLayer.class, name = "dropout"),
                @JsonSubTypes.Type(value = GlobalPoolingLayer.class, name = "GlobalPooling"),
                @JsonSubTypes.Type(value = ZeroPaddingLayer.class, name = "zeroPadding"),
                @JsonSubTypes.Type(value = ZeroPadding1DLayer.class, name = "zeroPadding1d"),
                @JsonSubTypes.Type(value = FrozenLayer.class, name = "FrozenLayer"),
                @JsonSubTypes.Type(value = Yolo2OutputLayer.class, name = "Yolo2OutputLayer")
})
@Data
@NoArgsConstructor
public abstract class Layer implements Serializable, Cloneable {
    protected String layerName;
    protected double dropOut;
    protected List<LayerConstraint> constraints;


    public Layer(Builder builder) {
        this.layerName = builder.layerName;
        this.dropOut = builder.dropOut;
        this.constraints = builder.constraints;
    }

    /**
     * Reset the learning related configs of the layer to default. When instantiated with a global neural network configuration
     * the parameters specified in the neural network configuration will be used.
     * For internal use with the transfer learning API. Users should not have to call this method directly.
     */
    public void resetLayerDefaultConfig() {
        //clear the learning related params for all layers in the origConf and set to defaults
        this.setDropOut(Double.NaN);
    }

    @Override
    public Layer clone() {
        try {
            return (Layer) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public abstract org.deeplearning4j.nn.api.Layer instantiate(NeuralNetConfiguration conf,
                    Collection<IterationListener> iterationListeners, int layerIndex, INDArray layerParamsView,
                    boolean initializeParams);

    public abstract ParamInitializer initializer();

    /**
     * For a given type of input to this layer, what is the type of the output?
     *
     * @param layerIndex Index of the layer
     * @param inputType Type of input for the layer
     * @return Type of output from the layer
     * @throws IllegalStateException if input type is invalid for this layer
     */
    public abstract InputType getOutputType(int layerIndex, InputType inputType);

    /**
     * Set the nIn value (number of inputs, or input depth for CNNs) based on the given input type
     *
     * @param inputType Input type for this layer
     * @param override  If false: only set the nIn value if it's not already set. If true: set it regardless of whether it's
     *                  already set or not.
     * @throws IllegalStateException if input type is invalid for this layer
     */
    public abstract void setNIn(InputType inputType, boolean override);


    /**
     * For the given type of input to this layer, what preprocessor (if any) is required?<br>
     * Returns null if no preprocessor is required, otherwise returns an appropriate {@link InputPreProcessor}
     * for this layer, such as a {@link org.deeplearning4j.nn.conf.preprocessor.CnnToFeedForwardPreProcessor}
     *
     * @param inputType InputType to this layer
     * @return Null if no preprocessor is required, otherwise the type of preprocessor necessary for this layer/input combination
     * @throws IllegalStateException if input type is invalid for this layer
     */
    public abstract InputPreProcessor getPreProcessorForInputType(InputType inputType);

    /**
     * Get the L1 coefficient for the given parameter.
     * Different parameters may have different L1 values, even for a single .l1(x) configuration.
     * For example, biases generally aren't L1 regularized, even if weights are
     *
     * @param paramName    Parameter name
     * @return L1 value for that parameter
     */
    public abstract double getL1ByParam(String paramName);

    /**
     * Get the L2 coefficient for the given parameter.
     * Different parameters may have different L2 values, even for a single .l2(x) configuration.
     * For example, biases generally aren't L1 regularized, even if weights are
     *
     * @param paramName    Parameter name
     * @return L2 value for that parameter
     */
    public abstract double getL2ByParam(String paramName);

    /**
     * Get the (initial) learning rate coefficient for the given parameter.
     * Different parameters may be configured to have different learning rates, though commonly all parameters will
     * have the same learning rate
     *
     * @param paramName    Parameter name
     * @return Initial learning rate value for that parameter
     */
    public abstract double getLearningRateByParam(String paramName);

    /**
     * Is the specified parameter a layerwise pretraining only parameter?<br>
     * For example, visible bias params in an autoencoder (or, decoder params in a variational autoencoder) aren't
     * used during supervised backprop.<br>
     * Layers (like DenseLayer, etc) with no pretrainable parameters will return false for all (valid) inputs.
     *
     * @param paramName Parameter name/key
     * @return True if the parameter is for layerwise pretraining only, false otherwise
     */
    public abstract boolean isPretrainParam(String paramName);

    /**
     * Get the updater for the given parameter. Typically the same updater will be used for all updaters, but this
     * is not necessarily the case
     *
     * @param paramName    Parameter name
     * @return             Updater for the parameter
     * @deprecated Use {@link #getIUpdaterByParam(String)}
     */
    @Deprecated
    public Updater getUpdaterByParam(String paramName) {
        throw new UnsupportedOperationException(
                        "Not supported: all layers with parameters should override this method");
    }

    /**
     * Get the updater for the given parameter. Typically the same updater will be used for all updaters, but this
     * is not necessarily the case
     *
     * @param paramName    Parameter name
     * @return             IUpdater for the parameter
     */
    public IUpdater getIUpdaterByParam(String paramName) {
        throw new UnsupportedOperationException(
                        "Not supported: all layers with parameters should override this method");
    }

    /**
     * This is a report of the estimated memory consumption for the given layer
     *
     * @param inputType Input type to the layer. Memory consumption is often a function of the input type
     * @return Memory report for the layer
     */
    public abstract LayerMemoryReport getMemoryReport(InputType inputType);

    @SuppressWarnings("unchecked")
    public abstract static class Builder<T extends Builder<T>> {
        protected String layerName = null;
        protected double dropOut = Double.NaN;
        protected List<LayerConstraint> constraints = null;

        /**
         * Layer name assigns layer string name.
         * Allows easier differentiation between layers.
         */
        public T name(String layerName) {
            this.layerName = layerName;
            return (T) this;
        }

        /**
         * Dropout probability. This is the probability of <it>retaining</it> each input activation value for a layer.
         * dropOut(x) will keep an input activation with probability x, and set to 0 with probability 1-x.<br>
         * dropOut(0.0) is a special value / special case - when set to 0.0., dropout is disabled (not applied). Note
         * that a dropout value of 1.0 is functionally equivalent to no dropout: i.e., 100% probability of retaining
         * each input activation.<br>
         * When useDropConnect(boolean) is set to true (false by default), this method sets the drop connect
         * probability instead.
         * <p>
         * Note 1: Dropout is applied at training time only - and is automatically not applied at test time
         * (for evaluation, etc)<br>
         * Note 2: This sets the probability per-layer. Care should be taken when setting lower values for
         * complex networks (too much information may be lost with aggressive (very low) dropout values).<br>
         * Note 3: Frequently, dropout is not applied to (or, has higher retain probability for) input (first layer)
         * layers. Dropout is also often not applied to output layers. This needs to be handled MANUALLY by the user
         * - set .dropout(0) on those layers when using global dropout setting.<br>
         * Note 4: Implementation detail (most users can ignore): DL4J uses inverted dropout, as described here:
         * <a href="http://cs231n.github.io/neural-networks-2/">http://cs231n.github.io/neural-networks-2/</a>
         * </p>
         *
         * @param inputRetainProbability Dropout probability (probability of retaining each input activation value for a layer)
         */
        public T dropOut(double inputRetainProbability) {
            this.dropOut = inputRetainProbability;
            return (T) this;
        }

        /**
         * Set constraints to be applied to this layer. Default: no constraints.<br>
         * Constraints can be used to enforce certain conditions (non-negativity of parameters, max-norm regularization,
         * etc). These constraints are applied at each iteration, after the parameters have been updated.
         *
         * @param constraints Constraints to apply to all layers
         */
        public T constraints(LayerConstraint... constraints) {
            return constraints(Arrays.asList(constraints));
        }

        /**
         * Set constraints to be applied to this layer. Default: no constraints.<br>
         * Constraints can be used to enforce certain conditions (non-negativity of parameters, max-norm regularization,
         * etc). These constraints are applied at each iteration, after the parameters have been updated.
         *
         * @param constraints Constraints to apply to all layers
         */
        public T constraints(List<LayerConstraint> constraints) {
            this.constraints = constraints;
            return (T) this;
        }

        public abstract <E extends Layer> E build();
    }
}
