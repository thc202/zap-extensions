/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2021 The ZAP Development Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.zaproxy.addon.reports.automation;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.control.Control;
import org.parosproxy.paros.core.scanner.Alert;
import org.zaproxy.addon.automation.AutomationData;
import org.zaproxy.addon.automation.AutomationEnvironment;
import org.zaproxy.addon.automation.AutomationJob;
import org.zaproxy.addon.automation.AutomationProgress;
import org.zaproxy.addon.automation.jobs.JobData;
import org.zaproxy.addon.automation.jobs.JobUtils;
import org.zaproxy.addon.reports.ExtensionReports;
import org.zaproxy.addon.reports.ReportData;
import org.zaproxy.addon.reports.ReportParam;
import org.zaproxy.addon.reports.Template;

public class ReportJob extends AutomationJob {

    private static final Logger LOGGER = LogManager.getLogger(ReportJob.class);

    private static final String JOB_NAME = "report";

    private static final String PARAM_TEMPLATE = "template";
    private static final String PARAM_THEME = "theme";
    private static final String PARAM_REPORT_DIR = "reportDir";
    private static final String PARAM_REPORT_FILE = "reportFile";
    private static final String PARAM_REPORT_TITLE = "reportTitle";
    private static final String PARAM_REPORT_DESC = "reportDescription";
    private static final String PARAM_DISPLAY_REPORT = "displayReport";

    private ExtensionReports extReport;

    private Parameters parameters = new Parameters();
    private Data data;

    public ReportJob() {
        data = new Data(this, this.parameters);
        this.getParameters().setTemplate(ReportParam.DEFAULT_TEMPLATE);
        this.getParameters().setReportTitle(this.getExtReport().getReportParam().getTitle());
        this.getParameters()
                .setReportDescription(this.getExtReport().getReportParam().getDescription());
    }

    @Override
    public void verifyParameters(AutomationProgress progress) {
        Map<?, ?> jobData = this.getJobData();
        if (jobData == null) {
            return;
        }
        JobUtils.applyParamsToObject(
                (LinkedHashMap<?, ?>) jobData.get("parameters"),
                this.parameters,
                this.getName(),
                null,
                progress);

        List<String> list = getJobDataList(jobData, "risks", progress);
        if (!list.isEmpty()) {
            // Validate
            list.stream().map(risk -> riskStringToInt(risk, progress));
            this.getData().setRisks(list);
        }

        list = getJobDataList(jobData, "confidences", progress);
        if (!list.isEmpty()) {
            // Validate
            list.stream().map(conf -> confidenceStringToInt(conf, progress));
            this.getData().setConfidences(list);
        }

        String templateName = this.getParameters().getTemplate();
        if (StringUtils.isEmpty(templateName)) {
            templateName = ReportParam.DEFAULT_TEMPLATE;
            this.getParameters().setTemplate(templateName);
        }
        Template template = getExtReport().getTemplateByConfigName(templateName);
        if (template == null) {
            errorUnknownTemplate(progress);
        }

        list = getJobDataList(jobData, "sections", progress);
        if (!list.isEmpty()) {
            if (template != null) {
                // Validate
                List<String> validSections = template.getSections();
                for (String section : list) {
                    if (!validSections.contains(section)) {
                        progress.warn(
                                Constant.messages.getString(
                                        "reports.automation.error.badsection",
                                        this.getName(),
                                        section,
                                        template.getConfigName(),
                                        validSections));
                    }
                }
            }
            this.getData().setSections(list);
        }

        list = getJobDataList(jobData, "sites", progress);
        if (!list.isEmpty()) {
            this.getData().setSites(list);
        }
    }

    private void errorUnknownTemplate(AutomationProgress progress) {
        progress.error(
                Constant.messages.getString(
                        "reports.automation.error.badtemplate",
                        getName(),
                        getParameters().getTemplate(),
                        getExtReport().getTemplateConfigNames()));
    }

    @Override
    public void applyParameters(AutomationProgress progress) {
        // Nothing to do
    }

