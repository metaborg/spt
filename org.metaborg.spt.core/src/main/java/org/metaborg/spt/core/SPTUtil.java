package org.metaborg.spt.core;

import java.util.Iterator;

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

    public static final String SOME = "Some";

    public static final String START_SYMBOL_CONS = "StartSymbol";
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

    public static String noAnnosString(IStrategoTerm term) {
        return buildNoAnnosString(term, new StringBuilder()).toString();
    }

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

    private static final String ANNO = "Anno";
    private static final String LIST = "List";
    private static final String APPL = "Appl";
    private static final String INT = "Int";
    private static final String STRING = "String";
    private static final String WLD = "Wld";

    public static boolean checkATermMatch(IStrategoTerm ast, IStrategoTerm match, ITermFactory factory) {
        logger.debug("Checking match {} against term {}", match, ast);
        IStrategoList matchList = null;
        Iterator<IStrategoTerm> matchIt;
        boolean stop;
        final boolean result;
        switch(SPTUtil.consName(match)) {
            case ANNO:
                // Anno(Match, [AnnoMatch, ...])
                // check the term, and then check the annotations of the term
                result = checkATermMatch(ast, match.getSubterm(0), factory) && checkATermMatch(ast.getAnnotations(),
                    factory.makeAppl(factory.makeConstructor(LIST, 1), match.getSubterm(1)), factory);
                break;
            case LIST:
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
            case APPL:
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
            case INT:
                // Int("n")
                result = Term.isTermInt(ast)
                    && Integer.parseInt(Term.asJavaString(match.getSubterm(0))) == Term.asJavaInt(ast);
                break;
            case STRING:
                // String("some string")
                result =
                    Term.isTermString(ast) && Term.asJavaString(match.getSubterm(0)).equals(Term.asJavaString(ast));
                break;
            case WLD:
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

}
