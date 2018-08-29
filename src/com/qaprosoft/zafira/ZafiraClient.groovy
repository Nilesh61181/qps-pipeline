package com.qaprosoft.zafira

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import com.qaprosoft.jenkins.pipeline.Configurator

class ZafiraClient {

	private String serviceURL
	private String token
	private def context
	private boolean isAvailable

	public ZafiraClient(context, String url, Boolean developMode) {
		this.context = context
		this.serviceURL = url
		context.println("zafiraUrl: ${serviceURL}")
		
		if (developMode) {
			isAvailable = false
		} else {
  			//TODO: execute ping call to zafira "/api/status"
            isAvailable = true
		}

	}
	
	public boolean isAvailable() {
		return this.isAvailable
	}

	public String getZafiraAuthToken(String accessToken) {
		if (!isAvailable) {
			return ""
		}
		context.println("accessToken: ${accessToken}")
		def response = context.httpRequest contentType: 'APPLICATION_JSON', \
			httpMode: 'POST', \
			requestBody: "{\"refreshToken\": \"${accessToken}\"}", \
			url: this.serviceURL + "/api/auth/refresh"

		// reread new accessToken and auth type
		def properties = (Map) new JsonSlurper().parseText(response.getContent())

		//new accessToken in response is authToken
		def authToken = properties.get("accessToken")
		def type = properties.get("type")

		this.token = "${type} ${authToken}"
		//context.println("${this.token}")
		return this.token
	}

	public void queueZafiraTestRun(String uuid) {
		if (!isAvailable) {
			return
		}

        def response = context.httpRequest customHeaders: [[name: 'Authorization', \
            value: "${token}"]], \
	        contentType: 'APPLICATION_JSON', \
	        httpMode: 'POST', \
	        requestBody: "{\"jobName\": \"${Configurator.get(Configurator.Parameter.JOB_BASE_NAME)}\", \
                       \"buildNumber\": \"${Configurator.get(Configurator.Parameter.BUILD_NUMBER)}\", \
                       \"branch\": \"${Configurator.get("branch")}\", \
                       \"env\": \"${Configurator.get("env")}\", \"ciRunId\": \"${uuid}\", \
                       \"ciParentUrl\": \"${Configurator.get("ci_parent_url")}\", \
                       \"ciParentBuild\": \"${Configurator.get("ci_parent_build")}\"}", \
            url: this.serviceURL + "/api/tests/runs/queue"
			
        String formattedJSON = JsonOutput.prettyPrint(response.content)
        context.println("Queued TestRun: ${formattedJSON}")
    }

	public void smartRerun() {
		if (!isAvailable) {
			return
		}

		def response = context.httpRequest customHeaders: [[name: 'Authorization',   \
            value: "${token}"]],   \
	        contentType: 'APPLICATION_JSON',   \
	        httpMode: 'POST',   \
	        requestBody: "{\"owner\": \"${Configurator.get("ci_user_id")}\", \
                           \"upstreamJobId\": \"${Configurator.get("ci_job_id")}\", \
                           \"upstreamJobBuildNumber\": \"${Configurator.get("ci_parent_build")}\", \
                           \"scmUrl\": \"${Configurator.get("scm_url")}\", \
                           \"hashcode\": \"${Configurator.get("hashcode")}\"}",   \
                  url: this.serviceURL + "/api/tests/runs/rerun/jobs?doRebuild=${Configurator.get("doRebuild")}&rerunFailures=${Configurator.get("rerunFailures")}",   \
                  timeout: 300000

		def responseJson = new JsonSlurper().parseText(response.content)

		context.println("Results : ${responseJson.size()}")
		context.println("Tests for rerun : ${responseJson}")
	}

	public void abortZafiraTestRun(String uuid, String comment) {
		if (!isAvailable) {
			return 
		}

		context.httpRequest customHeaders: [[name: 'Authorization', \
            value: "${token}"]], \
	    contentType: 'APPLICATION_JSON', \
	    httpMode: 'POST', \
	    requestBody: "{\"comment\": \"${comment}\"}", \
            url: this.serviceURL + "/api/tests/runs/abort?ciRunId=${uuid}"

	}

    void sendTestRunResultsEmail(String uuid, String emailList, String filter) {
        if (!isAvailable) {
            return
        }

        context.httpRequest customHeaders: [[name: 'Authorization',  \
             value: "${token}"]],  \
	     contentType: 'APPLICATION_JSON',  \
	     httpMode: 'POST',  \
	     requestBody: "{\"recipients\": \"${emailList}\"}",  \
             url: this.serviceURL + "/api/tests/runs/${uuid}/email?filter=${filter}"
    }

	public String exportZafiraReport(String uuid) {
		if (!isAvailable) {
			return ""
		}

		def response = context.httpRequest customHeaders: [[name: 'Authorization', \
			value: "${token}"]], \
		contentType: 'APPLICATION_JSON', \
		httpMode: 'GET', \
			url: this.serviceURL + "/api/tests/runs/${uuid}/export"
			
		//context.println("exportZafiraReport response: ${response.content}")
		return response.content
	}
}