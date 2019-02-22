/**
 * Copyright 2017-2019 The GreyCat Authors.  All rights reserved.
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
import java.util.List;

public class Attribute {

    private final String name;
    private final Type parent;
    final List<AttributeRef> references = new ArrayList<AttributeRef>();
    private List<List<Object>> value;

    private String type;

    Attribute(final String name, Type parent) {
        this.name = name;
        this.parent = parent;
    }

    public final String name() {
        return name;
    }

    public final String type() {
        return type;
    }

    public final Type parent() {
        return parent;
    }

    final void setType(String type) {
        this.type = type;
    }

    public final void setValue(List<List<Object>> value) {
        this.value = value;
    }

    public final List<List<Object>> value() {
        return this.value;
    }

}
