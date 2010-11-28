/* 
 * This file is part of the HyperGraphDB source distribution. This is copyrighted 
 * software. For permitted uses, licensing options and redistribution, please see  
 * the LicensingInformation file at the root level of the distribution.  
 * 
 * Copyright (c) 2005-2010 Kobrix Software, Inc.  All rights reserved. 
 */
package org.hypergraphdb.transaction;

import java.util.Stack;

import org.hypergraphdb.HGException;

class DefaultTransactionContext implements HGTransactionContext
{
	private Stack<HGTransaction> tstack = new Stack<HGTransaction>();
	private HGTransactionManager manager = null;
	
	DefaultTransactionContext(HGTransactionManager manager)
	{
		this.manager = manager;
	}
	
	public HGTransactionManager getManager()
	{
	    return manager;
	}
	
	/**
	 * <p>Return the currently active transaction or <code>null</code> if there is 
	 * no such transaction.</p>
	 */
	public HGTransaction getCurrent()
	{
		return tstack.isEmpty() ? null : tstack.peek();
	}
	
    public void beginTransaction(HGTransactionConfig config)
    {
        if (tstack.isEmpty())
            tstack.push(manager.createTransaction(null, config));
        else
            tstack.push(manager.createTransaction(tstack.peek(), config));
    }
    
	public void endTransaction(boolean success) throws HGTransactionException
	{
		if (tstack.isEmpty())
			throw new HGException("Attempt to end a transaction for an empty transaction context.");
		HGTransaction top = tstack.pop();
		if (manager.txMonitor != null)
			manager.txMonitor.transactionFinished(top);		
		if (success)
			top.commit();
		else
			top.abort();
	}
	
	public void endAll(boolean success) throws HGTransactionException
	{
		if (success)
			while (!tstack.isEmpty()) 
			{
				HGTransaction tx = tstack.pop();
				if (manager.txMonitor != null)
					manager.txMonitor.transactionFinished(tx);				 				
				tx.commit();
			}
		else
			while (!tstack.isEmpty()) 
			{
				HGTransaction tx = tstack.pop();
				if (manager.txMonitor != null)
					manager.txMonitor.transactionFinished(tx);								
				tx.abort();			
			}
	}
}
