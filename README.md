This script creates HTML report with the list of failed tests for given view in Jenkins.

Build the project and run executable JAR from "target" folder, like

java -jar target/jenkins-reporter-0.0.1-SNAPSHOT-standalone.jar "LR 2.7" "LR 2.7 Staging"

The app will generate two HTML files and will open them in your browser.

NB! It only shows failed jobs in the reports, which are not in running state.  

Jenkins Reporter
=========================

This project started from the need to get an overview of failing tests in Jenkins. Once you have
enough configurations and jobs it is kind of PITA to see a good overview of everything. Of course
you could go into the red jobs and see for yourself but the overall overview will be still lacking.

### Building the Project
```bash
git clone https://github.com/zeroturnaround/jenkins-reporter.git
cd jenkins-reporter

mvn install
```

### Usage
```bash
java -Dreporter.jenkins.url=http://jenkins/ -jar target/jenkins-reporter-standalone.jar Jenkins-View-Name
```

This will generate a report and if run on a desktop will open it in your browser.

### Usage vol 2

Create a Jenkins job that runs this command for you and archive the results. You will always have up to date results for your most important views.

### Screenshot



