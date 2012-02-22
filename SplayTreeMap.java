/* This code is released under the GPL v2.0 (see http://www.fsf.org)
 * and under the ASL (see http://www.apache.org)
 * and under the modified BSD license (see http://www.bsd.org)
 * Copyright (c) 2004 Martin Dengler.  All rights reserved.
 * Code now hosted at github.com/mdengler/SplayTreeMap
 */

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;

/**
 * Splay tree implementation of the <tt>AbstractMap</tt> class.
 * 
 * The same guarantees and constraints as TreeMap are maintained.
 * 
 * The asymptotic performance is roughly the same (log(n) cost) but with
 * a lower constant and a more compact algorithm.
 * 
 * Splay algorithm from D. Sleator's top_down_splay.c.
 * 
 * As with standard java collections classes, SplayTreeMap is NOT MT-safe.
 * Use the standard idiom(s) to make it MT-safe if necessary.
 * 
 * @author martin@martindengler.com
 *  
 */

public class SplayTreeMap
    extends AbstractMap
    implements Cloneable, java.io.Serializable {
    //      TODO: implement SortedMap

    private Comparator comparator;
    private transient int size = 0;
    private transient int modCount = 0;
    private transient Entry first, root, last;
    private transient int firstModCount = 0;
    private transient int lastModCount = 0;

    //inspired by java.util.TreeMap
    private transient volatile Set entrySet = null;

    public SplayTreeMap() {
    }

    public SplayTreeMap(Map map) {
        putAll(map);
    }

    public SplayTreeMap(Comparator comparator) {
        this.comparator = comparator;
    }

    public SplayTreeMap(SortedMap map) {
        this.comparator = map.comparator();
        putAll(map);
    }

    public int size() {
        return size;
    }

    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    public Comparator comparator() {
        return comparator;
    }

    public Object firstKey() {
        return firstEntry().getKey();
    }

    public Object lastKey() {
        return lastEntry().getKey();
    }

    private Entry firstEntry() {
        if (firstModCount == modCount) {
            return first;
        }
        Entry cur = root;
        while (cur.left != null) {
            cur = cur.left;
        }
        //should we be calling splay() here?
        first = cur;
        firstModCount = modCount;
        return first;
    }

    private Entry lastEntry() {
        if (lastModCount == modCount) {
            return last;
        }
        Entry cur = root;
        while (cur.right != null) {
            cur = cur.right;
        }
        last = cur;
        lastModCount = modCount;
        return last;
    }

    /**
     * Splay algorithm , from Sleator and Tarjan (top_down_splay.c, 1992)
     * 
     * @param key
     * @return null if key is null; or
     *         1) root of a new tree with <i>key</i>'s node (Entry) splayed
     *         to the top
     *         if an Entry with the desired key exists in the tree;
     *         otherwise,
     *         2) a new tree with the key that is just BEFORE
     *         where <i>key</i> would have been placed in the tree is returned.
     * 
     * @author martin@martindengler.com
     * 
     * 
     */
    protected Entry splay(Object key) {
        return splay(key, root);
    }

    /**
     * Splay algorithm , from Sleator and Tarjan (top_down_splay.c, 1992)
     * 
     * @param key
     * @param tree root of the tree to be splayed
     * @return null if key is null; or
     *         1) root of a new tree with <i>key</i>'s node (Entry) splayed
     *         to the top
     *         if an Entry with the desired key exists in the tree;
     *         otherwise,
     *         2) a new tree with the key that is just BEFORE
     *         where <i>key</i> would have been placed in the tree is returned.
     * 
     * @author martin@martindengler.com
     * 
     * 
     */
    protected Entry splay(Object key, Entry tree) {
        if (key == null || tree == null) {
            return null;
        }
        Entry tmpRoot = new Entry(null, null, null);
        Entry l, r, tmp;
        l = r = tmpRoot;

        Entry current = tree;
        while (true) {
            if (compare(key, current.key) < 0) { //search to the left?
                if (current.left != null
                    && compare(key, current.left.key) < 0) { //rotate right?

                    //rotate current and current.left to the right
                    //so current.left will become the daddy of current;
                    //  current will become the right child of current.left;
                    //  and current.left.right will become the new-child.left 

                    //current.left becomes the (future) daddy of current
                    tmp = current.left;

                    //current.left.right becomes the new-child.left 
                    current.left = tmp.right;
                    //tmp.right.parent = current;
                    current.left.parent = current;

                    //current becomes the right child of current.left
                    tmp.right = current;
                    //current.parent = tmp;
                    tmp.right.parent = tmp;

                    current = tmp;
                    tmp.parent = null;

                    if (current.left == null) {
                        break;
                    }
                }
                //link right
                r.left = current;
                r.left.parent = r;
                r = current;
                current = current.left;
            } else if (compare(key, current.key) > 0) { //search to the right?
                if (current.right != null
                    && compare(key, current.right.key) > 0) { //rotate left?

                    //rotate current and current.right to the left
                    //so current.right will become the daddy of current;
                    //  current will become the left child of current.right;
                    //  and current.right.left will become the new-child.right 

                    //current.right becomes the (future) daddy of current
                    tmp = current.right;

                    //current.right.left becomes the new-child.right 
                    current.right = tmp.left;
                    //tmp.left.parent = current;
                    current.right.parent = current;

                    //current becomes the left child of current.right
                    tmp.left = current;
                    //current.parent = tmp;
                    tmp.left.parent = tmp;

                    current = tmp;
                    current.parent = null;

                    if (current.right == null) {
                        break;
                    }
                }
                //link left
                l.right = current;
                l.right.parent = l;
                l = current;
                current = current.right;
            } else {
                break;
            }
        }

        //reassemble
        l.right = current.left;
        current.left.parent = l;

        r.left = current.right;
        r.left.parent = r;

        current.left = tmpRoot.right;
        current.left.parent = current;

        current.right = tmpRoot.left;
        current.right.parent = current;

        return current;
    }

    /**
     * Returns the successor of the specified Entry, or null if no such.
     * <br>
     * Performs a splay operation before returning.
     */
    private Entry successor(Entry t) {
        return successor(t, true);
    }

    /**
     * Returns the successor of the specified Entry, or null if no such.
     * <br>
     * Performs a splay operation before returning.
     * 
     * TODO: check if splaying should ever be optional 
     * 
     */
    private Entry successor(Entry t, boolean splayAfter) {
        if (t == null) {
            return null;
        } else {
            Entry p;
            if (t.right != null) {
                p = t.right;
                while (p.left != null)
                    p = p.left;
            } else {
                p = t.parent;
                Entry ch = t;
                while (p != null && ch == p.right) {
                    ch = p;
                    p = p.parent;
                }
            }
            if (p != null && splayAfter) {
                root = splay(p);
            }
            return p;
        }
    }

    /**
     * Removes the mapping for this key from this TreeMap if present.
     *
     * @param  key key for which mapping should be removed
     * @return previous value associated with specified key, or <tt>null</tt>
     *         if there was no mapping for key.  A <tt>null</tt> return can
     *         also indicate that the map previously associated
     *         <tt>null</tt> with the specified key.
     * 
     * @throws    ClassCastException key cannot be compared with the keys
     *            currently in the map.
     * @throws NullPointerException key is <tt>null</tt> and this map uses
     *         natural order, or its comparator does not tolerate
     *         <tt>null</tt> keys.
     */
    public Object remove(Object key) {
        Entry p = getEntry(key);
        if (p == null || p.getKey() == null)
            return null;

        Object oldValue = p.value;
        root = splay(p.getKey());
        deleteEntry(p);
        return oldValue;
    }

    /**
      * Delete node p, and then re-splay tree.
      * Assumes p is non-null, p.getKey is non-null, splay(p.getKey())
      * has been called, and p is
      * already in the tree (all checked/done by the calling
      * method, remove()).
      */

    private void deleteEntry(Entry p) {
        Entry newRoot;

        if (compare(root.getKey(), p.getKey()) == 0) {
            if (root.left == null) {
                newRoot = root.right;
            } else {
                newRoot = splay(p.getKey(), root.left);
                newRoot.right = root.right;
                root.right.parent = newRoot;
            }
            decsize();
            root = newRoot;
            root.parent = null;
        } else {
            //should never get here;
        }
    }

    public Object get(Object key) {
        if (key == null) {
            return null;
        }
        Entry e = getEntry(key);
        return e == null ? null : e.value;
    }

    /**
     * Returns this map's entry for the given key, or <tt>null</tt> if the map
     * does not contain an entry for the key.
     *
     * @return this map's entry for the given key, or <tt>null</tt> if the map
     *                does not contain an entry for the key.
     * @throws ClassCastException if the key cannot be compared with the keys
     *                  currently in the map.
     * @throws NullPointerException key is <tt>null</tt> and this map uses
     *                  natural order, or its comparator does not tolerate *
     *                  <tt>null</tt> keys.
     * 
     * @see java.util.TreeMap
     * 
     */
    private Entry getEntry(Object key) {
        root = splay(key);
        return root.key.equals(key) ? root : null;
    }

    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for this key, the old
     * value is replaced.
     *
     * @param key key with which the specified value is to be associated.
     * @param value value to be associated with the specified key.
     * 
     * @return previous value associated with specified key, or <tt>null</tt>
     *         if there was no mapping for key.  A <tt>null</tt> return can
     *         also indicate that the map previously associated <tt>null</tt>
     *         with the specified key.
     * @throws    ClassCastException key cannot be compared with the keys
     *            currently in the map.
     * @throws NullPointerException key is <tt>null</tt> and this map uses
     *         natural order, or its comparator does not tolerate
     *         <tt>null</tt> keys.
     * 
     */
    public Object put(Object key, Object value) {
        Entry newTree = new Entry(key, value, null);
        Object ret = null;

        if (root == null) {
            root = newTree;
            incsize();
            return null;
        }

        Entry oldTree = splay(key);

        if (compare(key, oldTree.key) < 0) {
            newTree.left = oldTree.left;
            newTree.right = oldTree;
            oldTree.left = null;
            oldTree.parent = newTree;
            incsize();
        } else if (compare(key, oldTree.key) > 0) {
            newTree.left = oldTree;
            newTree.right = oldTree.right;
            oldTree.right = null;
            oldTree.parent = newTree;
            incsize();
        } else {
            ret = oldTree.value;
            oldTree.value = value;
        }

        return ret;

    }

    /**
     * Compares two keys using the correct comparison method.
     * 
     * @see java.util.TreeMap
     * 
     */
    private int compare(Object k1, Object k2) {
        return (
            comparator == null
                ? ((Comparable) k1).compareTo(k2)
                : comparator.compare(k1, k2));
    }

    /* create a set backed by this SplayTreeMap */
    public Set entrySet() {
        if (entrySet == null) {
            entrySet = new AbstractSet() {
                public Iterator iterator() {
                    return new EntryIterator();
                }

                public boolean contains(Object o) {
                    if (!(o instanceof Map.Entry))
                        return false;
                    Map.Entry entry = (Map.Entry) o;
                    Object value = entry.getValue();
                    Entry p = getEntry(entry.getKey());
                    return p != null && valEquals(p.getValue(), value);
                }

                public boolean remove(Object o) {
                    if (!(o instanceof Map.Entry))
                        return false;
                    Map.Entry entry = (Map.Entry) o;
                    Object value = entry.getValue();
                    Entry p = getEntry(entry.getKey());
                    if (p != null && valEquals(p.getValue(), value)) {
                        deleteEntry(p);
                        return true;
                    }
                    return false;
                }

                public int size() {
                    return SplayTreeMap.this.size();
                }

                public void clear() {
                    SplayTreeMap.this.clear();
                }
            };
        }
        return entrySet;
    }

    private void incsize() {
        size++;
        modCount++;
    }
    private void decsize() {
        size--;
        modCount--;
    }

    /**
     * Test two values  for equality.  Differs from o1.equals(o2) only in
     * that it copes with with <tt>null</tt> o1 properly.
     * 
     * @see java.util.TreeMap
     * 
     */
    private static boolean valEquals(Object o1, Object o2) {
        return (o1 == null ? o2 == null : o1.equals(o2));
    }

    /**
     * SplayTreeMap Iterator, from java.util.TreeMap$EntryIterator
     */
    private class EntryIterator implements Iterator {
        private int expectedModCount = SplayTreeMap.this.modCount;
        private Entry lastReturned = null;
        Entry next;

        EntryIterator() {
            next = firstEntry();
        }

        // Used by SubMapEntryIterator
        EntryIterator(Entry first) {
            next = first;
        }

        public boolean hasNext() {
            return next != null;
        }

        final Entry nextEntry() {
            if (next == null)
                throw new NoSuchElementException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            lastReturned = next;
            next = successor(next, false); //don't splay when iterating
            return lastReturned;
        }

        public Object next() {
            return nextEntry();
        }

        public void remove() {
            if (lastReturned == null)
                throw new IllegalStateException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            if (lastReturned.left != null && lastReturned.right != null)
                next = lastReturned;
            deleteEntry(lastReturned);
            expectedModCount++;
            lastReturned = null;
        }
    }

    static class Entry implements Map.Entry {
        Object key;
        Object value;
        Entry left = null;
        Entry right = null;
        Entry parent;

        /**
         * Make a new cell with given key, value, and parent, and with 
         * <tt>null</tt> child links.
         */
        Entry(Object key, Object value, Entry parent) {
            this.key = key;
            this.value = value;
            this.parent = parent;
        }

        /**
         * Returns the key.
         *
         * @return the key.
         */
        public Object getKey() {
            return key;
        }

        /**
         * Returns the value associated with the key.
         *
         * @return the value associated with the key.
         */
        public Object getValue() {
            return value;
        }

        /**
         * Replaces the value currently associated with the key with the given
         * value.
         *
         * @return the value associated with the key before this method was
         *           called.
         */
        public Object setValue(Object value) {
            Object oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry e = (Map.Entry) o;

            return valEquals(key, e.getKey())
                && valEquals(value, e.getValue());
        }

        public int hashCode() {
            int keyHash = (key == null ? 0 : key.hashCode());
            int valueHash = (value == null ? 0 : value.hashCode());
            return keyHash ^ valueHash;
        }

        public String toString() {
            return key + "=" + value;
        }
    }

} //class SplayTreeMap
