package com.lapissea.unrolledlist;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Warmup(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(5)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
//@BenchmarkMode(Mode.AverageTime)
public class RandomGetJMH{
	
	@Param({"Linked", "Array", "Unrolled 16", "Unrolled 32"})
	public String _type;
	@Param({"20", "100", "300", "500"})
	public int    size;
	
	public int seed = -1;
	
	private List<Integer> list;
	private int[]         idx;
	
	@Setup(Level.Invocation)
	public void setUp(){
		var rand  = seed == -1? new Random() : new Random(seed);
		var pre   = "Unrolled ";
		var chSiz = _type.startsWith(pre)? Integer.parseInt(_type.substring(pre.length())) : 16;
		
		list = Utils.gen(rand, chSiz, size, 3, false);
		
		if(list.isEmpty()) list.add(69);
		if(_type.equals("Array")) list = new ArrayList<>(list);
		if(_type.equals("Linked")) list = new LinkedList<>(list);
		
		idx = rand.ints(100, 0, list.size()).toArray();
	}
	
	@Benchmark
	@OperationsPerInvocation(100)
	public void get(Blackhole bh){
		for(int i = 0; i<100; i++){
			var id = idx[i];
			bh.consume(list.get(id));
		}
	}
	
	
}
