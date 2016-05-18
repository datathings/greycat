package lu.snt.vldb;

import org.kevoree.modeling.KCallback;
import org.kevoree.modeling.KModel;
import org.kevoree.modeling.KObject;
import org.kevoree.modeling.cdn.impl.MemoryContentDeliveryDriver;
import org.kevoree.modeling.defer.KCounterDefer;
import org.kevoree.modeling.memory.manager.DataManagerBuilder;
import org.kevoree.modeling.memory.manager.internal.KInternalDataManager;
import org.kevoree.modeling.memory.space.impl.ManualChunkSpaceManager;
import org.kevoree.modeling.memory.space.impl.press.PressHeapChunkSpace;
import org.kevoree.modeling.meta.KMetaClass;
import org.kevoree.modeling.meta.KPrimitiveTypes;
import org.kevoree.modeling.meta.impl.MetaModel;
import org.kevoree.modeling.plugin.LevelDBPlugin;
import org.kevoree.modeling.plugin.RocksDBPlugin;
import org.kevoree.modeling.scheduler.impl.BlockingAsyncScheduler;
import org.kevoree.modeling.scheduler.impl.DirectScheduler;
import org.kevoree.modeling.scheduler.impl.ExecutorServiceScheduler;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class Minimal {

    public static void main(String[] arg) throws IOException {

        try {
            final int valuesToInsert = 10000000;
            final long timeOrigin = 1000;
            MetaModel dynamicMetaModel = new MetaModel("MyMetaModel");
            final KMetaClass sensorMetaClass = dynamicMetaModel.addMetaClass("Sensor");
            sensorMetaClass.addAttribute("value", KPrimitiveTypes.DOUBLE);
            int threads = Runtime.getRuntime().availableProcessors();
            System.out.println("Number of threads: " + threads);
            final KModel model = dynamicMetaModel.createModel(
                    DataManagerBuilder.create()
                            .withSpace(new PressHeapChunkSpace(100000, 10))
                            //.withScheduler(new ExecutorServiceScheduler())
                            //.withScheduler(new BlockingAsyncScheduler().workers(threads))
                            .withScheduler(new DirectScheduler())
                            .withContentDeliveryDriver(new RocksDBPlugin("/Users/duke/Documents/dev/sandbox/VLDBKmf/out"))
                            .withSpaceManager(new ManualChunkSpaceManager())
                            .build());
            //  final KModel model= dynamicMetaModel.createModel(DataManagerBuilder.create().withScheduler(new DirectScheduler()).build());
            //final MemoryContentDeliveryDriver castedCDN = (MemoryContentDeliveryDriver) ((KInternalDataManager) model.manager()).cdn();
            // MemoryContentDeliveryDriver.DEBUG = true;

            final long before = System.currentTimeMillis();

            model.connect(new KCallback() {
                @Override
                public void on(Object o) {

                    KObject object = model.universe(0).time(timeOrigin).create(sensorMetaClass);
                    object.destroy();
                    long uuid = object.uuid();

                    final KCounterDefer counter = model.counterDefer(valuesToInsert);
                    for (long i = 0; i < valuesToInsert; i++) {

                        if (i % 1000 == 0) {
                            object = model.universe(0).time(timeOrigin).create(sensorMetaClass);
                            uuid = object.uuid();
                            object.destroy();
                        }

                        if (i % 1000000 == 0) {
                            System.out.println(">" + i + " " + (System.currentTimeMillis() - before) / 1000 + "s");
                        }

                        final double value = i * 0.3;
                        final long time = timeOrigin + i;
                        model.lookup(0, time, uuid, new KCallback<KObject>() {
                            @Override
                            public void on(KObject kObject) {
                                kObject.set(kObject.metaClass().attribute("value"), value);

                                //  System.out.println(time);

                                counter.countDown();
                                //at.incrementAndGet();
                                //kill the object
                                kObject.destroy();
                            }
                        });


                    }

                    counter.then(new KCallback() {
                        @Override
                        public void on(Object o) {
                            // object.destroy();


                            model.disconnect(new KCallback() {
                                @Override
                                public void on(Object o) {
                                    //KInternalDataManager manager = (KInternalDataManager) model.manager();
                                    // manager.space().printDebug(model.metaModel());
                                    //System.out.println(">CDN_SIZE:" + castedCDN.size());

                                    System.out.println("end>" + " " + (System.currentTimeMillis() - before) / 1000 + "s");


                                }
                            });

                        }
                    });

                }
            });

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


}
