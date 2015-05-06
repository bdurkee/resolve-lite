package org.resolvelite.vcgen;

public interface RuleApplicationStrategy<T extends VCStat> {
    public void applyRule(T statement);
}
