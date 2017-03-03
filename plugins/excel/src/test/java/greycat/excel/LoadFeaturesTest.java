/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.excel;

import greycat.*;
import org.junit.Test;

import java.net.URL;

/**
 * Created by gnain on 27/02/17.
 */
public class LoadFeaturesTest {


    @Test
    public void loadFeaturesFromMeta() {
        loadFeatues("testfile.xlsx");
    }

    @Test
    public void loadFeaturesFromSheets() {
        loadFeatues("testfile2.xlsx");
    }

    private void loadFeatues(String file) {
        Graph graph = EnterpriseGraphBuilder.newBuilder().withMemorySize(1000000).withPlugin(new ExcelPlugin()).build();
        graph.connect(connected->{

            URL fileUrl = getClass().getClassLoader().getResource(file);
            System.out.println("File:" + fileUrl.getFile());


            Task task = Tasks.newTask()
                    .createNode()
                    .setAttribute("name", Type.STRING, "{{file}}")
                    .action(ExcelActions.LOAD_XSLX, "{{file}}");

            TaskContext context = task.prepare(graph, null, taskResult -> {
                if(taskResult.exception() != null) {
                    System.err.println("An exception occurred while processing task.");
                    taskResult.exception().printStackTrace();
                }

                if(taskResult.output() != null) {
                    System.out.println(taskResult.output());
                }
                System.out.println("========== RESULT ============");
                for(int i = 0; i < taskResult.size(); i++) {
                    System.out.println(taskResult.get(i));
                }

            });
            context.setVariable("file", fileUrl.toString());
            task.executeUsing(context);
        });
    }

}
