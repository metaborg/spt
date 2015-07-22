package org.metaborg.meta.lang.spt.strategies;

import static org.spoofax.interpreter.core.Tools.isTermString;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.language.ILanguageService;
import org.metaborg.core.resource.IResourceService;
import org.metaborg.core.source.ISourceTextService;
import org.metaborg.core.syntax.ISyntaxService;
import org.metaborg.spoofax.core.syntax.IParserConfig;
import org.metaborg.spoofax.core.syntax.JSGLRI;
import org.metaborg.spoofax.core.syntax.JSGLRParserConfiguration;
import org.metaborg.spoofax.core.syntax.JSGLRSyntaxService;
import org.metaborg.spoofax.core.syntax.ParserConfig;
import org.metaborg.sunshine.environment.ServiceRegistry;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.shared.SGLRException;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import com.google.inject.TypeLiteral;

/**
 * parse-spt-string strategy to get AST of Spoofax-Testing testsuite, where the input fragments have been annotated with
 * the AST of the input.
 * 
 * The current term is the string to parse and the sole term argument is an absolute path to the file this string is
 * coming from.
 */
public class parse_spt_file_0_0 extends Strategy {

    public static parse_spt_file_0_0 instance = new parse_spt_file_0_0();

    @Override public IStrategoTerm invoke(Context context, IStrategoTerm current) {
        if(!isTermString(current))
            return null;

        final ServiceRegistry services = ServiceRegistry.INSTANCE();
        final IResourceService resourceService = services.getService(IResourceService.class);
        final ILanguageService languageService = services.getService(ILanguageService.class);
        final ISourceTextService sourceTextService = services.getService(ISourceTextService.class);
        final JSGLRSyntaxService syntaxService =
            (JSGLRSyntaxService) services.getService(new TypeLiteral<ISyntaxService<IStrategoTerm>>() {});

        final String filename = ((IStrategoString) current).stringValue();
        final FileObject file = resourceService.resolve(filename);
        final ILanguageImpl language = languageService.get("Spoofax-Testing");
        final IParserConfig existingConfig = syntaxService.getParserConfig(language);
        final IParserConfig newConfig =
            new ParserConfig(existingConfig.getStartSymbol(), existingConfig.getParseTableProvider());

        try {
            final String inputText = sourceTextService.text(file);
            final JSGLRI jsglri = new JSGLRI(newConfig, context.getFactory(), language, null, file, inputText);
            final SpoofaxTestingJSGLRI parser = new SpoofaxTestingJSGLRI(jsglri);
            final IStrategoTerm res =
                (IStrategoTerm) parser.actuallyParse(IOUtils.toString(file.getContent().getInputStream()), filename,
                    new JSGLRParserConfiguration(true, false, false, 24 * 1000)).output;
            return res;
        } catch(SGLRException | InterruptedException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }
}
