/** Generated from T.resolve by RESOLVE version 0.0.1. This file should not be modified. */
package goo;

import java.lang.reflect.*;
import edu.clemson.resolve.runtime.*;
import facilities.Standard_Booleans;
import concepts.boolean_template.Boolean_Template;

public class T {
    public static void Main() {
        RType x = ((concepts.boolean_template.Boolean_Template)Standard_Booleans.INSTANCE).initBoolean();
        RESOLVEBase.assign(x, ((concepts.boolean_template.Boolean_Template)Standard_Booleans.INSTANCE).initBoolean(true));
    }
    public static void main(String[] args) {
        Main();
    }
}