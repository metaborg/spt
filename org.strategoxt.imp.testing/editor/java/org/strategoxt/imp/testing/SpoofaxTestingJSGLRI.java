package org.strategoxt.imp.testing;

import static org.spoofax.interpreter.core.Tools.asJavaString;
import static org.spoofax.interpreter.core.Tools.isTermList;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getLeftToken;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getRightToken;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getTokenizer;
import static org.spoofax.terms.Term.termAt;
import static org.spoofax.terms.Term.tryGetConstructor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.imp.language.ILanguageService;
import org.eclipse.imp.language.Language;
import org.eclipse.imp.language.LanguageRegistry;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoConstructor;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.jsglr.client.imploder.ImploderAttachment;
import org.spoofax.jsglr.client.imploder.Tokenizer;
import org.spoofax.jsglr.shared.BadTokenException;
import org.spoofax.jsglr.shared.SGLRException;
import org.spoofax.jsglr.shared.TokenExpectedException;
import org.spoofax.terms.StrategoListIterator;
import org.spoofax.terms.TermTransformer;
import org.spoofax.terms.TermVisitor;
import org.spoofax.terms.attachments.ParentAttachment;
import org.spoofax.terms.attachments.ParentTermFactory;
import org.spoofax.terms.attachments.TermAttachmentSerializer;
import org.spoofax.terms.io.binary.SAFWriter;
import org.spoofax.terms.io.binary.TermReader;
import org.strategoxt.imp.runtime.Debug;
import org.strategoxt.imp.runtime.EditorState;
import org.strategoxt.imp.runtime.Environment;
import org.strategoxt.imp.runtime.dynamicloading.Descriptor;
import org.strategoxt.imp.runtime.dynamicloading.IDynamicLanguageService;
import org.strategoxt.imp.runtime.parser.JSGLRI;
import org.strategoxt.imp.runtime.parser.SGLRParseController;

public class SpoofaxTestingJSGLRI extends JSGLRI {

	private static final int PARSE_TIMEOUT = 20 * 1000;

	private static final long DISAMBIGUATE_TIMEOUT = 10 * 1000;

	private static final IStrategoConstructor INPUT_4 = Environment.getTermFactory().makeConstructor("Input", 4);

	private static final IStrategoConstructor OUTPUT_4 = Environment.getTermFactory().makeConstructor("Output", 4);

	private static final IStrategoConstructor ERROR_1 = Environment.getTermFactory().makeConstructor("Error", 1);

	private static final IStrategoConstructor LANGUAGE_1 = Environment.getTermFactory().makeConstructor("Language", 1);

	private static final IStrategoConstructor TARGET_LANGUAGE_1 = Environment.getTermFactory().makeConstructor(
		"TargetLanguage", 1);

	private static final IStrategoConstructor SETUP_3 = Environment.getTermFactory().makeConstructor("Setup", 3);

	private static final IStrategoConstructor TARGET_SETUP_3 = Environment.getTermFactory().makeConstructor(
		"TargetSetup", 3);

	private static final IStrategoConstructor TOPSORT_1 = Environment.getTermFactory().makeConstructor("TopSort", 1);

	private static final IStrategoConstructor TARGET_TOPSORT_1 = Environment.getTermFactory().makeConstructor(
		"TargetTopSort", 1);

	private static final IStrategoConstructor MARKED_3 = Environment.getTermFactory().makeConstructor("Marked", 3);

	private static final IStrategoConstructor QUOTEPART_1 = Environment.getTermFactory()
		.makeConstructor("QuotePart", 1);

	private final FragmentParser fragmentParser = new FragmentParser(SETUP_3, TOPSORT_1);

	private final FragmentParser outputFragmentParser = new FragmentParser(TARGET_SETUP_3, TARGET_TOPSORT_1);

