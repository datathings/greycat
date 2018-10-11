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
package common;

import org.mwg.utility.HashHelper;

import java.util.Random;
import java.util.TreeMap;

/**
 * Created by assaad on 23/03/16.
 */
public class HashCollision {
    public static void main(String[] arg){
        hashing();
    }
    public static void hashing() {
        byte x;
        long p1, p2, p3;

        TreeMap<Long, Integer> hashTree = new TreeMap<Long, Integer>();
        TreeMap<Long, Integer> randomTree = new TreeMap<Long, Integer>();

        Random rand = new Random();
        long max = 1000000000;

        long trials = 10000000;

        for (long i = 0; i < trials; i++) {
            long yrand = rand.nextLong() % max;

            x = (byte) rand.nextInt(4);
            p1 = rand.nextLong();
            p2 = rand.nextLong();
            p3 = rand.nextLong();

            long uhash = HashHelper.tripleHash(x, p1, p2, p3, max);


            if (randomTree.containsKey(yrand)) {
                randomTree.put(yrand, randomTree.get(yrand) + 1);
            } else {
                randomTree.put(yrand, 1);
            }

            if (hashTree.containsKey(uhash)) {
                hashTree.put(uhash, hashTree.get(uhash) + 1);
            } else {
                hashTree.put(uhash, 1);
            }
        }

        double randcoll = trials - randomTree.keySet().size();
        randcoll = randcoll * 100 / trials;
        System.out.println("Random size: " + randomTree.keySet().size() + " collisions: " + randcoll + " %");

        double hashcoll = trials - hashTree.keySet().size();
        hashcoll = hashcoll * 100 / trials;
        System.out.println("Hash size: " + hashTree.keySet().size() + " collisions: " + hashcoll + " %");


    }
}
