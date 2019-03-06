package com.template.states

import net.corda.core.contracts.ContractState
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party

// *********
// * State *
// *********
object StateContract
{
    class UploadState(val hash: SecureHash,
                      val lender: Party,
                      val borrower: Party) : ContractState {
        override val participants get() = listOf(lender, borrower)
    }

    class SendState(val hash: SecureHash,
                    val name: String,
                    val description: String,
                    val lender: Party,
                    val borrower: Party) : ContractState {
        override val participants get() = listOf(lender, borrower)
    }

    class MarkState(val hash: SecureHash,
                    val name: String,
                    val mark: Int,
                    val lender: Party,
                    val borrower: Party) : ContractState {
        override val participants get() = listOf(lender, borrower)
    }
}