/*******************************************************************************
 * Copyright (c) 2007-2008 TeXlipse-Team and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package net.sourceforge.texlipse.editor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.sourceforge.texlipse.TexlipsePlugin;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IPartitionTokenScanner;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

import de.walware.ecommons.text.ui.BufferedDocumentScanner;
import de.walware.ecommons.text.ui.OperatorRule;


/**
 * 
 * Comment (only in default partitions, at moment):
 * %... EOL
 * \begin{comment} ... \end{comment}
 *
 * Math:
 * \begin{math} ... \end{math}
 * \begin{displaymath} ... \end{displaymath}
 * \begin{equation} ... \end{equation}
 * \( ... \)      - math
 * \[ ... \]      - displaymath
 * $ ... $        - math
 * $$ ... $$      - displaymath
 * \begin{eqnarray} ... \end{eqnarray}
 * \begin{align} ... \end{align}
 * \begin{alignat} ... \end{alignat}
 * \begin{flalign} ... \end{flalign}
 * \begin{multline} ... \end{multline}
 * \begin{gather} ... \end{gather}
 * 
 * Verbatim:
 * \begin{verbatim} ... \end{verbatim}
 * \begin{lstlisting} ... \end{lstlisting}
 * \verb? ... ?
 */
public class TexFastPartitionScanner implements IPartitionTokenScanner {
	
	
	private static final String[] MATHENV = {
			"equation", "eqnarray", "align", "alignat", "flalign", "multline", "gather",
			"equation*", "eqnarray*", "align*", "alignat*", "flalign*", "multline*", "gather*",
			"math", "displaymath",
	};
	
    /**
     * 
     * @param envName Name of the environment
     * @return true, if the given name denotes a math environment
     */
    public final static boolean isMathEnv(String envName) {
        for (String st : MATHENV) {
            if (st.equals(envName)) return true;
        }
        return false;
    }
	
	
	/**
	 * Enum of states of the scanner.
	 * Note: id is index in array of tokens
	 * 0-11 are reserved for this class.
	 **/
	protected static final int S_DEFAULT = 0;
	protected static final int S_MATH_SPECIAL = 1;
	protected static final int S_MATH_ENV = 2;
	protected static final int S_VERBATIM_SINGLELINE = 3;
	protected static final int S_VERBATIM_ENV = 4;
	protected static final int S_COMMENT_SINGLELINE = 5;
	protected static final int S_COMMENT_ENV = 6;
	protected static final int S_INTERNAL_ENDENV = 7;
	
	protected final static IToken T_DEFAULT = new Token(ITexDocumentConstants.TEX_DEFAULT_EXPL_CONTENT_TYPE);
	protected final static IToken T_MATH = new Token(ITexDocumentConstants.TEX_MATH_CONTENT_TYPE);
	protected final static IToken T_VERBATIM = new Token(ITexDocumentConstants.TEX_VERBATIM_CONTENT_TYPE);
	protected final static IToken T_COMMENT = new Token(ITexDocumentConstants.TEX_COMMENT_CONTENT_TYPE);
	
	
	/** Enum of last significant characters read. */
	protected static final int LAST_OTHER = 0;
	protected static final int LAST_BACKSLASH = 1;
	protected static final int LAST_NEWLINE = 2;
	
	private static final char[] SEQ_begin = "begin".toCharArray();
	private static final char[] SEQ_end = "end".toCharArray();
	private static final char[] SEQ_verb = "verb".toCharArray();
	private static final char[] SEQ_$ = "$".toCharArray();
	private static final char[] SEQ_$$ = "$$".toCharArray();
	private static final char[] SEQ_$$$$ = "$$$$".toCharArray();
	private static final char[] SEQ_BL_QC = "\\]".toCharArray();
	private static final char[] SEQ_BL_PC = "\\)".toCharArray();
	
	
	private static class EnvType {
		String name;
		char[] namePattern;
		int state;
		
