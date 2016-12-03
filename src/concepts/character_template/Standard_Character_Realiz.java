package concepts.character_template;

import edu.clemson.resolve.runtime.RESOLVEBase;
import edu.clemson.resolve.runtime.RType;

import facilities.Standard_Booleans;
import java.util.Scanner;
import java.lang.reflect.*;
import edu.clemson.resolve.runtime.*;

public class Standard_Character_Realiz extends RESOLVEBase implements Character_Template {

    public class Character implements Character_Template.Character {
        public char val;
        Character() {
            val = 0;
        }

        Character(char i) {
            val = i;
        }

        // getRep is special case, this will never be called
        public Object getRep() {
            return new Character(val);
        }

        // setRep is special case, this will never be called
        public void setRep(Object o) {
            val = ((Character)o).val;
        }

        public RType initialValue() {
            return new Character();
        }

        public String toString() {
            return new java.lang.Character(val).toString();
        }
    }

    public RType initCharacter(char ... e) {
        if (e.length >= 1) {
            return new Character(e[0]);
        }
        else {
            return new Character();
        }
    }
    
    @Override
    public RType Are_Equal(RType i1, RType i2) {
        return Standard_Booleans.Std_Bools.initBoolean(((Character)i1).val == ((Character)i2).val);
    }
    
    @Override
    public RType Are_Not_Equal(RType i1, RType i2) {
        return Standard_Booleans.Std_Bools.initBoolean(((Character) i1).val != ((Character) i2).val);
    }
    
    @Override
    public void Write(RType i) {
        System.out.print(((Character) i).val);
    }
    
    @Override
    public void Write_Line(RType i) {
        System.out.println(((Character) i).val);
    }
    
/*
    @Override public void Read(RType e) {
        Scanner sc = new Scanner(System.in);
        ((Integer)e).val = sc.nextInt();
    }*/
}
