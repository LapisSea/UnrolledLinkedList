package com.lapissea.unrolledlist;

import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public final class UnrolledLinkedList<T> extends AbstractList<T>{
	
	private sealed class UnrolledIterator implements Iterator<T>{
		
		protected Node node, nodeLastRet;
		protected int pos, lastRet;
		
		public UnrolledIterator(int start){
			var res = nodeWalk(start);
			node = res.node;
			pos = res.localPos;
			if(pos == 0 && node != null && node.size == 0){
				node = null;
			}
		}
		
		@Override
		public boolean hasNext(){
			return node != null;
		}
		@Override
		public T next(){
			checkNode(node);
			T val = (nodeLastRet = node).get(lastRet = pos);
			pos++;
			fixPos();
			return val;
		}
		
		private void checkNode(Node node){
			var n = head;
			if(head == null) return;
			while(n != null){
				if(n == node) return;
				n = n.next;
			}
			throw new IllegalStateException("node not in list");
		}
		
		protected void fixPos(){
			while(node != null && pos>=node.size){
				pos -= node.size;
				node = node.next;
			}
		}
		
		@Override
		public void remove(){
			if(nodeLastRet == null) throw new IllegalStateException();
			checkNode(nodeLastRet);
			var res = nodeLastRet.remove(lastRet);
			if(res != null){
				node = res.newNode;
				pos = lastRet + res.delta;
			}else{
				if(node == nodeLastRet && lastRet<pos) pos--;
			}
			fixPos();
			size--;
			
			nodeLastRet = null;
		}
	}
	
	private final class UnrolledListIterator extends UnrolledIterator implements ListIterator<T>{
		UnrolledListIterator(int index){
			super(index);
		}
		
		public boolean hasPrevious(){
			return pos>0 || (node != null && node.prev != null);
		}
		
		public T previous(){
			int i = pos - 1;
			if(i == -1){
				var prev = node.prev;
				return (nodeLastRet = node = prev).get(lastRet = pos = prev.size - 1);
			}else{
				return (nodeLastRet = node).get(lastRet = pos = i);
			}
		}
		
		public int nextIndex(){
			return pos;
		}
		
		public int previousIndex(){
			return pos - 1;
		}
		
		public void set(T e){
			if(nodeLastRet == null) throw new IllegalStateException();
			nodeLastRet.set(lastRet, e);
		}
		
		public void add(T e){
			int i = pos;
			node.add(i, e);
			size++;
			nodeLastRet = null;
			pos = i + 1;
			fixPos();
		}
	}
	
	private static final class UnrolledSpliterator<E> implements Spliterator<E>{
		
		private final UnrolledLinkedList<E> list;
		
		private int index; // current index, modified on advance/split
		private int fence; // -1 until used; then one past last index
		
		private UnrolledLinkedList<E>.Node node;
		private int                        localIndex;
		
		
		private UnrolledSpliterator(UnrolledLinkedList<E> list){
			this.list = list;
			this.index = 0;
			this.fence = -1;
		}
		
		/**
		 * Create new spliterator covering the given  range
		 */
		private UnrolledSpliterator(UnrolledSpliterator<E> parent, int origin, int fence){
			this.list = parent.list;
			this.index = origin;
			this.fence = fence;
			loadNode();
		}
		
		private int getFence(){ // initialize fence to size on first use
			int hi;
			if((hi = fence)<0){
				hi = fence = list.size();
				loadNode();
			}
			return hi;
		}
		
		@Override
		public Spliterator<E> trySplit(){
			int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
			return (lo>=mid)? null : // divide range in half unless too small
			       new UnrolledSpliterator<>(this, lo, index = mid);
		}
		
		@Override
		public boolean tryAdvance(Consumer<? super E> action){
			if(action == null) throw new NullPointerException();
			int hi = getFence(), i = index;
			if(i<hi){
				index = i + 1;
				action.accept(node.get(localIndex++));
				fixPos();
				return true;
			}
			return false;
		}
		
		@Override
		public void forEachRemaining(Consumer<? super E> action){
			Objects.requireNonNull(action);
			int hi = getFence();
			int i  = index;
			index = hi;
			for(; i<hi; i++){
				action.accept(node.get(localIndex++));
				fixPos();
			}
		}
		
		@Override
		public long estimateSize(){
			return getFence() - index;
		}
		
		@Override
		public int characteristics(){
			return Spliterator.ORDERED|Spliterator.SIZED|Spliterator.SUBSIZED;
		}
		
		@Override
		public long getExactSizeIfKnown(){
			return estimateSize();
		}
		
		private void fixPos(){
			while(node != null && localIndex>=node.size){
				node = node.next;
				localIndex = 0;
			}
		}
		private void loadNode(){
			var res = list.nodeWalk(index);
			node = res.node;
			localIndex = res.localPos;
		}
	}
	
	private record StructureChange<T>(UnrolledLinkedList<T>.Node newNode, int delta){ }
	
	private record NodeResult<T>(UnrolledLinkedList<T>.Node node, int localPos){
		private static final NodeResult<?> EMPTY = new NodeResult<>(null, 0);
	}
	
	private final class Node{
		private final Object[] arr;
		private       int      start;
		private       int      size;
		
		private Node next;
		private Node prev;
		
		public Node(Object[] arr){
			this.arr = arr;
		}
		
		@SuppressWarnings("unchecked")
		private T get(int localPos){
			return (T)arr[start + localPos];
		}
		@SuppressWarnings("unchecked")
		private T set(int localPos, T value){
			var truePos = start + localPos;
			var old     = (T)arr[truePos];
			arr[truePos] = value;
			return old;
		}
		
		private void add(int localPos, T element){
			Objects.checkIndex(localPos, size + 1);
			
			if(start>0 && localPos == 0){
				arr[start--] = element;
				size++;
				return;
			}
			
			var truePos = start + localPos;
			if(expand(localPos)){
				next.add(localPos - size, element);
				return;
			}
			var s = size;
			if(start + s>truePos){
				//System.arraycopy is for some reason slower?
				for(int i = s - 1; i>=truePos; i--) arr[i + 1] = arr[i];
//				System.arraycopy(
//					arr, truePos,
//					arr, truePos + 1,
//					s - truePos
//				);
			}
			arr[truePos] = element;
			size = s + 1;
		}
		
		private boolean expand(int localPos){
			int s;
			if((s = size) != arr.length) return false;
			
			var n = optimalNext();
			
			int copyPos   = 0;
			var available = n.arr.length - (n.start + n.size);
			
			var amount = Math.min(s/2, Math.max(1, available - 1));
			if(n.size>0){
				var toMove = Math.max(0, amount - n.start);
				System.arraycopy(n.arr, n.start, n.arr, n.start + toMove, n.size);
				copyPos = n.start + toMove - amount;
			}
			
			System.arraycopy(arr, s - amount, n.arr, copyPos, amount);
			for(int i = s - amount; i<s; i++) arr[i] = null;
			
			n.start = copyPos;
			n.size += amount;
			size = s -= amount;
			
			return localPos>s;
		}
		
		private boolean full(){
			return size>=arr.length - arr.length/4;
		}
		private Node optimalNext(){
			if(next == null || next.full()){
				insertNext();
			}
			return next;
		}
		
		private void insertNext(){
			var delta1 = new Node(new Object[rollSize]);
			delta1.prev = this;
			
			if(next != null){
				var delta2 = next;
				delta2.prev = delta1;
				delta1.next = delta2;
			}else tail = delta1;
			next = delta1;
		}
		
		private StructureChange<T> remove(int localPos){
			Objects.checkIndex(localPos, size);
			int i       = start + localPos;
			int newSize = size - 1;
			if(start + newSize>i){
				System.arraycopy(arr, i + 1, arr, i, start + newSize - i);
			}
			arr[size = newSize] = null;
			
			if(newSize<arr.length/2){
				if(newSize == 0){
					removeSelf();
					if(next == null){
						return prev == null? null : new StructureChange<>(prev, prev.size);
					}
					return new StructureChange<>(next, 0);
				}
				return defrag();
			}
			
			return null;
		}
		
		private StructureChange<T> defrag(){
			int siz = size;
			if(prev != null){
				int off;
				if(siz + (off = prev.size + prev.start)<arr.length){
					System.arraycopy(arr, start, prev.arr, off, siz);
					var olSiz = prev.size;
					prev.size += siz;
					removeSelf();
					return new StructureChange<>(prev, olSiz);
				}
			}
			if(next != null){
				int off;
				if((off = siz + start) + next.size<arr.length){
					System.arraycopy(next.arr, next.start, arr, off, next.size);
					size += next.size;
					next.removeSelf();
					return new StructureChange<>(this, 0);
				}
			}
			return null;
		}
		
		private void removeSelf(){
			if(prev != null) prev.next = next;
			else head = next;
			if(next != null) next.prev = prev;
			else tail = prev;
			size = -1;
		}
	}
	
	private       int size;
	private final int rollSize;
	
	private Node head, tail;
	
	public UnrolledLinkedList(){
		this(16);
	}
	public UnrolledLinkedList(int rollSize){
		this.rollSize = rollSize;
	}
	
	@Override
	public T get(int index){
		Objects.checkIndex(index, size);
		var res = nodeWalk(index);
		return res.node.get(res.localPos);
	}
	
	@Override
	public boolean add(T t){
		if(tail == null) makeFirst();
		var res = nodeWalk(size);
		res.node.add(res.localPos, t);
		size++;
		return true;
	}
	
	@Override
	public void add(int index, T element){
		Objects.checkIndex(index, size + 1);
		var res = nodeWalk(index);
		res.node.add(res.localPos, element);
		size++;
	}
	
	@Override
	public T remove(int index){
		Objects.checkIndex(index, size);
		var res      = nodeWalk(index);
		var node     = res.node;
		var localPos = res.localPos;
		
		var old = node.get(localPos);
		node.remove(localPos);
		size--;
		return old;
	}
	
	@Override
	public T set(int index, T element){
		Objects.checkIndex(index, size);
		var res = nodeWalk(index);
		return res.node.set(res.localPos, element);
	}
	
	@Override
	public void clear(){
		size = 0;
		tail = head = null;
	}
	
	private void makeFirst(){
		tail = head = new Node(new Object[rollSize]);
	}
	
	@Override
	public int size(){
		return size;
	}
	
	private NodeResult<T> nodeWalk(int offset){
		if(head == null){
			//noinspection unchecked
			return (NodeResult<T>)NodeResult.EMPTY;
		}
		if(offset>size>>1){
			return getBackwardsNode(offset);
		}
		return getForwardsNode(offset);
	}
	
	private NodeResult<T> getForwardsNode(int offset){
		var node      = head;
		var remaining = offset;
		while(remaining>=node.size){
			var next = node.next;
			if(next == null) break;
			remaining -= node.size;
			node = next;
		}
		return new NodeResult<>(node, remaining);
	}
	
	private NodeResult<T> getBackwardsNode(int offset){
		var node      = tail;
		var remaining = size - offset;
		while(remaining>node.size){
			var prev = node.prev;
			if(prev == null) break;
			remaining -= node.size;
			node = prev;
		}
		return new NodeResult<>(node, node.size - remaining);
	}
	
	@Override
	public Iterator<T> iterator(){ return new UnrolledIterator(0); }
	@Override
	public ListIterator<T> listIterator(int index){ return new UnrolledListIterator(index); }
	@Override
	public Spliterator<T> spliterator(){ return new UnrolledSpliterator<>(this); }
	
	@Override
	public String toString(){
		var rest = new StringJoiner(" - ", "[", "]");
		
		var node = head;
		while(node != null){
			var part = new StringJoiner(",");
			for(int i = 0; i<node.size; i++){
				part.add(Objects.toString(node.get(i)));
			}
			rest.add(part.toString());
//			rest.add(node.size + "");
			node = node.next;
		}
		
		return rest.toString();
	}
	
	@Override
	public Object[] toArray(){
		var r    = new Object[size()];
		int size = copyIntoArray(r);
		assert size == r.length;
		return r;
	}
	
	@Override
	public <T1> T1[] toArray(T1[] a){
		@SuppressWarnings("unchecked")
		var r = a.length>=size? a : (T1[])Array.newInstance(a.getClass().getComponentType(), size);
		int size = copyIntoArray(r);
		for(int i = size; i<r.length; i++) r[i] = null;
		return r;
	}
	
	private int copyIntoArray(Object[] r){
		var node = head;
		var pos  = 0;
		while(node != null){
			System.arraycopy(node.arr, node.start, r, pos, node.size);
			pos += node.size;
			node = node.next;
		}
		return pos;
	}
	
	@Override
	public void replaceAll(UnaryOperator<T> operator){
		Objects.requireNonNull(operator);
		var node = head;
		while(node != null){
			var arr = node.arr;
			for(int i = node.start, j = i + node.size; i<j; i++){
				//noinspection unchecked
				arr[i] = operator.apply((T)arr[i]);
			}
			node = node.next;
		}
	}
	
}
