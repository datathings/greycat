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
package greycatTest.internal.utility;

public class HashTest {

    /*
    @Test
    public void hashing(){
        int testTimes=1000000;

        int dim=1000;
        int[] values=new int[dim];
        int[] rndval=new int[dim];

        Random random=new Random();

        int temp=0;
        int tempr=0;
        for(int i=0;i<testTimes;i++){
            temp= (int) HashHelper.tripleHash((byte) 1, random.nextInt(), random.nextInt(), random.nextInt(), dim);
            tempr=random.nextInt(dim);
            rndval[tempr]++;
            values[temp]++;
        }

        int avg=testTimes/dim;
        int max=0;
        int t=0;

        int maxr=0;
        int tr=0;

        double sum=0;
        double sumsq=0;

        double sumr=0;
        double sumsqr=0;


        for(int i=0;i<dim;i++){
            t=Math.abs(values[i]-avg);
            if(t>max){
                max=t;
            }
            sum+=t;
            sumsq+=t*t;

            tr=Math.abs(rndval[i]-avg);
            if(tr>maxr){
                maxr=tr;
            }

            sumr+=tr;
            sumsqr+=tr*tr;
        }

        sum=sum/dim;
        sumr=sumr/dim;

        double cov=(sumsq/dim - sum*sum)*(dim/(dim-1));
        double covr=(sumsqr/dim - sumr*sumr)*(dim/(dim-1));

        System.out.println("Triple hash");
        System.out.println("max dev: "+max+" avg dev: "+sum+ " ,std: "+Math.sqrt(cov));
        System.out.println("Random");
        System.out.println("max dev: "+maxr+" avg dev: "+sumr+ " ,std: "+Math.sqrt(covr));
    }*/
}

