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

If you don't want to build the archive yourself then just check out the [releases page](https://github.com/zeroturnaround/jenkins-reporter/releases).

This will generate a report and if run on a desktop will open it in your browser.

### Usage vol 2

Create a Jenkins job that runs this command for you and archive the results. You will always have up to date results for your most important views.

### Screenshot

![Screenshot](https://raw.github.com/zeroturnaround/jenkins-reporter/master/etc/screenshot-001.png)
