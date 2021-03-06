package edu.harvard.seas.pl.formulog.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/*-
 * #%L
 * FormuLog
 * %%
 * Copyright (C) 2018 - 2019 President and Fellows of Harvard College
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import edu.harvard.seas.pl.formulog.Configuration;
import edu.harvard.seas.pl.formulog.ast.BindingType;
import edu.harvard.seas.pl.formulog.ast.Term;
import edu.harvard.seas.pl.formulog.ast.Terms;
import edu.harvard.seas.pl.formulog.symbols.RelationSymbol;
import edu.harvard.seas.pl.formulog.symbols.SymbolComparator;

public class SortedIndexedFactDb implements IndexedFactDb {

	private final Map<RelationSymbol, List<IndexedFactSet>> indices;
	private final Map<RelationSymbol, IndexedFactSet> masterIndex;

	private SortedIndexedFactDb(Map<RelationSymbol, List<IndexedFactSet>> indices,
			Map<RelationSymbol, IndexedFactSet> masterIndex) {
		this.indices = indices;
		this.masterIndex = masterIndex;
	}

	@Override
	public Set<RelationSymbol> getSymbols() {
		return Collections.unmodifiableSet(masterIndex.keySet());
	}

	@Override
	public Iterable<Term[]> getAll(RelationSymbol sym) {
		return masterIndex.get(sym).getAll();
	}

	@Override
	public boolean isEmpty(RelationSymbol sym) {
		return masterIndex.get(sym).isEmpty();
	}

	@Override
	public int countDistinct(RelationSymbol sym) {
		return masterIndex.get(sym).count();
	}

	@Override
	public int countDuplicates(RelationSymbol sym) {
		int count = 0;
		for (IndexedFactSet idx : indices.get(sym)) {
			count += idx.count();
		}
		return count;
	}

	@Override
	public Iterable<Term[]> get(RelationSymbol sym, Term[] key, int index) {
		return indices.get(sym).get(index).lookup(key);
	}

	@Override
	public boolean add(RelationSymbol sym, Term[] tup) {
		assert allNormal(tup);
		IndexedFactSet master = masterIndex.get(sym);
		if (master.add(tup)) {
			for (IndexedFactSet idx : indices.get(sym)) {
				if (!idx.equals(master)) {
					idx.add(tup);
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean addAll(RelationSymbol sym, Iterable<Term[]> tups) {
		IndexedFactSet master = masterIndex.get(sym);
		if (master.addAll(tups)) {
			for (IndexedFactSet idx : indices.get(sym)) {
				if (!idx.equals(master)) {
					idx.addAll(tups);
				}
			}
			return true;
		}
		return false;
	}

	private boolean allNormal(Term[] args) {
		for (Term arg : args) {
			if (!arg.isGround() || arg.containsUnevaluatedTerm()) {
				return false;
			}
		}
		return true;
	}

	private void forEachIndex(Consumer<IndexedFactSet> f) {
		for (Iterable<IndexedFactSet> idxs : indices.values()) {
			for (IndexedFactSet idx : idxs) {
				f.accept(idx);
			}
		}
	}

	@Override
	public boolean hasFact(RelationSymbol sym, Term[] args) {
		assert allGround(args);
		return masterIndex.get(sym).contains(args);
	}

	private boolean allGround(Term[] args) {
		for (Term arg : args) {
			if (!arg.isGround()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void clear() {
		forEachIndex(IndexedFactSet::clear);
	}

	@Override
	public String toString() {
		String s = "{\n";
		for (RelationSymbol sym : masterIndex.keySet()) {
			s += "\t" + sym + " = {\n";
			for (IndexedFactSet idx : indices.get(sym)) {
				s += idx.toString() + "\n";
			}
			s += "\t}\n";
		}
		return s + "}";
	}

	public String toSimplifiedString() {
		String s = "{\n";
		for (RelationSymbol sym : masterIndex.keySet()) {
			IndexedFactSet idx = masterIndex.get(sym);
			if (!idx.isEmpty()) {
				s += "\t" + sym + " = {\n";
				s += idx.toString() + "\n";
				s += "\t}\n";
			}
		}
		return s + "}";
	}

	@Override
	public int numIndices(RelationSymbol sym) {
		if (!indices.containsKey(sym)) {
			throw new IllegalArgumentException("Unrecognized symbol: " + sym);
		}
		return indices.get(sym).size();
	}

	public IndexInfo getIndexInfo(RelationSymbol sym, int idx) {
		if (idx < 0 || idx > numIndices(sym)) {
			throw new IllegalArgumentException("Unrecognized index for symbol " + sym + ": " + idx);
		}
		IndexedFactSet index = indices.get(sym).get(idx);
		return new IndexInfo(index.comparatorOrder, Collections.singleton(Arrays.asList(index.pat)));
	}

	public int getMasterIndex(RelationSymbol sym) {
		if (!indices.containsKey(sym)) {
			throw new IllegalArgumentException("Unrecognized symbol: " + sym);
		}
		int i = 0;
		IndexedFactSet master = masterIndex.get(sym);
		for (IndexedFactSet s : indices.get(sym)) {
			if (s.equals(master)) {
				break;
			}
			i++;
		}
		return i;
	}

	public static class SortedIndexedFactDbBuilder implements IndexedFactDbBuilder<SortedIndexedFactDb> {

		private final Map<RelationSymbol, Integer> counts = new HashMap<>();
		private final Map<RelationSymbol, Map<BindingTypeArrayWrapper, Integer>> pats = new LinkedHashMap<>();

		public SortedIndexedFactDbBuilder(Set<RelationSymbol> allSyms) {
			List<RelationSymbol> sortedSyms = allSyms.stream().sorted(SymbolComparator.INSTANCE)
					.collect(Collectors.toList());
			for (RelationSymbol sym : sortedSyms) {
				pats.put(sym, new HashMap<>());
				counts.put(sym, 0);
			}
		}

		@Override
		public synchronized int makeIndex(RelationSymbol sym, BindingType[] pat) {
			assert sym.getArity() == pat.length;
			Map<BindingTypeArrayWrapper, Integer> m = pats.get(sym);
			BindingTypeArrayWrapper key = new BindingTypeArrayWrapper(pat);
			assert m != null : "Symbol not registered with DB: " + sym;
			Integer idx = m.get(key);
			if (idx == null) {
				idx = counts.get(sym);
				counts.put(sym, idx + 1);
				m.put(key, idx);
			}
			return idx;
		}

		@Override
		public SortedIndexedFactDb build() {
			Map<RelationSymbol, List<IndexedFactSet>> indices = new HashMap<>();
			Map<RelationSymbol, IndexedFactSet> masterIndex = new HashMap<>();
			for (Map.Entry<RelationSymbol, Map<BindingTypeArrayWrapper, Integer>> e : pats.entrySet()) {
				RelationSymbol sym = e.getKey();
				List<IndexedFactSet> idxs = new ArrayList<>();
				List<Map.Entry<BindingTypeArrayWrapper, Integer>> sorted = e.getValue().entrySet().stream().sorted(cmp)
						.collect(Collectors.toList());
				for (Map.Entry<BindingTypeArrayWrapper, Integer> e2 : sorted) {
					IndexedFactSet idx = IndexedFactSet.make(e2.getKey().getArr());
					idxs.add(idx);
					if (!idx.isProjected()) {
						masterIndex.putIfAbsent(sym, idx);
					}
				}
				if (!masterIndex.containsKey(sym)) {
					BindingType[] pat = new BindingType[sym.getArity()];
					for (int i = 0; i < pat.length; ++i) {
						pat[i] = BindingType.FREE;
					}
					IndexedFactSet master = IndexedFactSet.make(pat);
					masterIndex.put(sym, master);
					idxs.add(master);
				}
				indices.put(sym, idxs);
			}
			List<RelationSymbol> sortedSyms = masterIndex.keySet().stream().sorted(SymbolComparator.INSTANCE)
					.collect(Collectors.toList());
			HashMap<RelationSymbol, IndexedFactSet> sorted = new LinkedHashMap<>();
			for (RelationSymbol sym : sortedSyms) {
				sorted.put(sym, masterIndex.get(sym));
			}
			return new SortedIndexedFactDb(indices, sorted);
		}

		private static final Comparator<Map.Entry<BindingTypeArrayWrapper, Integer>> cmp = new Comparator<Map.Entry<BindingTypeArrayWrapper, Integer>>() {

			@Override
			public int compare(Entry<BindingTypeArrayWrapper, Integer> o1, Entry<BindingTypeArrayWrapper, Integer> o2) {
				return Integer.compare(o1.getValue(), o2.getValue());
			}

		};

	}

	private static class IndexedFactSet {

		private final BindingType[] pat;
		private final NavigableSet<Term[]> s;
		private final AtomicInteger cnt = new AtomicInteger();
		private final List<Integer> comparatorOrder;

		private final static TupleComparatorGenerator gen = new TupleComparatorGenerator();

		public static IndexedFactSet make(BindingType[] pat) {
			List<Integer> order = new ArrayList<>();
			for (int i = 0; i < pat.length; ++i) {
				if (pat[i].isBound()) {
					order.add(i);
				}
			}
			for (int i = 0; i < pat.length; ++i) {
				if (pat[i].isFree()) {
					order.add(i);
				}
			}
			int[] a = new int[order.size()];
			for (int i = 0; i < a.length; ++i) {
				a[i] = order.get(i);
			}
			Comparator<Term[]> cmp;
			if (Configuration.genComparators) {
				try {
					cmp = gen.generate(a);
				} catch (InstantiationException | IllegalAccessException e) {
					throw new AssertionError(e);
				}
			} else {
				cmp = new TermArrayComparator(a);
			}
			return new IndexedFactSet(pat, new ConcurrentSkipListSet<>(cmp), order);
		}

		public Iterable<Term[]> getAll() {
			return s;
		}

		public boolean isProjected() {
			for (BindingType b : pat) {
				if (b.equals(BindingType.IGNORED)) {
					return true;
				}
			}
			return false;
		}

		public void clear() {
			s.clear();
			cnt.set(0);
		}

		public boolean isEmpty() {
			return s.isEmpty();
		}

		private IndexedFactSet(BindingType[] pat, NavigableSet<Term[]> s, List<Integer> comparatorOrder) {
			this.pat = pat;
			this.s = s;
			this.comparatorOrder = comparatorOrder;
		}

		public boolean add(Term[] arr) {
			boolean modified = s.add(arr);
			if (modified) {
				cnt.incrementAndGet();
			}
			return modified;
		}

		public boolean addAll(Iterable<Term[]> tups) {
			boolean modified = false;
			int delta = 0;
			for (Term[] tup : tups) {
				if (s.add(tup)) {
					modified = true;
					delta++;
				}
			}
			if (modified) {
				cnt.addAndGet(delta);
			}
			return modified;
		}

		public int count() {
			return cnt.get();
		}

		public Iterable<Term[]> lookup(Term[] tup) {
			Term[] lower = new Term[tup.length];
			Term[] upper = new Term[tup.length];
			for (int i = 0; i < tup.length; ++i) {
				if (pat[i].isBound()) {
					lower[i] = tup[i];
					upper[i] = tup[i];
				} else {
					lower[i] = Terms.minTerm;
					upper[i] = Terms.maxTerm;
				}
			}
			return s.subSet(lower, true, upper, true);
		}

		public boolean contains(Term[] tup) {
			return s.contains(tup);
		}

		@Override
		public String toString() {
			String str = "[\n\t";
			str += Arrays.toString(pat);
			for (Term[] tup : s) {
				str += "\n\t";
				str += Arrays.toString(tup);
			}
			return str + "\n]";
		}

	}

	private static class TermArrayComparator implements Comparator<Term[]> {

		private final int[] pat;

		public TermArrayComparator(int[] pat) {
			this.pat = pat;
		}

		@Override
		public int compare(Term[] o1, Term[] o2) {
			for (int i = 0; i < pat.length; i++) {
				int j = pat[i];
				int x = o1[j].getId();
				int y = o2[j].getId();
				if (x < y) {
					return -1;
				} else if (x > y) {
					return 1;
				}
			}
			return 0;
		}

	}

	public class IndexInfo {

		private final List<Integer> comparatorOrder;
		private final Set<List<BindingType>> bindingPatterns;

		private IndexInfo(List<Integer> comparatorOrder, Set<List<BindingType>> bindingPatterns) {
			this.comparatorOrder = Collections.unmodifiableList(comparatorOrder);
			this.bindingPatterns = Collections.unmodifiableSet(bindingPatterns);
		}

		public List<Integer> getComparatorOrder() {
			return comparatorOrder;
		}

		public Set<List<BindingType>> getBindingPatterns() {
			return bindingPatterns;
		}

	}

}
