package org.metaborg.meta.lang.spt.strategies;

import org.strategoxt.lang.JavaInteropRegisterer;
import org.strategoxt.lang.Strategy;

public class InteropRegisterer extends JavaInteropRegisterer {

    public InteropRegisterer() {
        super(new Strategy[] { plugin_strategy_invoke_0_2.instance, plugin_strategy_evaluate_1_2.instance,
            plugin_get_property_values_0_1.instance, get_service_input_term_0_1.instance,
            get_service_input_term_refactoring_0_1.instance, open_editor_0_0.instance,
            testlistener_init_0_0.instance, testlistener_add_testsuite_0_2.instance,
            testlistener_add_testcase_0_3.instance, testlistener_start_testcase_0_2.instance,
            testlistener_finish_testcase_0_4.instance, parse_spt_file_0_0.instance,
            get_completion_input_term_0_1.instance, get_observer_0_1.instance,
            get_reference_resolvers_0_1.instance,
            spt_exists_language_0_1.instance,
            spt_parse_fragments_0_3.instance,
            spt_resolve_marker_0_1.instance});
    }
}
