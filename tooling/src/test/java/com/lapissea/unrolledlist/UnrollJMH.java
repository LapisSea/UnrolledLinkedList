package com.lapissea.unrolledlist;

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
import java.util.concurrent.TimeUnit;

@Warmup(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(2)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
//@BenchmarkMode(Mode.AverageTime)
public class UnrollJMH{
	
	public static void main(String[] args){
		for(int i = 0; i<100000000; i++){
			var inst = new UnrollJMH();
			inst.chunkSize = 16;
			inst.size = 300;
			inst.setUp();
			inst.sort();
		}
	}
	
	@Param({"20", "200", "300", "500"})
	public int size;
	@Param({"-1", "16", "32"})
	public int chunkSize;
	
	private List<Integer> list;
	
	@Setup(Level.Invocation)
	public void setUp(){
		list = Utils.gen(new Random(69), chunkSize == -1? 16 : chunkSize, size, 3, false);
		if(chunkSize == -1) list = new ArrayList<>(list);
	}
	
	@Benchmark
	public void sort(){
		list.sort(Integer::compare);
	}
	
	
}