		public EnvType(final String name, final int state) {
			this.name = name;
			this.namePattern = name.toCharArray();
			this.state = state;
		}
	}
	
	
	/** The scanner. */
	private final BufferedDocumentScanner fScanner = new BufferedDocumentScanner(1000);	// faster implementation
	private boolean fTemplateMode;
	private IDocument fDocument;
	
	private IToken fToken;
	/** The offset of the last returned token. */
	private int fTokenOffset;
	/** The length of the last returned token. */
	private int fTokenLength;
	
	/** The current state of the scanner. */
	private int fState;
	/** The last significant characters read. */
	protected int fLast;
	/** The amount of characters already read on first call to nextToken(). */
	private int fPrefixLength;
	
	private int fRangeStart;
	private int fRangeEnd;
	
	private final IToken[] fStateTokens;
	
	private OperatorRule fEnvNameRule;
	private Map<String, EnvType> fEnvStates;
	private char[] fEndPattern;
	
	
	public TexFastPartitionScanner() {
		this(false);
	}
	
	/**
	 * 
	 * @param templateMode enabled mode for Eclipse template syntax with $ as prefix,
	 * so dollar must be doubled for math modes.
	 */
	public TexFastPartitionScanner(final boolean templateMode) {
		final Map<Integer, IToken> list = new HashMap<Integer, IToken>();
		fTemplateMode = templateMode;
		fEnvNameRule = new OperatorRule(new char[] {});
		fEnvStates = new HashMap<String, EnvType>();
		initTokens(list);
		final int count = maxState(list.keySet())+1;
		fStateTokens = new IToken[count];
		for (int i = 0; i < count; i++) {
			fStateTokens[i] = list.get(i);
		}
	}
	
	
	private int maxState(final Set<Integer> states) {
		int max = 0;
		final Iterator<Integer> iter = states.iterator();
		while (iter.hasNext()) {
			final int state = iter.next().intValue();
			if (state > max) {
				max = state;
			}
		}
		return max;
	}
	
	protected void addEnvRule(final String name, final int state) {
		fEnvStates.put(name, new EnvType(name, state));
		fEnvNameRule.addOp(name, null);
	}
	
	protected void initTokens(final Map<Integer, IToken> states) {
		states.put(S_DEFAULT, T_DEFAULT);
		states.put(S_MATH_SPECIAL, T_MATH);
		states.put(S_MATH_ENV, T_MATH);
		states.put(S_VERBATIM_SINGLELINE, T_VERBATIM);
		states.put(S_VERBATIM_ENV, T_VERBATIM);
		states.put(S_COMMENT_SINGLELINE, T_COMMENT);
		states.put(S_COMMENT_ENV, T_COMMENT);
		states.put(S_INTERNAL_ENDENV, T_DEFAULT);
		
		addEnvRule("comment", S_COMMENT_ENV);
		
		addEnvRule("verbatim", S_VERBATIM_ENV);
		addEnvRule("verbatim*", S_VERBATIM_ENV);
		addEnvRule("lstlisting", S_VERBATIM_ENV);
		
		for (String envName : MATHENV) {
			addEnvRule(envName, S_MATH_ENV);
		}
	}
	
	public void setRange(final IDocument document, final int offset, final int length) {
		setPartialRange(document, offset, length, null, -1);
	}
	
	public void setPartialRange(final IDocument document, final int offset, final int length, final String contentType, int partitionOffset) {
		if (partitionOffset < 0) {
			partitionOffset = offset;
		}
		fDocument = document;
		fTokenOffset = partitionOffset;
		fRangeStart = offset;
		fRangeEnd = offset+length;
		
		try {
			initState(contentType, offset-partitionOffset);
			return;
		} catch (final BadLocationException e) {
			TexlipsePlugin.log("Error occured when detect start char.", e);
			fState = S_DEFAULT;
			prepareScan(partitionOffset, 0);
		}
	}
	
