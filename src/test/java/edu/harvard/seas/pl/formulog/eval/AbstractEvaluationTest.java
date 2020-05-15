package edu.harvard.seas.pl.formulog.eval;

/*-
 * #%L
 * Formulog
 * %%
 * Copyright (C) 2018 - 2020 President and Fellows of Harvard College
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


import java.util.Collections;
import java.util.List;

public abstract class AbstractEvaluationTest<T extends Evaluation> {

	private final Tester<T> tester;
	
	public AbstractEvaluationTest(Tester<T> tester) {
		this.tester = tester;
	}
	
	protected void test(String file, List<String> inputDirs) {
		tester.test(file, inputDirs);
	}

	protected void test(String file) {
		test(file, Collections.singletonList(""));
	}

}
