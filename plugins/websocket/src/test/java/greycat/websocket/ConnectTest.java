package greycat.websocket;

import greycat.Callback;
import greycat.Graph;
import greycat.GraphBuilder;
import greycat.websocket.WSClient;

/**
 * Created by Gregory NAIN on 2019-06-14.
 */
public class ConnectTest {


    public static void main(String[] args) {


        GraphBuilder builder = GraphBuilder
                .newBuilder()
                .withMemorySize(1000000)
                .withStorage(new WSClient("wss://rainsat.datathings.com/ws", "rainsat", "rainsat2019"));

        Graph graph = builder.build();

        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean aBoolean) {

                System.out.println();


            }
        });

    }


}
