/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.excel;


import greycat.Action;
import greycat.Graph;
import greycat.Type;
import greycat.plugin.ActionFactory;
import greycat.plugin.Plugin;

/**
 * Created by gnain on 27/02/17.
 */
public class ExcelPlugin implements Plugin {

    @Override
    public void start(Graph graph) {
        graph.actionRegistry().declaration(ExcelActions.LOAD_XSLX).setParams(Type.STRING).setFactory(new ActionFactory() {
            @Override
            public Action create(Object[] params) {
                return new ActionLoadXlsx(String.valueOf(params[0]));
            }
        });
    }

    @Override
    public void stop() {

    }
}
