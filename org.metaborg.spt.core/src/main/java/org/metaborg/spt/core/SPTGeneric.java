package org.metaborg.spt.core;

import org.metaborg.core.analysis.IAnalyzeUnit;
import org.metaborg.core.analysis.IAnalyzeUnitUpdate;
import org.metaborg.core.syntax.IInputUnit;
import org.metaborg.core.syntax.IParseUnit;
import org.metaborg.core.transform.ITransformUnit;

/**
 * Generic facade for SPT.
 * 
 * Metaborg core works for all languages that support the types for which it is instantiated. That means that
 *
 * @param <I>
 * @param <P>
 * @param <A>
 * @param <AU>
 * @param <T>
 * @param <TP>
 * @param <TA>
 * @param <F>
 */
public class SPTGeneric<I extends IInputUnit, P extends IParseUnit, A extends IAnalyzeUnit, AU extends IAnalyzeUnitUpdate, T extends ITransformUnit<?>, TP extends ITransformUnit<P>, TA extends ITransformUnit<A>, F> {

}