	private void prepareScan(final int offset, final int prefixLength) {
		fTokenLength = 0;
		if (offset > 0) {
			fLast = LAST_OTHER;
			try {
				final char c = fDocument.getChar(offset-1);
				switch (c) {
				case '\r':
				case '\n':
					fLast = LAST_NEWLINE;
					break;
				}
			} catch (final BadLocationException e) {
				TexlipsePlugin.log("Error occured when detect last char.", e);
			}
		}
		else {
			fLast = LAST_NEWLINE;
		}
		fPrefixLength = prefixLength;
		fScanner.setRange(fDocument, offset, fRangeEnd-offset);
	}
	
	
	public int getTokenLength() {
		return fTokenLength;
	}
	
	public int getTokenOffset() {
		return fTokenOffset;
	}
	
	
	public IToken nextToken() {
		fToken = null;
		fTokenOffset += fTokenLength;
		fTokenLength = fPrefixLength;
		
		CHECK_NEXT: while (fToken == null) {
			final int c = fScanner.read();
			
			// characters
			if (c == ICharacterScanner.EOF) {
				fPrefixLength = 0;
				fToken = (fTokenLength > 0) ? fStateTokens[fState] : Token.EOF;
				break CHECK_NEXT;
			}
			
			fTokenLength++;
			handleChar(fState, c);
			continue CHECK_NEXT;
		}
		
//		System.out.println(fToken.getData() + " (" + fTokenOffset + "," + fTokenLength + ")");
		return fToken;
	}
	
	protected void handleChar(final int state, final int c) {
		switch (state) {
		case S_DEFAULT:
			fLast = LAST_OTHER;
			switch (c) {
			case '\r':
			case '\n':
				fLast = LAST_NEWLINE;
				return;
			case '\\': {
				fLast = LAST_OTHER;
				final int c2 = fScanner.read();
				if (c2 <= 32) {
					if (c2 >= 0) fScanner.unread();
					return;
				}
				fTokenLength++;
				switch (c2) {
				case 'b':
					if (readSeq2(SEQ_begin)) {
						checkForBeginEnv();
						return;
					}
					return;
 				case 'v':
					if (readSeq2(SEQ_verb)) {
						final int c6 = fScanner.read();
						if (c6 <= 32 | Character.isLetter(c6)) {
							if (c6 >= 0) fScanner.unread();
							return;
						}
						fTokenLength++;
						fEndPattern = new char[] { (char) c6 };
						newState(S_VERBATIM_SINGLELINE, 6); // \verb
						return;
					}
					return;
 				case '[':
					fEndPattern = SEQ_BL_QC;
					newState(S_MATH_SPECIAL, 2);
					return;
 				case '(':
 					fEndPattern = SEQ_BL_PC;
 					newState(S_MATH_SPECIAL, 2);
 					return;
 				default:
 					return;
				}
			}
			case '%':
				fLast = LAST_OTHER;
				newState(S_COMMENT_SINGLELINE, 1);
				return;
			case '$':
				fLast = LAST_OTHER;
				if (readChar('$')) {
					if (fTemplateMode && readChar('$')) {
						if (readChar('$')) {
							fEndPattern = SEQ_$$$$;
							newState(S_MATH_SPECIAL, 4);
							return;
						}
						fEndPattern = SEQ_$$;
						newState(S_MATH_SPECIAL, 3);
						return;
					}
					fEndPattern = SEQ_$$;
					newState(S_MATH_SPECIAL, 2);
					return;
				}
				if (fTemplateMode) {
					return;
				}
				fEndPattern = SEQ_$;
				newState(S_MATH_SPECIAL, 1);
				return;
			default:
				// Standard
				return;
			}
			
		case S_MATH_SPECIAL:
			if (c == fEndPattern[0]) {
				if (readSeq2(fEndPattern)) {
					fLast = LAST_OTHER;
					newState(S_DEFAULT, 0);
					return;
				}
			}
			else if (c == '\\') {
				final int c2 = fScanner.read();
				if (c2 <= 32) {
					if (c2 >= 0) fScanner.unread();
					fLast = LAST_BACKSLASH;
					return;
				}
				fLast = LAST_OTHER;
				fTokenLength++;
				return;
			}
			else if (c == '\r' || c == '\n') {
				fLast = LAST_NEWLINE;
				return;
			}
			fLast = LAST_OTHER;
			return;
			
		case S_MATH_ENV:
			if (c == '\\') {
				final int c2 = fScanner.read();
				if (c2 <= 32) {
					if (c2 >= 0) fScanner.unread();
					fLast = LAST_BACKSLASH;
					return;
				}
				fTokenLength++;
				fLast = LAST_OTHER;
				if (c2 == 'e') {
					if (readSeq2(SEQ_end)) {
						checkForEndEnv(false);
						return;
					}
					else {
						return;
					}
				}
				return;
			}
			else if (c == '\r' || c == '\n') {
				fLast = LAST_NEWLINE;
				return;
			}
			fLast = LAST_OTHER;
			return;
			
		case S_VERBATIM_SINGLELINE:
			if (c == fEndPattern[0]) {
				fLast = LAST_OTHER;
				newState(S_DEFAULT, 0);
				return;
			}
			else if (c == '\r' || c == '\n') {
				fLast = LAST_NEWLINE;
				newState(S_DEFAULT, 0);
				return;
			}
			fLast = LAST_OTHER;
			return;
			
		case S_COMMENT_SINGLELINE:
			readComment(c);
			return;
			
		case S_VERBATIM_ENV:
		case S_COMMENT_ENV:
			if (c == '\\') {
				final int c2 = fScanner.read();
				if (c2 <= 32) {
					if (c2 >= 0) fScanner.unread();
					fLast = LAST_BACKSLASH;
					return;
				}
				fTokenLength++;
				fLast = LAST_OTHER;
				if (c2 == 'e') {
					if (readSeq2(SEQ_end)) {
						checkForEndEnv(true);
						return;
					}
					else {
						return;
					}
				}
				return;
			}
			else if (c == '\r' || c == '\n') {
				fLast = LAST_NEWLINE;
				return;
			}
			fLast = LAST_OTHER;
			return;
			
		case S_INTERNAL_ENDENV:
			if (c == '}') {
				fLast = LAST_OTHER;
				newState(S_DEFAULT, 0);
				return;
			}
			else if (c == '\r' || c == '\n') {
				fLast = LAST_NEWLINE;
				return;
			}
			fLast = LAST_OTHER;
			return;
			
		default:
			handleExtState(state, c);
			return;
		}
	}
	
