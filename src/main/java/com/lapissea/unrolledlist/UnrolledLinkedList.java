package com.lapissea.unrolledlist;

import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public final class UnrolledLinkedList<T> extends AbstractList<T>{
	
	private record NodeForward<T>(UnrolledLinkedList<T>.Node start) implements Iterable<UnrolledLinkedList<T>.Node>{
		@Override
		public Iterator<UnrolledLinkedList<T>.Node> iterator(){
			return new Iterator<>(){
				private UnrolledLinkedList<T>.Node node = start;
				@Override
				public boolean hasNext(){
					return node != null;
				}
				@Override
				public UnrolledLinkedList<T>.Node next(){
					var n = node;
					node = n.next;
					return n;
				}
			};
		}
	}
	
	private sealed class UnrolledIterator implements Iterator<T>{
		
		protected Node node, nodeLastRet;
		protected int pos, lastRet;
		
		public UnrolledIterator(int start){
			toGlobalPos(start);
		}
		
		protected void toGlobalPos(int start){
			var res = nodeWalk(start);
			node = res.node;
			pos = res.localPos;
		}
		
		@Override
		public boolean hasNext(){
			return node != null;
		}
		
		@Override
		public T next(){
			T val = (nodeLastRet = node).get(lastRet = pos);
			pos++;
			fixPos();
			return val;
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
		private int globalPos;
		
		UnrolledListIterator(int index){
			super(index);
			globalPos = index;
		}
		
		public boolean hasPrevious(){
			return pos>0 || (node != null && node.prev != null);
		}
		
		@Override
		public T next(){
			if(!hasNext()) throw new NoSuchElementException();
			globalPos++;
			return super.next();
		}
		@Override
		public void remove(){
			if(node == nodeLastRet){
				globalPos += lastRet - pos;
			}else{
				globalPos = lastRet;
				var n = head;
				while(n != nodeLastRet){
					globalPos += n.size;
					n = n.next;
				}
			}
			
			super.remove();
		}
		
		public T previous(){
			if(!hasPrevious()) throw new NoSuchElementException();
			globalPos--;
			int i = pos - 1;
			if(i == -1){
				var prev = node.prev;
				return (nodeLastRet = node = prev).get(lastRet = pos = prev.size - 1);
			}else{
				return (nodeLastRet = node).get(lastRet = pos = i);
			}
		}
		
		public int nextIndex(){
			return globalPos;
		}
		
		public int previousIndex(){
			return globalPos - 1;
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
			toGlobalPos(++globalPos);
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
			Objects.checkIndex(localPos, size);
			return (T)arr[start + localPos];
		}
		@SuppressWarnings("unchecked")
		private T getLast(){
			if(size == 0) throw new IndexOutOfBoundsException();
			return (T)arr[start + size - 1];
		}
		
		@SuppressWarnings("unchecked")
		private T set(int localPos, T value){
			var truePos = start + localPos;
			var old     = (T)arr[truePos];
			arr[truePos] = value;
			return old;
		}
		
		private void add(int localPos, T element){
			var lSize = size;
			Objects.checkIndex(localPos, lSize + 1);
			var lStart = start;
			
			//Insert and consume start space
			if(lStart>0 && localPos<size/2){
				var lStartM1 = lStart - 1;
				if(localPos>0){
					System.arraycopy(arr, lStart, arr, lStartM1, localPos);
				}
				arr[lStartM1 + localPos] = element;
				start = lStartM1;
				size = lSize + 1;
				return;
			}
			
			if(lSize == arr.length){
				//Totally full, try pushing in to neighbour (position threshold is biased towards prev to passively pack data)
				if(localPos<=lSize/2){
					if(addByPrevTransfer(localPos, element)) return;
				}
				if(localPos>=lSize*3/4){
					if(addByNextTransfer(localPos, element)) return;
				}
				//Totally full and expand node space by allocating and splitting data
				if(expand(localPos)){
					next.add(localPos - size, element);
					return;
				}
				lSize = size;
			}
			
			var truePos  = lStart + localPos;
			var trueSize = lStart + lSize;
			
			//End space is full but there is space at start, move to 0
			if(lStart>0 && trueSize>=arr.length){
				System.arraycopy(arr, lStart, arr, 0, lSize);
				zeroRange(Math.max(lSize + 1, lStart), trueSize);
				start = 0;
				trueSize = lSize;
				truePos = localPos;
			}
			
			if(trueSize>truePos){
				//System.arraycopy is for some reason slower?
				for(int i = trueSize - 1; i>=truePos; i--) arr[i + 1] = arr[i];
//				System.arraycopy(arr, truePos, arr, truePos + 1, trueSize - truePos);
			}
			arr[truePos] = element;
			size = lSize + 1;
		}
		
		private boolean addByNextTransfer(int localPos, T element){
			var lNext = next;
			if(lNext == null || lNext.full()) return false;
			
			var lSize     = size;
			var nextStart = lNext.start;
			var toMove    = lSize - localPos;
			
			if(nextStart<toMove) return false;
			if(localPos == lSize){
				lNext.add(0, element);
				return true;
			}
			
			var trueSize = start + lSize;
			System.arraycopy(arr, trueSize - toMove, lNext.arr, nextStart - toMove, toMove);
			arr[localPos] = element;
			zeroRange(localPos + 1, trueSize);
			size -= toMove - 1;
			lNext.start -= toMove;
			lNext.size += toMove;
			return true;
		}
		
		private boolean addByPrevTransfer(int localPos, T element){
			var lPrev = prev;
			if(lPrev == null) return false;
			
			var prevEnd = lPrev.size + lPrev.start;
			
			if(prevEnd + localPos>lPrev.arr.length) return false;
			if(localPos == 0){
				lPrev.add(lPrev.size, element);
				return true;
			}
			
			System.arraycopy(arr, 0, lPrev.arr, prevEnd, localPos);
			var prev = localPos - 1;
			arr[prev] = element;
			zeroRange(0, prev);
			size -= prev;
			start += prev;
			lPrev.size += localPos;
			return true;
		}
		
		private void zeroRange(int from, int to){
			for(int i = from; i<to; i++) arr[i] = null;
		}
		
		private boolean expand(int localPos){
			int s;
			if((s = size) != arr.length) return false;
			
			var n = optimalNext();
			
			int copyPos   = 0;
			var available = n.arr.length - (n.start + n.size);
			
			var amount = Math.min(s/4, Math.max(1, available - 1));
			if(n.size>0){
				var toMove = Math.max(0, amount - n.start);
				if(toMove>0){
					System.arraycopy(n.arr, n.start, n.arr, n.start + toMove, n.size);
				}
				copyPos = n.start + toMove - amount;
			}
			var trueSize = start + s;
			System.arraycopy(arr, trueSize - amount, n.arr, copyPos, amount);
			zeroRange(trueSize - amount, trueSize);
			
			n.start = copyPos;
			n.size += amount;
			size = s -= amount;
			
			return localPos>s;
		}
		
		private boolean full(){
			return size>=arr.length*3/4;
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
			var s = size;
			Objects.checkIndex(localPos, s);
			int newSize = s - 1;
			if(localPos<s/2){
				if(localPos>0){
					System.arraycopy(arr, start, arr, start + 1, localPos);
				}
				arr[start] = null;
				start++;
				size = newSize;
				if(newSize == 0) start = arr.length/2;
			}else{
				if(newSize>localPos){
					int i = start + localPos;
					System.arraycopy(arr, i + 1, arr, i, start + newSize - i);
				}
				arr[start + (size = newSize)] = null;
			}
			
			if(newSize<arr.length/2){
				if(newSize == 0){
					removeSelf();
					if(next == null){
						if(prev == null) return null;
						return new StructureChange<>(prev, prev.size);
					}
					return new StructureChange<>(next, 0);
				}
				return defrag();
			}
			
			return null;
		}
		
		private StructureChange<T> defrag(){
			int siz   = size;
			var lPrev = prev;
			var lNext = next;
			
			
			if(lPrev != null){
				int off;
				if(siz + (off = lPrev.size + lPrev.start)<=arr.length){
					System.arraycopy(arr, start, lPrev.arr, off, siz);
					var olSiz = lPrev.size;
					lPrev.size += siz;
					removeSelf();
					return new StructureChange<>(lPrev, olSiz);
				}
			}
			if(lNext != null){
				int off;
				if((off = siz + start) + lNext.size<=arr.length){
					System.arraycopy(lNext.arr, lNext.start, arr, off, lNext.size);
					size += lNext.size;
					lNext.removeSelf();
					return new StructureChange<>(this, 0);
				}
			}
			
			if(lPrev != null && lNext != null){
				var prevEnd   = lPrev.size + lPrev.start;
				var nextStart = lNext.start;
				
				var prevToAdd = lPrev.arr.length - prevEnd;
				var nextToAdd = nextStart;
				
				var remaining = siz - prevToAdd - nextToAdd;
				if(remaining>0) return null;
				if(remaining<0){
					nextToAdd += remaining;
				}
				if(nextToAdd<=0) throw new IllegalStateException();
				
				
				var olSiz = lPrev.size;
				
				System.arraycopy(arr, start, lPrev.arr, prevEnd, prevToAdd);
				lPrev.size += prevToAdd;
				
				System.arraycopy(arr, start + prevToAdd, lNext.arr, nextStart - nextToAdd, nextToAdd);
				lNext.start -= nextToAdd;
				lNext.size += nextToAdd;
				
				removeSelf();
				return new StructureChange<>(lPrev, olSiz);
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
		@Override
		public String toString(){
			var res = new StringJoiner(", ", "{size=" + size + ", start=" + start + "}[", "]");
			for(int i = 0; i<size; i++){
				res.add(Objects.toString(get(i)));
			}
			return res.toString();
		}
		public void sort(Comparator<? super T> c){
			if(size>1){
				//noinspection unchecked
				Arrays.sort(arr, start, start + size, (Comparator<? super Object>)c);
			}
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
		
		for(var node : new NodeForward<>(head)){
			var part = new StringJoiner(", ");
			for(int i = 0; i<node.size; i++){
				part.add(Objects.toString(node.get(i)));
			}
			rest.add(part.toString());
//			rest.add((node.arr.length - node.size) + "");
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
		var pos  = 0;
		for(var node : new NodeForward<>(head)){
			System.arraycopy(node.arr, node.start, r, pos, node.size);
			pos += node.size;
		}
		return pos;
	}
	
	@Override
	public void replaceAll(UnaryOperator<T> operator){
		Objects.requireNonNull(operator);
		for(var node : new NodeForward<>(head)){
			var arr = node.arr;
			for(int i = node.start, j = i + node.size; i<j; i++){
				//noinspection unchecked
				arr[i] = operator.apply((T)arr[i]);
			}
		}
	}
	
	
	@Override
	public void sort(Comparator<? super T> c){
		final class SortChunk<E>{
			private final UnrolledLinkedList<E>.Node start;
			private       E                          last;
			private       int                        size;
			
			private ArrayDeque<E>              buffer;
			private UnrolledLinkedList<E>.Node current;
			private int                        pos, readPos;
			
			
			private SortChunk(UnrolledLinkedList<E>.Node node){
				this.start = node;
				last = node.getLast();
				size = node.size;
			}
			
			private static <E> void merge(Comparator<? super E> c, ArrayDeque<E> buffA, ArrayDeque<E> buffB, SortChunk<E> dest, SortChunk<E> l, SortChunk<E> r){
				var ls = l.size;
				var rs = r.size;
				dest.size = ls + rs;
				
				l.buffer = buffA;
				r.buffer = buffB;
				
				l.start();
				r.start();
				dest.start();
				
				for(int i = 0, j = dest.size; i<j; i++){
					E       val;
					boolean hasL = l.has(), hasR = r.has();
					if(hasL && hasR){
						var lv = l.next();
						var rv = r.next();
						if(c.compare(lv, rv)<0){
							val = lv;
							r.buffer.addFirst(rv);
						}else{
							val = rv;
							l.buffer.addFirst(lv);
						}
					}else{
						val = hasL? l.next() : r.next();
					}
					if(hasL && l.readPos<=dest.readPos && l.readPos<l.size) l.buffer.addLast(l.read());
					dest.add(val);
				}
			}
			
			private E next(){
				if(buffer.isEmpty()){
					return read();
				}
				return buffer.removeFirst();
			}
			private E read(){
				var pos = advance();
				return current.get(pos);
			}
			private void add(E val){
				var pos = advance();
				current.set(pos, val);
			}
			private void start(){
				current = start;
				pos = -1;
				readPos = 0;
			}
			private int advance(){
				if(readPos == size) throw new IllegalStateException();
				pos++;
				if(pos == current.size){
					current = current.next;
					pos = 0;
				}
				readPos++;
				return pos;
			}
			private boolean has(){
				return !buffer.isEmpty() || readPos<size;
			}
		}
		
		
		if(head == null) return;
		if(head.next == null){
			head.sort(c);
			return;
		}
		
		//noinspection unchecked
		var chunks = (SortChunk<T>[])new SortChunk[(int)(size/(double)rollSize*1.5 + 4)];
		int cPos   = 0;
		
		for(var node : new NodeForward<>(head)){
			node.sort(c);
			if(cPos != 0){
				var ch = chunks[cPos - 1];
				if(c.compare(node.get(0), ch.last)>=0){
					ch.size += node.size;
					ch.last = node.getLast();
					continue;
				}
			}
			
			if(chunks.length == cPos){
				int count = 0;
				for(var __ : new NodeForward<>(node)){
					count++;
				}
				chunks = Arrays.copyOf(chunks, chunks.length + count);
			}
			chunks[cPos++] = new SortChunk<>(node);
		}
		
		
		ArrayDeque<T> buffA = new ArrayDeque<>(), buffB = new ArrayDeque<>(1);
		
		int       inc = 1;
		final int s   = cPos;
		while(inc<s){
			for(int i = 0, step = inc*2; i + inc<s; i += step){
				SortChunk<T> l = chunks[i], r = chunks[i + inc];
				SortChunk.merge(c, buffA, buffB, new SortChunk<>(l.start), l, r);
				l.size += r.size;
			}
			inc *= 2;
		}
	}
}
