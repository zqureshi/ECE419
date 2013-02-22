/*
Copyright (C) 2004 Geoffrey Alan Washburn
   
This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.
   
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
   
You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
USA.
*/
  
import java.util.*;

/**
 * An implementation of the {@link SortedSet} interface that allows 
 * multiple "equivalent" objects to be stored, unlike {@link TreeSet}.
 * @author Geoffrey Washburn &lt;<a href="mailto:geoffw@cis.upenn.edu">geoffw@cis.upenn.edu</a>&gt;
 * @version $Id: SortedMultiSet.java 350 2004-01-24 05:31:17Z geoffw $
 */
public class SortedMultiSet extends LinkedList implements SortedSet {

    private Comparator comparator = null;

    public SortedMultiSet() {
    }

    public SortedMultiSet(Collection c) {
        super(c);
    }

    public SortedMultiSet(Comparator c) {
        this.comparator = c;
    }

    public SortedMultiSet(SortedSet s) {
       Iterator it = s.iterator();
        while(it.hasNext()) {
            this.add(it.next());
        }
    }

    public boolean add(Object o1) {
            if(contains(o1)) {
                    return false; 
            } else {
                    Iterator i = iterator();
                    while(i.hasNext()) {
                            Object o2 = i.next();
                            
                            int compare = 0;
                            if(comparator != null) {
                                    compare = comparator.compare(o1, o2);
                            } else {
                                    Comparable c = (Comparable)o1;
                                    compare = c.compareTo(o2);
                            }
                            if(compare <= 0) {
                                    super.add(indexOf(o2), o1);
                                    return true;
                            }
                    }
                    super.addLast(o1);
                    return true;
            }
    }

    public boolean addAll(Collection c) {
        Iterator it = c.iterator();
        int count = 0;
        while(it.hasNext()) {
            count++;
            this.add(it.next());
        }
        if(count > 0) {
            return true;
        } else {
            return false;
        }
    }

    public boolean containsAll(Collection c) {
        Iterator it = c.iterator();
        while(it.hasNext()) {
            if(!this.contains(it.next())) {
                return false;
            }
        }
        return true;
    }

    public Comparator comparator() {
        return comparator;
    }

    public Object first() {
            return getFirst();
    }

    public Object last() {
            return getLast();
    }

    public SortedSet headSet(Object toElement) {
            Iterator it = iterator();
            SortedMultiSet newSet = new SortedMultiSet();
            while(it.hasNext()) {
                    Object o = it.next();
                    int compare = 0;
                    if(comparator != null) {
                            compare = comparator.compare(o, toElement);
                    } else {
                            Comparable c = (Comparable)o;
                            compare = c.compareTo(toElement);
                    }
                    if(compare < 0) {
                            newSet.add(o);
                    }
            }
            return newSet;
    }

    public SortedSet subSet(Object fromElement, Object toElement) {
            Iterator it = iterator();
            SortedMultiSet newSet = new SortedMultiSet();
            while(it.hasNext()) {
                    Object o = it.next();
                    int compare1 = 0;
                    int compare2 = 0;
                    if(comparator != null) {
                            compare1 = comparator.compare(o, fromElement);
                            compare2 = comparator.compare(o, toElement);
                    } else {
                            Comparable c = (Comparable)o;
                            compare1 = c.compareTo(fromElement);
                            compare2 = c.compareTo(toElement);
                    }
                    if((compare1 >= 0) && (compare2 < 0)) {
                            newSet.add(o);
                    }
            }
            return newSet;
    }

    public SortedSet tailSet(Object fromElement) {
            Iterator it = iterator();
            SortedMultiSet newSet = new SortedMultiSet();
            while(it.hasNext()) {
                    Object o = it.next();
                    int compare = 0;
                    if(comparator != null) {
                            compare = comparator.compare(o, fromElement);
                    } else {
                            Comparable c = (Comparable)o;
                            compare = c.compareTo(fromElement);
                    }
                    if(compare >= 0) {
                            newSet.add(o);
                    }
            }
            return newSet;
    }

    public boolean removeAll(Collection c) {
        Iterator it = c.iterator();
        boolean result = false;
        while(it.hasNext()) {
            result |= this.remove(it.next());
        }
        return result;
    }

    public boolean retainAll(Collection c) {
        Iterator it = iterator();
        boolean result = false;
        while(it.hasNext()) {
            Object o = it.next();
            if(!c.contains(o)) {
                remove(o);
                result = true;
            }
        }
        return result;
    }

}
