package com.lapissea.unrolledlist;

import com.lapissea.util.UtilL;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;

public class Utils{
	
	
	public static List<Integer> gen(Random r, int maxSize, int digs){
		return gen(r, 16, maxSize, digs, true);
	}
	public static List<Integer> gen(Random r, int chunkSize, int maxSize, int digs, boolean doTest){
		int min = 1;
		int max = 9;
		for(int i = 0; i<digs - 1; i++){
			min *= 10;
			max = max*10 + 9;
		}
		
		var list = new UnrolledLinkedList<Integer>(chunkSize);
		int size = r.nextInt(maxSize);
		for(int i = 0; i<size; i++){
			list.add(r.nextInt(min, max));
			if(r.nextInt(3) == 0 && !list.isEmpty()){
				list.remove(r.nextInt(list.size()));
			}
		}
		return doTest? new CheckList<>(list, new ArrayList<>(list)) : list;
	}
	
	public static <T> Queue<List<T>> asyncGen(int iters, int chunkSize, int threads, IntFunction<T> gen){
		var queue = new ConcurrentLinkedQueue<List<T>>();
		
		var s = Executors.newFixedThreadPool(threads);
		
		Thread.ofVirtual().start(() -> {
			AtomicInteger doneCount = new AtomicInteger();
			for(int i = 0; i<iters; i++){
				int outerIter = i;
				s.submit(() -> {
					UtilL.sleepWhile(() -> queue.size()>10);
					List<T> list = new ArrayList<>(chunkSize);
					for(int inneriter = 0; inneriter<chunkSize; inneriter++){
						list.add(gen.apply(outerIter*1000 + inneriter));
					}
					queue.add(list);
					
					if(doneCount.incrementAndGet() == iters){
						s.close();
					}
				});
			}
		});
		
		return queue;
	}
}
