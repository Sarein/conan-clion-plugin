package conan.actions;

import com.google.common.collect.Lists;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import conan.commands.BuildConfigure;
import conan.commands.Install;
import conan.commands.task.AsyncConanTask;
import conan.persistency.settings.ConanProjectSettings;
import conan.profiles.CMakeProfile;
import conan.profiles.ConanProfile;
import conan.ui.ConanToolWindow;
import conan.ui.profileMatching.ProfileMatcher;

import static conan.utils.Utils.log;

/**
 * Created by Yahav Itzhak on Feb 2018.
 */
class ActionUtils {

    /**
     * Run conan install for the selected Conan profile.
     *
     * @param project   Intellij project.
     * @param component the {@link ProfileMatcher} dialog will be shown in this component position.
     * @param update    true if it's update and install action.
     */

    private static final Logger logger = Logger.getInstance(AsyncConanTask.class);

    static void runInstall(Project project, Component component, boolean update) {
        if (!ConanToolWindow.isConanInstalled(project)){
            return;
        }
        FileDocumentManager.getInstance().saveAllDocuments();
        ConanToolWindow conanToolWindow = ServiceManager.getService(project, ConanToolWindow.class);
        ConanProfile conanProfile = new ConanProfile(conanToolWindow.getSelectedTab());
        ConanProjectSettings conanProjectSettings = ConanProjectSettings.getInstance(project);
        List<CMakeProfile> cMakeProfiles = getMatchedCMakeProfiles(conanProjectSettings, conanProfile);
        if (StringUtils.isBlank(conanProfile.getName())) {
            matchProfilesAndInstall(project, component, update);
            return;
        }
        cMakeProfiles.forEach(cMakeProfile ->{
            String cleanedDirectory = cMakeProfile.getTargetDir();
            try {
                FileUtils.cleanDirectory(new File(cleanedDirectory));
                log(logger, "Clean directory: " + cleanedDirectory, "", NotificationType.INFORMATION);
            } catch (IOException e) {
                log(logger, "Can`t clean directory: " + cleanedDirectory, "", NotificationType.ERROR);
                e.printStackTrace();
            }
            new Install(project, cMakeProfile, conanProfile, update).run_async(conanProfile, null, null);
        });
    }



    static void runBuildConfigure(Project project, Component component) {
        if (!ConanToolWindow.isConanInstalled(project)){
            return;
        }

        FileDocumentManager.getInstance().saveAllDocuments();

        ConanToolWindow conanToolWindow = ServiceManager.getService(project, ConanToolWindow.class);
        ConanProfile conanProfile = new ConanProfile(conanToolWindow.getSelectedTab());
        ConanProjectSettings conanProjectSettings = ConanProjectSettings.getInstance(project);
        List<CMakeProfile> cMakeProfiles = getMatchedCMakeProfiles(conanProjectSettings, conanProfile);
        if (StringUtils.isBlank(conanProfile.getName())) {
            matchProfilesAndInstall(project, component, false);
        }

        cMakeProfiles.forEach(cMakeProfile -> new BuildConfigure(project, cMakeProfile).run_async(conanProfile, null, null));
    }

    /**
     * In case the user clicks on install and no Conan-CMake matching exist, we open the matching dialog for him.
     *
     * @param project   Intellij project.
     * @param component the {@link ProfileMatcher} dialog will be shown in this component position.
     * @param update    true if it's update and install action.
     */
    private static void matchProfilesAndInstall(Project project, Component component, boolean update) {
        if (!ConanToolWindow.isConanInstalled(project)){
            return;
        }
        ProfileMatcher.showDialog(project, component);
        ConanProjectSettings conanProjectSettings = ConanProjectSettings.getInstance(project);
        Map<CMakeProfile, ConanProfile> profileMapping = conanProjectSettings.getProfileMapping();
        profileMapping.forEach((cMakeProfile, conanProfile) -> {
            if (StringUtils.isNotBlank(conanProfile.getName())) {
                new Install(project, cMakeProfile, conanProfile, update).run_async(conanProfile, null, null);
            }
        });
    }

    /**
     * Get the matched CMake profiles for the selected Conan profile.
     *
     * @param conanProjectSettings the Conan project settings.
     * @param conanProfile         the conan profile to match.
     * @return list of matched CMake profiles for the selected Conan profile.
     */
    private static java.util.List<CMakeProfile> getMatchedCMakeProfiles(ConanProjectSettings conanProjectSettings, ConanProfile conanProfile) {
        List<CMakeProfile> cmakeProfiles = Lists.newArrayList();
        Map<CMakeProfile, ConanProfile> profileMatching = conanProjectSettings.getProfileMapping();
        for (Map.Entry<CMakeProfile, ConanProfile> matching : profileMatching.entrySet()) {
            if (matching.getValue().equals(conanProfile)) {
                cmakeProfiles.add(matching.getKey());
            }
        }
        return cmakeProfiles;
    }


}
