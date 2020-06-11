package com.qaprosoft.jenkins.pipeline.integration.reporting

import com.qaprosoft.jenkins.pipeline.integration.HttpClient
import groovy.json.JsonBuilder
import static com.qaprosoft.jenkins.Utils.*
import com.qaprosoft.jenkins.pipeline.Configuration

/*
 * Prerequisites: valid REPORTING_SERVICE_URL and REPORTING_ACCESS_TOKEN already defined in Configuration
 */

class ReportingClient extends HttpClient {

    private String serviceURL
    private String refreshToken
    private String authToken
    private long tokenExpTime

    public ReportingClient(context) {
        super(context)
        this.serviceURL = Configuration.get(Configuration.Parameter.REPORTING_SERVICE_URL)
        this.refreshToken = Configuration.get(Configuration.Parameter.REPORTING_ACCESS_TOKEN)
    }

    public def queueReportingTestRun(uuid) {
        if (!isReportingConnected()) {
            return
        }
        JsonBuilder jsonBuilder = new JsonBuilder()
        jsonBuilder jobUrl: replaceTrailingSlash(Configuration.get(Configuration.Parameter.JOB_URL)),
                buildNumber: Configuration.get(Configuration.Parameter.BUILD_NUMBER),
                branch: Configuration.get("branch"),
                env: Configuration.get("env"),
                ciRunId: uuid,
                ciParentUrl: replaceTrailingSlash(Configuration.get("ci_parent_url")),
                ciParentBuild: Configuration.get("ci_parent_build"),
                project: Configuration.get("reporting_project")

        logger.info("REQUEST: " + jsonBuilder.toPrettyString())
        String requestBody = jsonBuilder.toString()
        jsonBuilder = null

        def parameters = [customHeaders: [[name: 'Authorization', value: "${authToken}"]],
                          contentType: 'APPLICATION_JSON',
                          httpMode: 'POST',
                          requestBody: requestBody,
                          validResponseCodes: "200:401",
                          url: this.serviceURL + "/api/tests/runs/queue"]
        return sendRequestFormatted(parameters)
    }

    public def smartRerun() {
        if (!isReportingConnected()) {
            return
        }
        JsonBuilder jsonBuilder = new JsonBuilder()
        jsonBuilder owner: Configuration.get("owner"),
                cause: Configuration.get("cause"),
                upstreamJobId: Configuration.get("upstreamJobId"),
                upstreamJobBuildNumber: Configuration.get("upstreamJobBuildNumber"),
                scmURL: Configuration.get("scmURL"),
                hashcode: Configuration.get("hashcode")

        logger.info("REQUEST: " + jsonBuilder.toPrettyString())
        String requestBody = jsonBuilder.toString()
        jsonBuilder = null

        def parameters = [customHeaders: [[name: 'Authorization', value: "${authToken}"]],
                          contentType: 'APPLICATION_JSON',
                          httpMode: 'POST',
                          requestBody: requestBody,
                          validResponseCodes: "200:401",
                          url: this.serviceURL + "/api/tests/runs/rerun/jobs?doRebuild=${Configuration.get("doRebuild")}&rerunFailures=${Configuration.get("rerunFailures")}",
                          timeout: 300000]
        return sendRequestFormatted(parameters)
    }

    public def abortTestRun(uuid, failureReason) {
        if (!isReportingConnected()) {
            return
        }
        JsonBuilder jsonBuilder = new JsonBuilder()
        jsonBuilder comment: failureReason

        logger.debug("REQUEST: " + jsonBuilder.toPrettyString())
        String requestBody = jsonBuilder.toString()
        jsonBuilder = null

        def parameters = [customHeaders: [[name: 'Authorization', value: "${authToken}"]],
                          contentType: 'APPLICATION_JSON',
                          httpMode: 'POST',
                          requestBody: requestBody,
                          validResponseCodes: "200:500",
                          url: this.serviceURL + "/api/tests/runs/abort?ciRunId=${uuid}"]
        return sendRequestFormatted(parameters)
    }

    public def sendEmail(uuid, emailList, filter) {
        if (!isReportingConnected()) {
            return
        }
        JsonBuilder jsonBuilder = new JsonBuilder()
        jsonBuilder recipients: emailList

        logger.debug("REQUEST: " + jsonBuilder.toPrettyString())
        String requestBody = jsonBuilder.toString()
        jsonBuilder = null

        def parameters = [customHeaders: [[name: 'Authorization', value: "${authToken}"]],
                          contentType: 'APPLICATION_JSON',
                          httpMode: 'POST',
                          requestBody: requestBody,
                          validResponseCodes: "200:401",
                          url: this.serviceURL + "/api/tests/runs/${uuid}/email?filter=${filter}"]
        return sendRequest(parameters)
    }

    public def sendSlackNotification(uuid, channels) {
        if (!isReportingConnected()) {
            return
        }
        def parameters = [customHeaders: [[name: 'Authorization', value: "${authToken}"]],
                          contentType: 'APPLICATION_JSON',
                          httpMode: 'GET',
                          validResponseCodes: "200",
                          url: this.serviceURL + "/api/slack/testrun/${uuid}/finish?channels=${channels}"]
        return sendRequest(parameters)
    }

