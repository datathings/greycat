package greycat.language;

import java.util.LinkedList;
import java.util.List;

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
public class Key {
    private final String name;
    private final List<Attribute> attributes;

    private boolean withTime;


    public Key(String name) {
        this.name = name;
        this.attributes = new LinkedList<>();
        this.withTime = false;
    }

    public Attribute[] attributes() {
        return this.attributes.toArray(new Attribute[this.attributes.size()]);
    }


    public void addAttribute(Attribute attribute) {
        this.attributes.add(attribute);
    }

    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Key)) return false;
        final Key that = (Key) other;
        return this.name().equals(that.name());
    }

    @Override
    public int hashCode() {
        return this.name().hashCode();
    }

    public String name() {
        return this.name;
    }

    public boolean isWithTime() {
        return this.withTime;
    }

    public void setWithTime(boolean withTime) {
        this.withTime = withTime;
    }
}
