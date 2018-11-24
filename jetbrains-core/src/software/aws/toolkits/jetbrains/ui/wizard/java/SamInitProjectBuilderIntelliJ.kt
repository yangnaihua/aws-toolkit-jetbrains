// Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.ui.wizard.java

import com.intellij.ide.projectWizard.ProjectTemplateList
import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.Disposable
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ui.configuration.ModulesProvider
import icons.AwsIcons
import software.amazon.awssdk.services.lambda.model.Runtime
import software.aws.toolkits.jetbrains.services.lambda.RuntimeGroup
import software.aws.toolkits.jetbrains.services.lambda.execution.sam.SamCommon
import software.aws.toolkits.jetbrains.services.lambda.runtimeGroup
import software.aws.toolkits.jetbrains.ui.wizard.SAM_TEMPLATES
import software.aws.toolkits.jetbrains.ui.wizard.SamModuleType
import software.aws.toolkits.jetbrains.ui.wizard.SamProjectTemplateWrapper
import software.aws.toolkits.resources.message

class SamInitModuleBuilder : ModuleBuilder() {
    var runtime: Runtime? = null
    lateinit var runtimeSelectionPanel: SamInitRuntimeSelectionPanel
    lateinit var template: SamProjectTemplateWrapper

    /*  Trick IDEA to give us a custom first screen without using the WizardDelegate trick
        described in AndroidModuleBuilder
        https://github.com/JetBrains/android/blob/master/android/src/com/android/tools/idea/npw/ideahost/AndroidModuleBuilder.java
    */
    override fun getModuleType() = SamModuleType.instance

    // we want to use our own custom template selection step
    override fun isTemplateBased() = false

    fun getSdkType() = runtime?.runtimeGroup?.getIdeSdkType()

    override fun setupRootModel(rootModel: ModifiableRootModel) {
        // smart-cast fail workaround due to mutability of `runtime`
        val selectedRuntime = runtime ?: throw RuntimeException(message("sam.init.null_runtime"))
        val moduleType = selectedRuntime.runtimeGroup?.getModuleType() ?: ModuleType.EMPTY

        if (myJdk != null) {
            rootModel.sdk = myJdk
        } else {
            rootModel.inheritSdk()
        }
        rootModel.module.rootManager.modifiableModel.inheritSdk()
        rootModel.module.setModuleType(moduleType.id)
        val project = rootModel.project

        template.samProjectTemplate.build(selectedRuntime, project.baseDir)
        rootModel.addContentEntry(project.baseDir)

        SamCommon.excludeSamDirectory(rootModel.project.baseDir, rootModel)

        if (selectedRuntime.runtimeGroup == RuntimeGroup.PYTHON) {
            SamCommon.setSourceRoots(rootModel.project.baseDir, rootModel.project, rootModel)
        }
        // don't commit because it will be done for us
    }

    override fun getPresentableName() = SamModuleType.ID

    override fun getDescription() = SamModuleType.DESCRIPTION

    override fun getNodeIcon() = AwsIcons.Resources.SERVERLESS_APP

    override fun getCustomOptionsStep(context: WizardContext?, parentDisposable: Disposable?): ModuleWizardStep? {
        runtimeSelectionPanel = SamInitRuntimeSelectionPanel(this, context)
        return runtimeSelectionPanel
    }

    override fun createWizardSteps(wizardContext: WizardContext, modulesProvider: ModulesProvider) =
            arrayOf(SamInitTemplateSelectionStep(this, wizardContext))
}

class SamInitTemplateSelectionStep(
    val builder: SamInitModuleBuilder,
    val context: WizardContext
) : ModuleWizardStep() {
    val templateSelectionPanel = ProjectTemplateList()

    init {
        templateSelectionPanel.setTemplates(SAM_TEMPLATES.map { it.getModuleBuilderProjectTemplate(builder) }, true)
    }

    override fun updateDataModel() {
        context.projectBuilder = builder
        builder.template = templateSelectionPanel.selectedTemplate as SamProjectTemplateWrapper
    }

    override fun getComponent() = templateSelectionPanel
}