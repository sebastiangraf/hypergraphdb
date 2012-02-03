/*
 * This file is part of the HyperGraphDB source distribution. This is copyrighted
 * software. For permitted uses, licensing options and redistribution, please see
 * the LicensingInformation file at the root level of the distribution.
 * 
 * Copyright (c) 2005-2010 Kobrix Software, Inc. All rights reserved.
 */
package org.hypergraphdb.atom;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGPlainLink;

/**
 * 
 * <p>
 * A <code>HGBergeLink</code> represent a <em>hyperarc</em> or <em>hyperedge</em> in the mathematical theory
 * of hypergraphs. A hyperarc has a target set that is partitioned into a <em>head</em> and a <em>tail</em>.
 * The name comes from the presumed inventor of this type of arc, Claude Berge. If it turns out the latter did
 * not actually come up with the definition, too bad for the naming choice :)
 * </p>
 * 
 * @author Borislav Iordanov
 */
public class HGBergeLink extends HGPlainLink {
    private int tailIndex = 0;

    public HGBergeLink(HGHandle... targets) {
        super(targets);
    }

    public HGBergeLink(int tailIndex, HGHandle... targets) {
        super(targets);
        this.tailIndex = tailIndex;
    }

    /**
     * 
     * Constructor.
     * 
     * @param head
     *            the incoming edges of the link to a node (the target ones, the sink. the children)
     * @param tail
     *            the outcoming edges of the link from a node(the origin ones, the source, the parents)
     */
    public HGBergeLink(HGHandle[] head, HGHandle[] tail) {
        super.outgoingSet = new HGHandle[head.length + tail.length];
        System.arraycopy(head, 0, super.outgoingSet, 0, head.length);
        System.arraycopy(tail, 0, super.outgoingSet, head.length, tail.length);
        tailIndex = head.length;
    }

    public Set<HGHandle> getHead() {
        HashSet<HGHandle> set = new HashSet<HGHandle>();
        for (int i = 0; i < tailIndex; i++)
            set.add(getTargetAt(i));
        return set;
    }

    public Set<HGHandle> getTail() {
        HashSet<HGHandle> set = new HashSet<HGHandle>();
        for (int i = tailIndex; i < getArity(); i++)
            set.add(getTargetAt(i));
        return set;
    }

    public int getTailIndex() {
        return tailIndex;
    }

    public void setTailIndex(int tailIndex) {
        this.tailIndex = tailIndex;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + new HashSet<HGHandle>(Arrays.asList(super.outgoingSet)).hashCode();
        result = prime * result + tailIndex;
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        HGBergeLink other = (HGBergeLink)obj;
        if (!new HashSet<HGHandle>(Arrays.asList(super.outgoingSet)).equals(new HashSet<HGHandle>(Arrays
            .asList(other.outgoingSet))))
            return false;
        if (tailIndex != other.tailIndex)
            return false;
        return true;
    }
}
