package greycat.core;

import greycat.memory.Chunk;
import greycat.memory.Struct;

public class StructProxy implements Struct {

    Chunk target;
    final long world;
    final long time;
    final long id;

    public StructProxy(long id, long time, long world) {
        this.world = world;
        this.time = time;
        this.id = id;
    }

    Struct get() {
        //TODO refresh
        return target.payload();
    }

    @Override
    public Chunk chunk() {
        //TODO check synch
        return target;
    }

    @Override
    public final Object get(String name) {
        return chunk().payload().get(name);
    }

    @Override
    public final Object getAt(int index) {
        return chunk().payload().getAt(index);
    }

    @Override
    public final int type(String name) {
        return 0;
    }

    @Override
    public int typeAt(int index) {
        return 0;
    }

    @Override
    public Struct set(String name, int type, Object value) {
        return null;
    }

    @Override
    public Struct setAt(int index, int type, Object value) {
        return null;
    }

    @Override
    public Struct remove(String name) {
        return null;
    }

    @Override
    public Struct removeAt(int index) {
        return null;
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
    public Integer[] attributes() {
        return new Integer[0];
    }
}
