/*******************************************************************************
 * Copyright (c) 2009-2011 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   * Jurgen J. Vinju - Jurgen.Vinju@cwi.nl - CWI
 *   * Tijs van der Storm - Tijs.van.der.Storm@cwi.nl
 *   * Emilie Balland - (CWI)
 *   * Anya Helene Bagge - A.H.S.Bagge@cwi.nl (Univ. Bergen)
 *   * Mark Hills - Mark.Hills@cwi.nl (CWI)
 *   * Arnold Lankamp - Arnold.Lankamp@cwi.nl
*******************************************************************************/
package org.rascalmpl.eclipse.editor;

import java.net.URI;
import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.imp.language.Language;
import org.eclipse.imp.language.LanguageRegistry;
import org.eclipse.imp.model.ISourceProject;
import org.eclipse.imp.parser.IMessageHandler;
import org.eclipse.imp.parser.IParseController;
import org.eclipse.imp.parser.ISourcePositionLocator;
import org.eclipse.imp.pdb.facts.IConstructor;
import org.eclipse.imp.pdb.facts.ISourceLocation;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.services.IAnnotationTypeInfo;
import org.eclipse.imp.services.ILanguageSyntaxProperties;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.rascalmpl.eclipse.Activator;
import org.rascalmpl.eclipse.nature.ProjectEvaluatorFactory;
import org.rascalmpl.eclipse.nature.RascalMonitor;
import org.rascalmpl.eclipse.uri.ProjectURIResolver;
import org.rascalmpl.interpreter.Evaluator;
import org.rascalmpl.interpreter.control_exceptions.Throw;
import org.rascalmpl.parser.gtd.exception.ParseError;
import org.rascalmpl.values.uptr.Factory;
import org.rascalmpl.values.uptr.TreeAdapter;

public class ParseController implements IParseController, IMessageHandlerProvider {
	private IMessageHandler handler;
	private ISourceProject project;
	private IConstructor parseTree;
	private IConstructor lastParseTree = null;
	private byte[] lastParsedInput = null;
	private IPath path;
	private Language language;
	private IDocument document;
	private org.rascalmpl.eclipse.editor.ParseController.ParseJob job;
	
	private Evaluator getParser(IProject project) {
		return ProjectEvaluatorFactory.getInstance().getEvaluator(project);
	}
	
	public IAnnotationTypeInfo getAnnotationTypeInfo() {
		return null;
	}

	public IMessageHandler getMessageHandler() {
		return handler;
	}
	
	public Object getCurrentAst() {
		return parseTree;
	}
	
    public void setCurrentAst(IConstructor parseTree) {
		this.parseTree = parseTree;
	}

	public Language getLanguage() {
		if (language == null) {
			language = LanguageRegistry.findLanguage("Rascal");
		}
		return language;
	}

	public ISourcePositionLocator getSourcePositionLocator() {
		return new NodeLocator();
	}

	public IPath getPath() {
		return path;
	}

	public ISourceProject getProject() {
		return project;
	}

	public ILanguageSyntaxProperties getSyntaxProperties() {
		return new RascalSyntaxProperties();
	}
	
	public Iterator<Token> getTokenIterator(IRegion region) {
		return parseTree != null ? new TokenIterator(parseTree) : null;
	}

	public void initialize(IPath filePath, ISourceProject project, IMessageHandler handler) {
		this.path = filePath;
		this.handler = handler;
		this.project = project;
		this.job = new ParseJob("Rascal parser", ProjectURIResolver.constructProjectURI(project, path), handler);
	}

	public IDocument getDocument() {
		return document;
	}
	
	public Object parse(IDocument doc, IProgressMonitor monitor) {
		if (doc == null) {
			return null;
		}
		this.document = doc;
		return parse(doc.get(), monitor);
	}
	
	private class ParseJob extends Job {
		private final IMessageHandler handler;
		private final URI uri;

		private String input;
		public IConstructor parseTree = null;

		public ParseJob(String name, URI uri, IMessageHandler handler) {
			super(name);
			
			this.uri = uri;
			this.handler = handler;
		}
		
		public void initialize(String input) {
			this.input = input;
		}
		
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			RascalMonitor rm = new RascalMonitor(monitor);
			rm.startJob("parsing", 500);
			parseTree = null;
			if (input == null || project == null || path == null) {
				// may happen when project is deleted before Eclipse was started
				return null;
			}
			
			try{
				handler.clearMessages();
				
				// TODO: this may be a workaround for a bug that's not there anymore
				byte[] inputBytes = input.getBytes();
				boolean arraysMatch = true;
				if (lastParsedInput != null) { 
					if(inputBytes.length != lastParsedInput.length) {
						arraysMatch = false;
					} else {
						for (int n = 0; n < inputBytes.length; ++n)
							if (inputBytes[n] != lastParsedInput[n]) {
								arraysMatch = false;
								break;
							}
					}
				}
				
				if (lastParsedInput != null && arraysMatch) {
					parseTree = lastParseTree;
				} else {
					Evaluator parser = getParser(project.getRawProject());
					// Note: One can switch between error and no error trees here.
					synchronized(parser){
						parseTree = parser.parseModuleWithErrorTree(rm, input.toCharArray(), uri, null);
//						parseTree = parser.parseModule(rm, input.toCharArray(), uri, null);
					}
					lastParseTree = parseTree;
					
					if(parseTree.getConstructorType() == Factory.Tree_Error){
						ISourceLocation parsedLocation = TreeAdapter.getLocation(parseTree);
						
						// Set the error location to where the error tree ends.
						setParseError(Math.max(0, parsedLocation.getLength() - 1), 0, parsedLocation.getEndLine(), parsedLocation.getEndColumn(), parsedLocation.getEndLine(), parsedLocation.getEndColumn(), "Parse error: " + parsedLocation);
					}
				}
			}catch(FactTypeUseException ftue){
				Activator.getInstance().logException("parsing rascal failed", ftue);
			}catch(ParseError pe){
				int offset = pe.getOffset();
				if(offset == input.length()) --offset;

				setParseError(offset, pe.getLength(), pe.getBeginLine() + 1, pe.getBeginColumn(), pe.getEndLine() + 1, pe.getEndColumn(), pe.toString());
			}catch(Throw t){
				ISourceLocation loc = t.getLocation();
				
				setParseError(loc.getOffset(), loc.getLength(), loc.getBeginLine(), loc.getBeginColumn(), loc.getEndLine(), loc.getEndColumn(), t.getMessage());
			}finally{
				rm.endJob(true);
			}
			
			return Status.OK_STATUS;
		}
	}
	
	public Object parse(String input, IProgressMonitor monitor) {
		parseTree = null;
		try {
			job.initialize(input);
			job.schedule();
			job.join();
			parseTree = job.parseTree;
			return parseTree;
		} catch (InterruptedException e) {
			Activator.getInstance().logException("parser interrupted", e);
		}
		return null;
	}
	
	private void setParseError(int offset, int length, int beginLine, int beginColumn, int endLine, int endColumn, String message){
		if(offset >= 0){
			handler.handleSimpleMessage(message, offset, offset + length, beginColumn, endColumn, beginLine, endLine);
		}else{
			handler.handleSimpleMessage(message, 0, 0, 0, 0, 1, 1);
		}
	}
}
