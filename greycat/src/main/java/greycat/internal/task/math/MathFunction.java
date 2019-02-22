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
package greycat.internal.task.math;

import java.util.*;

/**
 * Abstract definition of a supported mathexp function. A function is
 * defined by a name, the number of parameters and the actual processing
 * implementation.
 */
class MathFunction implements MathToken {


    /**
     * Name of this function.
     */
    private String name;
    /**
     * Number of parameters expected for this function.
     */
    private int numParams;

    /**
     * @ignore ts
     */
    private TimeZone timeZone = TimeZone.getDefault();

    /**
     * @ignore ts
     */
    private Locale locale = Locale.getDefault(Locale.Category.FORMAT);

    /**
     * Creates a new function with given name and parameter count.
     *
     * @param name      The name of the function.
     * @param numParams The number of parameters for this function.
     */
    MathFunction(String name, int numParams) {
        this.name = name.toUpperCase();
        this.numParams = numParams;
    }

    public String getName() {
        return name;
    }

    int getNumParams() {
        return numParams;
    }

    double eval(double[] p) {
        if (name.equals("NOT")) {
            return (p[0] == 0) ? 1 : 0;
        } else if (name.equals("IF")) {
            return !(p[0] == 0) ? p[1] : p[2];
        } else if (name.equals("RAND")) {
            return Math.random();
        } else if (name.equals("SIN")) {
            return Math.sin(p[0]);
        } else if (name.equals("COS")) {
            return Math.cos(p[0]);
        } else if (name.equals("TAN")) {
            return Math.tan(p[0]);
        } else if (name.equals("ASIN")) {
            return Math.asin(p[0]);
        } else if (name.equals("ACOS")) {
            return Math.acos(p[0]);
        } else if (name.equals("ATAN")) {
            return Math.atan(p[0]);
        } else if (name.equals("MAX")) {
            return p[0] > p[1] ? p[0] : p[1];
        } else if (name.equals("MIN")) {
            return p[0] < p[1] ? p[0] : p[1];
        } else if (name.equals("ABS")) {
            return Math.abs(p[0]);
        } else if (name.equals("LOG")) {
            return Math.log(p[0]);
        } else if (name.equals("ROUND")) {
            long factor = (long) Math.pow(10, p[1]);
            double value = p[0] * factor;
            long tmp = Math.round(value);
            return (double) tmp / factor;
        } else if (name.equals("FLOOR")) {
            return Math.floor(p[0]);
        } else if (name.equals("CEILING")) {
            return Math.ceil(p[0]);
        } else if (name.equals("SQRT")) {
            return Math.sqrt(p[0]);
        } else if (name.equals("SECONDS")) {
            return date_to_seconds(p[0]);
        } else if (name.equals("MINUTES")) {
            return date_to_minutes(p[0]);
        } else if (name.equals( "HOURS")) {
            return date_to_hours(p[0]);
        } else if (name.equals("DAY")) {
            return date_to_days(p[0]);
        } else if (name.equals("MONTH")) {
            return date_to_months(p[0]);
        } else if (name.equals("YEAR")) {
            return date_to_year(p[0]);
        } else if (name.equals("DAYOFWEEK")) {
            return date_to_dayofweek(p[0]);
        }
        return 0;
    }


    /**
     * @native ts
     * var date = new Date(value);
     * return date.getSeconds();
     */
    private double date_to_seconds(double value) {
        GregorianCalendar calendar = new GregorianCalendar(timeZone, locale);
        calendar.setTime(new Date((long) value));
        return calendar.get(Calendar.SECOND);
    }

    /**
     * @native ts
     * var date = new Date(value);
     * return date.getMinutes();
     */
    private double date_to_minutes(double value) {
        GregorianCalendar calendar = new GregorianCalendar(timeZone, locale);
        calendar.setTime(new Date((long) value));
        return calendar.get(Calendar.MINUTE);
    }

    /**
     * @native ts
     * var date = new Date(value);
     * return date.getHours();
     */
    private double date_to_hours(double value) {
        GregorianCalendar calendar = new GregorianCalendar(timeZone, locale);
        calendar.setTime(new Date((long) value));
        return calendar.get(Calendar.HOUR_OF_DAY);
    }


    /**
     * @native ts
     * var date = new Date(value);
     * return date.getDate();
     */
    private double date_to_days(double value) {
        GregorianCalendar calendar = new GregorianCalendar(timeZone, locale);
        calendar.setTime(new Date((long) value));
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    //January is 0, december is 11

    /**
     * @native ts
     * var date = new Date(value);
     * return date.getMonth();
     */
    private double date_to_months(double value) {
        GregorianCalendar calendar = new GregorianCalendar(timeZone, locale);
        calendar.setTime(new Date((long) value));
        return calendar.get(Calendar.MONTH);
    }

    /**
     * @native ts
     * var date = new Date(value);
     * return date.getFullYear();
     */
    private double date_to_year(double value) {
        GregorianCalendar calendar = new GregorianCalendar(timeZone, locale);
        calendar.setTime(new Date((long) value));
        return calendar.get(Calendar.YEAR);
    }

    //Sunday is 0, friday is 6

    /**
     * @native ts
     * var date = new Date(value);
     * return date.getDay();
     */
    private double date_to_dayofweek(double value) {
        GregorianCalendar calendar = new GregorianCalendar(timeZone, locale);
        calendar.setTime(new Date((long) value));
        return calendar.get(Calendar.DAY_OF_WEEK) - 1;

    }

    @Override
    public int type() {
        return 1;
    }

}

