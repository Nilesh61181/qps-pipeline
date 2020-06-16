package com.qaprosoft.jenkins.pipeline.runner.maven

public class QARunner extends TestNG {

    public QARunner(context) {
        super(context)
        setBuildName("BUILD_NUMBER|suite|branch|env|browser|browserVersion|locale|language")
    }

    public QARunner(context, jobType) {
        super(context, jobType)
        setBuildName("BUILD_NUMBER|suite|branch|env|browser|browserVersion|locale|language")
    }
}