	public SpoofaxTestingJSGLRI(JSGLRI template) {
		super(template.getParseTable(), template.getStartSymbol(), template.getController());
		setTimeout(PARSE_TIMEOUT, DISAMBIGUATE_TIMEOUT);
		setUseRecovery(true);
	}

	@Override
	protected IStrategoTerm doParse(String input, String filename) throws TokenExpectedException, BadTokenException,
		SGLRException, IOException, InterruptedException {

		IStrategoTerm ast = super.doParse(input, filename);
		return parseTestedFragments(ast);
	}

	private IStrategoTerm parseTestedFragments(final IStrategoTerm root) {
		final Tokenizer oldTokenizer = (Tokenizer) getTokenizer(root);
		final Retokenizer retokenizer = new Retokenizer(oldTokenizer);
		final ITermFactory nonParentFactory = Environment.getTermFactory();
		final ITermFactory factory = new ParentTermFactory(nonParentFactory);
		final FragmentParser testedParser = configureFragmentParser(root, getLanguage(root), fragmentParser);
		final FragmentParser outputParser =
			getTargetLanguage(root) == null ? testedParser : configureFragmentParser(root, getTargetLanguage(root),
				outputFragmentParser);
		assert !(nonParentFactory instanceof ParentTermFactory);

		if(testedParser == null || !testedParser.isInitialized() || outputParser == null
			|| !outputParser.isInitialized()) {
			return root;
		}

		IStrategoTerm result = new TermTransformer(factory, true) {
			@Override
			public IStrategoTerm preTransform(IStrategoTerm term) {
				IStrategoConstructor cons = tryGetConstructor(term);
				FragmentParser parser = null;
				if(cons == INPUT_4) {
					parser = testedParser;
				} else if(cons == OUTPUT_4) {
					parser = outputParser;
				}
				if(parser != null) {
					IStrategoList selected = getMarked(factory, term);
					IStrategoTerm fragmentHead = termAt(term, 1);
					IStrategoTerm fragmentTail = termAt(term, 2);
					retokenizer.copyTokensUpToIndex(getLeftToken(fragmentHead).getIndex() - 1);
					try {
						IStrategoTerm parsed = parser.parse(oldTokenizer, term, /* cons == OUTPUT_4 */false);
						int oldFragmentEndIndex = getRightToken(fragmentTail).getIndex();
						retokenizer.copyTokensFromFragment(fragmentHead, fragmentTail, parsed,
							getLeftToken(fragmentHead).getStartOffset(), getRightToken(fragmentTail).getEndOffset());
						if(!parser.isLastSyntaxCorrect())
							parsed = nonParentFactory.makeAppl(ERROR_1, parsed);
						ImploderAttachment implodement = ImploderAttachment.get(term);
						term = factory.annotateTerm(term, nonParentFactory.makeListCons(parsed, selected));
						term.putAttachment(implodement.clone());
						retokenizer.skipTokensUpToIndex(oldFragmentEndIndex);
					} catch(IOException e) {
						Debug.log("Could not parse tested code fragment", e);
					} catch(SGLRException e) {
						// TODO: attach ErrorMessage(_) term with error?
						Debug.log("Could not parse tested code fragment", e);
					} catch(CloneNotSupportedException e) {
						Environment.logException("Could not parse tested code fragment", e);
					} catch(RuntimeException e) {
						Environment.logException("Could not parse tested code fragment", e);
					} catch(InterruptedException e) {
						// TODO: attach ErrorMessage(_) term with error?
						Debug.log("Could not parse tested code fragment", e);
					}
				}
				return term;
			}

			@Override
			public IStrategoTerm postTransform(IStrategoTerm term) {
				Iterator<IStrategoTerm> iterator = TermVisitor.tryGetListIterator(term);
				for(int i = 0, max = term.getSubtermCount(); i < max; i++) {
					IStrategoTerm kid = iterator == null ? term.getSubterm(i) : iterator.next();
					ParentAttachment.putParent(kid, term, null);
				}
				return term;
			}
		}.transform(root);

		retokenizer.copyTokensAfterFragments();
		retokenizer.getTokenizer().setAst(result);
		retokenizer.getTokenizer().initAstNodeBinding();
		return result;
	}

