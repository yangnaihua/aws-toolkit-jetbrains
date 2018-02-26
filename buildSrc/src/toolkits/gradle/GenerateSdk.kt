// Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package toolkits.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import software.amazon.awssdk.codegen.C2jModels
import software.amazon.awssdk.codegen.CodeGenerator
import software.amazon.awssdk.codegen.model.config.BasicCodeGenConfig
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig
import software.amazon.awssdk.codegen.model.service.ServiceModel
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils
import java.io.File

open class GenerateSdk : DefaultTask() {
    @InputDirectory
    lateinit var c2jFolder: File

    @OutputDirectory
    lateinit var outputDir: File

    @TaskAction
    fun generate() {
        LOG.info("Generating SDK from $c2jFolder")
        val models = C2jModels.builder()
            .serviceModel(loadServiceModel())
            .customizationConfig(loadCustomizationConfig())
            .applyMutation { builder -> loadCodeGenConfig()?.let(builder::codeGenConfig) }
            .build()

        CodeGenerator.builder()
            .models(models)
            .sourcesDirectory(outputDir.absolutePath)
            .fileNamePrefix(models.serviceModel().metadata.serviceId)
            .build()
            .execute()
    }

    private fun loadServiceModel(): ServiceModel? =
        ModelLoaderUtils.loadModel(ServiceModel::class.java, File(c2jFolder, "service-2.json"))

    private fun loadCustomizationConfig(): CustomizationConfig = ModelLoaderUtils.loadOptionalModel(
        CustomizationConfig::class.java,
        File(c2jFolder, "customization.config")
    ).orElse(CustomizationConfig.create())

    private fun loadCodeGenConfig(): BasicCodeGenConfig? =
        ModelLoaderUtils.loadOptionalModel(BasicCodeGenConfig::class.java, File(c2jFolder, "codegen.config"))
            .orElse(null)?.also {
                LOG.info("WTF: ${it.interfaceName}")
            }

    private companion object {
        private val LOG = Logging.getLogger(GenerateSdk::class.java)
    }
}