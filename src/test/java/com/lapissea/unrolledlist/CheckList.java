package com.lapissea.unrolledlist;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import static org.testng.Assert.assertEquals;

public class CheckList<T> implements List<T>{
	
	private final List<T> a, b;
	public CheckList(List<T> a, List<T> b){
		this.a = a;
		this.b = b;
	}
	
	@Override
	public int size(){
		var va = a.size();
		var vb = b.size();
		assertEquals(va, vb);
		return va;
	}
	@Override
	public boolean isEmpty(){
		var va = a.isEmpty();
		var vb = b.isEmpty();
		assertEquals(va, vb);
		return va;
	}
	@Override
	public boolean contains(Object o){
		var va = a.contains(o);
		var vb = b.contains(o);
		assertEquals(va, vb);
		return va;
	}
	@Override
	public Iterator<T> iterator(){
		return new Iterator<>(){
			private final Iterator<T> iterA = a.iterator();
			private final Iterator<T> iterB = b.iterator();
			
			@Override
			public boolean hasNext(){
				var va = iterA.hasNext();
				var vb = iterB.hasNext();
				assertEquals(va, vb);
				return va;
			}
			@Override
			public T next(){
				var va = iterA.next();
				var vb = iterB.next();
				assertEquals(va, vb);
				return va;
			}
			@Override
			public void remove(){
				iterA.remove();
				iterB.remove();
				assertEquals(a, b);
			}
		};
	}
	
	@Override
	public Object[] toArray(){
		var va = a.toArray();
		var vb = b.toArray();
		assertEquals(va, vb);
		return va;
	}
	@Override
	public <T1> T1[] toArray(T1[] a1){
		var va = a.toArray(a1);
		var vb = b.toArray(a1);
		assertEquals(va, vb);
		return va;
	}
	@Override
	public boolean add(T t){
		var va = a.add(t);
		var vb = b.add(t);
		assertEquals(va, vb);
		assertEquals(a, b);
		return va;
	}
	@Override
	public boolean remove(Object o){
		var va = a.remove(o);
		var vb = b.remove(o);
		assertEquals(va, vb);
		assertEquals(a, b);
		return va;
	}
	@Override
	public boolean containsAll(Collection<?> c){
		var va = a.containsAll(c);
		var vb = b.containsAll(c);
		assertEquals(va, vb);
		return va;
	}
	@Override
	public boolean addAll(Collection<? extends T> c){
		var va = a.addAll(c);
		var vb = b.addAll(c);
		assertEquals(va, vb);
		assertEquals(a, b);
		return va;
	}
	@Override
	public boolean addAll(int index, Collection<? extends T> c){
		var va = a.addAll(index, c);
		var vb = b.addAll(index, c);
		assertEquals(va, vb);
		assertEquals(a, b);
		return va;
	}
	@Override
	public boolean removeAll(Collection<?> c){
		var va = a.removeAll(c);
		var vb = b.removeAll(c);
		assertEquals(va, vb);
		assertEquals(a, b);
		return va;
	}
	@Override
	public boolean retainAll(Collection<?> c){
		var va = a.retainAll(c);
		var vb = b.retainAll(c);
		assertEquals(va, vb);
		assertEquals(a, b);
		return va;
	}
	@Override
	public void clear(){
		a.clear();
		b.clear();
		assertEquals(a, b);
	}
	@Override
	public T get(int index){
		var va = a.get(index);
		var vb = b.get(index);
		assertEquals(va, vb);
		return va;
	}
	@Override
	public T set(int index, T element){
		var va = a.set(index, element);
		var vb = b.set(index, element);
		assertEquals(va, vb);
		assertEquals(a, b);
		return va;
	}
	@Override
	public void add(int index, T element){
		a.add(index, element);
		b.add(index, element);
		assertEquals(a, b);
	}
	@Override
	public T remove(int index){
		var va = a.remove(index);
		var vb = b.remove(index);
		assertEquals(va, vb);
		assertEquals(a, b);
		return va;
	}
	@Override
	public int indexOf(Object o){
		var va = a.indexOf(o);
		var vb = b.indexOf(o);
		assertEquals(va, vb);
		return va;
	}
	@Override
	public int lastIndexOf(Object o){
		var va = a.lastIndexOf(o);
		var vb = b.lastIndexOf(o);
		assertEquals(va, vb);
		return va;
	}
	@Override
	public ListIterator<T> listIterator(){
		return listIterator(0);
	}
	
	@Override
	public ListIterator<T> listIterator(int index){
		return new ListIterator<T>(){
			private final ListIterator<T> iterA = a.listIterator(index);
			private final ListIterator<T> iterB = b.listIterator(index);
			@Override
			public boolean hasNext(){
				var va = iterA.hasNext();
				var vb = iterB.hasNext();
				assertEquals(va, vb);
				return va;
			}
			@Override
			public T next(){
				var va = iterA.next();
				var vb = iterB.next();
				assertEquals(va, vb);
				return va;
			}
			@Override
			public boolean hasPrevious(){
				var va = iterA.hasPrevious();
				var vb = iterB.hasPrevious();
				assertEquals(va, vb);
				return va;
			}
			@Override
			public T previous(){
				var va = iterA.previous();
				var vb = iterB.previous();
				assertEquals(va, vb);
				return va;
			}
			@Override
			public int nextIndex(){
				var va = iterA.nextIndex();
				var vb = iterB.nextIndex();
				assertEquals(va, vb);
				return va;
			}
			@Override
			public int previousIndex(){
				var va = iterA.previousIndex();
				var vb = iterB.previousIndex();
				assertEquals(va, vb);
				return va;
			}
			@Override
			public void remove(){
				iterA.remove();
				iterB.remove();
				assertEquals(a, b);
			}
			@Override
			public void set(T t){
				iterA.set(t);
				iterB.set(t);
				assertEquals(a, b);
			}
			@Override
			public void add(T t){
				iterA.add(t);
				iterB.add(t);
				assertEquals(a, b);
			}
		};
	}
	
	@Override
	public List<T> subList(int fromIndex, int toIndex){
		var va = a.subList(fromIndex, toIndex);
		var vb = b.subList(fromIndex, toIndex);
		assertEquals(va, vb);
		return va;
	}
	@Override
	public String toString(){
		return a.toString();
	}
}
