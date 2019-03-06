package com.template.web

import com.template.states.StateContract
import com.template.web.SparkUpload.setConnection
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.core.messaging.CordaRPCOps
import freemarker.cache.ClassTemplateLoader
import freemarker.template.Configuration
import spark.ModelAndView
import spark.Service
import spark.template.freemarker.FreeMarkerEngine
import java.util.HashMap

object SparkViewResults {

    @JvmStatic
    fun main(args: Array<String>) {
        val freeMarkerEngine = SparkUpload.initFreemarker(this::class.java)

        val http = Service.ignite().port(args.getOrNull(0)?.toInt()?:6789)
        http.staticFileLocation("/spark")

        val proxy = setConnection(args.getOrNull(1) ?: "localhost:10021")

        http.get("/"
        ) { req, _ ->

            val vaultData = proxy.vaultQuery(StateContract.MarkState::class.java)

            val model = HashMap<String, Any>()
            val dataArray = vaultData.states.groupBy ({it.state.data.hash})


            var counter = 1
            val list = mutableListOf<Triple<Int, String, Double>>()

            for((hash, markStates) in dataArray)
            {
                if (markStates.size == 3) {
                    list.add(Triple(counter, markStates[0].state.data.name,
                                  markStates.sumBy ({ it.state.data.mark })/3.0))

                    counter ++


                }
            }


            //val arr = vaultData.states.groupBy ({it.state.data.hash}, {it.state.data.description})

            model["pairs"] = list

            freeMarkerEngine.render(ModelAndView(model, "SparkViewResults.ftl"))
        }
    }
}