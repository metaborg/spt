package org.strategoxt.imp.testing.strategies;

import static org.spoofax.interpreter.core.Tools.isTermString;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;
import org.metaborg.spoofax.core.language.ILanguage;
import org.metaborg.spoofax.core.language.ILanguageService;
import org.metaborg.spoofax.core.parser.IParseService;
import org.metaborg.spoofax.core.parser.jsglr.IParserConfig;
import org.metaborg.spoofax.core.parser.jsglr.JSGLRI;
import org.metaborg.spoofax.core.parser.jsglr.JSGLRParseService;
import org.metaborg.spoofax.core.parser.jsglr.ParserConfig;
import org.metaborg.spoofax.core.resource.IResourceService;
import org.metaborg.sunshine.environment.ServiceRegistry;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.shared.SGLRException;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

/**
 * parse-spt-string strategy to get AST of Spoofax-Testing testsuite, where the input fragments have been
 * annotated with the AST of the input.
 * 
 * The current term is the string to parse and the sole term argument is an absolute path to the file this
 * string is coming from.
 */
public class parse_spt_file_0_0 extends Strategy {

    public static parse_spt_file_0_0 instance = new parse_spt_file_0_0();

    @Override public IStrategoTerm invoke(Context context, IStrategoTerm current) {
        if(!isTermString(current))
            return null;
        String filename = ((IStrategoString) current).stringValue();
        FileObject file = ServiceRegistry.INSTANCE().getService(IResourceService.class).resolve(filename);

        ILanguage l = ServiceRegistry.INSTANCE().getService(ILanguageService.class).get("Spoofax-Testing");
        JSGLRParseService parseService =
            (JSGLRParseService) ServiceRegistry.INSTANCE().getService(IParseService.class);
        IParserConfig existingConfig = parseService.getParserConfig(l);

        IParserConfig c =
            new ParserConfig(existingConfig.getStartSymbol(), existingConfig.getParseTableProvider(),
                24 * 1000);
        JSGLRI p = new JSGLRI(c, context.getFactory(), file);
        SpoofaxTestingJSGLRI parser = new SpoofaxTestingJSGLRI(p);
        parser.setUseRecovery(false);


        try {
            IStrategoTerm res =
                parser.actuallyParse(IOUtils.toString(file.getContent().getInputStream()), filename);
            // System.out.println("PARSE RESULT: " + res.toString());
            return res;
        } catch(SGLRException | InterruptedException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

}
