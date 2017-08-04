package greycat.base;

import greycat.Node;
import greycat.memory.Chunk;
import greycat.memory.Struct;

public class GenericNode implements Node {

    private final Struct content;

    public GenericNode(final Struct content) {
        this.content = content;
    }

    @Override
    public final Chunk chunk() {
        return this.content.chunk();
    }

    @Override
    public final Object get(final String name) {
        return this.content.get(name);
    }

    @Override
    public final Object getAt(int index) {
        return this.content.getAt(index);
    }

    @Override
    public final int type(String name) {
        return this.content.type(name);
    }

    @Override
    public final int typeAt(int index) {
        return this.content.typeAt(index);
    }

    @Override
    public final Struct set(String name, int type, Object value) {
        return this.content.set(name, type, value);
    }

    @Override
    public final Struct setAt(int index, int type, Object value) {
        return this.content.setAt(index, type, value);
    }

    @Override
    public final Struct remove(String name) {
        return this.content.remove(name);
    }

    @Override
    public final Struct removeAt(int index) {
        return this.content.removeAt(index);
    }

    @Override
    public final Object getOrCreate(String name, int type) {
        return this.content.getOrCreate(name, type);
    }

    @Override
    public final Object getOrCreateAt(int index, int type) {
        return this.content.getOrCreateAt(index, type);
    }

    @Override
    public final Object getOrCreateCustom(String name, String typeName) {
        return this.content.getOrCreateCustom(name, typeName);
    }

    @Override
    public final Object getOrCreateCustomAt(int index, String typeName) {
        return this.content.getOrCreateCustomAt(index, typeName);
    }

    @Override
    public final <A> A getWithDefault(String key, A defaultValue) {
        return this.content.getWithDefault(key, defaultValue);
    }

    @Override
    public final <A> A getAtWithDefault(int key, A defaultValue) {
        return this.content.getAtWithDefault(key, defaultValue);
    }

    @Override
    public final Integer[] attributes() {
        return this.content.attributes();
    }

    @Override
    public final String toString() {
        return this.content.toString();
    }

}
