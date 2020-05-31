/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package com.github.vgramer.opaplugin.lang

import com.github.vgramer.opaplugin.OpaTestBase
import com.github.vgramer.opaplugin.OpaTestCase
import com.github.vgramer.opaplugin.lang.parser.RegoParserDefinition
import com.intellij.testFramework.ParsingTestCase


abstract class RegoParsingTestCaseBase() : ParsingTestCase(
    "com/github/vgramer/opaplugin/lang/parser/fixtures",
    "rego",
    true,
    RegoParserDefinition()
), OpaTestCase {

    override fun getTestDataPath(): String = "src/test/resources"

    override fun getTestName(lowercaseFirstLetter: Boolean): String {
        val camelCase = super.getTestName(lowercaseFirstLetter)
        return OpaTestBase.camelOrWordsToSnake(camelCase)
    }

    /**
     * Test that parsing does not return error
     *
     * This test is very basic bacic, it's does not check the generated psi match the expected one.
     * At this time the grammar / psi is not finish; this test ensure no regressions are introduce when modifying grammar
     */
    fun doTestNoError() {
        super.doTest(false, false)
        ensureNoErrorElements()

    }
}