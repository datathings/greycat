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
import java.util.List;

public class Attribute {

    private final String name;
    private final Container parent;
    final List<AttributeRef> references = new ArrayList<AttributeRef>();

    private String type;

    Attribute(final String name, Container parent) {
        this.name = name;
        this.parent = parent;
    }

    public final String name() {
        return name;
    }

    public final String type() {
        return type;
    }

    public final Container parent() {
        return parent;
    }

    final void setType(String type) {
        this.type = type;
    }

}
