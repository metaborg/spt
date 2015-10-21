package org.metaborg.meta.lang.spt.strategies;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.context.IContext;
import org.metaborg.core.resource.IResourceService;
import org.metaborg.spoofax.core.terms.ITermFactoryService;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.terms.Term;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import com.google.inject.Injector;

/**
 * Return the absolute path to the file.
 * The relative filePath will be resolved in the absolute projectPath.
 */
public class get_absolute_path_0_2 extends Strategy {
	public static final get_absolute_path_0_2 instance = new get_absolute_path_0_2();
	private get_absolute_path_0_2(){}
	
	@Override
	public IStrategoTerm invoke(Context context, IStrategoTerm UNUSED,
			IStrategoTerm projectPath, IStrategoTerm filePath) {
		final Injector i = ((IContext) context.contextObject()).injector();
		final IResourceService r = i.getInstance(IResourceService.class);
		final ITermFactory f = i.getInstance(ITermFactoryService.class).getGeneric();
		final FileObject path = r.resolve(r.resolve(Term.asJavaString(projectPath)), Term.asJavaString(filePath));
		return f.makeString(r.localPath(path).getAbsolutePath());
	}

}
