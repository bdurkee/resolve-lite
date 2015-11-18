package edu.clemson.resolve;

import org.junit.Test;
import org.rsrg.semantics.MathSymbolTable;
import org.rsrg.semantics.NoSuchModuleException;
import org.rsrg.semantics.NoSuchSymbolException;

public class TestMathSymbolTable extends BaseTest {

    @Test(expected=NoSuchModuleException.class)
    public void testFreshMathSymbolTable()
            throws NoSuchModuleException {
        MathSymbolTable b = new MathSymbolTable();
        b.getModuleScope("NonExistent");
    }

}