    public def exportTagData(uuid, tagName) {
        if (!isReportingConnected()) {
            return
        }
        def parameters = [customHeaders: [[name: 'Authorization', value: "${authToken}"]],
                          contentType: 'APPLICATION_JSON',
                          httpMode: 'GET',
                          validResponseCodes: "200",
                          url: this.serviceURL + "/api/tags/${uuid}/integration?integrationTag=${tagName}"]
        return sendRequestFormatted(parameters)
    }

    public def sendFailureEmail(uuid, emailList, suiteOwner, suiteRunner) {
        if (!isReportingConnected()) {
            return
        }
        JsonBuilder jsonBuilder = new JsonBuilder()
        jsonBuilder recipients: emailList

        logger.debug("REQUEST: " + jsonBuilder.toPrettyString())

        String requestBody = jsonBuilder.toString()
        jsonBuilder = null
        def parameters = [customHeaders: [[name: 'Authorization', value: "${authToken}"]],
                          contentType: 'APPLICATION_JSON',
                          httpMode: 'POST',
                          requestBody: requestBody,
                          validResponseCodes: "200:401",
                          url: this.serviceURL + "/api/tests/runs/${uuid}/emailFailure?suiteOwner=${suiteOwner}&suiteRunner=${suiteRunner}"]
        return sendRequest(parameters)
    }

    public def exportReportingReport(uuid) {
        if (!isReportingConnected()) {
            return
        }
        def parameters = [customHeaders: [[name: 'Authorization', value: "${authToken}"]],
                          contentType: 'APPLICATION_JSON',
                          httpMode: 'GET',
                          validResponseCodes: "200:500",
                          url: this.serviceURL + "/api/tests/runs/${uuid}/export"]

        return sendRequest(parameters)
    }

    public def getTestRunByCiRunId(uuid) {
        if (!isReportingConnected()) {
            return
        }
        def parameters = [customHeaders: [[name: 'Authorization', value: "${authToken}"]],
                          contentType: 'APPLICATION_JSON',
                          httpMode: 'GET',
                          validResponseCodes: "200:404",
                          url: this.serviceURL + "/api/tests/runs?ciRunId=${uuid}"]

        return sendRequestFormatted(parameters)
    }


    public def createLaunchers(jenkinsJobsScanResult) {
        if (!isReportingConnected()) {
            return
        }

        JsonBuilder jsonBuilder = new JsonBuilder()
        jsonBuilder jenkinsJobsScanResult

        logger.info("REQUEST: " + jsonBuilder.toPrettyString())
        String requestBody = jsonBuilder.toString()
        jsonBuilder = null

        def parameters = [customHeaders: [[name: 'Authorization', value: "${authToken}"]],
                          contentType: 'APPLICATION_JSON',
                          httpMode: 'POST',
                          requestBody: requestBody,
                          validResponseCodes: "200",
                          url: this.serviceURL + "/api/launchers/create"]
        return sendRequestFormatted(parameters)
    }

    public def createJob(jobUrl) {
        if (!isReportingConnected()) {
            return
        }
        JsonBuilder jsonBuilder = new JsonBuilder()
        jsonBuilder jobUrlValue: jobUrl

        logger.debug("REQUEST: " + jsonBuilder.toPrettyString())
        String requestBody = jsonBuilder.toString()
        jsonBuilder = null

        def parameters = [customHeaders: [[name: 'Authorization', value: "${authToken}"]],
                          contentType: 'APPLICATION_JSON',
                          httpMode: 'POST',
                          requestBody: requestBody,
                          validResponseCodes: "200:401",
                          url: this.serviceURL + "/api/jobs/url"]
        return sendRequestFormatted(parameters)
    }

    protected boolean isTokenExpired() {
        return authToken == null || System.currentTimeMillis() > tokenExpTime
    }

    /** Verify if ReportingConnected and refresh authToken if needed. Return false if connection can't be established or disabled **/
    protected boolean isReportingConnected() {
		if (!isTokenExpired()) {
			return true
		}
		
		if (isParamEmpty(this.refreshToken) || isParamEmpty(this.serviceURL) || Configuration.mustOverride.equals(this.serviceURL)) {
			return false
		}

		
        logger.debug("refreshToken: " + refreshToken)
        JsonBuilder jsonBuilder = new JsonBuilder()
        jsonBuilder refreshToken: this.refreshToken

        String requestBody = jsonBuilder.toString()
        jsonBuilder = null

        def parameters = [contentType: 'APPLICATION_JSON',
                          httpMode: 'POST',
                          validResponseCodes: "200:404",
                          requestBody: requestBody,
                          url: this.serviceURL + "/api/auth/refresh"]
        logger.debug("parameters: " + parameters)
        Map properties = (Map)sendRequestFormatted(parameters)
        logger.debug("properties: " + properties)
        if (isParamEmpty(properties)) {
            // #669: no sense to start tests if reporting is configured and not available!
            logger.info("properties: " + properties)
            throw new RuntimeException("Unable to get auth token, check Reporting integration!")
        }
        this.authToken = properties.type + " " + properties.accessToken
        logger.debug("authToken: " + authToken)
        this.tokenExpTime = System.currentTimeMillis() + 470 * 60 * 1000 //8 hours - interval '10 minutes'
        return true
    }

}