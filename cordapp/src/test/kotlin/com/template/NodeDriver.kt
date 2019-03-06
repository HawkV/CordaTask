package com.template

import com.template.web.SparkMark
import com.template.web.SparkSend
import com.template.web.SparkUpload
import com.template.web.SparkViewResults
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.getOrThrow
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.driver
import net.corda.testing.node.User

/**
 * Allows you to run your nodes through an IDE (as opposed to using deployNodes). Do not use in a production
 * environment.
 */
fun main(args: Array<String>) {
    val rpcUsers = listOf(User("user1", "test", permissions = setOf("ALL")))

    driver(DriverParameters(startNodesInProcess = true, waitForAllNodesToFinish = true)) {
        startNode(providedName = CordaX500Name("PartyA", "London", "GB"), rpcUsers = rpcUsers).getOrThrow()
        SparkUpload.main(arrayOf("1234", "localhost:10005"))
        startNode(providedName = CordaX500Name("PartyB", "New York", "US"), rpcUsers = rpcUsers).getOrThrow()
        SparkSend.main(arrayOf("2345", "localhost:10009"))
        startNode(providedName = CordaX500Name("PartyC", "New York", "US"), rpcUsers = rpcUsers).getOrThrow()
        startNode(providedName = CordaX500Name("PartyD", "New York", "US"), rpcUsers = rpcUsers).getOrThrow()
        startNode(providedName = CordaX500Name("PartyF", "New York", "US"), rpcUsers = rpcUsers).getOrThrow()
        SparkMark.main(arrayOf("4567", "localhost:10013", "5678", "localhost:10017", "6789", "localhost:10021"))
        startNode(providedName = CordaX500Name("PartyE", "New York", "US"), rpcUsers = rpcUsers).getOrThrow()
        SparkViewResults.main(arrayOf("7890", "localhost:10025"))
    }
}
