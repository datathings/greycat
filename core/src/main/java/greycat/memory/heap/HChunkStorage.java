package greycat.memory.heap;

import greycat.memory.ChunkStorage;

import java.util.HashMap;
import java.util.Map;

public class HChunkStorage implements ChunkStorage {

    private final Map<String, String> backend = new HashMap<String, String>();

    @Override
    public String get(String key) {
        return backend.get(key);
    }

    @Override
    public void put(String key, String value) {
        backend.put(key, value);
    }

}