	private IStrategoList getMarked(final ITermFactory factory, IStrategoTerm testsuite) {
		final List<IStrategoTerm> results = new ArrayList<IStrategoTerm>();

		new TermTransformer(factory, false) {

			@Override
			public IStrategoTerm preTransform(IStrategoTerm term) {
				IStrategoConstructor cons = tryGetConstructor(term);

				if(cons != null && cons.equals(MARKED_3)) {
					IStrategoAppl quotepart = (IStrategoAppl) term.getSubterm(1);
					if(quotepart.getConstructor().equals(QUOTEPART_1)) {
						IStrategoTerm marked = quotepart.getSubterm(0);

						// Cloning term because (origin) attachments of this term are changed by parseTestedFragments.
						results.add(cloneTerm(factory, marked));
					}
				}

				return term;
			}
		}.transform(testsuite);

		return factory.makeList(results);
	}

	private IStrategoTerm cloneTerm(final ITermFactory factory, IStrategoTerm term) {
		try {
			// Serialize attachments to annotations.
			final TermAttachmentSerializer serializer = new TermAttachmentSerializer(factory);
			term = serializer.toAnnotations(term);

			// Write term to memory as byte array.
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(bos);
			SAFWriter.writeTermToSAFStream(term, out);
			out.flush();

			// Read term from memory.
			TermReader reader = new TermReader(factory);
			ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
			ObjectInputStream in = new ObjectInputStream(bis);
			term = reader.parseFromStream(in);

			// Close streams
			out.close();
			in.close();

			// Deserialize annotations to attachements.
			return serializer.fromAnnotations(term, false);
		} catch(IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private FragmentParser
		configureFragmentParser(IStrategoTerm root, Language language, FragmentParser fragmentParser) {
		if(language == null)
			return null;
		Descriptor descriptor = Environment.getDescriptor(language);
		if(descriptor == null)
			return null;
		fragmentParser.configure(descriptor, getController().getRelativePath(), getController().getProject(), root);
		attachToLanguage(language);
		return fragmentParser;
	}

	private String getLanguageName(IStrategoTerm root, IStrategoConstructor which) {
		if(root.getSubtermCount() < 1 || !isTermList(termAt(root, 0)))
			return null;
		IStrategoList headers = termAt(root, 0);
		for(IStrategoTerm header : StrategoListIterator.iterable(headers)) {
			if(tryGetConstructor(header) == which) {
				IStrategoString name = termAt(header, 0);
				return asJavaString(name);
			}
		}
		return null;
	}

	private Language getLanguage(IStrategoTerm root) {
		final String languageName = getLanguageName(root, LANGUAGE_1);
		if(languageName == null)
			return null;
		return LanguageRegistry.findLanguage(languageName);
	}

	private Language getTargetLanguage(IStrategoTerm root) {
		String languageName = getLanguageName(root, TARGET_LANGUAGE_1);
		if(languageName == null)
			return null;
		return LanguageRegistry.findLanguage(languageName);
	}

	/**
	 * Add our language service to the descriptor of a fragment language, so our service gets reinitialized once the
	 * fragment language changes.
	 */
	private void attachToLanguage(Language theirLanguage) {
		SGLRParseController myController = getController();
		EditorState myEditor = myController.getEditor();
		if(myEditor == null)
			return;
		ILanguageService myWrapper = myEditor.getEditor().getParseController();
		if(myWrapper instanceof IDynamicLanguageService) {
			Descriptor theirDescriptor = Environment.getDescriptor(theirLanguage);
			theirDescriptor.addActiveService((IDynamicLanguageService) myWrapper);
		} else {
			Environment.logException("SpoofaxTestingParseController wrapper is not IDynamicLanguageService");
		}
	}
}