	protected final boolean readSeq2(final char[] seq) {
		for (int i = 1; i < seq.length; i++) {
			final int c = fScanner.read();
			if (c != seq[i]) {
				unread((c >= 0) ? i : (i-1));
				return false;
			}
		}
		fTokenLength += seq.length-1;
		return true;
	}
	
	protected final boolean readTempSeq(final char[] seq) {
		for (int i = 0; i < seq.length; i++) {
			final int c = fScanner.read();
			if (c != seq[i]) {
				unread((c >= 0) ? (i+1) : (i));
				return false;
			}
		}
		return true;
	}
	
	protected final boolean readChar(final char c1) {
		final int c = fScanner.read();
		if (c == c1) {
			fTokenLength ++;
			return true;
		}
		if (c >= 0) fScanner.unread();
		return false;
	}
	
	protected final boolean readTempChar(final char c1) {
		final int c = fScanner.read();
		if (c == c1) {
			return true;
		}
		if (c >= 0) fScanner.unread();
		return false;
	}
	
	protected final int readTempWhitespace() {
		int readed = 0;
		do {
			final int c = fScanner.read();
			if (c == ' ' || c == '\r' || c == '\n' || c == '\t') {
				readed++;
				continue;
			}
			if (c >= 0) fScanner.unread();
			return readed;
		} while (true);
	}
	
	private void unread(int count) {
		while (count-- > 0) {
			fScanner.unread();
		}
	}
	
