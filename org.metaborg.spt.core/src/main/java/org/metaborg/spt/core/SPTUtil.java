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
import org.spoofax.terms.Term;

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

    /**
     * Get the name of the constructor of this term if it has one.
     * 
     * @return the constructor name, or null if this term has no constructor.
     */
    public static String consName(IStrategoTerm term) {
        if(Term.isTermAppl(term)) {
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
        if(Term.isTermAppl(term) || Term.isTermList(term) || Term.isTermTuple(term)) {
            // print constructor name
            if(Term.isTermAppl(term)) {
                b.append(((IStrategoAppl) term).getConstructor().getName());
            }

            // print opening brace
            if(Term.isTermAppl(term) || Term.isTermTuple(term)) {
                b.append('(');
            } else if(Term.isTermList(term)) {
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
            if(Term.isTermAppl(term) || Term.isTermTuple(term)) {
                b.append(')');
            } else if(Term.isTermList(term)) {
                b.append(']');
            }
        } else if(Term.isTermString(term)) {
            b.append(Term.asJavaString(term));
        } else if(Term.isTermInt(term)) {
            b.append(Term.asJavaInt(term));
        } else {
            logger.debug("Term {} is not a String or Int or thing with kids.", term.getClass());
            b.append(term.toString());
        }
        return b;
    }

    private static final String ANNO_CONS = "Anno";
    private static final String LIST_CONS = "List";
    private static final String APPL_CONS = "Appl";
    private static final String INT_CONS = "Int";
    private static final String STRING_CONS = "String";
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
                if(!Term.isTermList(ast)) {
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
                if(!Term.isTermAppl(ast)) {
                    logger.debug("The term is not an application.");
                    result = false;
                    break;
                }
                if(!SPTUtil.consName(ast).equals(Term.asJavaString(match.getSubterm(0)))) {
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
                result = Term.isTermInt(ast)
                    && Integer.parseInt(Term.asJavaString(match.getSubterm(0))) == Term.asJavaInt(ast);
                break;
            case STRING_CONS:
                // String("some string")
                result =
                    Term.isTermString(ast) && Term.asJavaString(match.getSubterm(0)).equals(Term.asJavaString(ast));
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
                b.append(Term.asJavaString(match.getSubterm(0))).append('(');
                prettyPrintListOfMatches((IStrategoList) match.getSubterm(1), ", ", b);
                b.append(')');
                return b;
            case INT_CONS:
                // Int("n")
                b.append(Term.asJavaString(match.getSubterm(0)));
                return b;
            case STRING_CONS:
                // String("some string")
                b.append(Term.asJavaString(match.getSubterm(0)));
                return b;
            case WLD_CONS:
                b.append('_');
                return b;
            default:
                logger.warn("Can't pretty print the pattern {}", match);
                throw new IllegalArgumentException(String.format("Can't pretty print the pattern %s.", match));
        }
    }

    private static StringBuilder prettyPrintListOfMatches(IStrategoList matches, String join, StringBuilder b) {
        Iterator<IStrategoTerm> matchIt = matches.iterator();
        for(int i = 0; i < matches.size(); i++) {
            prettyPrintMatch(matchIt.next(), b);
            if(i < matches.size() - 1) {
                b.append(join);
            }
        }
        return b;
    }
}
