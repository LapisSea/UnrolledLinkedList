package com.lapissea.unrolledlist;

import com.lapissea.util.LogUtil;
import com.lapissea.util.NanoTimer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class UnrolledTest{
	
	@Test
	void simpleAdd(){
		var list = new UnrolledLinkedList<Integer>();
		Assert.assertEquals(list.size(), 0);
		list.add(69);
		Assert.assertEquals(list.size(), 1);
		Assert.assertEquals(list.get(0), 69);
	}
	
	@Test(dependsOnMethods = "simpleAdd")
	void simpleRemove(){
		var list = new UnrolledLinkedList<Integer>();
		list.add(69);
		list.add(420);
		Assert.assertEquals(list.get(0), 69);
		Assert.assertEquals(list.remove(0), 69);
		Assert.assertEquals(list.get(0), 420);
	}
	
	@Test(dependsOnMethods = "simpleAdd")
	void simpleContains(){
		var list = new UnrolledLinkedList<Integer>();
		list.add(69);
		list.add(420);
		Assert.assertTrue(list.contains(69));
		Assert.assertTrue(list.contains(420));
		Assert.assertFalse(list.contains(360));
	}
	
	public static void main(String[] args){
		new UnrolledTest().addRemoveContainsFuzz();
	}
	
	@Test(dependsOnMethods = {"simpleRemove", "simpleContains"})
	void addRemoveContainsFuzz(){
		enum Actions{
			ADD(false),
			ADD_I(true),
			STREAM_SKIP_GET(true),
			CONTAINS(false),
			REMOVE(false),
			REMOVE_I(true),
			SET(true);
			final boolean needsIndex;
			Actions(boolean needsIndex){ this.needsIndex = needsIndex; }
		}
		var allActions = Actions.values();
		
		var rand = new Random(69);
		
		var unrolled = new UnrolledLinkedList<Integer>();
		var t        = new NanoTimer.Simple();
		var list     = new CheckList<>(unrolled, new ArrayList<>());
		var iters    = 10_000_000;
		t.start();
		for(int i = 0; i<iters; i++){
			if(i%(iters/100) == 0) LogUtil.println(i/(double)iters);
			
			if(rand.nextInt(1000) == 0) list.clear();
			
			var val = rand.nextInt(10, 99);
			
			var action = allActions[rand.nextInt(allActions.length)];
			if(action.needsIndex && list.isEmpty()) continue;
			
			var index = action.needsIndex? rand.nextInt(list.size()) : 0;
			try{
				switch(action){
					case null -> throw new NullPointerException();
					case ADD -> list.add(val);
					case ADD_I -> list.add(index, val);
					case STREAM_SKIP_GET -> Assert.assertEquals(unrolled.stream().skip(index).findFirst().orElseThrow(), list.get(index));
					case CONTAINS -> list.contains(val);
					case REMOVE -> list.remove((Integer)val);
					case REMOVE_I -> list.remove(index);
					case SET -> list.set(index, val);
				}
//				if(List.of(Actions.ADD, Actions.ADD_I, Actions.REMOVE_I, Actions.REMOVE).contains(action)) LogUtil.println(list.toString());
			}catch(Throwable e){
				Assert.fail(
					"Fail on iteration: " + i + "\n" +
					"value: " + val + ", action: " + action + (action.needsIndex? " at " + index : ""),
					e
				);
			}
		}
		t.end();
		LogUtil.println(t.ms());
	}
	
	List<Integer> gen(Random r){
		var unrolled = new UnrolledLinkedList<Integer>();
		var list     = new CheckList<>(unrolled, new ArrayList<>());
		int size     = r.nextInt(200);
		for(int i = 0; i<size; i++){
			list.add(r.nextInt(10, 99));
			if(r.nextInt(3) == 0 && !list.isEmpty()){
				list.remove(r.nextInt(list.size()));
			}
		}
		return list;
	}
	
	@Test(dependsOnMethods = "simpleAdd")
	void iteratorFuzz(){
		var rand = new Random(69);
		
		var iters = 100_000;
		for(int i = 0; i<iters; i++){
			if(i%(iters/100) == 0) LogUtil.println(i/(double)iters);
			
			var list = gen(rand);
//			LogUtil.println(i,list.toString());
			
			var     iter = list.iterator();
			boolean any  = false;
			int     j    = 0;
			while(true){
				try{
					if(!iter.hasNext()) break;
					if(rand.nextInt(5) == 0 && any){
						iter.remove();
						any = false;
					}else{
						iter.next();
						any = true;
					}
					j++;
				}catch(Throwable e){
					Assert.fail("Fail on iteration: " + i + " inner: " + j, e);
				}
			}
		}
	}
	
	@Test(dependsOnMethods = "simpleAdd")
	void listIteratorFuzz(){
		enum Action{
			NEXT(false), PREV(false), SET(true), ADD(true), REMOVE(true);
			
			private final boolean needsAny;
			Action(boolean needsAny){ this.needsAny = needsAny; }
		}
		var actions = Action.values();
		
		var rand = new Random(69);
		
		var iters = 50_000;
		for(int i = 0; i<iters; i++){
			if(i%(iters/100) == 0) LogUtil.println(i/(double)iters);
			
			var list = gen(rand);
//			LogUtil.println(i, list.toString());
			
			var     iter = list.listIterator(list.isEmpty()? 0 : rand.nextInt(list.size()));
			boolean any  = false;
			int     j    = 0;
			while(j<10000){
				try{
					if(!iter.hasNext()) break;
					
					Action action;
					do{
						action = actions[rand.nextInt(actions.length)];
					}while(!any && action.needsAny);
					
//					if(i == 4585 && j == 46){
//						int aai = 0;
//					}
//					if(i == 4585)	LogUtil.println(j, list.toString());
					
					switch(action){
						case null -> throw new NullPointerException();
						case NEXT -> {
							if(iter.hasNext()){
								iter.next();
								any = true;
							}
						}
						case PREV -> {
							if(iter.hasPrevious()){
								iter.previous();
								any = true;
							}
						}
						case SET -> iter.set(rand.nextInt(10, 99));
						case ADD -> {
							iter.add(rand.nextInt(10, 99));
							any = false;
						}
						case REMOVE -> {
							iter.remove();
							any = false;
						}
					}
					
					j++;
				}catch(Throwable e){
					Assert.fail("Fail on iteration: " + i + " inner: " + j, e);
				}
			}
		}
	}
}
