package edu.clemson.resolve.codegen.model;

import java.util.ArrayList;
import java.util.List;

public class IfStat extends Stat {

    @ModelElement public List<Stat> ifStats = new ArrayList<>();
    @ModelElement public List<Stat> elseStats = new ArrayList<>();
    @ModelElement public Expr cond;

    public IfStat(Expr cond) {
        this.cond = cond;
    }

}
