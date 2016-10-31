package org.mwg.ml.neuralnet;

import org.mwg.Callback;
import org.mwg.Graph;
import org.mwg.Type;
import org.mwg.plugin.AbstractNode;
import org.mwg.plugin.NodeState;
import org.mwg.struct.LongLongMap;
import org.mwg.struct.Relationship;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by assaad on 20/09/16.
 */
public class NeuralNode extends AbstractNode {
    public static String NAME = "NeuralNode";

    public static String INPUTS = "inputs"; //Input relationships
    public static String INPUTS_MAP = "inputs_map"; //order of the relationships

    public static String OUTPUTS = "outputs"; //output relationships
    public static String OUTPUTS_MAP = "outputs_map"; //order of the relationships

    private static String WEIGHTS = "weights"; //weights of the network
    private static String NODE_TYPE = "node_type";

    public static String LEARNINGRATE="learning";
    public static final double LEARNINGRATE_DEF = 0.01;

    public NeuralNode(long p_world, long p_time, long p_id, Graph p_graph) {
        super(p_world, p_time, p_id, p_graph);
    }


    public NeuralNode configure(int inputs, int outputs, int hiddenlayers, int nodesPerLayer) {
        ArrayList<NeuralNode> internalNodes = new ArrayList<NeuralNode>();//inputs + outputs + hiddenlayers * nodesPerLayer + 1
        internalNodes.add(this);

        ArrayList<NeuralNode> previousLayer = new ArrayList<NeuralNode>();

        NodeState state =phasedState();
        state.setFromKey(NODE_TYPE,Type.INT,NeuralNodeType.ROOT);

        //create input layers:
        for (int i = 0; i < inputs; i++) {
            NeuralNode inputNode = createNewNode(NeuralNodeType.INPUT);
            this.forwardConnect(inputNode);
            internalNodes.add(inputNode);
            previousLayer.add(inputNode);
        }

        ArrayList<NeuralNode> nextLayer = new ArrayList<NeuralNode>();


        //Create hidden layers
        for (int i = 0; i < hiddenlayers; i++) {
            for (int j = 0; j < nodesPerLayer; j++) {
                NeuralNode hidden = createNewNode(NeuralNodeType.HIDDEN);
                nextLayer.add(hidden);
                internalNodes.add(hidden);

                for (int k = 0; k < previousLayer.size(); k++) {
                    previousLayer.get(k).forwardConnect(hidden);
                }
            }

            previousLayer = nextLayer;
            nextLayer = new ArrayList<NeuralNode>();
        }

        //Create output layers
        for (int i = 0; i < outputs; i++) {
            NeuralNode output = createNewNode(NeuralNodeType.OUTPUT);
            for (int k = 0; k < previousLayer.size(); k++) {
                previousLayer.get(k).forwardConnect(output);
            }
            output.forwardConnect(this);
            internalNodes.add(output);
        }

        for (int i = 0; i < internalNodes.size(); i++) {
            if(internalNodes.get(i).id()!=this.id()) {
                internalNodes.get(i).initWeightsRadomly(0.1);
                internalNodes.get(i).free();
            }
        }
        return this;
    }

    private static Random random=new Random();

    private void initWeightsRadomly(double maxValue){
        NodeState state =phasedState();
        int type = state.getFromKeyWithDefault(NODE_TYPE, NeuralNodeType.HIDDEN);
        if(type== NeuralNodeType.HIDDEN|| type==NeuralNodeType.OUTPUT){
            Relationship input= (Relationship) state.getFromKey(INPUTS);
            double[] weights = new double[input.size()+1];
            for(int i=0;i<weights.length;i++){
                weights[i]= random.nextDouble()*maxValue;
            }
            state.setFromKey(WEIGHTS,Type.DOUBLE_ARRAY,weights);
        }
    }

    private void forwardConnect(NeuralNode inputNode) {
        Relationship outputs = getOrCreateRel(OUTPUTS);
        outputs.add(inputNode.id());
        int pos = outputs.size() - 1;
        LongLongMap map = (LongLongMap) getOrCreate(OUTPUTS_MAP, Type.LONG_TO_LONG_MAP);
        map.put(inputNode.id(), pos);


        Relationship inputs = inputNode.getOrCreateRel(INPUTS);
        inputs.add(this.id());
        int posint = inputs.size() - 1;
        LongLongMap mapin = (LongLongMap) inputNode.getOrCreate(INPUTS_MAP, Type.LONG_TO_LONG_MAP);
        mapin.put(id(),posint);
    }

    private NeuralNode createNewNode(int neuralNodeType) {
        NeuralNode temp = (NeuralNode) graph().newTypedNode(world(), time(), NAME);
        temp.setProperty(NODE_TYPE, Type.INT, neuralNodeType);
        return temp;
    }


    // todo to be replaced in more generic way after
    private static double integrationFct(double[] values, double[] weights) {
        double value = 0;

        for (int i = 0; i < values.length; i++) {
            value += weights[i] * values[i];
        }
        //Add bias
        value +=weights[values.length];
        return value;
    }

    private static double activationFunction(double x, int type) {
        if (type == NeuralNodeType.HIDDEN) {
            return 1 / (1 + Math.exp(-x)); //Sigmoid by default, todo to be changed later to a generic activation
        } else {
            return x;
        }
    }

    private static double derivateActivationFunction(double fctVal, double x, int type) {
        if (type == NeuralNodeType.HIDDEN) {
            return fctVal * (1 - fctVal);
        } else {
            return 1;
        }
        // return fctVal * (1 - fctVal);

    }

    private static double calculateErr(double calculated, double target) {
        return (target - calculated) * (target - calculated) / 2;
    }

    private static double calculateDerivativeErr(double calculated, double target) {
        return -(target - calculated);
    }



    private static long msgIdCounter = -1;
    private long generateMsgId() {
        msgIdCounter++;
        return msgIdCounter;
    }






    public void predict(final double[] inputs, final Callback<double[]> callback){



        double[] prediction =null;
        callback.on(prediction);
    }

    public void learn(final double[] inputs, final double[] outputs, final Callback<double[]> callback) {
        final NodeState state= phasedState();
        //forward propagate
        predict(inputs, new Callback<double[]>() {
            @Override
            public void on(double[] predictions) {
                final double[] errors=new double[outputs.length];
                final double[] derivatives= new double[outputs.length];

                for(int i=0;i<errors.length;i++){
                    errors[i]=calculateErr(predictions[i],outputs[i]);
                    derivatives[i]=calculateDerivativeErr(predictions[i],outputs[i]);
                }

                double learningRate= state.getFromKeyWithDefault(LEARNINGRATE,LEARNINGRATE_DEF);

                //back-propagate derivatives[i] to outputs and lunch back propagation

                if(callback!=null) {
                    callback.on(errors);
                }

            }
        });
    }




}
