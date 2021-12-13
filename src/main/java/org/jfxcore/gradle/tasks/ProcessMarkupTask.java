/*
 * Copyright (c) 2021, JFXcore
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
package org.jfxcore.gradle.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.jfxcore.gradle.compiler.CompilerService;
import org.jfxcore.gradle.util.PathHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.Predicate;

public abstract class ProcessMarkupTask extends DefaultTask {

    @Internal
    public abstract Property<CompilerService> getCompilerService();

    @TaskAction
    public void process() {
        Project project = getProject();
        PathHelper pathHelper = new PathHelper(project);
        CompilerService compilerService = getCompilerService().get();
        Set<File> compileClasspath = pathHelper.getCompileClasspath();

        try {
            // Invoke the FXML parse and source generation stages for every source set.
            // This will generate .java source files that are placed in the generated sources directory.
            for (SourceSet sourceSet : pathHelper.getSourceSets()) {
                var compiler = compilerService.newCompiler(sourceSet, compileClasspath, getLogger());

                for (File sourceDir : sourceSet.getAllSource().getSrcDirs()) {
                    compiler.parseFiles(sourceDir);
                }

                Path genSrcDir = pathHelper.getGeneratedSourcesDir(sourceSet).toPath();
                compiler.generateSources(genSrcDir.toFile());

                // Delete all .class files that may have been created by a previous compiler run.
                // This is necessary because the FXML compiler needs a 'clean slate' to work with.
                Predicate<Path> fileFilter = path -> path.toString().toLowerCase().endsWith(".java");
                for (Path file : PathHelper.enumerateFiles(genSrcDir, fileFilter)) {
                    String fileName = PathHelper.getFileNameWithoutExtension(file.toFile()) + ".class";
                    Path relFile = genSrcDir.relativize(file).getParent().resolve(fileName);
                    Path classesDir = sourceSet.getJava().getClassesDirectory().get().getAsFile().toPath();
                    Path classFile = classesDir.resolve(relFile);

                    if (Files.exists(classFile)){
                        try {
                            Files.delete(classFile);
                        } catch (IOException ex) {
                            throw new GradleException("Cannot delete " + classFile, ex);
                        }
                    }
                }
            }
        } catch (RuntimeException ex) {
            if (compilerService.getExceptionHelper().isMarkupException(ex)) {
                project.getLogger().error(compilerService.getExceptionHelper().format(ex));
            } else {
                String message = ex.getMessage();
                throw new GradleException(
                    message == null || message.isEmpty() ? "Internal compiler error" : message, ex);
            }

            throw new GradleException("Compilation failed; see the compiler error output for details.");
        } catch (Throwable ex) {
            String message = ex.getMessage();
            throw new GradleException(
                message == null || message.isEmpty() ? "Internal compiler error" : message, ex);
        }
    }

}
