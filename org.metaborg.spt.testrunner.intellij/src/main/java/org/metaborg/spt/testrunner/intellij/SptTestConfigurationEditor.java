package org.metaborg.spt.testrunner.intellij;

import com.intellij.application.options.ModulesComboBox;
import com.intellij.execution.ui.CommonJavaParametersPanel;
import com.intellij.execution.ui.ConfigurationModuleSelector;
import com.intellij.execution.ui.DefaultJreSelector;
import com.intellij.execution.ui.JrePathEditor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.PanelWithAnchor;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class SptTestConfigurationEditor extends SettingsEditor<SptTestConfiguration> implements PanelWithAnchor {
    private JPanel mainPanel;
    private LabeledComponent<TextFieldWithBrowseButton> testPathPicker;
    private CommonJavaParametersPanel commonJavaParametersPanel;
    private JrePathEditor jrePathEditor;
    private LabeledComponent<ModulesComboBox> lutModulePicker;
    private final ConfigurationModuleSelector lutModuleSelector;
    private JComponent anchor;

    public SptTestConfigurationEditor(Project project) {
        TextFieldWithBrowseButton testFolderComponent = testPathPicker.getComponent();
        ModulesComboBox lutModulesComponent = lutModulePicker.getComponent();

        testFolderComponent.addBrowseFolderListener(
                "Test Folder",
                "Specify folder with tests",
                project,
                FileChooserDescriptorFactory.createSingleFolderDescriptor()
        );

        this.lutModuleSelector = new ConfigurationModuleSelector(project, lutModulesComponent);
        lutModulesComponent.addActionListener(e -> commonJavaParametersPanel.setModuleContext(lutModuleSelector.getModule()));

        this.commonJavaParametersPanel.setModuleContext(this.lutModuleSelector.getModule());
        this.jrePathEditor.setDefaultJreSelector(DefaultJreSelector.fromModuleDependencies(lutModulesComponent, false));

        this.anchor = UIUtil.mergeComponentsWithAnchor(
                this.testPathPicker,
                this.commonJavaParametersPanel,
                this.lutModulePicker,
                this.jrePathEditor
        );
    }

    @Override
    protected void resetEditorFrom(@NotNull SptTestConfiguration configuration) {
        this.testPathPicker.getComponent().setText(configuration.getTestPath());
        this.commonJavaParametersPanel.reset(configuration);
        this.lutModuleSelector.reset(configuration);

        this.jrePathEditor.setPathOrName(configuration.getAlternativeJrePath(), configuration.isAlternativeJrePathEnabled());
    }

    @Override
    protected void applyEditorTo(@NotNull SptTestConfiguration configuration) throws ConfigurationException {
        configuration.setTestPath(this.testPathPicker.getComponent().getText().trim());
        this.commonJavaParametersPanel.applyTo(configuration);
        this.lutModuleSelector.applyTo(configuration);

        configuration.setAlternativeJrePathEnabled(this.jrePathEditor.isAlternativeJreSelected());
        configuration.setAlternativeJrePath(this.jrePathEditor.getJrePathOrName());
    }

    @NotNull
    @Override
    protected JComponent createEditor() {
        return this.mainPanel;
    }

    @Override
    public JComponent getAnchor() {
        return this.anchor;
    }

    @Override
    public void setAnchor(JComponent anchor) {
        this.anchor = anchor;
        this.testPathPicker.setAnchor(anchor);
        this.commonJavaParametersPanel.setAnchor(anchor);
        this.lutModulePicker.setAnchor(anchor);
        this.jrePathEditor.setAnchor(anchor);
    }
}
