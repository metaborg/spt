package org.metaborg.spt.core;

import java.util.Iterator;

import javax.annotation.Nullable;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.terms.StrategoAnnotation;
import org.spoofax.terms.util.StringUtils;
import org.spoofax.terms.util.TermUtils;

public class SPTUtil {

    private static final ILogger logger = LoggerUtils.logger(SPTUtil.class);

    public static final String SOME_CONS = "Some";
    public static final String NONE_CONS = "None";

    public static final String NAME_CONS = "Name";
    public static final String START_SYMBOL_CONS = "StartSymbol";
    public static final String LANG_CONS = "Language";
    public static final String FIXTURE_CONS = "Fixture";
    public static final String TEST_CONS = "Test";
    public static final String SELECTION_CONS = "Selection";
    public static final String FRAGMENT_CONS = "Fragment";
    public static final String TAILPART_DONE_CONS = "Done";
    public static final String TAILPART_MORE_CONS = "More";
    private static final String INT_CONS = "Int";
    private static final String STRING_CONS = "String";
    private static final String SELECTION_REF_CONS ="SelectionRef";

    /**
     * Get the name of the constructor of this term if it has one.
     * 
     * @return the constructor name, or null if this term has no constructor.
     */
    @Nullable public static String consName(IStrategoTerm term) {
        if(TermUtils.isAppl(term)) {
            return ((IStrategoAppl) term).getConstructor().getName();
        } else {
            return null;
        }
    }

    /**
     * Check if the term is an Option type.
     * 
     * @param term
     *            the term to check.
     * @return true iff the term is a Some with 1 child or a None().
     */
    public static boolean checkOption(IStrategoTerm term) {
        switch(consName(term)) {
            case SOME_CONS:
                return term.getSubtermCount() == 1;
            case NONE_CONS:
                return term.getSubtermCount() == 0;
            default:
                return false;
        }
    }

    /**
     * Get the value from an Option.
     * 
     * @param term
     *            the Option term (see {@link #checkOption(IStrategoTerm)}).
     * @return the term inside the Option if it was Some, null otherwise.
     */
    public static @Nullable IStrategoTerm getOptionValue(IStrategoTerm term) {
        if(checkOption(term)) {
            return term.getSubtermCount() == 0 ? null : term.getSubterm(0);
        } else {
            throw new IllegalArgumentException("The term " + noAnnosString(term) + " is not an Option.");
        }
    }

    /**
     * Create a String representation of an IStrategoTerm, ignoring any annotations on the term.
     * 
     * @param term
     *            the term to print.
     * @return a String representation of the term, without any annotations.
     */
    public static String noAnnosString(IStrategoTerm term) {
        return buildNoAnnosString(term, new StringBuilder()).toString();
    }

    /**
     * Build a String representation of an IStrategoTerm, ignoring any annotations on the term.
     * 
     * @param term
     *            the term to print.
     * @param b
     *            a StringBuilder to append the String to.
     * @return a String representation of the term, without any annotations.
     */
    public static StringBuilder buildNoAnnosString(IStrategoTerm term, StringBuilder b) {
        while(term instanceof StrategoAnnotation) {
            term = ((StrategoAnnotation) term).getWrapped();
        }
        if(TermUtils.isAppl(term) || TermUtils.isList(term) || TermUtils.isTuple(term)) {
            // print constructor name
            if(TermUtils.isAppl(term)) {
                b.append(((IStrategoAppl) term).getConstructor().getName());
            }

            // print opening brace
            if(TermUtils.isAppl(term) || TermUtils.isTuple(term)) {
                b.append('(');
            } else if(TermUtils.isList(term)) {
                b.append('[');
            }

            // print children
            for(IStrategoTerm child : term.getAllSubterms()) {
                buildNoAnnosString(child, b);
                b.append(',');
            }
            if(term.getSubtermCount() > 0) {
                b.delete(b.length() - 1, b.length());
            }

            // print closing brace
            if(TermUtils.isAppl(term) || TermUtils.isTuple(term)) {
                b.append(')');
            } else if(TermUtils.isList(term)) {
                b.append(']');
            }
        } else if(TermUtils.isString(term)) {
            b.append(TermUtils.toJavaString(term));
        } else if(TermUtils.isInt(term)) {
            b.append(TermUtils.toJavaInt(term));
        } else {
            logger.debug("Term {} is not a String or Int or thing with kids.", term.getClass());
            b.append(term.toString());
        }
        return b;
    }

    private static final String ANNO_CONS = "Anno";
    private static final String LIST_CONS = "List";
    private static final String APPL_CONS = "Appl";
    private static final String WLD_CONS = "Wld";

