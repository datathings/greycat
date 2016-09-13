package org.mwg.importer.util;

import com.eclipsesource.json.JsonObject;
import org.mwg.task.TaskResult;
import org.mwg.task.TaskResultIterator;

public class JsonMemberResult implements TaskResult<Object> {

    private final JsonObject.Member _member;

    public JsonMemberResult(JsonObject.Member member) {
        _member = member;
    }

    @Override
    public TaskResultIterator iterator() {
        return new TaskResultIterator() {
            private int currentIndex = 0;

            @Override
            public Object next() {
                Object result = null;
                if (currentIndex == 0) {
                    result = _member.getName();
                }
                if (currentIndex == 1) {
                    result = JsonValueResultBuilder.build(_member.getValue());
                }
                currentIndex++;
                return result;
            }
        };
    }

    @Override
    public Object get(int index) {
        if (index == 0) {
            return _member.getName();
        } else {
            return JsonValueResultBuilder.build(_member.getValue());
        }
    }

    @Override
    public void set(int index, Object input) {

    }

    @Override
    public void allocate(int index) {
    }

    @Override
    public void add(Object input) {
    }

    @Override
    public void clear() {
    }

    @Override
    public TaskResult<Object> clone() {
        return this;
    }

    @Override
    public void free() {

    }

    @Override
    public int size() {
        return 2;
    }

    @Override
    public Object[] asArray() {
        return new Object[]{_member.getName(), _member.getValue()};
    }

    @Override
    public String toString() {
        return "<" + _member.getName() + "->" + _member.getValue() + ">";
    }
}