package com.lapissea.unrolledlist;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
}
