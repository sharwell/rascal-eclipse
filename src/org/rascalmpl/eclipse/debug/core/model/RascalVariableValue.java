/*******************************************************************************
 * Copyright (c) 2009-2011 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   * Jurgen J. Vinju - Jurgen.Vinju@cwi.nl - CWI
 *   * Emilie Balland - (CWI)
 *   * Arnold Lankamp - Arnold.Lankamp@cwi.nl
*******************************************************************************/
package org.rascalmpl.eclipse.debug.core.model;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.rascalmpl.interpreter.result.Result;

public class RascalVariableValue implements IValue {
	
	/* do not print more than MAX_VALUE_STRING characters */
	private final static int MAX_VALUE_STRING = 100;

	private RascalDebugTarget target;
	private Result<org.eclipse.imp.pdb.facts.IValue> value;

	public RascalVariableValue(RascalDebugTarget target,
			Result<org.eclipse.imp.pdb.facts.IValue> value) {
		this.value = value;
		this.target = target;
	}

	public String getReferenceTypeName() throws DebugException {
		return value.getType().toString();
	}

	public String getValueString() throws DebugException {
		if (value.getValue() == null) return "";
		String s = value.getValue().toString();
		if (s.length() > MAX_VALUE_STRING) {
			return s.substring(0,MAX_VALUE_STRING)+"...";
		}
		
		return s;
	}

	public IVariable[] getVariables() throws DebugException {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean hasVariables() throws DebugException {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isAllocated() throws DebugException {
		// TODO Auto-generated method stub
		return false;
	}

	public IDebugTarget getDebugTarget() {
		return target;
	}

	public ILaunch getLaunch() {
		return target.getLaunch();
	}

	public String getModelIdentifier() {
		return target.getModelIdentifier();
	}

	public Object getAdapter(Class adapter) {
		return target.getAdapter(adapter);
	}

	public String toString() {
		return value.getValue().toString();	
	}
	
	public org.eclipse.imp.pdb.facts.IValue getValue() {
		return value.getValue();
	}

}
