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
package greycatTest.utility;

import greycat.Type;
import greycat.base.BaseCustomTypeSingle;
import greycat.struct.EStructArray;
import greycat.utility.HashHelper;

public class GPSPosition extends BaseCustomTypeSingle {

    private static final String LAT = "lat";
    private static final int LAT_H = HashHelper.hash(LAT);
    private static final String LNG = "lng";
    private static final int LNG_H = HashHelper.hash(LNG);

    public GPSPosition(EStructArray p_backend) {
        super(p_backend);
    }

    @Override
    public void init() {
        setAt(LAT_H, Type.DOUBLE, 1.5d).setAt(LNG_H, Type.DOUBLE, 1.5d);
    }

    public final double lat() {
        return (double) getAt(LAT_H);
    }

    public final double lng() {
        return (double) getAt(LNG_H);
    }

    public final void setPosition(final double lat, final double lng) {
        this._backend.estruct(DEF_NODE).setAt(LAT_H, Type.DOUBLE, lat).setAt(LNG_H, Type.DOUBLE, lng);
    }

    @Override
    public String toString() {
        return "position(" + lat() + "," + lng() + ")";
    }

}
