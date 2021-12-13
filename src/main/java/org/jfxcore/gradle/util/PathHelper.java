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
package org.jfxcore.gradle.util;

import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.jfxcore.gradle.JavaFXOptions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PathHelper {

    private final Project project;

    public PathHelper(Project project) {
        this.project = project;
    }

    public File getGeneratedSourcesDir(SourceSet sourceSet) {
        return project.getBuildDir().toPath()
            .resolve("generated/sources/fxml/java")
            .resolve(sourceSet.getName()).toFile();
    }

    public Set<SourceSet> getSourceSets() {
        return project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets();
    }

    public Set<File> getCompileClasspath() {
        return project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().stream()
            .flatMap(sourceSet -> sourceSet.getCompileClasspath().getFiles().stream())
            .collect(Collectors.toSet());
    }

    public File getRuntimeDependencyJar(String groupId, String name) {
        JavaFXOptions options = project.getExtensions().getByType(JavaFXOptions.class);
        var configuration = project.getConfigurations().findByName("runtimeClasspath");
        if (configuration != null) {
            var files = configuration.files(dep -> groupId.equals(dep.getGroup()) && name.equals(dep.getName()));
            if (!files.isEmpty()) {
                return files.stream().iterator().next();
            }
        }

        return null;
    }

    public static Iterable<Path> enumerateFiles(Path basePath, Predicate<Path> filter) throws IOException {
        Iterator<Path> it;
        if (Files.isDirectory(basePath)) {
            try (Stream<Path> stream = Files.walk(basePath)) {
                it = stream.filter(p -> Files.isRegularFile(p) && filter.test(p)).collect(Collectors.toList()).iterator();
            }

            return () -> it;
        }

        return Collections::emptyIterator;
    }

    public static String getFileNameWithoutExtension(File file) {
        String name = file.getName();
        int lastIdx = name.lastIndexOf('.');
        return name.substring(0, lastIdx < 0 ? name.length() : lastIdx);
    }

}
