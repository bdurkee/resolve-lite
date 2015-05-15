package org.resolvelite.semantics;

import org.resolvelite.misc.Utils;

import java.util.*;

public class MTUnion extends MTAbstract<MTUnion> {

    private List<MTType> members = new ArrayList<>();

    public MTUnion(TypeGraph g, List<MTType> members) {
        super(g);
        this.members.addAll(members);
    }

    public MTUnion(TypeGraph g, MTType... members) {
        this(g, Arrays.asList(members));
    }

    public void addMember(MTType t) {
        this.members.add(t);
    }

    public boolean containsMember(MTType member) {
        return members.contains(member);
    }

    @Override public boolean isKnownToContainOnlyMathTypes() {
        for (MTType member : members) {
            if ( !member.isKnownToContainOnlyMathTypes() ) {
                return false;
            }
        }
        return true;
    }

    @Override public void acceptOpen(TypeVisitor v) {
        v.beginMTType(this);
        v.beginMTAbstract(this);
        v.beginMTUnion(this);
    }

    @Override public void accept(TypeVisitor v) {
        acceptOpen(v);
        v.beginChildren(this);

        for (MTType t : members) {
            t.accept(v);
        }
        v.endChildren(this);
        acceptClose(v);
    }

    @Override public void acceptClose(TypeVisitor v) {
        v.endMTUnion(this);
        v.endMTAbstract(this);
        v.endMTType(this);
    }

    @Override public List<MTType> getComponentTypes() {
        return Collections.unmodifiableList(members);
    }

    @Override public String toString() {
        return "(" + Utils.join(members, " union ") + ")";
    }

}
