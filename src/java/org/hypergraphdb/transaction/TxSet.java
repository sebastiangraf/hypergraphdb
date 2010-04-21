package org.hypergraphdb.transaction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;

import org.hypergraphdb.HGException;
import org.hypergraphdb.HGRandomAccessResult;
import org.hypergraphdb.util.CloneMe;
import org.hypergraphdb.util.HGSortedSet;

/**
 * <p>
 * A transactional {@link HGSortedSet} that implements MVCC for concurrency instead of 
 * locking. It uses an underlying implementation that is copied-on-write: when a transaction 
 * attempts to modify the set, it is cloned into a local copy and that copy receives all
 * the writes. In addition, each write operation is recorded in a write log. At commit time, 
 * if there's no conflict, the write log is replayed on the latest committed version of the 
 * set (which may be different from the copy the transaction started from). This may sound
 * a bit counter-intuitive: if the set is only being written to during a transaction, there won't
 * be a conflict, even if another transaction modified and committed in the meantime. A conflict 
 * will occur only if the set has been read, but somebody else in the meantime modified it.
 * </p>
 * 
 * <p>
 * Assuming the underlying set implementation is only accessed in the context of HGDB 
 * transactions, it doesn't need to be thread-safe, it doesn't need any locks.
 * </p>
 * 
 * @author Borislav Iordanov
 *
 * @param <E>
 */
public class TxSet<E> implements HGSortedSet<E>
{
    // Set modif operations, recorded in the write log.
    private static final int ADD = 0;
    private static final int REMOVE = 1;
    private static final int CLEAR = 2;
    private static final int ADDALL = 3;
    private static final int REMOVEALL = 4;
    private static final int RETAINALL = 5;
    
    private static class LogEntry
    {
        int op;
        Object el;
    }

    private HGTransactionManager txManager;
    private VBox<HGSortedSet<E>> S;
    
    void log(int op, Object el)
    {
        LogEntry entry = new LogEntry();
        entry.op = op;
        entry.el = el;
        List<LogEntry> log = txManager.getContext().getCurrent().getAttribute(this);
        if (log == null)
        {
            log = new ArrayList<LogEntry>();
            txManager.getContext().getCurrent().setAttribute(this, log);
        }
        log.add(entry);
    }
    
    @SuppressWarnings("unchecked")
    void applyLog(List<LogEntry> log, HGSortedSet<E> set)
    {
        for (LogEntry e : log)
            switch (e.op)
            {
                case ADD: { set.add((E)e.el); break; }
                case REMOVE: { set.remove((E)e.el); break; }
                case CLEAR: { set.clear(); break; }
                case ADDALL: { set.addAll((Collection<E>)e.el); break; }
                case REMOVEALL: { set.removeAll((Collection<E>)e.el); break; }
                case RETAINALL: { set.retainAll((Collection<E>)e.el); break; }
            }
    }
    
    @SuppressWarnings("unchecked")
    HGSortedSet<E> cloneSet(HGSortedSet<E> S)
    {
        if (S instanceof CloneMe)
            return ((CloneMe)S).clone();
        else
        {
            try
            {
                HGSortedSet<E> S2 = S.getClass().newInstance();
                S2.addAll(S);
                return S2;
            }
            catch (Exception ex)
            {
                throw new HGException(ex);
            }
        }        
    }
    
    HGSortedSet<E> read()
    {
        HGSortedSet<E> x = S.get();
        return x;
    }
    
    HGSortedSet<E> write()
    {
        List<LogEntry> log = txManager.getContext().getCurrent().getAttribute(this);
        if (log == null) // should we copy-on-write?
        {
            HGSortedSet<E> readOnly = S.get(); // S.getForWrite();
            HGSortedSet<E> writeable = cloneSet(readOnly);
            S.put(writeable);
        }
        return S.get();
    }
    
    public TxSet(final HGTransactionManager txManager, final HGSortedSet<E> backingSet)
    {
        this.txManager = txManager;
        this.S = new VBox<HGSortedSet<E>>(txManager, backingSet)
        {
            @Override
            public VBoxBody<HGSortedSet<E>> commit(HGTransaction tx, HGSortedSet<E> newvalue, long txNumber)
            {
                if (tx != null)
                {                                    
                    HGSortedSet<E> lastCommitted = body.getBody(txNumber).value;
                    if (lastCommitted == null) // is this the very first commit?
                        return super.commit(tx, newvalue, txNumber);
                    List<LogEntry> log = tx.getAttribute(TxSet.this);
                    if (log != null) // did we do any modifications to the set?
                    {
                        lastCommitted = cloneSet(lastCommitted);
                        applyLog(log, lastCommitted);
                        return super.commit(tx, lastCommitted, txNumber);
                    }
                    else
                        return body; // nothing to do, we've just been reading
                }
                else
                {
                    return super.commit(tx, newvalue, txNumber); // hope caller knows what they are doing
                }            
            }              
        };
    }    
    
    public HGRandomAccessResult<E> getSearchResult()
    {
        return read().getSearchResult();
    }

    public Comparator<? super E> comparator()
    {
        return read().comparator();
    }

    public E first()
    {
        return read().first();
    }

    public SortedSet<E> headSet(E toElement)
    {
        return read().headSet(toElement);
    }

    public E last()
    {
        return read().last();
    }

    public SortedSet<E> subSet(E fromElement, E toElement)
    {
        return read().subSet(fromElement, toElement);
    }

    public SortedSet<E> tailSet(E fromElement)
    {
        return read().tailSet(fromElement);
    }

    public boolean add(E e)
    {
        boolean b = write().add(e);
        if (b)
            log(ADD, e);
        return b;
    }

    public boolean addAll(Collection<? extends E> c)
    {
        boolean b = write().addAll(c);
        if (b) 
            log(ADDALL, c);
        return b;
    }

    public void clear()
    {
        write().clear();
        log(CLEAR, null);
    }

    public boolean contains(Object o)
    {
        return read().contains(o);
    }

    public boolean containsAll(Collection<?> c)
    {
        return read().containsAll(c);
    }

    public boolean isEmpty()
    {
        return read().isEmpty();
    }

    public Iterator<E> iterator()
    {
        return read().iterator();
    }

    public boolean remove(Object o)
    {
        boolean b = write().remove(o);
        if (b)
            log(REMOVE, o);
        return b;
    }

    public boolean removeAll(Collection<?> c)
    {
        boolean b = write().removeAll(c);
        if (b)
            log(REMOVEALL, c);        
        return b;
    }

    public boolean retainAll(Collection<?> c)
    {
        boolean b = write().retainAll(c);
        if (b)
            log(RETAINALL, c);
        return b;
    }

    public int size()
    {
        return read().size();
    }

    public Object[] toArray()
    {
        return read().toArray();
    }

    public <T> T[] toArray(T[] a)
    {
        return read().toArray(a);
    }
}