package greycat.memory;

public interface Array<T> extends StructProp {

    Array<T> set(int offset, T elem);

    T get(int offset);

    /**
     * @return the complete size of the array o(1)
     */
    int size();

    /**
     * Empty the array from all its element.
     */
    void clear();

    /**
     * Initialize the capacity of the array, enabling later usage of set method for instance.
     *
     * @param size desired capacity
     */
    void allocate(int size);

}
