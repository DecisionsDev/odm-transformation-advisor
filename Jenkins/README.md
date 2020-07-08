# Jenkins integration 

## Prerequisites
- `JDK`, `Ant` and `ODM 8.10.x` are installed in the Jenkins node.

## Jenkins set-up
- Add `ANT_HOME` in Jenkins Node environment variable.
- Install the `Generic Webhook Trigger Plugin` Jenkins plugin.
- Disable the *Content Security Policy* to view the HTML report properly.
To do so, run the following script in the Jenkins Script Console:

```
System.setProperty("hudson.model.DirectoryBrowserSupport.CSP", "")
```

For more details, please refer to the [Configuring Content Security Policy](https://wiki.jenkins.io/display/JENKINS/Configuring+Content+Security+Policy) page of the Jenkins wiki.

- Add Github credential
- Import Jenkins job

```
java -jar jenkins-cli.jar -s http://<JENKINS_HOST>:8080/ -auth user:password create-job OTA < jenkins/OTA.xml
```

- Update the `ssh-key`, `ota.url`, `odm.dir`, `ota.username` and `ota.password` variables in the Jenkins job pipeline script.

## Register Jenkins Webhook URL into Decision Center.

Open the Decision Center Swagger UI: `http://<DC_HOST>:9090/decisioncenter-api/swagger-ui.html#!/Manage/registerWebhookUsingPUT`
Two parameters are required to register a webhook:
- The webhook url: `http://<JENKINS_HOST>:8080/generic-webhook-trigger/invoke`
- The token which is defined in the Jenkins job.

