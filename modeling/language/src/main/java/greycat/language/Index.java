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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

public class Index implements Edge {

    private final String name;
    private final Collection<AttributeRef> attributes;
    private Opposite opposite;

    private String type;

    public Index(String name) {
        this.name = name;
        this.attributes = new ArrayList<AttributeRef>();
    }

    @Override
    public String name() {
        return this.name;
    }

    public String type() {
        return this.type;
    }

    @Override
    public Opposite opposite() {
        return opposite;
    }

    @Override
    public void setOpposite(Opposite opposite) {
        this.opposite = opposite;
    }

    public Collection<AttributeRef> attributes() {
        return this.attributes;
    }

    void addAttributeRef(AttributeRef attribute) {
        this.attributes.add(attribute);
    }

    void setType(String type) {
        this.type = type;
    }

}
