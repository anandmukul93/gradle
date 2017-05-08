/*
 * Copyright 2016 the original author or authors.
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

package org.gradle.composite.internal;

import org.gradle.StartParameter;
import org.gradle.api.internal.artifacts.ImmutableModuleIdentifierFactory;
import org.gradle.api.internal.artifacts.ivyservice.projectmodule.ProjectArtifactBuilder;
import org.gradle.api.internal.composite.CompositeBuildContext;
import org.gradle.api.internal.tasks.TaskReferenceResolver;
import org.gradle.initialization.BuildIdentity;
import org.gradle.initialization.IncludedBuildExecuter;
import org.gradle.initialization.IncludedBuildFactory;
import org.gradle.initialization.IncludedBuildTaskGraph;
import org.gradle.initialization.IncludedBuilds;
import org.gradle.initialization.NestedBuildFactory;
import org.gradle.internal.composite.CompositeContextBuilder;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.internal.service.ServiceRegistration;
import org.gradle.internal.service.scopes.PluginServiceRegistry;

public class CompositeBuildServices implements PluginServiceRegistry {
    public void registerGlobalServices(ServiceRegistration registration) {
    }

    public void registerBuildSessionServices(ServiceRegistration registration) {
        registration.addProvider(new CompositeBuildSessionScopeServices());
    }

    public void registerBuildServices(ServiceRegistration registration) {
        registration.addProvider(new CompositeBuildBuildScopeServices());
    }

    public void registerGradleServices(ServiceRegistration registration) {
    }

    public void registerProjectServices(ServiceRegistration registration) {
    }

    private static class CompositeBuildGlobalScopeServices {
    }

    private static class CompositeBuildSessionScopeServices {
        public DefaultIncludedBuilds createIncludedBuilds() {
            return new DefaultIncludedBuilds();
        }

        public CompositeBuildContext createCompositeBuildContext(IncludedBuilds includedBuilds, ImmutableModuleIdentifierFactory moduleIdentifierFactory) {
            return new DefaultBuildableCompositeBuildContext(includedBuilds, moduleIdentifierFactory);
        }

        public DefaultProjectPathRegistry createProjectPathRegistry() {
            return new DefaultProjectPathRegistry();
        }

        public CompositeContextBuilder createCompositeContextBuilder(DefaultIncludedBuilds includedBuilds, DefaultProjectPathRegistry projectRegistry, CompositeBuildContext context) {
            return new DefaultCompositeContextBuilder(includedBuilds, projectRegistry, context);
        }

        public IncludedBuildExecuter createIncludedBuildExecuter(IncludedBuilds includedBuilds) {
            IncludedBuildExecuter includedBuildExecuter = new DefaultIncludedBuildExecuter(includedBuilds);
            return new ErrorHandlingIncludedBuildExecuter(includedBuildExecuter);
        }

        public IncludedBuildTaskGraph createIncludedBuildTaskGraph(IncludedBuildExecuter includedBuildExecuter) {
            return new DefaultIncludedBuildTaskGraph(includedBuildExecuter);
        }

        public IncludedBuildArtifactBuilder createIncludedBuildArtifactBuilder(IncludedBuildTaskGraph includedBuildTaskGraph) {
            return new IncludedBuildArtifactBuilder(includedBuildTaskGraph);
        }
    }

    private static class CompositeBuildBuildScopeServices {
        public IncludedBuildFactory createIncludedBuildFactory(Instantiator instantiator, StartParameter startParameter, NestedBuildFactory nestedBuildFactory, ImmutableModuleIdentifierFactory moduleIdentifierFactory) {
            return new DefaultIncludedBuildFactory(instantiator, startParameter, nestedBuildFactory, moduleIdentifierFactory);
        }

        public TaskReferenceResolver createResolver(IncludedBuildTaskGraph includedBuilds, BuildIdentity buildIdentity) {
            return new IncludedBuildTaskReferenceResolver(includedBuilds, buildIdentity);
        }

        public ProjectArtifactBuilder createProjectArtifactBuilder(IncludedBuildArtifactBuilder builder, BuildIdentity buildIdentity) {
            return new CompositeProjectArtifactBuilder(builder, buildIdentity);
        }
    }

}