    /**
     * Check if the given AST matches the given SPT ATerm match pattern.
     * 
     * @param ast
     *            the AST to compare to the pattern.
     * @param match
     *            the SPT pattern to match the AST against.
     * @param factory
     *            required to make SPT constructors.
     * @return true iff the AST matched against the given pattern.
     */
    public static boolean checkATermMatch(IStrategoTerm ast, IStrategoTerm match, ITermFactory factory) {
        logger.debug("Checking match {} against term {}", match, ast);
        IStrategoList matchList = null;
        Iterator<IStrategoTerm> matchIt;
        boolean stop;
        final boolean result;
        switch(SPTUtil.consName(match)) {
            case ANNO_CONS:
                // Anno(Match, [AnnoMatch, ...])
                // check the term, and then check the annotations of the term
                result = checkATermMatch(ast, match.getSubterm(0), factory) && checkATermMatch(ast.getAnnotations(),
                    factory.makeAppl(factory.makeConstructor(LIST_CONS, 1), match.getSubterm(1)), factory);
                break;
            case LIST_CONS:
                // List([Match, ...])
                if(!TermUtils.isList(ast)) {
                    result = false;
                    break;
                }
                final IStrategoList list = (IStrategoList) ast;
                matchList = (IStrategoList) match.getSubterm(0);
                if(matchList.size() != list.size()) {
                    result = false;
                    break;
                }
                matchIt = matchList.iterator();
                final Iterator<IStrategoTerm> listIt = list.iterator();
                stop = false;
                while(matchIt.hasNext()) {
                    if(!checkATermMatch(listIt.next(), matchIt.next(), factory)) {
                        stop = true;
                        break;
                    }
                }
                result = !stop;
                break;
            case APPL_CONS:
                // Appl("ConsName", [KidMatch, ...])
                // we ignore any annotations on the AST
                if(!TermUtils.isAppl(ast)) {
                    logger.debug("The term is not an application.");
                    result = false;
                    break;
                }
                if(!SPTUtil.consName(ast).equals(TermUtils.toJavaString(match.getSubterm(0)))) {
                    logger.debug("The constructor {}, did not match the expected constructor {}.",
                        SPTUtil.consName(ast), match.getSubterm(0));
                    result = false;
                    break;
                }
                matchList = (IStrategoList) match.getSubterm(1);
                if(ast.getSubtermCount() != matchList.size()) {
                    logger.debug("The number of children {}, did not match the expected number {}",
                        ast.getSubtermCount(), matchList.size());
                    result = false;
                    break;
                }
                matchIt = matchList.iterator();
                stop = false;
                for(int i = 0; i < ast.getSubtermCount(); i++) {
                    if(!checkATermMatch(ast.getSubterm(i), matchIt.next(), factory)) {
                        stop = true;
                        break;
                    }
                }
                result = !stop;
                break;
            case INT_CONS:
                // Int("n")
                result = TermUtils.isInt(ast)
                    && Integer.parseInt(TermUtils.toJavaString(match.getSubterm(0))) == TermUtils.toJavaInt(ast);
                break;
            case STRING_CONS:
                // String("some string")
                result =
                    TermUtils.isString(ast) && TermUtils.toJavaString(match.getSubterm(0)).equals(TermUtils.toJavaString(ast));
                break;
            case WLD_CONS:
                result = true;
                break;
            default:
                logger.warn("Can't check an ast against the pattern {}", match);
                result = false;
                break;
        }
        logger.debug("Result of match: {}", result);
        return result;
    }

    /**
     * Pretty print the given SPT ATerm pattern.
     * 
     * @param match
     *            the SPT ATerm pattern to pretty print to a 'normal' ATerm.
     * @return the pretty printed result.
     */
    public static String prettyPrintMatch(IStrategoTerm match) {
        return prettyPrintMatch(match, new StringBuilder()).toString();
    }

    /**
     * Append a pretty printed version of the given SPT pattern to the given builder.
     * 
     * @param match
     *            the SPT ATerm pattern to pretty print to a 'normal' ATerm.
     * @param b
     *            the builder to append to.
     * @return the given builder.
     */
    public static StringBuilder prettyPrintMatch(IStrategoTerm match, StringBuilder b) {
        switch(SPTUtil.consName(match)) {
            case ANNO_CONS:
                // Anno(Match, [AnnoMatch, ...])
                prettyPrintMatch(match.getSubterm(0), b).append("{");
                prettyPrintListOfMatches((IStrategoList) match.getSubterm(1), ", ", b);
                b.append('}');
                return b;
            case LIST_CONS:
                // List([Match, ...])
                b.append('[');
                prettyPrintListOfMatches((IStrategoList) match.getSubterm(0), ", ", b);
                b.append(']');
                return b;
            case APPL_CONS:
                // Appl("ConsName", [KidMatch, ...])
                b.append(TermUtils.toJavaString(match.getSubterm(0))).append('(');
                prettyPrintListOfMatches((IStrategoList) match.getSubterm(1), ", ", b);
                b.append(')');
                return b;
            case INT_CONS:
                // Int("n")
                b.append(TermUtils.toJavaString(match.getSubterm(0)));
                return b;
            case STRING_CONS:
                // String("some string")
                b.append(StringUtils.escape(TermUtils.toJavaStringAt(match, 0)));
                return b;
            case WLD_CONS:
                b.append('_');
                return b;
            default:
                logger.warn("Can't pretty print the pattern {}", match);
                throw new IllegalArgumentException(String.format("Can't pretty print the pattern %s.", match));
        }
    }

    private static void prettyPrintListOfMatches(IStrategoList matches, String join, StringBuilder b) {
        Iterator<IStrategoTerm> matchIt = matches.iterator();
        for(int i = 0; i < matches.size(); i++) {
            prettyPrintMatch(matchIt.next(), b);
            if(i < matches.size() - 1) {
                b.append(join);
            }
        }
    }
    
    public static boolean isStringLiteral(IStrategoTerm arg) {
		return isStringConsEqual(arg, STRING_CONS);
    }
    
    public static boolean isIntLiteral(IStrategoTerm arg) {
		return isStringConsEqual(arg, INT_CONS);
    }
    
    public static boolean isSelectionRef(IStrategoTerm arg) {
		return isStringConsEqual(arg, SELECTION_REF_CONS);
    }
    
    public static boolean isStringConsEqual(IStrategoTerm arg, String consName) {
    	if (arg == null || consName == null) {
    		return false;
    	}
    	
		String argConsName = consName(arg);
		return consName.equals(argConsName);
    }
}