	private void checkForBeginEnv() {
		int count = readTempWhitespace();
		
		count ++;
		int c = fScanner.read();
		if (c == '*') {
			count += readTempWhitespace();
			count ++;
			c = fScanner.read();
		}
		if (c != '{') {
			unread((c >= 0) ? count : (count-1));
			return;
		}
		
		final String name = fEnvNameRule.searchString(fScanner);
		if (name == null) {
			unread(count);
			return;
		}
		count += name.length();
		
		count ++;
		c = fScanner.read();
		if (c != '}') {
			unread((c >= 0) ? count : (count-1));
			return;
		}
		
		unread(count);
		final EnvType state = fEnvStates.get(name);
		fEndPattern = state.namePattern;
		newState(state.state, 6); // \begin Note: we don't prefix all, because of new line handling for chunks
	}
	
	private void checkForEndEnv(final boolean strict) {
		int count = 0;
		
		if (!strict) {
			count += readTempWhitespace();
		}
		
		count ++;
		if (fScanner.read() != '{') {
			unread(count);
			return;
		}
		
		if (!readTempSeq(fEndPattern)) {
			unread(count);
			return;
		}
		count += fEndPattern.length;
		
		count ++;
		if (fScanner.read() != '}') {
			unread(count);
			return;
		}
		
		if (strict) {
			fTokenLength += count;
			newState(S_DEFAULT, 0);
			return;
		}
		else {
			unread(count);
			fStateTokens[S_INTERNAL_ENDENV] = fStateTokens[fState];
			fState = S_INTERNAL_ENDENV;
			return;
		}
	}
	
	private void readComment(int c) {
		while (true) {
			if (c == '\r') {
				readChar('\n');
				fLast = LAST_NEWLINE;
				newState(S_DEFAULT, 0);
				return;
			}
			if (c == '\n') {
				readChar('\r');
				fLast = LAST_NEWLINE;
				newState(S_DEFAULT, 0);
				return;
			}
			c = fScanner.read();
			if (c < 0) {
				return;
			}
			fTokenLength++;
		}
	}
	
	
	protected void handleExtState(final int state, final int c) {
		if (c == '\r' || c == '\n') {
			fLast = LAST_NEWLINE;
			return;
		}
	}
	
	protected final void newState(final int newState, final int prefixLength) {
		if (fTokenLength-prefixLength > 0) {
			fToken = fStateTokens[fState];
			fState = newState;
			fTokenLength -= prefixLength;
			fPrefixLength = prefixLength;
			return;
		}
		fState = newState;
		fTokenLength = prefixLength;
		fPrefixLength = 0;
	}
	
	protected final void forceReturn(final int newState, final int prefixLength) {
		fToken = fStateTokens[fState];
		fState = newState;
		fTokenLength -= prefixLength;
		fPrefixLength = prefixLength;
		return;
	}
	
	protected final void initState(String contentType, final int prefixLength) throws BadLocationException {
		if (contentType == null) {
			fState = S_DEFAULT;
			prepareScan(fRangeStart, prefixLength);
			return;
		}
		contentType = contentType.intern();
		if (contentType == ITexDocumentConstants.TEX_DEFAULT_EXPL_CONTENT_TYPE || contentType == IDocument.DEFAULT_CONTENT_TYPE) {
			fState = S_DEFAULT;
			prepareScan(fRangeStart, prefixLength);
			return;
		}
		if (contentType == ITexDocumentConstants.TEX_MATH_CONTENT_TYPE
				|| contentType == ITexDocumentConstants.TEX_VERBATIM_CONTENT_TYPE
				|| contentType == ITexDocumentConstants.TEX_COMMENT_CONTENT_TYPE) {
			// we need the start pattern;
			if (prefixLength > 0) {
				prepareScan(fTokenOffset, 0);
				fTokenLength++;
				handleChar(S_DEFAULT, fScanner.read());
				fToken = null;
				if (prefixLength > 100) {
					prepareScan(fRangeStart, prefixLength);
				}
				else {
					fPrefixLength = fTokenLength;
					fTokenLength = 0;
				}
				return;
			}
			fState = S_DEFAULT;
			prepareScan(fRangeStart, 0);
			return;
		}
		fState = getExtState(contentType);
		prepareScan(fRangeStart, prefixLength);
	}
	
	protected int getExtState(final String contentType) {
		return S_DEFAULT;
	}
	
	
}
