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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import edu.harvard.seas.pl.formulog.ast.Atoms.Atom;

public class BasicRule implements Rule {

	private final Atom head;
	private final List<Atom> body;

	private BasicRule(Atom head, List<Atom> body) {
		this.head = head;
		this.body = Collections.unmodifiableList(new ArrayList<>(body));
		if (this.body.isEmpty()) {
			throw new IllegalArgumentException("A rule cannot have an empty body");
		}
	}

	public static BasicRule get(Atom head, List<Atom> body) {
		if (body.isEmpty()) {
			return get(head);
		}
		return new BasicRule(head, body);
	}

	public static BasicRule get(Atom head) {
		return get(head, Collections.singletonList(Atoms.trueAtom));
	}

	public Atom getHead() {
		return head;
	}

	@Override
	public Iterable<Atom> getBody() {
		return body;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((body == null) ? 0 : body.hashCode());
		result = prime * result + ((head == null) ? 0 : head.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BasicRule other = (BasicRule) obj;
		if (body == null) {
			if (other.body != null)
				return false;
		} else if (!body.equals(other.body))
			return false;
		if (head == null) {
			if (other.head != null)
				return false;
		} else if (!head.equals(other.head))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(head);
		sb.append(" :-");
		if (body.size() == 1) {
			sb.append(" ");
		} else {
			sb.append("\n\t");
		}
		for (Iterator<Atom> it = body.iterator(); it.hasNext();) {
			sb.append(it.next());
			if (it.hasNext()) {
				sb.append(",\n\t");
			}
		}
		sb.append(".");
		return sb.toString();
	}

	@Override
	public int getBodySize() {
		return body.size();
	}

	@Override
	public Atom getBody(int idx) {
		return body.get(idx);
	}

}
