/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui;


import static com.microsoft.azuretools.telemetry.TelemetryConstants.PLUGIN_INSTALL;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.PLUGIN_UPGRADE;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.SYSTEM;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.TELEMETRY_ALLOW;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.TELEMETRY_DENY;
import static com.microsoft.intellij.ui.messages.AzureBundle.message;

import java.io.File;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextPane;

import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.azure.toolkit.lib.common.utils.InstallationIdUtils;
import com.microsoft.azuretools.adauth.StringUtils;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.azurecommons.util.ParserXMLUtility;
import com.microsoft.azuretools.azurecommons.xmlhandling.DataOperations;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.utils.TelemetryUtils;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.CommonConst;
import com.microsoft.intellij.util.PluginHelper;
import com.microsoft.intellij.util.PluginUtil;

import org.w3c.dom.Document;


public class AzurePanel implements AzureAbstractConfigurablePanel {
    private static final String DISPLAY_NAME = "Azure";

    private JCheckBox checkBox1;
    private JTextPane textPane1;
    private JPanel contentPane;
    String dataFile = PluginHelper.getTemplateFile(message("dataFileName"));

    public AzurePanel() {
    }

    public void init() {
        if (!AzurePlugin.IS_ANDROID_STUDIO) {
            Messages.configureMessagePaneUi(textPane1, message("preferenceLinkMsg"));
            if (new File(dataFile).exists()) {
                String prefValue = DataOperations.getProperty(dataFile, message("prefVal"));
                if (prefValue != null && !prefValue.isEmpty()) {
                    if (prefValue.equals("true")) {
                        checkBox1.setSelected(true);
                    }
                }
            }
        }
    }

    public JComponent getPanel() {
        return contentPane;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public boolean doOKAction() {
        try {
            if (new File(dataFile).exists()) {
                Document doc = ParserXMLUtility.parseXMLFile(dataFile);
                String oldPrefVal = DataOperations.getProperty(dataFile, message("prefVal"));
                DataOperations.updatePropertyValue(doc, message("prefVal"), String.valueOf(checkBox1.isSelected()));
                String version = DataOperations.getProperty(dataFile, message("pluginVersion"));
                if (version == null || version.isEmpty()) {
                    DataOperations.updatePropertyValue(doc, message("pluginVersion"), AzurePlugin.PLUGIN_VERSION);
                } else if (!AzurePlugin.PLUGIN_VERSION.equalsIgnoreCase(version)) {
                    DataOperations.updatePropertyValue(doc, message("pluginVersion"), AzurePlugin.PLUGIN_VERSION);
                    AppInsightsClient.createByType(AppInsightsClient.EventType.Plugin, "", AppInsightsConstants.Upgrade, null, true);
                    EventUtil.logEvent(EventType.info, SYSTEM, PLUGIN_UPGRADE, null, null);
                }
                String instID = DataOperations.getProperty(dataFile, message("instID"));
                if (instID == null || instID.isEmpty() || !InstallationIdUtils.isValidHashMac(instID)) {
                    DataOperations.updatePropertyValue(doc, message("instID"), InstallationIdUtils.getHashMac());
                    AppInsightsClient.createByType(AppInsightsClient.EventType.Plugin, "", AppInsightsConstants.Install, null, true);
                    EventUtil.logEvent(EventType.info, SYSTEM, PLUGIN_INSTALL, null, null);
                }
                ParserXMLUtility.saveXMLFile(dataFile, doc);
                // Its necessary to call application insights custom create event after saving data.xml
                final boolean acceptTelemetry = checkBox1.isSelected();
                if (StringUtils.isNullOrEmpty(oldPrefVal) || Boolean.valueOf(oldPrefVal) != acceptTelemetry) {
                    // Boolean.valueOf(oldPrefVal) != acceptTelemetry means user changes his mind.
                    // Either from Agree to Deny, or from Deny to Agree.
                    final String action = acceptTelemetry ? AppInsightsConstants.Allow : AppInsightsConstants.Deny;
                    AppInsightsClient.createByType(AppInsightsClient.EventType.Telemetry, "", action, null, true);
                    EventUtil.logEvent(EventType.info, SYSTEM, acceptTelemetry ? TELEMETRY_ALLOW : TELEMETRY_DENY, null,
                        null);
                }
            } else {
                AzurePlugin.copyResourceFile(message("dataFileName"), dataFile);
                setValues(dataFile);
            }
            String userAgent = String.format(AzurePlugin.USER_AGENT, CommonConst.PLUGIN_VERISON,
                    TelemetryUtils.getMachieId(dataFile, message("prefVal"), message("instID")));
            CommonSettings.setUserAgent(userAgent);
        } catch (Exception ex) {
            AzurePlugin.log(ex.getMessage(), ex);
            PluginUtil.displayErrorDialog(message("errTtl"), message("updateErrMsg"));
            return false;
        }
        return true;
    }

    private void setValues(String dataFile) throws Exception {
        Document doc = ParserXMLUtility.parseXMLFile(dataFile);
        DataOperations.updatePropertyValue(doc, message("pluginVersion"), AzurePlugin.PLUGIN_VERSION);
        DataOperations.updatePropertyValue(doc, message("instID"), InstallationIdUtils.getHashMac());
        DataOperations.updatePropertyValue(doc, message("prefVal"), String.valueOf(checkBox1.isSelected()));
        ParserXMLUtility.saveXMLFile(dataFile, doc);
    }

    @Override
    public String getSelectedValue() {
        return null;
    }

    public ValidationInfo doValidate() {
        return null;
    }

    @Override
    public String getHelpTopic() {
        return null;
    }

    @Override
    public boolean isModified() {
        return true;
    }

    @Override
    public void reset() {
    }
}
