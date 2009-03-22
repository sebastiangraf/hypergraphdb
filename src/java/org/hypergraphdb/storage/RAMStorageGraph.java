package org.hypergraphdb.storage;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hypergraphdb.HGPersistentHandle;
import org.hypergraphdb.util.Pair;

/**
 * <p>
 * A {@link StorageGraph} bound to a RAM map to be populated explicitly 
 * before use.
 * </p>
 * 
 * @author Borislav Iordanov
 *
 */
public class RAMStorageGraph implements StorageGraph
{
    private HGPersistentHandle root;
    private Map<HGPersistentHandle, Object> map = new HashMap<HGPersistentHandle, Object>();
    
    public RAMStorageGraph(HGPersistentHandle root)
    {
        this.root = root;
    }
    
    public void put(HGPersistentHandle handle, HGPersistentHandle [] linkData)
    {
        map.put(handle, linkData);
    }
    
    public void put(HGPersistentHandle handle, byte [] data)
    {
        map.put(handle, data);
    }
    
    public byte[] getData(HGPersistentHandle handle)
    {
        return (byte[])map.get(handle);
    }

    public HGPersistentHandle[] getLink(HGPersistentHandle handle)
    {
        return (HGPersistentHandle[])map.get(handle);
    }

    public HGPersistentHandle getRoot()
    {
        return root;
    }

    public Iterator<Pair<HGPersistentHandle, Object>> iterator()
    {
        return new Iterator<Pair<HGPersistentHandle, Object>>()
        {
            final Iterator<Map.Entry<HGPersistentHandle, Object>> i = map.entrySet().iterator();

            public boolean hasNext()
            {
                return i.hasNext();
            }

            public Pair<HGPersistentHandle, Object> next()
            {
                Map.Entry<HGPersistentHandle, Object> e = i.next();
                return new Pair<HGPersistentHandle, Object>(e.getKey(), e.getValue());
            }

            public void remove()
            {
                i.remove();
            }            
        };
    }
}