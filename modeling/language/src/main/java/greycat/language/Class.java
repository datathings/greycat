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

public class Class extends Type {

    Class(String name) {
        this.name = name;
        this.properties = new HashMap<String, Object>();
    }


    Relation getOrCreateRelation(String name) {
        Object att = properties.get(name);
        if (att == null) {
            att = new Relation(name);
            properties.put(name, att);
        } else if (!(att instanceof Relation)) {
            throw new RuntimeException("Property name conflict relation name conflict with " + att);
        }
        return (Relation) att;
    }

    Reference getOrCreateReference(String name) {
        Object att = properties.get(name);
        if (att == null) {
            att = new Reference(name);
            properties.put(name, att);
        } else if (!(att instanceof Reference)) {
            throw new RuntimeException("Property name conflict relation name conflict with " + att);
        }
        return (Reference) att;
    }

    Index getOrCreateIndex(String name) {
        Object att = properties.get(name);
        if (att == null) {
            att = new Index(name);
            properties.put(name, att);
        } else if (!(att instanceof Index)) {
            throw new RuntimeException("Property name conflict index name conflict with " + att);
        }
        return (Index) att;
    }


    @Override
    public String toString() {
        return "Class(" + name + ")";
    }
}
