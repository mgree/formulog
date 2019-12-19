package edu.harvard.seas.pl.formulog.types;

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

import static edu.harvard.seas.pl.formulog.symbols.BuiltInConstructorSymbol.CMP_EQ;
import static edu.harvard.seas.pl.formulog.symbols.BuiltInConstructorSymbol.CMP_GT;
import static edu.harvard.seas.pl.formulog.symbols.BuiltInConstructorSymbol.CMP_LT;
import static edu.harvard.seas.pl.formulog.symbols.BuiltInConstructorSymbol.CONS;
import static edu.harvard.seas.pl.formulog.symbols.BuiltInConstructorSymbol.NIL;
import static edu.harvard.seas.pl.formulog.symbols.BuiltInConstructorSymbol.NONE;
import static edu.harvard.seas.pl.formulog.symbols.BuiltInConstructorSymbol.SOME;
import static edu.harvard.seas.pl.formulog.symbols.BuiltInTypeSymbol.ARRAY_TYPE;
import static edu.harvard.seas.pl.formulog.symbols.BuiltInTypeSymbol.BOOL_TYPE;
import static edu.harvard.seas.pl.formulog.symbols.BuiltInTypeSymbol.BV;
import static edu.harvard.seas.pl.formulog.symbols.BuiltInTypeSymbol.CMP_TYPE;
import static edu.harvard.seas.pl.formulog.symbols.BuiltInTypeSymbol.FP;
import static edu.harvard.seas.pl.formulog.symbols.BuiltInTypeSymbol.INT_TYPE;
import static edu.harvard.seas.pl.formulog.symbols.BuiltInTypeSymbol.LIST_TYPE;
import static edu.harvard.seas.pl.formulog.symbols.BuiltInTypeSymbol.MODEL_TYPE;
import static edu.harvard.seas.pl.formulog.symbols.BuiltInTypeSymbol.OPTION_TYPE;
import static edu.harvard.seas.pl.formulog.symbols.BuiltInTypeSymbol.SMT_TYPE;
import static edu.harvard.seas.pl.formulog.symbols.BuiltInTypeSymbol.STRING_TYPE;
import static edu.harvard.seas.pl.formulog.symbols.BuiltInTypeSymbol.SYM_TYPE;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import edu.harvard.seas.pl.formulog.symbols.BuiltInConstructorGetterSymbol;
import edu.harvard.seas.pl.formulog.symbols.ConstructorSymbol;
import edu.harvard.seas.pl.formulog.symbols.SymbolManager;
import edu.harvard.seas.pl.formulog.types.Types.AlgebraicDataType;
import edu.harvard.seas.pl.formulog.types.Types.AlgebraicDataType.ConstructorScheme;
import edu.harvard.seas.pl.formulog.types.Types.Type;
import edu.harvard.seas.pl.formulog.types.Types.TypeVar;

public class BuiltInTypesFactory {

	private final SymbolManager sm;
	
	public BuiltInTypesFactory(SymbolManager sm) {
		this.sm = sm;
		// Only need to set constructors for types that should be interpreted as
		// algebraic data types in SMT-LIB.
		setCmpConstructors();
		setListConstructors();
		setOptionConstructors();
	}

	public final TypeVar a = TypeVar.fresh();
	public final TypeVar b = TypeVar.fresh();
	public final TypeVar c = TypeVar.fresh();
	public final TypeVar d = TypeVar.fresh();

	public final Type i32 = bv(32);
	public final Type i64 = bv(64);
	public final Type fp32 = fp(8, 24);
	public final Type fp64 = fp(11, 53);
	public final Type string = AlgebraicDataType.make(STRING_TYPE);
	public final Type bool = AlgebraicDataType.make(BOOL_TYPE);
	public final Type cmp = AlgebraicDataType.make(CMP_TYPE);
	public final Type model = AlgebraicDataType.make(MODEL_TYPE);
	public final Type int_ = AlgebraicDataType.make(INT_TYPE);

	private void setCmpConstructors() {
		ConstructorScheme gt = new ConstructorScheme(CMP_GT, Collections.emptyList(), Collections.emptyList());
		ConstructorScheme lt = new ConstructorScheme(CMP_LT, Collections.emptyList(), Collections.emptyList());
		ConstructorScheme eq = new ConstructorScheme(CMP_EQ, Collections.emptyList(), Collections.emptyList());
		AlgebraicDataType.setConstructors(CMP_TYPE, Collections.emptyList(), Arrays.asList(gt, lt, eq));
	}

	private void setListConstructors() {
		ConstructorScheme nil = new ConstructorScheme(NIL, Collections.emptyList(), Collections.emptyList());
		List<ConstructorSymbol> consGetters = Arrays.asList(BuiltInConstructorGetterSymbol.CONS_1,
				BuiltInConstructorGetterSymbol.CONS_2);
		ConstructorScheme cons = new ConstructorScheme(CONS, Arrays.asList(a, list(a)), consGetters);
		AlgebraicDataType.setConstructors(LIST_TYPE, Collections.singletonList(a), Arrays.asList(nil, cons));
	}

	private void setOptionConstructors() {
		ConstructorScheme none = new ConstructorScheme(NONE, Collections.emptyList(), Collections.emptyList());
		List<ConstructorSymbol> someGetters = Arrays.asList(BuiltInConstructorGetterSymbol.SOME_1);
		ConstructorScheme some = new ConstructorScheme(SOME, Collections.singletonList(a), someGetters);
		AlgebraicDataType.setConstructors(OPTION_TYPE, Collections.singletonList(a), Arrays.asList(none, some));
	}

	public AlgebraicDataType list(Type a) {
		return AlgebraicDataType.make(LIST_TYPE, Collections.singletonList(a));
	}

	public AlgebraicDataType option(Type a) {
		return AlgebraicDataType.make(OPTION_TYPE, Collections.singletonList(a));
	}

	public AlgebraicDataType smt(Type a) {
		return AlgebraicDataType.make(SMT_TYPE, Collections.singletonList(a));
	}

	public AlgebraicDataType sym(Type a) {
		return AlgebraicDataType.make(SYM_TYPE, Collections.singletonList(a));
	}

	public AlgebraicDataType bv(TypeVar a) {
		return AlgebraicDataType.make(sm.instantiate(BV, a));
	}

	public AlgebraicDataType fp(TypeVar a, TypeVar b) {
		return AlgebraicDataType.make(sm.instantiate(FP, a, b));
	}
	
	public AlgebraicDataType bv(int width) {
		return AlgebraicDataType.make(sm.instantiate(BV, width));
	}

	public AlgebraicDataType fp(int exponent, int significand) {
		return AlgebraicDataType.make(sm.instantiate(FP, exponent, significand));
	}
	
	public AlgebraicDataType array(Type a, Type b) {
		return AlgebraicDataType.make(ARRAY_TYPE, Arrays.asList(a, b));
	}

}