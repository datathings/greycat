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
package ml.classifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by andrey.boytsov on 20/06/16.
 */
public class Generator {
    public static void main(String[] args) {
        Random rng = new Random(3);

        List<Integer> f1 = new ArrayList<Integer>();
        //List<Double> f1 = new ArrayList<Double>();

        for (int i = 0; i <= 600; i++) {
            f1.add(rng.nextInt(10));
            //f2.add(rng.nextDouble());
        }
        //Now starting to make errors:
        //for (int i = 1001; i < 1019; i++) {
            //f1.add(rng.nextDouble());
            //f2.add(rng.nextDouble());
        //}
        //This should be the last drop: 19 errors of 60 value buffer (0.31...>0.3) should push us back into bootstrap
        //f1.add(rng.nextDouble());
        //f2.add(rng.nextDouble());

        //System.out.println(""+f1.size()+"  "+f2.size());

        System.out.print("private static final int f3[] = new int[]{");
        for (int i=0;i<f1.size();i++){
            System.out.print(f1.get(i)+", ");
            if (i % 50 == 0){
                System.out.println();
            }
        }
        System.out.println("};");

        /*System.out.print("double f2Array[] = new double[]{");
        for (int i=0;i<f1.size();i++){
            System.out.print(f2.get(i)+", ");
            if (i % 6 == 0){
                System.out.println();
            }
        }
        System.out.println("};");*/
    }
}
