/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.excel;

import greycat.Action;
import greycat.Graph;
import greycat.Type;
import greycat.plugin.ActionFactory;
import greycat.plugin.Plugin;

import java.io.File;

/**
 * Created by gnain on 27/02/17.
 */
public class ExcelPlugin implements Plugin {

    private String _basePath;

    /**
     * Excel plugin constructor
     *
     * @param basePath Base path from which files can be reached
     */
    public ExcelPlugin(String basePath) {
        _basePath = basePath;
        if (!_basePath.endsWith(File.separator)) {
            _basePath += File.separator;
        }
    }

    @Override
    public void start(Graph graph) {
        graph.actionRegistry().getOrCreateDeclaration(ExcelActions.LOAD_XSLX).setParams(Type.STRING, Type.STRING, Type.STRING).setFactory(new ActionFactory() {
            @Override
            public Action create(Object[] params) {
                return new ActionLoadXlsx(_basePath, String.valueOf(params[0]), String.valueOf(params[1]), String.valueOf(params[2]));
            }
        });
    }

    @Override
    public void stop() {

    }
}
