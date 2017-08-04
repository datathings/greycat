package greycat.memory;

public interface Struct {

    Chunk chunk();

    Object get(String name);

    /**
     * Returns the value of an attribute of the container.
     *
     * @param index index of attribute.
     * @return The value of the required attribute in this container for the current timepoint and world.
     * The type of the returned object (i.e. of the attribute) is given by {@link #type(String)}
     * (typed by one of the Type)
     */
    Object getAt(int index);

    /**
     * Returns the type of an attribute. The returned value is one of {@link greycat.Type}.
     *
     * @param name The name of the attribute for which the type is asked.
     * @return The type of the attribute inform of an int belonging to {@link greycat.Type}.
     */
    int type(String name);

    int typeAt(int index);

    /**
     * Sets the value of an attribute of this container (for its current world and time for Node container).<br>
     *
     * @param name  Must be unique per node.
     * @param type  Must be one of {@link greycat.Type} int value.
     * @param value Must be consistent with the propertyType.
     * @return The node for fluent API.
     */
    Struct set(String name, int type, Object value);

    /**
     * Sets the value of an attribute of this container (for its current world and time for Node container).<br>
     *
     * @param index Must be unique per node.
     * @param type  Must be one of {@link greycat.Type} int value.
     * @param value Must be consistent with the propertyType.
     * @return The node for fluent API.
     */
    Struct setAt(int index, int type, Object value);

    /**
     * Removes an attribute from the container.
     *
     * @param name The name of the attribute to remove.
     * @return The node for fluent API.
     */
    Struct remove(String name);

    Struct removeAt(int index);

    /**
     * Gets or creates atomically a complex mutable attribute (e.g. Maps).<br>
     *
     * @param name The name of the object to create. Must be unique per node.
     * @param type The type of the attribute. Must be one of {@link greycat.Type} int value.
     * @return An instance that can be altered at the current world and time.
     */
    Object getOrCreate(String name, int type);

    /**
     * Gets or creates atomically a complex mutable attribute (e.g. Maps).<br>
     *
     * @param index The name of the object to create. Must be unique per node.
     * @param type  The type of the attribute. Must be one of {@link greycat.Type} int value.
     * @return An instance that can be altered at the current world and time.
     */
    Object getOrCreateAt(int index, int type);

    Object getOrCreateCustom(String name, String typeName);

    Object getOrCreateCustomAt(int index, String typeName);

    <A> A getWithDefault(String key, A defaultValue);

    <A> A getAtWithDefault(int key, A defaultValue);
    
    int[] attributeIndexes();

}
