package org.rsrg.semantics;

import edu.clemson.resolve.typereasoning.TypeGraph;

import java.util.*;

public class MTIntersect extends MTAbstract<MTIntersect> {

    private final static int BASE_HASH = "MTIntersect".hashCode();

    private List<MTType> members = new LinkedList<>();

    public MTIntersect(TypeGraph g) {
        super(g);
    }

    public MTIntersect(TypeGraph g, List<MTType> elements) {
        this(g);
        members.addAll(elements);
    }

    public MTIntersect(TypeGraph g, MTType... elements) {
        this(g, Arrays.asList(elements));
    }

    public void addMember(MTType t) {
        members.add(t);
    }

    public boolean containsMember(MTType member) {
        return members.contains(member);
    }

    @Override public boolean isKnownToContainOnlyMTypes() {
        Iterator<MTType> members = this.members.iterator();
        while (members.hasNext()) {
            MTType member = members.next();
            if ( !member.isKnownToContainOnlyMTypes() ) {
                return false;
            }
        }
        return true;
    }

    @Override public boolean membersKnownToContainOnlyMTypes() {
        Iterator<MTType> members = this.members.iterator();
        while (members.hasNext()) {
            MTType member = members.next();
            if ( !member.membersKnownToContainOnlyMTypes() ) {
                return false;
            }
        }
        return true;
    }

    @Override public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("(");
        Iterator<MTType> members = this.members.iterator();
        while (members.hasNext()) {
            MTType member = members.next();
            if ( sb.length() > 1 ) {
                sb.append(" intersect ");
            }
            sb.append(member.toString());
        }
        sb.append(")");
        return sb.toString();
    }

    @Override public void acceptOpen(TypeVisitor v) {
        v.beginMTType(this);
        v.beginMTAbstract(this);
        v.beginMTIntersect(this);
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
        v.endMTIntersect(this);
        v.endMTAbstract(this);
        v.endMTType(this);
    }

    @Override public List<MTType> getComponentTypes() {
        return Collections.unmodifiableList(members);
    }

    @Override public MTType withComponentReplaced(int index, MTType newType) {
        List<MTType> newMembers = new LinkedList<MTType>(members);
        newMembers.set(index, newType);
        return new MTIntersect(getTypeGraph(), newMembers);
    }

    @Override public int getHashCode() {
        int result = BASE_HASH;

        for (MTType t : members) {
            result *= 45;
            result += t.hashCode();
        }
        return result;
    }
}
