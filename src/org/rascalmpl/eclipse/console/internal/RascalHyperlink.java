package org.rascalmpl.eclipse.console.internal;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IHyperlink;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;
import org.rascalmpl.eclipse.Activator;
import org.rascalmpl.uri.URIResolverRegistry;
import org.rascalmpl.uri.URIUtil;
import org.rascalmpl.uri.UnsupportedSchemeException;

public class RascalHyperlink implements IHyperlink {

	private URIResolverRegistry resolver;
	private String target;
	private PrintWriter err;

	public RascalHyperlink(String target, URIResolverRegistry resolver, PrintWriter err) {
		this.resolver = resolver;
		this.target = target;
		this.err = err;
	}

	@Override
	public void linkEntered() {
	}

	@Override
	public void linkExited() {
	}

	@Override
	public void linkActivated() {
		try {
			URI uri = resolver.getResourceURI(getURIPart());
			if (uri != null) {
				IFileStore fileStore = EFS.getLocalFileSystem().getStore(uri);
				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				IEditorPart part = IDE.openEditorOnFileStore(page, fileStore);
				if (getOffsetPart() > -1 && part instanceof ITextEditor) {
					((ITextEditor)part).selectAndReveal(getOffsetPart(), getLength());
				}
			}
			else {
				err.println("Cannot open link " + target);
			}
		} catch (PartInitException e) {
			Activator.log("Cannot get editor part", e);
		} catch (UnsupportedSchemeException use) {
			//
			if (getURIPart().getScheme().equals("http")) {
				try {
					PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(getURIPart().toURL());
				} catch (PartInitException e) {
					Activator.log("Cannot get editor part", e);
				} catch (MalformedURLException e) {
					err.println("Cannot open link " + target);
					Activator.log("Cannot resolve link", e);
				}
			}
			else {
				err.println("Cannot open link " + target);
				Activator.log("Cannot resolve link", use);
			}
		} catch (IOException e1) {
			err.println("Cannot open link " + target);
			Activator.log("Cannot resolve link", e1);
		}
	}

	private int length = -1;
	private int getLength() {
		makeSureLinkIsParsed();
		return length;
	}

	private int offset = -1;
	private int getOffsetPart() {
		makeSureLinkIsParsed();
		return offset;
	}

	private URI uri = null;
	private URI getURIPart() {
		makeSureLinkIsParsed();
		return uri;
	}
	
	private boolean linkParsed = false;
	private Pattern splitParts = Pattern.compile("\\|([^\\|]*)\\|(?:\\(\\s*([0-9]+)\\s*,\\s*([0-9]+))?");
	private void makeSureLinkIsParsed() {
		if (!linkParsed) {
			linkParsed = true;
			Matcher m = splitParts.matcher(target);
			if (m.find()) {
				uri = URIUtil.assumeCorrect(m.group(1));
				if (m.group(2) != null) {
					offset = Integer.parseInt(m.group(2));
					length = Integer.parseInt(m.group(3));
				}
			}
		}

	}
}
