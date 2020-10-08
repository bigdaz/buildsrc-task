import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
public class IdlToJava extends DefaultTask {
    @InputFiles
    //@PathSensitive(PathSensitivity.RELATIVE)
    FileCollection idlFiles

    @Classpath
    FileCollection jacorbClasspath

    // We currently only consider the include _paths_ for cache key.
    // This means that a change to a file within an include directory won't invalidate the cache.
    // We should really be inspecting the contents of these dirs and invalidating cache if anything changes
    // but this could be quite expensive, depending on how many files are in each
    @Input
    List<String> includes

    @OutputDirectory
    File outputDirectory

    IdlToJava() {
        description = 'Create .java files from selected IDL files'
    }

    @TaskAction
    void processIdl() {
        // Create batches to send to the JacORB compiler
        int count = 0
        FileCollection batch = project.files()
        idlFiles.each { File idlFile->
            count++
            batch += project.files(idlFile)
            if (count == 7) {
                compileBatch(batch)
                count = 0
                batch = project.files()
            }
        }
        if (count > 0) {
            compileBatch(batch)
        }
    }
    private void compileBatch(def idlFiles) {
        // DUMMY IMPLEMENTATION
        project.mkdir(outputDirectory)
        project.copy {
            from idlFiles
            into outputDirectory
        }
        println idlFiles.collect {it.path}
        return

        // REAL IMPLEMENTATION
        // Compile IDL to Java using JacORB compiler
        project.javaexec {
            main = 'org.jacorb.idl.parser'
            classpath = jacorbClasspath
            args '-DNBU_USES_JACORB_FOR_COMPILATION'
            includes.each { include ->
                args "-I${include}"
            }
            args '-d',"${outputDirectory}"
            args '-sloppy_forward'
            idlFiles.each {
                File idlFile ->
                    args "${idlFile}"
            }
        }
    }
}
