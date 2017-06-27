Changelog
===

### 1.1

Release date: _June 27, 2017_

* [JENKINS-45056](https://issues.jenkins-ci.org/browse/JENKINS-45056) - 
Fix issue with the incorrect plugin packaging in 1.0 due to the release flow issue.
  * Now it is a plugin available via update center
* [JENKINS-45055](https://issues.jenkins-ci.org/browse/JENKINS-45055) - 
Update EnvInject Lib from 1.25 to 1.26 in order to pick FindBugs fixes ([changelog](https://github.com/jenkinsci/envinject-lib/blob/master/CHANGELOG.md#126)).

### 1.0

Release date: _May 05, 2017_

* [JENKINS-43845](https://issues.jenkins-ci.org/browse/JENKINS-43845) -
Create EnvInject API Plugin, which provides a shared [Jenkins EnvInject Library](https://github.com/jenkinsci/envinject-lib).
* [JENKINS-43535](https://issues.jenkins-ci.org/browse/JENKINS-43535) - 
Provide new version of _EnvInject Library_ utility classes, which are compatible with non-`AbstractProject` job types.
* Extend the migrated utility classes by methods migrated from [EnvInject Plugin](https://github.com/jenkinsci/envinject-plugin/).

#### Developer notes (1.0)

* `EnvInjectAction`, `EnvInjectException`, `EnvInjectLogger` classes stay in the original library due to the compatibility reasons.
* Once a plugin adds dependency on this library, it is recommended to...
  * Remove explicit dependency on EnvInject Library
  * Replace all usages of the 
`org.jenkinsci.lib.envinject.service` package by the new methods offered by the plugin. 

### Changes before 1.0

See [EnvInject Library changelog](https://github.com/jenkinsci/envinject-lib/blob/master/CHANGELOG.md).
