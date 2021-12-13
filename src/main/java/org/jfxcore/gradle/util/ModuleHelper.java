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

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class ModuleHelper {

    private final Set<String> kotlinModuleNames = new HashSet<>();
    private final Set<File> kotlinJarPaths = new HashSet<>();

    public ModuleHelper(Project project) {
        try {
            var kotlinCompileClass = Class.forName("org.jetbrains.kotlin.gradle.tasks.KotlinCompile");

            if (project.getTasks().matching(kotlinCompileClass::isInstance).size() > 0) {
                var pathHelper = new PathHelper(project);

                var stdlib = pathHelper.getRuntimeDependencyJar("org.jetbrains.kotlin", "kotlin-stdlib");
                if (stdlib != null) {
                    kotlinModuleNames.add("kotlin.stdlib");
                    kotlinJarPaths.add(stdlib);
                }

                var reflect = pathHelper.getRuntimeDependencyJar("org.jetbrains.kotlin", "kotlin-reflect");
                if (reflect != null) {
                    kotlinModuleNames.add("kotlin.reflect");
                    kotlinJarPaths.add(reflect);
                }
            }
        } catch (ClassNotFoundException ignored) {
        }
    }

    public Set<String> getKotlinModuleNames() {
        return kotlinModuleNames;
    }

    public Set<File> getKotlinJarPaths() {
        return kotlinJarPaths;
    }

}
