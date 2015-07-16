package edu.clemson.resolve.codegen.model;

import java.util.ArrayList;
import java.util.List;

public class WhileStat extends Stat {
    @ModelElement public Expr cond;
    @ModelElement public List<Stat> stats = new ArrayList<>();

    public WhileStat(Expr cond) {
        this.cond = cond;
    }
}