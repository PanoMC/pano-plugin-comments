package com.panomc.plugins.comments

import com.panomc.platform.api.PanoPlugin
import com.panomc.platform.api.config.PluginConfigManager
import com.panomc.platform.setup.SetupManager
import com.panomc.plugins.comments.config.CommentsConfig
import com.panomc.plugins.license.PluginLicenseClient

class CommentsPlugin : PanoPlugin() {
    private val pluginDatabaseManager by lazy {
        applicationContext.getBean(com.panomc.platform.api.PluginDatabaseManager::class.java)
    }

    private val setupManager by lazy {
        applicationContext.getBean(SetupManager::class.java)
    }

    /**
     * Premium plugin: enforce a panomc.com license at startup. No-op for free builds.
     */
    private val licenseClient by lazy { PluginLicenseClient(this) }

    private var isInitialized = false

    override suspend fun onStart() {
        // Verify license BEFORE doing any startup work. If invalid, throws and the
        // host marks this plugin failed and records the failure for the panel UI.
        // Pano core itself keeps running so the operator can resolve the issue.
        licenseClient.requireValidLicense()

        logger.info("Starting...")

        startPlugin()
    }

    override suspend fun verifyLicense() {
        licenseClient.requireValidLicense()
    }

    internal suspend fun startPlugin() {
        if (isInitialized) return
        isInitialized = true

        if (!setupManager.isSetupDone()) {
            logger.info("Setup is not finished, waiting for setup completion...")
            return
        }

        val configManager = PluginConfigManager(this, CommentsConfig::class.java)
        pluginBeanContext.beanFactory.registerSingleton(PluginConfigManager::class.java.name, configManager)

        pluginDatabaseManager.initialize(this)

        logger.info("Started!")
    }

    override suspend fun onEnable() {
        logger.info("Enabled!")
    }

    override suspend fun onDisable() {
        isInitialized = false
    }

    override suspend fun onUninstall() {
        logger.info("Uninstalling...")
        pluginDatabaseManager.uninstall(this)
    }
}

