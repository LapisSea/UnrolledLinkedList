package com.lapissea.unrolledlist;

import com.lapissea.util.LogUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class CheckList<T> implements List<T>{
	
	private final List<T> a, b;
	public CheckList(List<T> a, List<T> b){
		this.a = a;
		this.b = b;
		listEquality();
	}
	
	private void listEquality(){
		try{
			if(!a.equals(b)){
				assertEquals(a.size(), b.size(), "Lists are not of equal length");
				
				List<String> lines = new ArrayList<>();
				lines.add(b.stream().map(Objects::toString).collect(Collectors.joining(", ", "Expected list: [", "]")));
				lines.add(a.stream().map(Objects::toString).collect(Collectors.joining(", ", "Actual list:   [", "]")));
				
				var ai = a.iterator();
				var bi = a.iterator();
				int i  = 0;
				while(ai.hasNext()){
					i++;
					var av = ai.next();
					var bv = bi.next();
					if(Objects.equals(av, bv)) continue;
					
					lines.add("\tExpected \"" + bv + "\" at " + (i - 1) + " but got \"" + av + "\"");
					if(lines.size()>=15 && ai.hasNext()){
						lines.add("\tAnd possibly more...");
						break;
					}
				}
				
				fail(String.join("\n", lines));
			}
		}catch(Throwable e){
			LogUtil.println(a);
			LogUtil.println(b);
			throw e;
		}
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
				listEquality();
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
		var a2 = a1.clone();
		var va = a.toArray(a1);
		var vb = b.toArray(a2);
		assertEquals(va, vb);
		return va;
	}
	@Override
	public boolean add(T t){
		var va = a.add(t);
		var vb = b.add(t);
		assertEquals(va, vb);
		listEquality();
		return va;
	}
	@Override
	public boolean remove(Object o){
		var va = a.remove(o);
		var vb = b.remove(o);
		assertEquals(va, vb);
		listEquality();
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
		listEquality();
		return va;
	}
	@Override
	public boolean addAll(int index, Collection<? extends T> c){
		var va = a.addAll(index, c);
		var vb = b.addAll(index, c);
		assertEquals(va, vb);
		listEquality();
		return va;
	}
	@Override
	public boolean removeAll(Collection<?> c){
		var va = a.removeAll(c);
		var vb = b.removeAll(c);
		assertEquals(va, vb);
		listEquality();
		return va;
	}
	@Override
	public boolean retainAll(Collection<?> c){
		var va = a.retainAll(c);
		var vb = b.retainAll(c);
		assertEquals(va, vb);
		listEquality();
		return va;
	}
	@Override
	public void clear(){
		a.clear();
		b.clear();
		listEquality();
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
		listEquality();
		return va;
	}
	@Override
	public void add(int index, T element){
		a.add(index, element);
		b.add(index, element);
		listEquality();
	}
	@Override
	public T remove(int index){
		var va = a.remove(index);
		var vb = b.remove(index);
		assertEquals(va, vb);
		listEquality();
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
		return new ListIterator<>(){
			private final ListIterator<T> iterA = a.listIterator(index);
			private final ListIterator<T> iterB = b.listIterator(index);
			
			private void iterCheck(){
				assertEquals(iterA.previousIndex(), iterB.previousIndex(), "previousIndex mismatch:");
				assertEquals(iterA.nextIndex(), iterB.nextIndex(), "nextIndex mismatch:");
			}
			
			@Override
			public boolean hasNext(){
				var va = iterA.hasNext();
				var vb = iterB.hasNext();
				assertEquals(va, vb);
				iterCheck();
				return va;
			}
			@Override
			public T next(){
				var va = iterA.next();
				var vb = iterB.next();
				iterCheck();
				assertEquals(va, vb, "pos a/b: " + iterA.previousIndex() + "/" + iterB.previousIndex());
				return va;
			}
			
			@Override
			public boolean hasPrevious(){
				var va = iterA.hasPrevious();
				var vb = iterB.hasPrevious();
				assertEquals(va, vb);
				iterCheck();
				return va;
			}
			@Override
			public T previous(){
				var va = iterA.previous();
				var vb = iterB.previous();
				iterCheck();
				assertEquals(va, vb);
				return va;
			}
			@Override
			public int nextIndex(){
				iterCheck();
				return iterA.nextIndex();
			}
			@Override
			public int previousIndex(){
				iterCheck();
				return iterA.previousIndex();
			}
			@Override
			public void remove(){
				iterA.remove();
				iterB.remove();
				listEquality();
				iterCheck();
			}
			@Override
			public void set(T t){
				iterA.set(t);
				iterB.set(t);
				listEquality();
				iterCheck();
			}
			@Override
			public void add(T t){
				iterA.add(t);
				iterB.add(t);
				listEquality();
				iterCheck();
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
		if(a.equals(b)) return a.toString();
		return a + " != " + b;
	}
	
	@Override
	public void replaceAll(UnaryOperator<T> operator){
		a.replaceAll(operator);
		b.replaceAll(operator);
		listEquality();
	}
	@Override
	public void sort(Comparator<? super T> c){
		a.sort(c);
		b.sort(c);
		listEquality();
	}
	@Override
	public Spliterator<T> spliterator(){
		return a.spliterator();
	}
	@Override
	public boolean removeIf(Predicate<? super T> filter){
		var va = a.removeIf(filter);
		var vb = b.removeIf(filter);
		listEquality();
		assertEquals(va, vb);
		return va;
	}
	@Override
	public Stream<T> stream(){
		return a.stream();
	}
	@Override
	public Stream<T> parallelStream(){
		return a.parallelStream();
	}
}
