package recipes

import Recipes
import RecipesVscRoot
import enableMatrixBuild
import enableVcsTrigger
import input
import jetbrains.buildServer.configs.kotlin.BuildType

object ImportJUnitReport : BuildType({
    name = "ImportJUnitReport"

    vcs { root(RecipesVscRoot) }

    enableMatrixBuild()

    steps {
        step {
            id = "CreateFile"
            type = Recipes.CreateFile
            input("path", "./reports/report.xml")
            input(
                "content", """
                <?xml version="1.0" encoding="UTF-8"?>
                <testsuites time="15.682687">
                    <testsuite name="Tests.Registration" time="6.605871">
                        <testcase name="testCase1" classname="Tests.Registration" time="2.113871" />
                        <testcase name="testCase2" classname="Tests.Registration" time="1.051" />
                        <testcase name="testCase3" classname="Tests.Registration" time="3.441" />
                    </testsuite>
                    <testsuite name="Tests.Authentication" time="9.076816">
                        <testsuite name="Tests.Authentication.Login" time="4.356">
                            <testcase name="testCase4" classname="Tests.Authentication.Login" time="2.244" />
                            <testcase name="testCase5" classname="Tests.Authentication.Login" time="0.781" />
                            <testcase name="testCase6" classname="Tests.Authentication.Login" time="1.331" />
                        </testsuite>
                        <testcase name="testCase7" classname="Tests.Authentication" time="2.508" />
                        <testcase name="testCase8" classname="Tests.Authentication" time="1.230816" />
                    </testsuite>
                </testsuites>
            """.trimIndent()
            )
        }
        step {
            id = "ImportJUnitReport"
            type = Recipes.ImportXmlReport
            input("report_type", "junit")
            input("import_path", "./reports/report.xml")
        }
    }

    enableVcsTrigger()
})