    @Override
    public void runJob(AutomationEnvironment env, AutomationProgress progress) {

        String templateName = this.getParameters().getTemplate();
        if (StringUtils.isEmpty(templateName)) {
            templateName = ReportParam.DEFAULT_TEMPLATE;
        }
        Template template = getExtReport().getTemplateByConfigName(templateName);
        ReportData reportData = new ReportData(templateName);

        if (template == null) {
            errorUnknownTemplate(progress);
            return;
        }
        String theme = this.getParameters().getTheme();
        if (StringUtils.isEmpty(theme) && template.getThemes().size() > 0) {
            theme = template.getThemes().get(0);
        }
        reportData.setTheme(theme);

        // Work out the file name based on the pattern
        String filePattern = env.replaceVars(getParameters().getReportFile());
        if (StringUtils.isEmpty(filePattern)) {
            filePattern = ReportParam.DEFAULT_NAME_PATTERN;
        }
        // Replace envs in the URL first so that the site sanitization is used, then replace in the
        // full pattern
        String fileName =
                env.replaceVars(
                        ExtensionReports.getNameFromPattern(
                                filePattern,
                                env.replaceVars(env.getDefaultContextWrapper().getUrls().get(0))));

        if (!fileName.endsWith("." + template.getExtension())) {
            fileName += "." + template.getExtension();
        }

        File file;
        String reportDir = getParameters().getReportDir();
        if (reportDir != null && reportDir.length() > 0) {
            File dir = JobUtils.getFile(reportDir, getPlan());
            file = new File(dir, fileName);
        } else {
            file = JobUtils.getFile(fileName, getPlan());
        }
        reportData.setTitle(this.getParameters().getReportTitle());
        reportData.setDescription(this.getParameters().getReportDescription());
        reportData.setContexts(env.getContexts());
        reportData.addReportObjects("automation.progress", progress);

        if (this.getData().getRisks() == null) {
            reportData.setIncludeAllRisks(true);
        } else {
            for (String risk : this.getData().getRisks()) {
                reportData.setIncludeRisk(riskStringToInt(risk, progress), true);
            }
        }

        if (this.getData().getConfidences() == null) {
            reportData.setIncludeAllConfidences(true);
        } else {
            for (String confidence : this.getData().getConfidences()) {
                reportData.setIncludeConfidence(confidenceStringToInt(confidence, progress), true);
            }
        }

        if (this.getData().getSections() == null) {
            reportData.setSections(template.getSections());
        } else {
            List<String> validSections = template.getSections();
            for (String section : this.getData().getSections()) {
                if (validSections.contains(section)) {
                    reportData.addSection(section);
                } else {
                    progress.warn(
                            Constant.messages.getString(
                                    "reports.automation.error.badsection",
                                    this.getName(),
                                    section,
                                    template.getConfigName(),
                                    validSections));
                }
            }
        }

        if (this.getData().getSites() == null) {
            reportData.setSites(ExtensionReports.getSites());
        } else {
            List<String> validSites = ExtensionReports.getSites();

            for (String str : this.getData().getSites()) {
                boolean siteAdded = false;
                String siteSubstr = env.replaceVars(str);
                for (String site : validSites) {
                    if (site.contains(siteSubstr)) {
                        reportData.addSite(site);
                        siteAdded = true;
                        break;
                    }
                }
                if (!siteAdded) {
                    progress.warn(
                            Constant.messages.getString(
                                    "reports.automation.error.badsite",
                                    this.getName(),
                                    siteSubstr,
                                    validSites));
                }
            }
        }

        reportData.setAlertTreeRootNode(getExtReport().getFilteredAlertTree(reportData));

        try {
            file =
                    getExtReport()
                            .generateReport(
                                    reportData,
                                    template,
                                    file.getAbsolutePath(),
                                    JobUtils.unBox(this.getParameters().getDisplayReport()));
            progress.info(
                    Constant.messages.getString(
                            "reports.automation.info.reportgen",
                            this.getName(),
                            file.getAbsolutePath()));
        } catch (Exception e) {
            LOGGER.warn("Failed to generate the report:", e);
            progress.error(
                    Constant.messages.getString(
                            "reports.automation.error.generate",
                            this.getName(),
                            e.getClass().getSimpleName(),
                            e.getMessage()));
        }
    }

    private int riskStringToInt(String str, AutomationProgress progress) {
        switch (str.toLowerCase()) {
            case "high":
                return Alert.RISK_HIGH;
            case "medium":
                return Alert.RISK_MEDIUM;
            case "low":
                return Alert.RISK_LOW;
            case "info":
                return Alert.RISK_INFO;
            case "information":
                return Alert.RISK_INFO;
        }
        progress.warn(
                Constant.messages.getString(
                        "reports.automation.error.badrisk", this.getName(), str));
        return -2;
    }

