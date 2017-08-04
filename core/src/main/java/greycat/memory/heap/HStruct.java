package greycat.memory.heap;

import greycat.Helper;
import greycat.memory.Chunk;
import greycat.memory.Struct;
import greycat.memory.Tuple;

import java.util.HashMap;
import java.util.Map;

public class HStruct implements Struct {

    private final Chunk _chunk;
    private final Map<Integer, Tuple<Integer, Object>> backend = new HashMap<Integer, Tuple<Integer, Object>>();

    public HStruct(Chunk chunk) {
        _chunk = chunk;
    }

    @Override
    public Chunk chunk() {
        return _chunk;
    }

    @Override
    public Object get(String name) {
        return getAt(Helper.hash(name));
    }

    @Override
    public Object getAt(int index) {
        Tuple<Integer, Object> result = backend.get(index);
        if (result != null) {
            return result.right();
        }
        return null;
    }

    @Override
    public int type(String name) {
        return typeAt(Helper.hash(name));
    }

    @Override
    public int typeAt(int index) {
        Tuple<Integer, Object> result = backend.get(index);
        if (result != null) {
            return result.left();
        }
        return -1;
    }

    @Override
    public Struct set(String name, int type, Object value) {
        return setAt(Helper.hash(name), type, value);
    }

    @Override
    public Struct setAt(int index, int type, Object value) {
        _chunk.setDirty();
        backend.put(index, new Tuple<Integer, Object>(type, value));
        return this;
    }

    @Override
    public Struct remove(String name) {
        return removeAt(Helper.hash(name));
    }

    @Override
    public Struct removeAt(int index) {
        _chunk.setDirty();
        backend.remove(index);
        return this;
    }

    @Override
    public Object getOrCreate(String name, int type) {
        return null;
    }

    @Override
    public Object getOrCreateAt(int index, int type) {
        return null;
    }

    @Override
    public Object getOrCreateCustom(String name, String typeName) {
        return null;
    }

    @Override
    public Object getOrCreateCustomAt(int index, String typeName) {
        return null;
    }

    @Override
    public <A> A getWithDefault(String key, A defaultValue) {
        return null;
    }

    @Override
    public <A> A getAtWithDefault(int key, A defaultValue) {
        return null;
    }

    @Override
    public int[] attributeIndexes() {
        //TODO
        return new int[0];
    }

}
