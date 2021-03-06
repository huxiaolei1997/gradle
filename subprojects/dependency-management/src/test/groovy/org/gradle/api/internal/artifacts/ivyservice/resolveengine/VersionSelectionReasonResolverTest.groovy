/*
 * Copyright 2013 the original author or authors.
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

package org.gradle.api.internal.artifacts.ivyservice.resolveengine

import org.gradle.api.internal.artifacts.ivyservice.resolveengine.result.VersionSelectionReasons
import spock.lang.Specification

class VersionSelectionReasonResolverTest extends Specification {

    def "configures selection reason"() {
        def delegate = Mock(ModuleConflictResolver)
        VersionSelectionReasonResolver resolver = new VersionSelectionReasonResolver(delegate)

        def candidate1 = Mock(ComponentResolutionState)
        def candidate2 = Mock(ComponentResolutionState)
        def out

        when:
        def details = Mock(ConflictResolverDetails) {
            getCandidates() >> [candidate1, candidate2]
            select(_) >> { args ->
                out = args[0]
            }
            hasFailure() >> false
            getSelected() >> { out }
        }
        resolver.select(details)


        then:
        out == candidate2

        and:
        1 * delegate.select(_) >> { args ->
            args[0].select(candidate2)
        }
        1 * candidate2.addCause(VersionSelectionReasons.CONFLICT_RESOLUTION)
        0 * _._
    }
}
