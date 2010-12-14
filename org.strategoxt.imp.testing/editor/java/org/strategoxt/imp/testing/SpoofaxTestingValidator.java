package org.strategoxt.imp.testing;

import org.strategoxt.imp.runtime.dynamicloading.Descriptor;
import org.strategoxt.imp.runtime.services.MetaFileLanguageValidator;

public class SpoofaxTestingValidator extends MetaFileLanguageValidator 
{ 
  @Override public Descriptor getDescriptor()
  { 
    return SpoofaxTestingParseController.getDescriptor();
  }
}