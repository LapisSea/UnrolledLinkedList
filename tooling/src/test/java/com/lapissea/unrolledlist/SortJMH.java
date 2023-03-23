package com.lapissea.unrolledlist;

import com.lapissea.util.UtilL;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@Warmup(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(2)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class SortJMH{
	
	public static void main(String[] args){
		var queue = new ConcurrentLinkedQueue<List<SortJMH>>();
		
		ExecutorService s = Executors.newFixedThreadPool(2);
		
		var iters = 40000;
		Thread.ofVirtual().start(() -> {
			for(int i = 0; i<iters; i++){
				int finalI = i;
				s.submit(() -> {
					UtilL.sleepWhile(() -> queue.size()>10);
					queue.add(IntStream.range(0, 10000).mapToObj(i1 -> {
						var inst = new SortJMH();
						inst.chunkSize = 16;
						inst.size = 300;
						inst.seed = finalI*1000 + i1;
						inst.setUp();
						return inst;
					}).toList());
				});
			}
		});
		UtilL.sleepWhile(queue::isEmpty);
		for(int i = 0; i<iters; i++){
			for(var u : queue.remove()){
				u.sort();
			}
		}
		s.close();
	}
	
	@Param({"20", "200", "300", "500"})
	public int size;
	@Param({"-1", "16", "32"})
	public int chunkSize;
	
	public int seed = 69;
	
	private List<Integer> list;
	
	@Setup(Level.Invocation)
	public void setUp(){
		list = Utils.gen(new Random(seed), chunkSize == -1? 16 : chunkSize, size, 3, false);
		if(chunkSize == -1) list = new ArrayList<>(list);
	}
	
	@Benchmark
	public void sort(){
		list.sort(Integer::compare);
	}
	
	
}
