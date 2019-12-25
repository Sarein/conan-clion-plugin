package conan.commands;

import com.intellij.openapi.project.Project;

import conan.profiles.CMakeProfile;
import conan.profiles.ConanProfile;

public class BuildConfigure extends ConanCommandBase {

    public BuildConfigure(Project project, CMakeProfile cMakeProfile) {
        super(project, "build", project.getBasePath(), "-bf=" + cMakeProfile.getTargetDir(), "--configure");
    }
}
