package org.mwg.core.task;

import org.mwg.Callback;
import org.mwg.Constants;
import org.mwg.Node;
import org.mwg.task.TaskAction;
import org.mwg.task.TaskContext;

class ActionFromIndex implements TaskAction {

    private final String _indexName;

    private final String _query;

    ActionFromIndex(final String p_indexName, final String p_query) {
        _indexName = p_indexName;
        _query = p_query;
    }

    @Override
    public void eval(final TaskContext context) {
        context.graph().find(context.world(), context.time(), _indexName, _query, new Callback<Node[]>() {
            @Override
            public void on(Node[] result) {
                context.setResult(result);
            }
        });
    }

    @Override
    public String toString() {
        return "fromIndex(\'" + _indexName + "\'" + Constants.QUERY_SEP + "\'" + _query + "\')";
    }

}
