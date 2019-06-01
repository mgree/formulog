package edu.harvard.seas.pl.formulog.ast;

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

import edu.harvard.seas.pl.formulog.ast.Atoms.Atom;

public interface Rule {

	/*
	 * TODO Instead of returning the whole list, should probably just provide
	 * methods to index into head/body and get size of each.
	 * 
	 * Could maybe provide all of head/body as Iterable.
	 */

	public Iterable<Atom> getHead();

	public Iterable<Atom> getBody();
	
	public int getHeadSize();
	
	public int getBodySize();
	
	public Atom getHead(int idx);
	
	public Atom getBody(int idx);

}
