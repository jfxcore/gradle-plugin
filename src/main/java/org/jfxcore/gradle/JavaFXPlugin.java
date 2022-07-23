/*
 * Copyright (c) 2018, Gluon
 * Copyright (c) 2022, JFXcore
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.jfxcore.gradle;

import com.google.gradle.osdetector.OsDetectorPlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskCollection;
import org.gradle.api.tasks.compile.GroovyCompile;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.scala.ScalaCompile;
import org.javamodularity.moduleplugin.ModuleSystemPlugin;
import org.jfxcore.gradle.compiler.CompilerService;
import org.jfxcore.gradle.tasks.CompileMarkupTask;
import org.jfxcore.gradle.tasks.ExecTask;
import org.jfxcore.gradle.tasks.ProcessMarkupTask;
import org.jfxcore.gradle.util.PathHelper;
import java.util.Map;

public class JavaFXPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getPlugins().apply(OsDetectorPlugin.class);
        project.getPlugins().apply(ModuleSystemPlugin.class);

        project.getExtensions().create("javafx", JavaFXOptions.class, project);

        project.getTasks().create("configJavafxRun", ExecTask.class, project);

        // Exclude OpenJFX module dependencies
        for (var configuration : project.getConfigurations()) {
            for (var module : JavaFXModule.values()) {
                configuration.exclude(Map.of("group", "org.openjfx", "module", module.getModuleName()));
            }
        }

        // For each source set, add the corresponding generated sources directory, so it can be
        // picked up by the Java compiler.
        var pathHelper = new PathHelper(project);
        for (SourceSet sourceSet : pathHelper.getSourceSets()) {
            sourceSet.getJava().srcDir(pathHelper.getGeneratedSourcesDir(sourceSet));
        }

        // Configure parseMarkup to run before, and compileMarkup to run after the source code is compiled.
        project.afterEvaluate(p -> {
            var provider = createProvider(project);
            Task processMarkup = project.getTasks().create("processMarkup",
                ProcessMarkupTask.class, task -> task.getCompilerService().set(provider));
            Task compileMarkup = project.getTasks().create("compileMarkup",
                CompileMarkupTask.class, task -> task.getCompilerService().set(provider));

            TaskCollection<JavaCompile> javaCompileTasks = project.getTasks().withType(JavaCompile.class);
            TaskCollection<GroovyCompile> groovyCompileTasks = project.getTasks().withType(GroovyCompile.class);
            TaskCollection<ScalaCompile> scalaCompileTasks = project.getTasks().withType(ScalaCompile.class);
            TaskCollection<?> kotlinCompileTasks = null;

            try {
                var kotlinCompileClass = Class.forName("org.jetbrains.kotlin.gradle.tasks.KotlinCompile");
                kotlinCompileTasks = project.getTasks().matching(kotlinCompileClass::isInstance);
            } catch (ClassNotFoundException ignored) {
            }

            for (TaskCollection<?> collection : new TaskCollection[] {
                    javaCompileTasks, groovyCompileTasks, scalaCompileTasks, kotlinCompileTasks}) {
                if (collection != null) {
                    for (Task task : collection) {
                        task.dependsOn(processMarkup);
                        task.finalizedBy(compileMarkup);
                        compileMarkup.dependsOn(task);
                    }
                }
            }
        });
    }

    @SuppressWarnings("UnstableApiUsage")
    private Provider<CompilerService> createProvider(Project project) {
        var options = (JavaFXOptions)project.getExtensions().findByName("javafx");

        return project.getGradle().getSharedServices()
            .registerIfAbsent("compilerService:" + project.getName(), CompilerService.class, spec -> {
                spec.getParameters().getCompileClasspath().set(new PathHelper(project).getCompileClasspath());

                if (options != null && options.getCompiler() != null && !options.getCompiler().isEmpty()) {
                    spec.getParameters().getCompilerJar().set(options.getCompiler());
                }
            });
    }
}
