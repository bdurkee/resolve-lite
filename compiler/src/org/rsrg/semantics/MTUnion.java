package org.rsrg.semantics;

import edu.clemson.resolve.misc.Utils;

import java.util.*;

public class MTUnion extends MTAbstract<MTUnion> {

    private final static int BASE_HASH = "MTUnion".hashCode();

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

    @Override public boolean isKnownToContainOnlyMTypes() {
        for (MTType member : members) {
            if ( !member.isKnownToContainOnlyMTypes() ) {
                return false;
            }
        }
        return true;
    }

    @Override public MTType withComponentReplaced(int index, MTType newType) {
        List<MTType> newMembers = new LinkedList<>(members);
        newMembers.set(index, newType);
        return new MTUnion(getTypeGraph(), newMembers);
    }

    @Override public int getHashCode() {
        int result = BASE_HASH;

        for (MTType t : members) {
            result *= 61;
            result += t.hashCode();
        }

        return result;
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
