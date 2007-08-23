/*
 * This file is part of the HyperGraphDB source distribution. This is copyrighted
 * software. For permitted uses, licensing options and redistribution, please see 
 * the LicensingInformation file at the root level of the distribution. 
 *
 * Copyright (c) 2005-2006
 *  Kobrix Software, Inc.  All rights reserved.
 */
/*
 * Created on Aug 11, 2005
 *
 */
package org.hypergraphdb.query;

import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HyperGraph;

/**
 * <p>
 * A generic negating <code>HGQueryCondition</code>.
 * </p>
 * 
 * @author Borislav Iordanov
 */
public class Not implements HGQueryCondition, HGAtomPredicate 
{
	private HGAtomPredicate negated;
	
	public Not(HGAtomPredicate negated)
	{
		this.negated = negated;
	}
	
	public boolean satisfies(HyperGraph hg, HGHandle  value)
	{
		return !negated.satisfies(null, value);
	}	
	
	public String toString()
	{
		StringBuffer result = new StringBuffer("Not(");
		result.append(negated.toString());
		result.append(")");
		return result.toString();
	}
}