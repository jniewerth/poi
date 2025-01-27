/*
 *  ====================================================================
 *    Licensed to the Apache Software Foundation (ASF) under one or more
 *    contributor license agreements.  See the NOTICE file distributed with
 *    this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0
 *    (the "License"); you may not use this file except in compliance with
 *    the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 * ====================================================================
 */

package org.apache.poi.ss.formula.functions;

import static org.apache.poi.ss.util.Utils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.poi.hssf.HSSFTestDataSamples;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.formula.OperationEvaluationContext;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.NumericValueEval;
import org.apache.poi.ss.formula.eval.StringEval;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.usermodel.FormulaError;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * Test cases for MAXIFS()
 */
final class TestMaxifs {

    private static final OperationEvaluationContext EC = new OperationEvaluationContext(null, null, 0, 1, 0, null);

    private static ValueEval invokeMaxifs(ValueEval[] args) {
        return new Maxifs().evaluate(args, EC);
    }

    private static void confirmDouble(double expected, ValueEval actualEval) {
        assertTrue(actualEval instanceof NumericValueEval, "Expected numeric result");
        NumericValueEval nve = (NumericValueEval)actualEval;
        assertEquals(expected, nve.getNumberValue(), 0);
    }

    private static void confirm(double expectedResult, ValueEval[] args) {
        confirmDouble(expectedResult, invokeMaxifs(args));
    }

    /**
     *  Example 1 from
     *  https://support.microsoft.com/en-us/office/maxifs-function-dfd611e6-da2c-488a-919b-9b6376b28883
     */
    @Test
    void testExample1() {
        ValueEval[] a2a7 = new ValueEval[] {
                new NumberEval(89),
                new NumberEval(93),
                new NumberEval(96),
                new NumberEval(85),
                new NumberEval(91),
                new NumberEval(88)
        };

        ValueEval[] b2b7 = new ValueEval[] {
               new NumberEval(1),
               new NumberEval(2),
               new NumberEval(2),
               new NumberEval(3),
               new NumberEval(1),
               new NumberEval(1)
        };

        ValueEval[] args;
        // "=MAXIFS(A2:A7,B2:B7,1)"
        args = new ValueEval[]{
                EvalFactory.createAreaEval("A2:A7", a2a7),
                EvalFactory.createAreaEval("B2:B7", b2b7),
                new NumberEval(1)
        };
        confirm(91.0, args);
        
    }

    /**
     *  Example 2 from
     *  https://support.microsoft.com/en-us/office/maxifs-function-dfd611e6-da2c-488a-919b-9b6376b28883
     */
    @Test
    void testExample2() {
        ValueEval[] a2a5 = new ValueEval[] {
                new NumberEval(10),
                new NumberEval(11),
                new NumberEval(100),
                new NumberEval(111)
        };
        
        ValueEval[] b3b6 = new ValueEval[] {
                new StringEval("a"),
                new StringEval("a"),
                new StringEval("b"),
                new StringEval("a")
        };
        
        ValueEval[] args;

        // "=MAXIFS(A2:A5,B3:B6,"a")"
        args = new ValueEval[]{
                EvalFactory.createAreaEval("A2:A5", a2a5),
                EvalFactory.createAreaEval("B3:B6", b3b6),
                new StringEval("a")
        };
        confirm(111.0, args); // the support article wrongly states 10.0
    }
  
}
