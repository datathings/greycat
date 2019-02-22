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
package greycatTest.internal.task;

import greycat.internal.heap.HeapBuffer;
import greycat.internal.task.CoreProgressReport;
import greycat.struct.Buffer;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Gregory NAIN on 14/04/17.
 */
public class CoreProgressReportTest {

    @Test
    public void saveLoadTest() {

        CoreProgressReport report = new CoreProgressReport().setActionPath("1.2.3").setSumPath("3.3.3").setProgress(-1).setComment("Yaha");
        Buffer b = new HeapBuffer();
        report.saveToBuffer(b);
        CoreProgressReport reportBack = new CoreProgressReport();
        reportBack.loadFromBuffer(b);

        Assert.assertEquals(report.actionPath(), reportBack.actionPath());
        Assert.assertTrue(report.progress() == reportBack.progress());
        Assert.assertEquals(report.comment(), reportBack.comment());

        report = new CoreProgressReport().setActionPath("1.2.3").setSumPath("3.3.3").setProgress(0.64).setComment("Yehe");
        b = new HeapBuffer();
        report.saveToBuffer(b);
        reportBack = new CoreProgressReport();
        reportBack.loadFromBuffer(b);

        Assert.assertEquals(report.actionPath(), reportBack.actionPath());
        Assert.assertTrue(report.progress() == reportBack.progress());
        Assert.assertEquals(report.comment(), reportBack.comment());

    }



}
