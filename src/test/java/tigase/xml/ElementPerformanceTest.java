/*
 * Tigase XML Tools - Tigase XML Tools
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
package tigase.xml;

import org.junit.Ignore;
import org.junit.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Ignore
public class ElementPerformanceTest {

	@Test
	public void launchBenchmark() throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(this.getClass().getName() + ".*")
//				.include(this.getClass().getName() + ".benchmarkFlatMap*")
//				.include(this.getClass().getName() + ".benchmarkFlatMapStreamToList*")
//				.include(this.getClass().getName() + ".benchmarkFindChild*")
//				.include(this.getClass().getName() + ".benchmarkFindChildNew*")
//				.include(this.getClass().getName() + ".benchmarkFindChildStream*")
//				.include(this.getClass().getName() + ".benchmarkFlatMapStream*")
//				.include(this.getClass().getName() + ".benchmarkGetChildAnd*")
				// Set the following options as needed
//				.mode (Mode.AverageTime)
				.timeUnit(TimeUnit.MICROSECONDS)
				.warmupTime(TimeValue.seconds(1))
				.warmupIterations(2)
				.measurementTime(TimeValue.seconds(1))
				.measurementIterations(5)
				.threads(2)
				.forks(1)
				.shouldFailOnError(true)
				.shouldDoGC(true)
				//.jvmArgs("-XX:+UnlockDiagnosticVMOptions", "-XX:+PrintInlining")
				//.addProfiler(WinPerfAsmProfiler.class)
				.build();
		new Runner(opt).run();
	}

	@State(Scope.Thread)
	public static class BenchmarkState {
		Element element;
		int i;

		@Param({ "1", "5", "10", "20" })
		int maxChild;
		String name;

		@Param({"false", "true"})
		boolean randomChild;

		@Param({"ArrayList", "LinkedList"})
		String clazz;

		@Setup(Level.Trial)
		public void initializeElement() {
			switch (clazz) {
				case "ArrayList" -> Element.listSupplier = ArrayList::new;
				case "LinkedList" -> Element.listSupplier = LinkedList::new;
			}

			element = new Element("root");
			for (int childIdx = 1; childIdx <= maxChild; childIdx++) {
				element.addChild(new Element("child-" + childIdx).withElement("subchild-1", null));
			}
		}

		@Setup(Level.Iteration)
		public void initializeVariable() {
			i = randomChild ? (new Random().nextInt(1, maxChild + 1)) : 1;
			name = ("child-" + i);
		}
	}

	@State(Scope.Thread)
	public static class BenchmarkStateStatic extends BenchmarkState {
		@Setup(Level.Iteration)
		@Override
		public void initializeVariable() {
			super.initializeVariable();
			name = name.intern();
		}
	}

	@State(Scope.Thread)
	public static class BenchmarkState2 {
		List<String> strings = List.of(FROM, TO, NAME, ID, XMLNS, LABEL, VAR);
		Element.XMLIdentityHashMap<String,String> map = new Element.XMLIdentityHashMap<>(5);

		@Setup(Level.Trial)
		public void initialize() {
		}
	}

	@State(Scope.Thread)
	public static class BenchmarkState3 {
		List<String> strings = List.of(FROM, TO, NAME, ID, XMLNS, LABEL, VAR);
		Element.XMLIdentityHashMap<String,String> map = new Element.XMLIdentityHashMap<>(5);

		@Setup(Level.Trial)
		public void initialize() {
			for (String item : strings) {
				map.put(item.intern(), item);
			}
		}
	}

	@Benchmark
	@Measurement(iterations = 10)
	@BenchmarkMode(Mode.AverageTime)
	public void benchmarkIndentityMapSet(BenchmarkState2 state) {
		for (String item : state.strings) {
			state.map.put(item.intern(), item);
		}
	}

	@Benchmark
	@Measurement(iterations = 10)
	@BenchmarkMode(Mode.AverageTime)
	public void benchmarkIndentityMapGet(BenchmarkState3 state, Blackhole blackhole) {
		for (String item : state.strings) {
			blackhole.consume(state.map.get(item.intern()));
		}
	}

	@Benchmark
	@Measurement(iterations = 10)
	@BenchmarkMode(Mode.AverageTime)
	public void benchmarkIndentityMapGetStatic(BenchmarkState3 state, Blackhole blackhole) {
		for (String item : state.map.keySet()) {
			blackhole.consume(state.map.get(item));
		}
	}

	@State(Scope.Thread)
	public static class BenchmarkState4 {
		List<String> strings = List.of(FROM, TO, NAME, ID, XMLNS, LABEL, VAR);
		Map<String,String> map = new HashMap<>(5);

		@Setup(Level.Trial)
		public void initialize() {
		}
	}

	@Benchmark
	@Measurement(iterations = 10)
	@BenchmarkMode(Mode.AverageTime)
	public void benchmarkHashMapSet(BenchmarkState4 state) {
		for (String item : state.strings) {
			state.map.put(item, item);
		}
	}

	@State(Scope.Thread)
	public static class BenchmarkState5 {
		List<String> strings = List.of(FROM, TO, NAME, ID, XMLNS, LABEL, VAR);
		Map<String,String> map = new HashMap<>(5);

		@Setup(Level.Trial)
		public void initialize() {
			for (String str : strings) {
				map.put(new String(str), str);
			}
		}
	}

	@Benchmark
	@Measurement(iterations = 10)
	@BenchmarkMode(Mode.AverageTime)
	public void benchmarkHashMapGet(BenchmarkState5 state, Blackhole blackhole) {
		for (String item : state.strings) {
			blackhole.consume(state.map.get(item));
		}
	}

	@State(Scope.Thread)
	public static class BenchmarkState6 {
		List<String> strings = List.of(FROM, TO, NAME, ID, XMLNS, LABEL, VAR);
		Map<String,String> map = new HashMap<>(5);

		@Setup(Level.Trial)
		public void initialize() {
		}
	}

	protected static final String FROM = "from";
	protected static final String TO = "to";
	protected static final String NAME = "name";
	protected static final String ID = "id";
	protected static final String XMLNS = "xmlns";
	protected static final String LABEL = "label";
	protected static final String VAR = "var";

	protected static final String getOptimizedString(String name) {
		return switch (name) {
			case FROM -> FROM;
			case TO -> TO;
			case NAME -> NAME;
			case ID -> ID;
			case XMLNS -> XMLNS;
			case LABEL -> LABEL;
			case VAR -> VAR;
			default -> name;
		};
	}

	@Benchmark
	@Measurement(iterations = 10)
	@BenchmarkMode(Mode.AverageTime)
	public void benchmarkHashMapSetMemoryOptimized(BenchmarkState6 state) {
		for (String item : state.strings) {
			state.map.put(getOptimizedString(item), item);
		}
	}

	@State(Scope.Thread)
	public static class BenchmarkState7 {
		List<String> strings = List.of(FROM, TO, NAME, ID, XMLNS, LABEL, VAR);
		Map<String,String> map = new HashMap<>(5);

		@Setup(Level.Trial)
		public void initialize() {
			strings = strings.stream().map(String::new).collect(Collectors.toList());
			for (String item : strings) {
				map.put(getOptimizedString(new String(item)), item);
			}
		}
	}

	@Benchmark
	@Measurement(iterations = 10)
	@BenchmarkMode(Mode.AverageTime)
	public void benchmarkHashMapGetMemoryOptimized(BenchmarkState7 state, Blackhole blackhole) {
		for (String item : state.strings) {
			blackhole.consume(state.map.get(getOptimizedString(item)));
		}
	}

	@State(Scope.Thread)
	public static class BenchmarkState8 {
		List<String> strings = List.of(FROM, TO, NAME, ID, XMLNS, LABEL, VAR);
		Map<String,String> map = new HashMap<>(5);

		@Setup(Level.Trial)
		public void initialize() {
		}
	}

	@Benchmark
	@Measurement(iterations = 10)
	@BenchmarkMode(Mode.AverageTime)
	public void benchmarkHashMapSetIdentity(BenchmarkState8 state) {
		for (String item : state.strings) {
			state.map.put(item.intern(), item);
		}
	}

	@State(Scope.Thread)
	public static class BenchmarkState9 {
		List<String> strings = List.of(FROM, TO, NAME, ID, XMLNS, LABEL, VAR);
		Map<String,String> map = new HashMap<>(5);

		@Setup(Level.Trial)
		public void initialize() {
			strings = strings.stream().map(String::new).collect(Collectors.toList());
			for (String item : strings) {
				map.put(item.intern(), item);
			}
		}
	}

	@Benchmark
	@Measurement(iterations = 10)
	@BenchmarkMode(Mode.AverageTime)
	public void benchmarkHashMapGetIdentity(BenchmarkState9 state, Blackhole blackhole) {
		for (String item : state.strings) {
			blackhole.consume(state.map.get(item.intern()));
		}
	}


	// FIXME: Usage of HashMap instead of IndentityMap is faster 16 times!!
	// It will not deduplicate strings, but would improve performance.
	// Since Java 9, String which are only "latin-1" are using 1 byte per char instead of 2 bytes (CompactString, so memory usage is optimized)
	// Attributes are usually the same, but is it worth it to slow down the app?
	// In Java 18 there are plans for String deduplication in all GC mechanisms, so it should deduplicate memory usage but only AFTER 3 GC passes (so long lived objects only)

	// I .findChild tests

	//
	@Benchmark
	@Measurement(iterations = 1000)
	@BenchmarkMode(Mode.Throughput)
	public void benchmarkFindChildIdentity(BenchmarkState state, Blackhole blackhole) {
		blackhole.consume(state.element.findChild(el -> el.getName() == state.name));
	}
	@Benchmark
	@Measurement(iterations = 1000)
	@BenchmarkMode(Mode.Throughput)
	public void benchmarkFindChildEquality(BenchmarkState state, Blackhole blackhole) {
		blackhole.consume(state.element.findChild(el -> (state.name).equals(el.getName())));
	}

	@Benchmark
	@Measurement(iterations = 1000)
	@BenchmarkMode(Mode.Throughput)
	public void benchmarkFindChildStreamIdentity(BenchmarkState state, Blackhole blackhole) {
		blackhole.consume(state.element.findChildStream(el -> el.getName() == state.name));
	}

	@Benchmark
	@Measurement(iterations = 1000)
	@BenchmarkMode(Mode.Throughput)
	public void benchmarkFindChildStreamDirectIdentity(BenchmarkState state, Blackhole blackhole) {
		blackhole.consume(state.element.findChildStreamDirect(el -> el.getName() == state.name));
	}

	@Benchmark
	@Measurement(iterations = 1000)
	@BenchmarkMode(Mode.Throughput)
	public void benchmarkFindChildNewMatcher(BenchmarkState state, Blackhole blackhole) {
		blackhole.consume(state.element.findChild(Element.Matcher.byName(state.name)));
	}

	@Benchmark
	@Measurement(iterations = 1000)
	@BenchmarkMode(Mode.Throughput)
	public void benchmarkFindChildGetChildStaticStr(BenchmarkStateStatic state, Blackhole blackhole) {
		blackhole.consume(state.element.getChildStaticStr(state.name));
	}

	@Benchmark
	@Measurement(iterations = 1000)
	@BenchmarkMode(Mode.Throughput)
	public void benchmarkFindChildGetChildStaticStrOptional(BenchmarkStateStatic state, Blackhole blackhole) {
		blackhole.consume(Optional.ofNullable(state.element.getChildStaticStr(state.name)).orElse(null));
	}

//	@Benchmark
//	@Measurement(iterations = 1000)
//	@BenchmarkMode(Mode.Throughput)
//	public void benchmarkFindChildOldEmpty(BenchmarkState state, Blackhole blackhole) {
//		blackhole.consume(Optional.ofNullable(state.element.getChildStaticStr(state.name)).orElse(null));
//	}

	@Benchmark
	@Measurement(iterations = 1000)
	@BenchmarkMode(Mode.Throughput)
	public void benchmarkFindChildOptionalMapperAndGetName(BenchmarkState state, Blackhole blackhole) {
		blackhole.consume(state.element.findChild(state.name, null).map(Element::getName).orElse(null));
	}

	@Benchmark
	@Measurement(iterations = 1000)
	@BenchmarkMode(Mode.Throughput)
	public void benchmarkFindChildNullableAndGetName(BenchmarkState state, Blackhole blackhole) {
		final Element child = state.element.getChild(state.name, null);
		blackhole.consume(child != null ? child.getName() : null);
	}


	@Benchmark
	@Measurement(iterations = 100)
	@BenchmarkMode(Mode.AverageTime)
	public void benchmarkFlatMapManual(BenchmarkState state, Blackhole blackhole) {
		List<Element> children = state.element.getChildren();
		List<Element> result = new LinkedList<>();
		for (Element child : children) {
			for (Element subChild : child.getChildren()) {
				if (subChild.getName() == "subchild-1") {
					result.add(subChild);
				}
			}
		}
		blackhole.consume(result);
	}

	@Benchmark
	@Measurement(iterations = 100)
	@BenchmarkMode(Mode.AverageTime)
	public void benchmarkFlatMap(BenchmarkState state, Blackhole blackhole) {
		List<Element> children = state.element.flatMapChildren(Element::getChildren);
		List<Element> children2 = new LinkedList<>();
		for (Element child : children) {
			if (child.getName().equals("subchild-1")) {
				children2.add(child);
			}
		}
		blackhole.consume(children2);
	}

	@Benchmark
	@Measurement(iterations = 100)
	@BenchmarkMode(Mode.AverageTime)
	public void benchmarkFlatMapStreamToListGetChildrenStream(BenchmarkState state, Blackhole blackhole) {
		List<Element> children = state.element.getChildren()
				.stream()
				.map(Element::getChildren)
				.flatMap(List::stream)
				.filter(el -> el.getName().equals("subchild-1"))
				.toList();
		blackhole.consume(children);
	}

	@Benchmark
	@Measurement(iterations = 100)
	@BenchmarkMode(Mode.AverageTime)
	public void benchmarkFlatMapStreamToListStreamChildren(BenchmarkState state, Blackhole blackhole) {
		List<Element> children = state.element.streamChildren()
				.flatMap(Element::streamChildren)
				.filter(el -> el.getName().equals("subchild-1"))
				.toList();
		blackhole.consume(children);
	}

	@Benchmark
	@Measurement(iterations = 100)
	@BenchmarkMode(Mode.AverageTime)
	public void benchmarkFlatMapStreamDirectToListStreamChildrenDirect(BenchmarkState state, Blackhole blackhole) {
		List<Element> children = state.element.streamChildrenDirect()
				.flatMap(Element::streamChildrenDirect)
				.filter(el -> el.getName().equals("subchild-1"))
				.toList();
		blackhole.consume(children);
	}

	@Benchmark
	@Measurement(iterations = 100)
	@BenchmarkMode(Mode.AverageTime)
	public void benchmarkFlatMapStreamToArray(BenchmarkState state, Blackhole blackhole) {
		Element[] children = state.element.streamChildren()
				.flatMap(Element::streamChildren)
				.filter(el -> el.getName().equals("subchild-1"))
				.toArray(Element[]::new);
		blackhole.consume(children);
	}

	@Benchmark
	@Measurement(iterations = 100)
	@BenchmarkMode(Mode.AverageTime)
	public void benchmarkMapStreamToList(BenchmarkState state, Blackhole blackhole) {
		List<String> children = state.element.streamChildren()
				.filter(el -> el.getName().equals(state.name))
				.map(Element::getName)
				.toList();
		blackhole.consume(children);
	}

	// Benchmarking .getChildMethod: null vs optional

	@Benchmark
	@Measurement(iterations = 1000)
	@BenchmarkMode(Mode.Throughput)
	public void benchmarkGetChildAndNullComparison(BenchmarkState state, Blackhole blackhole) {
		Element el = state.element.getChild(state.name);
		if (el != null) {
			blackhole.consume(el.getChildren());
		}
	}

	@Benchmark
	@Measurement(iterations = 1000)
	@BenchmarkMode(Mode.Throughput)
	public void benchmarkGetChildAndOptional(BenchmarkState state, Blackhole blackhole) {
		Optional.ofNullable(state.element.getChild(state.name)).map(Element::getChildren).ifPresent(blackhole::consume);
	}

//	@State(Scope.Thread)
//	public static class BenchmarkState10 {
//		LinkedList<Element> list = Stream.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
//				.map(i -> new Element("child-" + i))
//				.collect(Collectors.toCollection(LinkedList::new));
//
//		@Setup(Level.Trial)
//		public void initialize() {
//		}
//	}
//
//	@Benchmark
//	@Measurement(iterations = 100)
//	@BenchmarkMode(Mode.AverageTime)
//	public void benchmarkFindChildLinkedList(BenchmarkState10 state, Blackhole blackhole) {
//		for (int i=0; i<1000; i++) {
//			Element child = null;
//			for (Element el : state.list) {
//				if ("child-5".equals(el.getName())) {
//					child = el;
//					break;
//				}
//			}
//			blackhole.consume(child);
//		}
//	}
//
//	@State(Scope.Thread)
//	public static class BenchmarkState11 {
//		ArrayList<Element> list = Stream.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
//				.map(i -> new Element("child-" + i))
//				.collect(Collectors.toCollection(ArrayList::new));
//
//		@Setup(Level.Trial)
//		public void initialize() {
//		}
//	}
//
//	@Benchmark
//	@Measurement(iterations = 100)
//	@BenchmarkMode(Mode.AverageTime)
//	public void benchmarkFindChildArrayList(BenchmarkState11 state, Blackhole blackhole) {
//		for (int i=0; i<1000; i++) {
//			Element child = null;
//			for (Element el : state.list) {
//				if ("child-5".equals(el.getName())) {
//					child = el;
//					break;
//				}
//			}
//			blackhole.consume(child);
//		}
//	}
}
