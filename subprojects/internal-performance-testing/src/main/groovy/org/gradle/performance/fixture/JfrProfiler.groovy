/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.performance.fixture

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import org.gradle.performance.util.JCmd

/**
 * Profiles performance test scenarios using the Java Flight Recorder.
 *
 * TODO support pause/resume so we can exclude clean tasks from measurement
 * TODO move flamegraph generation to buildSrc and offer it as a task so it can be used when people send us .jfr files
 */
@CompileStatic
@PackageScope
class JfrProfiler extends Profiler {

    private final File logDirectory
    private final JCmd jCmd
    private final PidInstrumentation pid
    private final JfrFlameGraphGenerator flameGraphGenerator

    JfrProfiler(File targetDir) {
        logDirectory = targetDir
        jCmd = new JCmd()
        flameGraphGenerator = new JfrFlameGraphGenerator()
        pid = new PidInstrumentation()
    }

    @Override
    List<String> getAdditionalJvmOpts(BuildExperimentSpec spec) {
        String flightRecordOptions = "stackdepth=1024"
        def jfrFile = getJfrFile(spec)
        jfrFile.parentFile.mkdirs()
        if (!useDaemon(spec)) {
            flightRecordOptions += ",defaultrecording=true,dumponexit=true,dumponexitpath=${jfrFile},settings=profile"
        }
        ["-XX:+UnlockCommercialFeatures", "-XX:+FlightRecorder", "-XX:FlightRecorderOptions=$flightRecordOptions", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"] as List<String>
    }

    @Override
    List<String> getAdditionalGradleArgs(BuildExperimentSpec spec) {
        pid.gradleArgs
    }

    private File getJfrFile(BuildExperimentSpec spec) {
        def fileSafeName = spec.displayName.replaceAll('[^a-zA-Z0-9.-]', '-').replaceAll('-+', '-')
        def baseDir = new File(logDirectory, fileSafeName)
        new File(baseDir, "profile.jfr")
    }

    void start(BuildExperimentSpec spec) {
        if (useDaemon(spec)) {
            jCmd.execute(pid.pid, "JFR.start", "name=profile", "settings=profile")
        }
    }

    void stop(BuildExperimentSpec spec) {
        def jfrFile = getJfrFile(spec)
        if (useDaemon(spec)) {
            jCmd.execute(pid.pid, "JFR.stop", "name=profile", "filename=${jfrFile}")
        }
        flameGraphGenerator.generateGraphs(jfrFile)
    }

    private boolean useDaemon(BuildExperimentSpec spec) {
        if (spec instanceof GradleBuildExperimentSpec) {
            (spec.invocation as GradleInvocationSpec).useDaemon
        } else {
            false
        }
    }
}
