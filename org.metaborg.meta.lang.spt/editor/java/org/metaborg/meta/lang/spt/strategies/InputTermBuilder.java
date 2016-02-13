package org.metaborg.meta.lang.spt.strategies;

import static org.spoofax.terms.attachments.ParentAttachment.getRoot;

import java.io.File;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.metaborg.core.context.IContext;
import org.metaborg.core.project.IProjectService;
import org.metaborg.core.resource.IResourceService;
import org.metaborg.spoofax.core.syntax.SourceAttachment;
import org.metaborg.spoofax.core.terms.ITermFactoryService;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.IStrategoTuple;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.jsglr.client.imploder.TermTreeFactory;
import org.spoofax.terms.StrategoSubList;
import org.strategoxt.lang.Context;

import com.google.inject.Injector;

/**
 * Builder of Stratego editor service input tuples.
 * 
 * @author Lennart Kats <lennart add lclnet.nl>
 */
public class InputTermBuilder {
    private final ITermFactory factory;

    private final Context context;
    
    private final Injector injector;

    public InputTermBuilder(Context context) {
        this.context = context;
        this.injector = ((IContext) context.contextObject()).injector();
        // TODO: should we use context.getFactory() instead? 
        this.factory = injector.getInstance(ITermFactoryService.class).getGeneric();
    }

    public IStrategoTuple makeInputTermSourceAst(IStrategoTerm node, boolean includeSubNode)
        throws FileSystemException {
        IStrategoTerm targetTerm = node;
        IStrategoList termPath = StrategoTermPath.createPath(factory, node);
        IStrategoTerm rootTerm = getRoot(node);
        return makeInputTerm(node, includeSubNode, termPath, targetTerm, rootTerm);
    }

    public IStrategoTuple makeInputTermResultingAst(IStrategoTerm resultingAst, IStrategoTerm node,
        boolean includeSubNode) throws FileSystemException {
        IStrategoList termPath = StrategoTermPath.getTermPathWithOrigin(context, resultingAst, node);
        if(termPath == null)
            return makeInputTermSourceAst(node, includeSubNode);
        IStrategoTerm targetTerm = StrategoTermPath.getTermAtPath(context, resultingAst, termPath);
        if(node instanceof StrategoSubList) {
            if(!(targetTerm instanceof IStrategoList))
                return makeInputTermSourceAst(node, includeSubNode);
            targetTerm = mkSubListTarget(resultingAst, (IStrategoList) targetTerm, (StrategoSubList) node);
            if(targetTerm == null) // only accept sublists that correspond to selection
                return makeInputTermSourceAst(node, includeSubNode);
        }
        IStrategoTerm rootTerm = resultingAst;
        return makeInputTerm(node, includeSubNode, termPath, targetTerm, rootTerm);
    }

    public IStrategoTuple makeInputTerm(IStrategoTerm resultingAst, IStrategoTerm node,
        boolean includeSubNode, boolean source) throws FileSystemException {
        return source ? makeInputTermSourceAst(node, includeSubNode) : makeInputTermResultingAst(
            resultingAst, node, includeSubNode);
    }

    public IStrategoTerm makeInputTermRefactoring(IStrategoTerm resultingAst, IStrategoTerm userInput,
        IStrategoTerm node, boolean includeSubNode, boolean source) throws FileSystemException {
        IStrategoTuple tuple = makeInputTerm(resultingAst, node, includeSubNode, source);
        IStrategoTerm[] inputParts = new IStrategoTerm[tuple.getSubtermCount() + 1];
        inputParts[0] = userInput;
        System.arraycopy(tuple.getAllSubterms(), 0, inputParts, 1, tuple.getSubtermCount());
        return factory.makeTuple(inputParts);
    }

    private IStrategoTerm mkSubListTarget(IStrategoTerm resultingAst, IStrategoList targetTerm,
        StrategoSubList node) {
        IStrategoTerm firstChild = getResultingTerm(resultingAst, node.getFirstChild());
        IStrategoTerm lastChild = getResultingTerm(resultingAst, node.getLastChild());
        if(firstChild == null || lastChild == null)
            return null;
        return new TermTreeFactory(factory).createSublist(targetTerm, firstChild, lastChild);
    }

    private IStrategoTerm getResultingTerm(IStrategoTerm resultingAst, IStrategoTerm originTerm) {
        IStrategoList pathFirstChild =
            StrategoTermPath.getTermPathWithOrigin(context, resultingAst, originTerm);
        IStrategoTerm firstChild = null;
        if(pathFirstChild != null)
            firstChild = StrategoTermPath.getTermAtPath(context, resultingAst, pathFirstChild);
        return firstChild;
    }

    public IStrategoTuple makeInputTerm(IStrategoTerm node, boolean includeSubNode, IStrategoList termPath,
        IStrategoTerm targetTerm, IStrategoTerm rootTerm) throws FileSystemException {
        assert factory.getDefaultStorageType() == IStrategoTerm.MUTABLE;
        final IResourceService resourceService = injector.getInstance(IResourceService.class);
        final IProjectService projService = injector.getInstance(IProjectService.class);
        final FileObject file = SourceAttachment.getResource(node, resourceService);
        final FileObject project = projService.get(file).location();
        String path, projectPath;
        if(file != null && project != null) {
            projectPath = project.getName().getPath();
            path = project.getName().getRelativeName(file.getName());
            assert !new File(path).isAbsolute();
        } else {
            projectPath = ".";
            path = "string";
        }

        if(includeSubNode) {
            IStrategoTerm[] inputParts =
                { targetTerm, termPath, rootTerm, factory.makeString(path), factory.makeString(projectPath) };
            return factory.makeTuple(inputParts);
        } else {
            IStrategoTerm[] inputParts = { node, factory.makeString(path), factory.makeString(projectPath) };
            return factory.makeTuple(inputParts);
        }
    }
}