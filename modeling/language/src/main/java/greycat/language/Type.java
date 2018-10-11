/**
 * Copyright 2017-2018 The GreyCat Authors.  All rights reserved.
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
import java.util.stream.Collectors;

public abstract class Type {
    protected String name;

    protected Type parent;
    public Map<String, Object> properties;

    Attribute getOrCreateAttribute(String name) {
        Object att = properties.get(name);
        if (att == null) {
            att = new Attribute(name, this);
            properties.put(name, att);
        } else if (!(att instanceof Attribute)) {
            throw new RuntimeException("Property name conflict attribute name conflict with " + att);
        }
        return (Attribute) att;
    }

    Annotation getOrCreateAnnotation(String name) {
        Object annotation = properties.get(name);
        if (annotation == null) {
            annotation = new Annotation(name);
            properties.put(name, annotation);
        } else if (!(annotation instanceof Annotation)) {
            throw new RuntimeException("Property name conflict annotation name conflict with " + annotation);
        }
        return (Annotation) annotation;
    }

    Constant getOrCreateConstant(String name) {
        Object att = properties.get(name);
        if (att == null) {
            att = new Constant(name);
            properties.put(name, att);
        } else if (!(att instanceof Constant)) {
            throw new RuntimeException("Property name conflict constant name conflict with " + att);
        }
        return (Constant) att;
    }

    Attribute attributeFromParent(String name) {
        Object found;
        Type loop_parent = parent;
        while (loop_parent != null) {
            found = loop_parent.properties.get(name);
            if (found != null) {
                if (found instanceof Attribute) {
                    return (Attribute) found;
                } else {
                    throw new RuntimeException("Inconsistency error in " + this.name + " -> " + found + " already present in parents with another type in " + this.name);
                }
            }
            loop_parent = loop_parent.parent;
        }
        return null;
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

    void setParent(Type parent) {
        //check parent cycle
        Type loop_parent = parent;
        while (loop_parent != null) {
            if (loop_parent == this) {
                throw new RuntimeException("Inheritance cycle " + parent + " and " + this);
            }
            loop_parent = loop_parent.parent;
        }
        this.parent = parent;
    }

    public final Type parent() {
        return parent;
    }

    public final String name() {
        return name;
    }

    public final Collection<Object> properties() {
        return properties.values();
    }

    public final Map<String, Object> allProperties() {
        final Map<String, Object> allProperties = new HashMap<String, Object>();
        allProperties.putAll(properties);
        Type loop_parent = parent;
        while (loop_parent != null) {
            loop_parent.properties.forEach((s, o) -> {
                if (!allProperties.containsKey(s)) {
                    allProperties.put(s, o);
                }
            });
            loop_parent = loop_parent.parent;
        }
        return allProperties;
    }


}
