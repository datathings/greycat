/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.excel;

import greycat.Action;
import greycat.Graph;
import greycat.Type;
import greycat.plugin.ActionFactory;
import greycat.plugin.Plugin;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by gnain on 27/02/17.
 */
public class ExcelPlugin implements Plugin {

    private Path _basePath;
    /**
     * Excel plugin constructor
     * @param basePath Base path from which files can be reached
     */
    public ExcelPlugin(String basePath) {
        _basePath = Paths.get(basePath);
    }

    @Override
    public void start(Graph graph) {
        graph.actionRegistry().declaration(ExcelActions.LOAD_XSLX).setParams(Type.STRING).setFactory(new ActionFactory() {
            @Override
            public Action create(Object[] params) {
                Path resolved = _basePath.resolve(String.valueOf(params[0]));
                if(resolved.toFile().exists()) {
                    return new ActionLoadXlsx(resolved.toString());
                } else {
                    return new ActionLoadXlsx(String.valueOf(params[0]));
                }

            }
        });
    }

    @Override
    public void stop() {

    }
}
