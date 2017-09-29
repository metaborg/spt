package org.metaborg.spt.testrunner.intellij

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile

/**
 * Utility functions.
 */
object LanguageUtils {

  /**
   * Gets the language content root of the specified module.
   *
   * @param module The module; or null.
   * @return The first language content root; or null if not found.
   */
  fun getLanguageRoot(module: Module?): VirtualFile? {
    if (module == null) return null
    return ModuleRootManager.getInstance(module).contentRoots
        .firstOrNull { r -> r.findChild("metaborg.yaml") != null }
  }
}