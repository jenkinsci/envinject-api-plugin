Changelog
===

### Newer versions

See [GitHub releases](https://github.com/jenkinsci/envinject-api-plugin/releases)

### 1.5

Release date: _Dec 28, 2017_

* Update EnvInject Lib to 1.29 ([changelog](https://github.com/jenkinsci/envinject-lib/blob/master/CHANGELOG.md#129))
* [PR #4](https://github.com/jenkinsci/envinject-api-plugin/pull/4) - 
Remove hidden string concatenation in `EnvInjectVarsIO#toTxt()` to reduce memory consumption.

### 1.4

Release date: _Oct 24, 2017_

* Update EnvInject Lib from 1.27 to 1.28 in order to pick fixes ([changelog](https://github.com/jenkinsci/envinject-lib/blob/master/CHANGELOG.md#128)).
* [JENKINS-47574](https://issues.jenkins-ci.org/browse/JENKINS-47574) -
EnvInject Lib 1.28: Prevent `NullPointerException` in `EnvInjectAction#getEnvironment()` when it's called in XML deserialization without build reference.
* [JENKINS-47167](https://issues.jenkins-ci.org/browse/JENKINS-47167) - 
EnvInject Lib 1.28: Prevent `NullPointerException` when serializing `EnvInjectAction` without build reference to the disk.

### 1.3

Release date: _Oct 07, 2017_

* Update to EnvInject Lib 1.27
([full changelog](https://github.com/jenkinsci/envinject-lib/blob/master/CHANGELOG.md#127)).
* [JENKINS-46479](https://issues.jenkins-ci.org/browse/JENKINS-46479) -
EnvInject Lib 1.27: Prevent environment variables from being deleted on existing builds.

### 1.2

Release date: _June 27, 2017_

* [JENKINS-45056](https://issues.jenkins-ci.org/browse/JENKINS-45056) - 
Fix issue with the incorrect plugin packaging in 1.0 due to the release flow issue.
  * Now it is a plugin available via update center

### 1.1

Release date: _June 27, 2017_

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