    public static String riskIntToString(int i) {
        switch (i) {
            case 0:
                return "info";
            case 1:
                return "low";
            case 2:
                return "medium";
            case 3:
                return "high";
            default:
                return "";
        }
    }

    private int confidenceStringToInt(String str, AutomationProgress progress) {
        switch (str.toLowerCase()) {
            case "high":
                return Alert.CONFIDENCE_HIGH;
            case "medium":
                return Alert.CONFIDENCE_MEDIUM;
            case "low":
                return Alert.CONFIDENCE_LOW;
            case "falsepositive":
                return Alert.CONFIDENCE_FALSE_POSITIVE;
            case "confirmed":
                return Alert.CONFIDENCE_USER_CONFIRMED;
        }
        progress.warn(
                Constant.messages.getString(
                        "reports.automation.error.badconf", this.getName(), str));
        return -2;
    }

    public static String confidenceIntToString(int i) {
        switch (i) {
            case 0:
                return "falsepositive";
            case 1:
                return "low";
            case 2:
                return "medium";
            case 3:
                return "high";
            case 4:
                return "confirmed";
            default:
                return "";
        }
    }

    private List<String> getJobDataList(
            Map<?, ?> jobData, String key, AutomationProgress progress) {
        List<String> list = new ArrayList<>();
        Object o = jobData.get(key);
        if (o == null) {
            // Do nothing
        } else if (o instanceof List) {
            for (Object item : (List<?>) o) {
                list.add(item.toString());
            }
        } else {
            progress.warn(
                    Constant.messages.getString(
                            "reports.automation.error.badlist",
                            this.getName(),
                            key,
                            o.getClass().getCanonicalName()));
        }

        return list;
    }

    private ExtensionReports getExtReport() {
        if (extReport == null) {
            extReport =
                    Control.getSingleton()
                            .getExtensionLoader()
                            .getExtension(ExtensionReports.class);
        }
        return extReport;
    }

    @Override
    public Map<String, String> getCustomConfigParameters() {
        Map<String, String> map = super.getCustomConfigParameters();
        map.put(PARAM_TEMPLATE, this.getParameters().getTemplate());
        map.put(PARAM_THEME, this.getParameters().getTheme());
        map.put(PARAM_REPORT_DIR, this.getParameters().getReportDir());
        map.put(PARAM_REPORT_FILE, this.getParameters().getReportFile());
        map.put(PARAM_REPORT_TITLE, this.getParameters().getReportTitle());
        map.put(PARAM_REPORT_DESC, this.getParameters().getReportDescription());
        map.put(
                PARAM_DISPLAY_REPORT,
                Boolean.toString(JobUtils.unBox(this.getParameters().getDisplayReport())));
        return map;
    }

    @Override
    public String getTemplateDataMin() {
        return ExtensionReportAutomation.getResourceAsString(this.getType() + "-min.yaml");
    }

    @Override
    public String getTemplateDataMax() {
        return ExtensionReportAutomation.getResourceAsString(this.getType() + "-max.yaml");
    }

    @Override
    public String getType() {
        return JOB_NAME;
    }

    @Override
    public Order getOrder() {
        return Order.REPORT;
    }

    @Override
    public Object getParamMethodObject() {
        return null;
    }

    @Override
    public String getParamMethodName() {
        return null;
    }

    @Override
    public Parameters getParameters() {
        return parameters;
    }

    @Override
    public void showDialog() {
        new ReportJobDialog(this).setVisible(true);
    }

    @Override
    public String getSummary() {
        return Constant.messages.getString(
                "reports.automation.dialog.summary", this.getParameters().getTemplate());
    }

    @Override
    public Data getData() {
        return data;
    }

    @Getter
    @Setter
    public static class Data extends JobData {
        private Parameters parameters;
        private List<String> risks;
        private List<String> confidences;
        private List<String> sections;
        private List<String> sites;

        public Data(AutomationJob job, Parameters parameters) {
            super(job);
            this.parameters = parameters;
        }
    }

    @Getter
    @Setter
    public static class Parameters extends AutomationData {
        private String template = ReportParam.DEFAULT_TEMPLATE;
        private String theme = "";
        private String reportDir = "";
        private String reportFile = ReportParam.DEFAULT_NAME_PATTERN;
        private String reportTitle = "";
        private String reportDescription = "";
        private Boolean displayReport = false;
    }
}
