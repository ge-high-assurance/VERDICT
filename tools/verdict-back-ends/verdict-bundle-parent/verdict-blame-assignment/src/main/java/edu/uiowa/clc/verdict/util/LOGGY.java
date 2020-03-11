/* Copyright (c) 2019-2020, Board of Trustees of the University of Iowa.
   Copyright (c) 2019-2020, General Electric Company.
   All rights reserved.

   Use of this source code is governed by a BSD 3-Clause License that
   can be found in the LICENSE file.

    @author: Paul Meng
    @author: M. Fareed Arif
*/

package edu.uiowa.clc.verdict.util;

// import org.slf4j.Logger;

public class LOGGY {
    public static void info(final String message) {
        System.out.println(message);
        //        logger.info(message);
    }

    public static void warn(final String message) {
        System.err.println(message);
        //        logger.warn(message);
    }
}
