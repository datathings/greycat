package lu.snt.vldb;

import org.kevoree.modeling.KCallback;
import org.kevoree.modeling.KModel;
import org.kevoree.modeling.KObject;
import org.kevoree.modeling.KUniverse;
import org.kevoree.modeling.defer.KCounterDefer;
import org.kevoree.modeling.memory.manager.DataManagerBuilder;
import org.kevoree.modeling.memory.space.impl.ManualChunkSpaceManager;
import org.kevoree.modeling.memory.space.impl.press.PressHeapChunkSpace;
import org.kevoree.modeling.meta.KMetaClass;
import org.kevoree.modeling.meta.KPrimitiveTypes;
import org.kevoree.modeling.meta.impl.MetaModel;
import org.kevoree.modeling.plugin.RocksDBPlugin;
import org.kevoree.modeling.scheduler.impl.DirectScheduler;

import java.io.IOException;

public class MinimalUni {

    public static void main(String[] arg) throws IOException {

        try {
            final int valuesToInsert = 20000;
            final int step = 100;
            final long timeOrigin = 1000;
            MetaModel dynamicMetaModel = new MetaModel("MyMetaModel");
            final KMetaClass sensorMetaClass = dynamicMetaModel.addMetaClass("Sensor");
            sensorMetaClass.addAttribute("value", KPrimitiveTypes.DOUBLE);
            int threads = Runtime.getRuntime().availableProcessors();
            System.out.println("Number of threads: " + threads);
            final KModel model = dynamicMetaModel.createModel(
                    DataManagerBuilder.create()
                            .withSpace(new PressHeapChunkSpace(100000, 10))
                            //.withScheduler(new AsyncScheduler().workers(threads))
                            .withScheduler(new DirectScheduler())
                            // .withContentDeliveryDriver(new RocksDBPlugin("/Users/assaad/work/github/data/rockdb"))
                            .withSpaceManager(new ManualChunkSpaceManager())
                            .build());
            //  final KModel model= dynamicMetaModel.createModel(DataManagerBuilder.create().withScheduler(new DirectScheduler()).build());
            //final MemoryContentDeliveryDriver castedCDN = (MemoryContentDeliveryDriver) ((KInternalDataManager) model.manager()).cdn();
            // MemoryContentDeliveryDriver.DEBUG = true;

            final long before = System.currentTimeMillis();
            final long[] times = new long[2];
            times[0]=1000;
            times[1]=1002;

            model.connect(new KCallback() {
                @Override
                public void on(Object o) {
                    KObject object = model.universe(0).time(timeOrigin).create(sensorMetaClass);
                    object.destroy();
                    long uuid = object.uuid();
                    long universe = 0;
                    final KCounterDefer counter = model.counterDefer(valuesToInsert);
                    for (long i = 0; i < valuesToInsert; i++) {
                    /*    if (i % step == 0) {
                            KUniverse uni = model.universe(universe).diverge();
                            universe = uni.key();
                        }
                        if (i % 1000000 == 0) {
                            System.out.println(">" + i + " " + (System.currentTimeMillis() - before) / 1000 + "s");
                        }*/
                        final double value = i * 0.3;
                      /*  model.lookup(universe, timeOrigin + i, uuid, new KCallback<KObject>() {
                            @Override
                            public void on(KObject kObject) {

                                if (kObject == null) {
                                    System.err.println("WTF");
                                }

                                kObject.set(kObject.metaClass().attribute("value"), value);
                                counter.countDown();
                                kObject.destroy();
                            }
                        });*/
                        model.lookupAllTimes(universe, times, uuid, new KCallback<KObject[]>() {
                            @Override
                            public void on(KObject[] kObjects) {
                                for (int k = 0; k < kObjects.length; k++) {
                                    if(kObjects[k] == null){
                                        System.err.println("WTF");
                                    }
                                    kObjects[k] .set(kObjects[k].metaClass().attribute("value"), value);
                                    kObjects[k].destroy();
                                }
                                counter.countDown();
                            }
                        });
                        KUniverse uni = model.universe(universe).diverge();
                        universe = uni.key();
                    }

                    //after everything
                    counter.then(new KCallback() {
                        @Override
                        public void on(Object o) {
                            model.disconnect(new KCallback() {
                                @Override
                                public void on(Object o) {
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
