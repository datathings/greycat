package greycat.memory;

public interface ChunkStorage {

    String get(String key);

    void put(String key, String value);

}
