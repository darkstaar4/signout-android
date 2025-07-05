/**
 * Precompiled [io.element.android-root.gradle.kts][Io_element_android_root_gradle] script plugin.
 *
 * @see Io_element_android_root_gradle
 */
public
class Io_element_androidRootPlugin : org.gradle.api.Plugin<org.gradle.api.Project> {
    override fun apply(target: org.gradle.api.Project) {
        try {
            Class
                .forName("Io_element_android_root_gradle")
                .getDeclaredConstructor(org.gradle.api.Project::class.java, org.gradle.api.Project::class.java)
                .newInstance(target, target)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
    }
}
