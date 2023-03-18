package com.lapissea.unrolledlist;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.Objects;
import java.util.StringJoiner;

public final class UnrolledLinkedList<T> extends AbstractList<T>{
	
	private final class UnrolledItertor implements Iterator<T>{
		
		private Node<T> node, nodeLastRet;
		private int pos, lastRet;
		
		public UnrolledItertor(){
			node = head;
			while(node != null && node.size == 0) node = node.next;
		}
		
		@Override
		public boolean hasNext(){
			return node != null;
		}
		@Override
		public T next(){
			var val = (nodeLastRet = node).get(lastRet = pos);
			pos++;
			while(node != null && pos>=node.size){
				node = node.next;
				pos = 0;
			}
			return val;
		}
		@Override
		public void remove(){
			if(lastRet<0) throw new IllegalStateException();
			size--;
			nodeLastRet.remove(lastRet);
			if(lastRet<pos) pos--;
			lastRet = -1;
		}
	}
	
	private record NodeResult<T>(Node<T> node, int localPos){ }
	
	private static final class Node<T>{
		private final Object[] arr;
		private       int      start;
		private       int      size;
		
		private Node<T> next;
		private Node<T> prev;
		
		public Node(Object[] arr){
			this.arr = arr;
		}
		
		@SuppressWarnings("unchecked")
		private T get(int localPos){
			return (T)arr[start + localPos];
		}
		
		private void add(int localPos, T element){
//			if(localPos == size){
//				add(element);
//				return;
//			}
			
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
				for(int i = s - 1; i>=truePos; i--){
					arr[i + 1] = arr[i];
				}
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
		private Node<T> optimalNext(){
			if(next == null || next.full()){
				insertNext();
			}
			return next;
		}
		
		private void insertNext(){
			var delta1 = new Node<T>(new Object[arr.length]);
			delta1.prev = this;
			
			if(next != null){
				var delta2 = next;
				delta2.prev = delta1;
				delta1.next = delta2;
			}
			next = delta1;
		}
		
		private void add(T element){
			var truePos = start + size;
			
			if(truePos == arr.length){
				var n = optimalNext();
				n.add(0, element);
				return;
			}
			
			size++;
			arr[truePos] = element;
		}
		
		private T remove(int localPos){
			int i       = start + localPos;
			int newSize = size - 1;
			@SuppressWarnings("unchecked")
			var old = (T)arr[i];
			if(start + newSize>i){
				System.arraycopy(arr, i + 1, arr, i, start + newSize - i);
			}
			arr[size = newSize] = null;
			
			if(newSize<arr.length/2){
				defrag();
			}
			
			return old;
		}
		
		private void defrag(){
			int siz = size;
			if(prev != null){
				int off;
				if(siz + (off = prev.size + prev.start)<arr.length){
					System.arraycopy(arr, start, prev.arr, off, siz);
					prev.size += siz;
					removeSelf();
				}
			}else if(next != null){
				int off;
				if((off = siz + start) + next.size<arr.length){
					System.arraycopy(next.arr, next.start, arr, off, next.size);
					size += next.size;
					next.removeSelf();
				}
			}
		}
		
		private void removeSelf(){
			prev.next = next;
			if(next != null) next.prev = prev;
		}
	}
	
	private       int size;
	private final int rollSize;
	
	private Node<T> head, tail;
	
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
		Objects.checkIndex(index, size);
		var res = nodeWalk(index);
		res.node.add(res.localPos, element);
		size++;
	}
	
	@Override
	public T remove(int index){
		Objects.checkIndex(index, size);
		size--;
		var res = nodeWalk(index);
		return res.node.remove(res.localPos);
	}
	
	@Override
	public void clear(){
		size = 0;
		tail = head = null;
	}
	
	private void makeFirst(){
		tail = head = new Node<>(new Object[rollSize]);
	}
	
	@Override
	public int size(){
		return size;
	}
	
	private NodeResult<T> nodeWalk(int offset){
//		if(offset>size/2){
//			return getBackwardsNode(offset);
//		}
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
		while(remaining>=node.size){
			var prev = node.prev;
			if(prev == null) break;
			remaining -= node.size;
			node = prev;
		}
		return new NodeResult<>(node, remaining);
	}
	
	@Override
	public Iterator<T> iterator(){
		return new UnrolledItertor();
	}
	
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
		
		return size + " " + rest.toString();
	}
}
