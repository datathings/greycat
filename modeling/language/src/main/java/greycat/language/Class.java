/**
 * Copyright 2017 The GreyCat Authors.  All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package greycat.language;


import java.util.*;

public class Class implements Classifier {
    private final String name;
    private final Map<String, Property> properties;
    private final List<Key> keys;

    private Class parent;

    public Class(String name) {
        this.name = name;
        this.properties = new HashMap<>();
        this.keys = new LinkedList<>();
    }

    public Property[] properties() {
        return properties.values().toArray(new Property[properties.size()]);
    }

    public Property getProperty(String name) {
        for (Property property : properties()) {
            if (property.name().equals(name)) {
                return property;
            }
        }
        return null;
    }

    public void addProperty(Property property) {
        properties.put(property.name(), property);
    }

    public Class parent() {
        return parent;
    }

    public void setParent(Class parent) {
        this.parent = parent;
    }

    public void addKey(Key index) {
        keys.add(index);
    }

    public Key[] keys() {
        return keys.toArray(new Key[keys.size()]);
    }


    @Override
    public String name() {
        return name;
    }
}
